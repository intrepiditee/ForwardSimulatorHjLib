package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.*;

import static com.intrepiditee.Configs.*;
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.forallChunked;

public class Segment implements Serializable, Comparable<Segment> {

    final int start;
    final int end;
    final int founderID;
    final byte whichChromosome;

    final static long serialVersionUID = 5320532892517112834L;

    private static String pathPrefx = "ibd/";

    public static void main(String[] args){
        if (args.length != 4) {
            Utils.printUsage();
            return;
        }

        System.out.println();

        startGeneration = Integer.parseInt(args[1]);
        endGeneration = Integer.parseInt(args[2]);
        numThreads = Integer.parseInt(args[3]);

        HjSystemProperty.setSystemProperty(HjSystemProperty.numWorkers, numThreads);

        launchHabaneroApp(() -> {
            forallChunked(1, numChromosomes, c -> {
                try {
                    writeIBDForChromosome(c);
                    System.out.println("Chromosome " + c + ": IBDs written");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

            });
        });
    }

    private static void writeIBDForChromosome(int chromosomeNumber) throws IOException {
        Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair = VCFGenerator.readChromosomesFromChromosome(chromosomeNumber);
        writeIBDForChromosome(idToChromosomesPair, chromosomeNumber);
    }

    private static void writeIBDForChromosome(
        Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair, int chromosomeNumber) throws IOException {

        Set<Integer> ids = idToChromosomesPair.keySet();
        int minID = Integer.MAX_VALUE;
        int maxID = Integer.MIN_VALUE;
        for (int id : ids) {
            minID = Math.min(minID, id);
            maxID = Math.max(maxID, id);
        }

        byte[] sexes = new byte[]{FEMALE, MALE};

        BufferedWriter w = Utils.getBufferedGZipWriter(pathPrefx + "ibd_chr" + chromosomeNumber + ".txt.gz");
        writeHeader(w);

        for (int id1 = minID; id1 <= maxID; id1++) {
            for (int id2 = id1 + 1; id2 <= maxID; id2++) {
                Map<Byte, List<Segment>> chromosomesPair1 = idToChromosomesPair.get(id1);
                Map<Byte, List<Segment>> chromosomesPair2 = idToChromosomesPair.get(id2);

                List<String> outs = new ArrayList<>();
                for (byte sex1 : sexes) {
                    for (byte sex2 : sexes) {
                        List<Segment> ibds = computeIBDsFromTwoChromosomes(chromosomesPair1.get(sex1), chromosomesPair2.get(sex2));
                        outs.addAll(getIBDOutputStrings(chromosomeNumber, id1, id2, sex1, sex2, ibds));
                    }
                }

                for (String out : outs) {
                    w.write(out);
                }
            }
        }

        w.close();
    }


    private static void writeHeader(Writer w) throws IOException {
        w.write(String.join(
            "\t",
            "Chromosome",
            "individual1", "individual2",
            "haplotype1", "haplotype2",
            "starting_position", "ending_position",
            "starting_site", "ending_site",
            "genetic_length",
            "physical_length",
            "founder_id\n"
            )
        );
    }

    static void writeIBDForGenerations(
        List<Generation> generations) throws SuspendableException {

        forallChunked(1, numChromosomes, c -> {
            Map<Integer, Map<Byte, List<Segment>>> idToChromosomePairs =
                getIdToChromosomePairsFromGenerations(generations, c);

            try {
                writeIBDForChromosome(idToChromosomePairs, c);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            System.out.println("Chromosome " + c + " IBDs written");
        });
    }

    private static Map<Integer, Map<Byte, List<Segment>>>
    getIdToChromosomePairsFromGenerations(List<Generation> generations, int chromosomeNumber) {

        Map<Integer, Map<Byte, List<Segment>>> idToChromosomePairs = new HashMap<>(
            generations.size() * generations.get(0).males.size() * 2
        );
        for (Generation generation : generations) {
            for (Individual male : generation.males) {
                idToChromosomePairs.put(male.id, male.genome.get(chromosomeNumber));
            }

            for (Individual female : generation.females) {
                idToChromosomePairs.put(female.id, female.genome.get(chromosomeNumber));
            }
        }
        return idToChromosomePairs;
    }

    private static List<String> getIBDOutputStrings(
        int chromosomeNumber, int id1, int id2, byte sex1, byte sex2, List<Segment> ibds) {

        List<String> outs = new ArrayList<>();
        for (Segment ibd : ibds) {
            double startGenetic = GeneticMap.chromosomeNumberToGeneticMap
                .get(chromosomeNumber)
                .physicalToGeneticPosition(ibd.start);
            double endGenetic = GeneticMap.chromosomeNumberToGeneticMap
                .get(chromosomeNumber)
                .physicalToGeneticPosition(ibd.end - 1);

            outs.add(
                String.join(
                    "\t",
                    String.valueOf(chromosomeNumber),
                    String.valueOf(id1), String.valueOf(id2),
                    sexToString(sex1), sexToString(sex2),
                    String.format("%.7s", startGenetic),
                    String.format("%.7s", endGenetic),
                    String.valueOf(ibd.start + 1), String.valueOf(ibd.end),

                    // Genetic Length
                    String.format("%.7s", endGenetic - startGenetic),
                    // Physical Length in Mbp
                    String.format("%.7s", (ibd.end - ibd.start) / 1000000.0),

                    String.valueOf(ibd.founderID)
                ) + "\n"
            );
        }
        return outs;
    }

    private static String sexToString(byte sex) {
        return sex == FEMALE ? "0" : "1";
    }


    static List<Segment> computeIBDsFromTwoChromosomes(
        List<Segment> oneSegmentList, List<Segment> anotherSegmentList) {

        List<Segment> ibds = new ArrayList<>();

        int oneIndex = 0;
        int anotherIndex = 0;
        while (oneIndex < oneSegmentList.size()
            && anotherIndex < anotherSegmentList.size()) {

            Segment oneSegment = oneSegmentList.get(oneIndex);
            Segment anotherSegment = anotherSegmentList.get(anotherIndex);
            if (Segment.intersect(oneSegment, anotherSegment) &&
                oneSegment.founderID == anotherSegment.founderID) {

                ibds.add(intersection(oneSegment, anotherSegment));
                oneIndex++;
                anotherIndex++;
            } else if (oneSegment.end > anotherSegment.end) {
                anotherIndex++;
            } else if (oneSegment.end < anotherSegment.end) {
                oneIndex++;
            } else {
                oneIndex++;
                anotherIndex++;
            }
        }
        return ibds;
    }

    static Segment make(int start, int end, int founderID, byte whichChromosome) {
        assert start < end;
        return new Segment(start, end, founderID, whichChromosome);
    }

    private Segment(int start, int end, int founderID, byte whichChromosome) {
        this.start = start;
        this.end = end;
        this.founderID = founderID;
        this.whichChromosome = whichChromosome;
    }

    boolean contains(int index) {
        return start <= index && index < end;
    }

    // Merge two intersecting Segments. canMerge must first be called.
    static Segment merge(Segment seg1, Segment seg2) {
        return make(
            seg1.start < seg2.start ? seg1.start : seg2.start,
            seg1.end > seg2.end ? seg1.end : seg2.end,
            seg1.founderID,
            seg1.whichChromosome
        );
    }

    static boolean intersect(Segment seg1, Segment seg2) {
        return Math.max(seg1.start, seg2.start) < Math.min(seg1.end, seg2.end);
    }

    static Segment intersection(Segment seg1, Segment seg2) {
        return make(
            Math.max(seg1.start, seg2.start), Math.min(seg1.end, seg2.end),
            seg1.founderID,
            (byte) -1
        );
    }

    static boolean canMerge(Segment seg1, Segment seg2) {
        if (seg1.founderID != seg2.founderID || seg1.whichChromosome != seg2.whichChromosome) {
            return false;
        }

        return intersect(seg1, seg2) ||
            Math.max(seg1.start, seg2.start) == Math.min(seg1.end, seg2.end);
    }

    List<Segment> split(List<Integer> excludingIndices) {
        int prevEnd = start;
        List<Segment> split = new ArrayList<>(excludingIndices.size() + 1);
        for (int i : excludingIndices) {
            if (prevEnd < i) {
                split.add(make(prevEnd, i, founderID, whichChromosome));
            }
            prevEnd = i + 1;
        }
        // If last i is end - 1, prevEnd is end
        if (prevEnd < end) {
            split.add(make(prevEnd, end, founderID, whichChromosome));
        }

        return split;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Segment)) {
            return false;
        }

        Segment o = (Segment) other;
        return o.start == start &&
            o.end == end &&
            o.founderID == founderID &&
            o.whichChromosome == whichChromosome;

    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + ", " + founderID + ", " + whichChromosome + "]";
    }


