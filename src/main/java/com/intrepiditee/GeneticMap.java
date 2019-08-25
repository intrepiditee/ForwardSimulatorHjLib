package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.intrepiditee.Configs.*;


class GeneticMap {

    // Both are inclusive
    private double minGeneticDistance;
    private double maxGeneticDistance;

    private int minPhysicalDistance;
    private int maxPhysicalDistance;

    private final Scanner sc;

    private TreeMap<Double, Integer> geneticToPhysicalDistance;
    private TreeMap<Integer, Double> physicalToGeneticDistance;

    static Map<Integer, Integer> chromosomeNumberToPhysicalLength;

    static Map<Integer, GeneticMap> chromosomeNumberToGeneticMap;

    static String pathPrefix = "decode_maps_hg19_filtered/";
    private static final String filenamePrefix = "decode_";
    private static final String filenamePostfix = "_hg19.txt";

    private static final String chromosomeLengthFilename = "hg19_chromosome_lengths.txt";

    static final byte GENETIC_TO_PHYSICAL = 3;
    private static final byte PHYSICAL_TO_GENETIC = 4;


    // Generate genetic mapping files for rapid from sites covered by ukb
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println();

        makeFromChromosomeNumbers(getChromosomeNumbers());

        for (int c = 1; c <= numChromosomes; c++) {
            String sitesFilename = VCFParser.pathPrefix + "sites.chr" + c;
            ObjectInputStream in = Utils.getBufferedObjectInputStream(sitesFilename);

            int[] sites = null;
            sites = (int[]) in.readUnshared();

            GeneticMap map = chromosomeNumberToGeneticMap.get(c);
            map.parseDirection(PHYSICAL_TO_GENETIC);

            String outFilename = "map/" + "chr" + c + ".txt";
            BufferedWriter w = Utils.getBufferedWriter(outFilename);

            for (int i = 0; i < sites.length; i++) {
                StringBuilder s = new StringBuilder();
                s.append(i);
                s.append("\t");

                int siteIndex = sites[i] - 1;
                if (siteIndex < map.minPhysicalDistance) {
                    s.append(map.minGeneticDistance);
                } else if (siteIndex > map.maxPhysicalDistance) {
                    s.append(map.maxGeneticDistance);
                } else {
                    // Both floorEntry and ceilEntry will not be null because siteIndex is larger than
                    // minPhysicalDistance and smaller than maxGeneticDistance.
                    Map.Entry<Integer, Double> floorEntry = map.physicalToGeneticDistance.floorEntry(siteIndex);
                    Map.Entry<Integer, Double> ceilEntry = map.physicalToGeneticDistance.ceilingEntry(siteIndex);

                    if (ceilEntry.equals(floorEntry)) {
                        s.append(floorEntry.getValue());
                    } else {
                        s.append(
                            interpolate(
                                floorEntry.getKey(), floorEntry.getValue(),
                                ceilEntry.getKey(), ceilEntry.getValue(),
                                siteIndex
                            )
                        );
                    }
                }

                s.append("\n");
                w.write(s.toString());
            }

            w.close();
            System.out.println(outFilename + " written");


            in.close();

        } // end of all chromosomes

    }


    static int[] getChromosomeNumbers() {
        int[] nums = new int[numChromosomes];
        for (int i = 0; i < numChromosomes; i++) {
            nums[i] = i + 1;
        }
        return nums;
    }


    static void parseLengths() {
        chromosomeNumberToPhysicalLength = new HashMap<>();

        String filename = pathPrefix + chromosomeLengthFilename;
        Scanner sc = Utils.getScanner(filename);
        while (sc.hasNext()) {
            String line = sc.nextLine();
            String[] split = line.split("\t");
            int chromosomeNumber = Integer.parseInt(split[0].substring("chr".length()));
            chromosomeNumberToPhysicalLength.put(
                chromosomeNumber,
                Integer.parseInt(split[1])
            );
        }
        sc.close();
    }

    static void makeFromChromosomeNumbers(int... chromosomeNumbers) {
        if (chromosomeNumberToGeneticMap == null) {
            chromosomeNumberToGeneticMap = new HashMap<>();
        }

        for (int c : chromosomeNumbers) {
            chromosomeNumberToGeneticMap.putIfAbsent(c, make(c));
        }
    }

    private static GeneticMap make(int chromosomeNumber) {
        return makeFromFilename(filenamePrefix + chromosomeNumber + filenamePostfix);
    }

    private static GeneticMap makeFromFilename(String filename) {
        return new GeneticMap(filename);
    }

    private GeneticMap(String filename) {
        sc = Utils.getScanner(pathPrefix + filename);
    }


    static void parseAllMaps(byte direction) {
        for (GeneticMap m : chromosomeNumberToGeneticMap.values()) {
            m.parseDirection(direction);
        }
    }

    @SuppressWarnings("unchecked")
    private GeneticMap parseDirection(byte direction) {
        if (direction == GENETIC_TO_PHYSICAL && geneticToPhysicalDistance != null) {
            return this;
        }
        if (direction == PHYSICAL_TO_GENETIC && physicalToGeneticDistance != null) {
            return this;
        }
        if (direction == BOTH && geneticToPhysicalDistance != null && physicalToGeneticDistance != null) {
            return this;
        }

        TreeMap mapGeneticToPhysical = new TreeMap<>();
        TreeMap mapPhysicalToGenetic = new TreeMap<>();

        double geneticDistance = 0.0;
        int physicalDistance = 0;
        boolean isFirst = true;

        while (sc.hasNextLine()) {
            String[] fields = sc.nextLine().split("\t");
            // Genetic map is 1 based
            physicalDistance = Integer.parseInt(fields[1]) - 1;
            geneticDistance = Double.parseDouble(fields[4]);

            if (direction == BOTH || direction == GENETIC_TO_PHYSICAL) {
                mapGeneticToPhysical.put(geneticDistance, physicalDistance);
            }
            if (direction == BOTH || direction == PHYSICAL_TO_GENETIC) {
                mapPhysicalToGenetic.put(physicalDistance, geneticDistance);
            }

            if (isFirst) {
                minGeneticDistance = geneticDistance;
                minPhysicalDistance = physicalDistance;
                isFirst = false;
            }
        }
        sc.close();

        maxGeneticDistance = geneticDistance;
        maxPhysicalDistance = physicalDistance;
        if (direction == BOTH || direction == GENETIC_TO_PHYSICAL) {
            geneticToPhysicalDistance = mapGeneticToPhysical;
        }
        if (direction == BOTH || direction == PHYSICAL_TO_GENETIC) {
            physicalToGeneticDistance = mapPhysicalToGenetic;
        }

        return this;
    }

    /**
     *
     * @return a List of inclusive indices where recombinations should begin
     */
    List<Integer> getRecombinationIndices(int chromosomeNumber) {
        int chromosomeLength = chromosomeNumberToPhysicalLength.get(chromosomeNumber);
        int numIndices = getPoisson(
            // 1 cM = 1 Mbp
            // 50 cM = 1 recombination
            chromosomeLength / 50000000.0
        );
        List<Integer> indices = new ArrayList<>(numIndices);
        if (numIndices == 0) {
            indices.add(chromosomeLength - 1);
            return indices;
        }

        for (int i = 0; i < numIndices; i++) {
            double prob = ThreadLocalRandom.current().nextDouble(minGeneticDistance, maxGeneticDistance);
            // ceilEntry can never be null because prob cannot be larger than maxGeneticDistance
            Map.Entry<Double, Integer> ceilEntry = geneticToPhysicalDistance.ceilingEntry(prob);
            // floorEntry can never be null because prob cannot be smaller than minGeneticDistance
            Map.Entry<Double, Integer> floorEntry = geneticToPhysicalDistance.floorEntry(prob);
            int index = (int) interpolate(
                floorEntry.getKey(), floorEntry.getValue(),
                ceilEntry.getKey(), ceilEntry.getValue(),
                prob
            );
            indices.add(index);
        }
        Collections.sort(indices);

        if (indices.get(indices.size() - 1) < chromosomeLength - 1) {
            // Add end of chromosome as a recombination index to allow all segments be added
            indices.add(chromosomeLength - 1);
        }

        return indices;
    }


    // https://stackoverflow.com/questions/1241555/algorithm-to-generate-poisson-and-binomial-random-numbers
    static int getPoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= Math.random();
        } while (p > L);

        return k - 1;
    }

    private static double interpolate(double x1, double y1, double x2, double y2, double x) {
        double intercept = (y1 * x2 - x1 * y2) / (x2 - x1);
        double slope = (y1 - intercept) / x1;
        return slope * x + intercept;
    }


    double getGeneticLengthBetween(int start, int end) {
        double startGenetic = physicalToGeneticPosition(start);
        double endGenetic = physicalToGeneticPosition(end - 1);

        return endGenetic - startGenetic;
    }


    double physicalToGeneticPosition(int physicalPosition) {
        double geneticPosition;
        if (physicalPosition < minPhysicalDistance) {
            geneticPosition = minGeneticDistance;
        } else if (physicalPosition > maxPhysicalDistance) {
            geneticPosition = maxGeneticDistance;
        } else {
            Map.Entry<Integer, Double> floorEntry = physicalToGeneticDistance.floorEntry(physicalPosition);
            Map.Entry<Integer, Double> ceilEntry = physicalToGeneticDistance.ceilingEntry(physicalPosition);

            if (ceilEntry.equals(floorEntry)) {
                geneticPosition = floorEntry.getValue();
            } else {
                geneticPosition = interpolate(
                    floorEntry.getKey(), floorEntry.getValue(),
                    ceilEntry.getKey(), ceilEntry.getValue(),
                    physicalPosition
                );
            }
        }
        return geneticPosition;
    }

}
