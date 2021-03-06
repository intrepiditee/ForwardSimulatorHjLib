package com.intrepiditee;

import edu.rice.hj.runtime.config.HjSystemProperty;

import java.io.*;
import java.util.*;

import static com.intrepiditee.Configs.*;
import static com.intrepiditee.Utils.*;
import static edu.rice.hj.Module0.*;

public class VCFGenerator {

    private static int minID = Integer.MAX_VALUE;
    private static int maxID = Integer.MIN_VALUE;

    final private static String pathPrefix = "final/";

    public static void main(String[] args) {
        System.out.println();

        if (args.length != 5 || (!args[0].equals("--generate"))) {
            Utils.printUsage();
            System.exit(-1);
        }

        generationSize = Integer.parseInt(args[1]);
        startGeneration = Integer.parseInt(args[2]);
        endGeneration = Integer.parseInt(args[3]);
        numThreads = Integer.parseInt(args[4]);

        HjSystemProperty.setSystemProperty(HjSystemProperty.numWorkers, numThreads);

        launchHabaneroApp(() -> {
            forallPhased(1, numChromosomes, c -> {
                Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair = readChromosomesFromChromosome(c);
                Map<Integer, Map<Byte, Set<Integer>>> idToMutationIndices = readMutationIndicesFromChromosome(c);
                if (c == 1) {
                    getMinMaxIDs(idToChromosomesPair);
                }

                next();

                try {
                    writeVCFForChromosome(c, idToChromosomesPair, idToMutationIndices);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            });

        });

    }

    private static String getBasesFromEncoding(byte encoding) {
        String bases = null;
        switch (encoding) {
            case 0:
                bases = "0|0";
                break;
            case 1:
                bases = "0|1";
                break;
            case 2:
                bases = "1|0";
                break;
            case 3:
                bases = "1|1";
        }
        return bases;
    }

    private static String getBasesAt(
        int site, Map<Byte, List<Segment>> chromosomePairs,
        Map<Byte, Set<Integer>> mutationIndicesPair, byte[] founderBases) {
        int siteIndex = site - 1;

        List<Segment> paternalChromosome = chromosomePairs.get(MALE);
        List<Segment> maternalChromosome = chromosomePairs.get(FEMALE);
        Segment target = Segment.make(siteIndex, site, -1, (byte) -1);

        // Determine founders of paternal and maternal chromosomes
        int paternalIndex = Collections.binarySearch(paternalChromosome, target);
        int maternalIndex = Collections.binarySearch(maternalChromosome, target);
        assert paternalIndex >= 0 && maternalIndex >= 0;

        Segment paternalSegment = paternalChromosome.get(paternalIndex);
        Segment maternalSegment = maternalChromosome.get(maternalIndex);

        // Of the form "1|0". Paternal base at index 0. Maternal base at index 2.
        String paternalFounderBases = getBasesFromEncoding(founderBases[paternalSegment.founderID]);
        String maternalFounderBases = getBasesFromEncoding(founderBases[maternalSegment.founderID]);

        Set<Integer> paternalMutationIndices = mutationIndicesPair.get(MALE);
        Set<Integer> maternalMutationIndices = mutationIndicesPair.get(FEMALE);

        StringBuilder bases = new StringBuilder();
        if (paternalSegment.whichChromosome == MALE) {
            char base = paternalFounderBases.charAt(0);
            bases.append(paternalMutationIndices.contains(siteIndex) ? revertBase(base) : base);
        } else {
            char base = paternalFounderBases.charAt(2);
            bases.append(paternalMutationIndices.contains(siteIndex) ? revertBase(base) : base);
        }
        bases.append("|");
        if (maternalSegment.whichChromosome == MALE) {
            char base = maternalFounderBases.charAt(0);
            bases.append(maternalMutationIndices.contains(siteIndex) ? revertBase(base) : base);
        } else {
            char base = maternalFounderBases.charAt(2);
            bases.append(maternalMutationIndices.contains(siteIndex) ? revertBase(base) : base);
        }

        return bases.toString();
    }


    private static char revertBase(char base) {
        return base == '0' ? '1' : '0';
    }

    private static int[] readSitesFromChromosome(int chromosomeNumber) {
        String filename = VCFParser.pathPrefix + "sites.chr" + chromosomeNumber;
        return readIntArray(filename);
    }


    private static Map<Integer, Map<Byte, Set<Integer>>>readMutationIndicesFromChromosome(
        int chromosomeNumber) {

        Map<Integer, Map<Byte, Set<Integer>>> idToMutationIndicesPair = new HashMap<>();
        for (int i = startGeneration; i <= endGeneration; i++) {
            idToMutationIndicesPair.putAll(readMutationIndicesAt(i, chromosomeNumber));
        }
        return idToMutationIndicesPair;
    }


    @SuppressWarnings("unchecked")
    private static Map<Integer, Map<Byte, Set<Integer>>> readMutationIndicesAt(
        int generation, int chromosomeNumber
    ) {
        String filename = Simulator.prefix + generation + "_chr" + chromosomeNumber + "_mutations";
        ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
        Map<Integer, Map<Byte, Set<Integer>>> idToMutationIndicesPair = new HashMap<>();
        while (true) {
            try {
                int id = in.readInt();
                Map<Byte, List<Integer>> mutationIndexListsPair = (Map<Byte, List<Integer>>) in.readUnshared();
                Map<Byte, Set<Integer>> mutationIndicesPair = new HashMap<>();
                mutationIndicesPair.put(MALE, new HashSet<>(mutationIndexListsPair.get(MALE)));
                mutationIndicesPair.put(FEMALE, new HashSet<>(mutationIndexListsPair.get(FEMALE)));
                idToMutationIndicesPair.put(id, mutationIndicesPair);
            } catch (EOFException e) {
                break;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return idToMutationIndicesPair;

    }

    static Map<Integer, Map<Byte, List<Segment>>> readChromosomesFromChromosome(
        int chromosomeNumber) {

        Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair = new HashMap<>();
        for (int i = startGeneration; i <= endGeneration; i++) {
            idToChromosomesPair.putAll(readChromosomesAt(i, chromosomeNumber));
        }
        return idToChromosomesPair;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Map<Byte, List<Segment>>> readChromosomesAt(
        int generation, int chromosomeNumber) {

        String filename = Simulator.prefix + generation + "_chr" + chromosomeNumber;
        ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
        Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair = new HashMap<>();
        while (true) {
            try {
                int id = in.readInt();
                Map<Byte, List<Segment>> chromosomesPair = (Map<Byte, List<Segment>>) in.readUnshared();
                idToChromosomesPair.put(id, chromosomesPair);
            } catch (EOFException e) {
                break;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return idToChromosomesPair;
    }


    private static void getMinMaxIDs(Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair) {
        Set<Integer> ids = idToChromosomesPair.keySet();
        for (int id : ids) {
            minID = Math.min(minID, id);
            maxID = Math.max(maxID, id);
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeVCFForChromosome(
        int chromosomeNumber, Map<Integer, Map<Byte, List<Segment>>> idToChromosomesPair,
        Map<Integer, Map<Byte, Set<Integer>>> idToMutationIndices)
        throws IOException, ClassNotFoundException {

        String filename = pathPrefix + "chr" + chromosomeNumber + ".vcf.gz";
        BufferedWriter w = getBufferedGZipWriter(filename);
        ObjectInputStream basesIn = getBufferedObjectInputStream(
            VCFParser.pathPrefix + "bases.chr" + chromosomeNumber
        );

        int[] sites = readSitesFromChromosome(chromosomeNumber);

        int count = 0;

        for (int site : sites) {
            if (site == sites[0]) {
                // Write meta information
                w.write("##fileformat=VCFv4.0");
                w.write("\n");

                // Write header line before writing the first site
                w.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
                for (int id = minID; id <= maxID; id++) {
                    w.write("\t" + id);
                }
                w.write("\n");
            }

            // Should never encounter an EOFException because iterating over sites.
            // Number of sites equals number of byte array bases.
            byte[] founderBases = (byte[]) basesIn.readUnshared();

            StringBuilder record = new StringBuilder();
            record.append(chromosomeNumber)
                .append("\t")
                .append(site)
                .append("\trs")
                .append(site)
                .append("\tA\tC\t.\t.\t.\tGT");
            for (int id = minID; id <= maxID; id++) {
                Map<Byte, List<Segment>> chromosomesPair = idToChromosomesPair.get(id);
                Map<Byte, Set<Integer>> mutationIndicesPair = idToMutationIndices.get(id);
                String bases = getBasesAt(site, chromosomesPair, mutationIndicesPair, founderBases);
                record.append("\t");
                record.append(bases);
            }
            record.append("\n");
            w.write(record.toString());

            count++;
            if (count % 1000 == 0) {
                System.out.println(
                    "Chromosome " + chromosomeNumber + ": " +
                        count / 1000 + "k out of " +
                        sites.length / 1000 + "k sites written"
                );
            }
        }
        w.close();


        System.out.println(filename + " written");
        System.out.println();

    }


//    private static void writeVCF() throws SuspendableException {
//        int[][][] idToChromosomes = readChromosomes();
//
//        int numSites = variantSiteIndices.length;
//        int siteBatchSize = Math.min(numSites, 10000);
//        int numBatches = (int) Math.ceil(numSites / (double) siteBatchSize);
//        int numSitesPerThread = siteBatchSize / Configs.numThreads;
//
//        String[] batchRecords = new String[siteBatchSize];
//
//        BufferedWriter out = getBufferedGZipWriter("out.vcf.gz");
//        try {
//            out.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
//            for (int i = minID; i <= maxID; i++) {
//                out.write("\t" + i);
//            }
//            out.write("\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }
//
//        boolean[] basesIfInSegment = new boolean[numSites];
//        for (int i = 0; i < numSites; i++) {
//            basesIfInSegment[i] = singletonRand.nextBoolean();
//        }
//
//        // Generate record strings in batch. In each batch, records are generated
//        // in parallel. After all records in a batch are generated, one task writes
//        // then all out. Then, all the tasks continue to the next batch.
//        forallPhased(0, Configs.numThreads - 1, (i) -> {
//            int count = 0;
//
//            for (int j = 0; j < numBatches; j++) {
//                int offset = j * siteBatchSize;
//                int start = offset + i * numSitesPerThread;
//                if (start >= numSites) {
//                    break;
//                }
//                int end = Math.min(
//                    numSites,
//                    start + (i == Configs.numThreads - 1 ?
//                        siteBatchSize - i * numSitesPerThread : numSitesPerThread)
//                );
//
//                for (int k = start; k < end; k++) {
//                    int variantSiteIndex = variantSiteIndices[k];
//                    StringBuilder s = new StringBuilder();
//                    s.append("22\t");
//                    s.append(variantSiteIndex + 1);
//                    s.append("\trs");
//                    s.append(variantSiteIndex + 1);
//                    s.append("\tA\tC\t.\tPASS\t.\tGT");
//
//                    boolean baseIfInSegment = basesIfInSegment[k];
//                    for (int ID = minID; ID <= maxID; ID++) {
//                        int idIndex = ID - minID;
//                        int[] paternalChromosome = idToChromosomes[idIndex][0];
//                        int[] maternalChromosome = idToChromosomes[idIndex][1];
//
//                        String bases = getBasesAt(
//                            paternalChromosome, maternalChromosome,
//                            variantSiteIndex, baseIfInSegment
//                        );
//
//                        s.append("\t");
//                        s.append(bases);
//                    }
//                    s.append("\n");
//
//                    batchRecords[k - offset] = s.toString();
//
//                }
//
//                // Wait for all tasks in this batch to finish
//                next();
//
//                // Task 0 writes out all records in this batch
//                if (i == 0) {
//                    for (String record : batchRecords) {
//                        // Last batch may not be full
//                        if (record == null) {
//                            break;
//                        }
//
//                        try {
//                            out.write(record);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            System.exit(-1);
//                        }
//                        count++;
//                        if (count % 1000 == 0) {
//                            String s =
//                                "Records: " +
//                                count / 1000 +
//                                "k out of " +
//                                numSites / 1000 +
//                                "k written";
//                            System.out.println(s);
//                        }
//                    }
//                }
//
//                // Other tasks wait for task 0 to finish writing
//                next();
//
//            } // End of all batches
//
//        });
//
//        try {
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }
//    }


}