    static int[] segmentsToArray(List<Segment> segments) {
        int[] indices = new int[segments.size() * 2];
        int j = 0;
        for (Segment seg : segments) {
            indices[j] = seg.start;
            indices[j + 1] = seg.end;
            j += 2;
        }
        return indices;
    }

    static int[] getFounderArray(List<Segment> segments) {
        int[] founderIDs = new int[segments.size()];
        int j = 0;
        for (Segment seg : segments) {
            founderIDs[j] = seg.founderID;
        }
        return founderIDs;
    }

    static String segmentsToString(List<Segment> segs) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < segs.size(); i++) {
            Segment seg = segs.get(i);
            s.append(seg.start);
            s.append(",");
            s.append(seg.end);
            s.append(",");
            s.append(seg.founderID);
            s.append(",");
            s.append(seg.whichChromosome);
            if (i != segs.size() - 1) {
                s.append(" ");
            }
        }
        return s.toString();
    }

    // Two segments are compareTo equal if they intersect.
    // Because lists of segments are always sorted and non-intersecting,
    // two compareTo equal segments must be that one contains the other.
    @Override
    public int compareTo(Segment other) {
        if (Segment.intersect(this, other)) {
            return 0;
        } else if (this.end <= other.start) {
            return -1;
        } else {
            return 1;
        }
    }


}
