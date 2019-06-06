package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.intrepiditee.Configs.BOTH;
import static com.intrepiditee.Configs.FEMALE;
import static com.intrepiditee.Configs.MALE;


public class GeneticMap {

    // Both are inclusive
    double minGeneticDistance;
    double maxGeneticDistance;

    int minPhysicalDistance;
    int maxPhysicalDistance;

    Scanner sc;

    TreeMap<Double, Integer> geneticToPhysicalDistance;
    TreeMap<Integer, Double> physicalToGeneticDistance;

    static Map<Integer, Integer> chromosomeNumberToPhysicalLength;

    static Map<Integer, GeneticMap> chromosomeNumberToGeneticMap;

    static String pathPrefix = "decode_maps_hg19_filtered/";
    static String filenamePrefix = "decode_";
    static String filenamePostfix = "_hg19.txt";

    static String chromosomeLengthFilename = "hg19_chromosome_lengths.txt";

    static byte GENETIC_TO_PHYSICAL = 3;
    static byte PHYSICAL_TO_GENETIC = 4;

    static int numChromosomes = 22;


    // Generate genetic mapping files for rapid from indices of snps covered by ukb
    public static void main(String[] args) {
        System.out.println();

        makeFromChromosomeNumbers(getChromosomeNumbers());

        ObjectInputStream in = Utils.getBufferedObjectInputStream("variantSiteIndices");

        int[] indices = null;
        try {
            in.readInt();
            in.readInt();
            indices = (int[]) in.readUnshared();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            for (Map.Entry<Integer, GeneticMap> e : chromosomeNumberToGeneticMap.entrySet()) {
                for (byte sex : new byte[]{MALE, FEMALE}) {
                    GeneticMap map = e.getValue().parseDirection(PHYSICAL_TO_GENETIC);

                    int chromosomeNumber = e.getKey();
                    StringBuilder sb = new StringBuilder();
                    sb.append("map/");
                    sb.append(sex == MALE ? "male" : "female");
                    sb.append(".chr");
                    sb.append(chromosomeNumber);
                    sb.append(".txt");
                    String filename = sb.toString();

                    BufferedWriter w = Utils.getBufferedWriter(filename);

                    for (int i = 0; i < indices.length; i++) {
                        StringBuilder s = new StringBuilder();
                        s.append(i);
                        s.append("\t");

                        int index = indices[i];
                        if (index < map.minPhysicalDistance) {
                            s.append(map.minGeneticDistance);
                        } else if (index > map.maxPhysicalDistance) {
                            s.append(map.maxGeneticDistance);
                        } else {
                            // Both will not be null
                            Map.Entry<Integer, Double> floorEntry = map.physicalToGeneticDistance.floorEntry(index);
                            Map.Entry<Integer, Double> ceilEntry = map.physicalToGeneticDistance.ceilingEntry(index);

                            if (ceilEntry.equals(floorEntry)) {
                                s.append(floorEntry.getValue());
                            } else {
                                s.append(
                                    interpolate(
                                        floorEntry.getKey(), floorEntry.getValue(),
                                        ceilEntry.getKey(), ceilEntry.getValue(),
                                        index
                                    )
                                );
                            }
                        }

                        s.append("\n");
                        w.write(s.toString());
                    }

                    w.close();
                    System.out.println(filename + " written");

                } // End of male and female
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

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

    static Map<Integer, GeneticMap> makeFromChromosomeNumbers(int... chromosomeNumbers) {
        if (chromosomeNumberToGeneticMap != null) {
            return chromosomeNumberToGeneticMap;
        }

        chromosomeNumberToGeneticMap = new HashMap<>();
        for (int c : chromosomeNumbers) {
            chromosomeNumberToGeneticMap.put(c, make(c));
        }
        return chromosomeNumberToGeneticMap;

    }

    static GeneticMap make(int chromosomeNumber) {
        return makeFromFilename(filenamePrefix + chromosomeNumber + filenamePostfix);
    }

    static GeneticMap makeFromFilename(String filename) {
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
    GeneticMap parseDirection(byte direction) {
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
            physicalDistance = Integer.parseInt(fields[1]);
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
            return indices;
        }

        for (int i = 0; i < numIndices; i++) {
            while (true) {
                double prob = ThreadLocalRandom.current().nextDouble(minGeneticDistance, maxGeneticDistance);
                Map.Entry<Double, Integer> entry = geneticToPhysicalDistance.ceilingEntry(prob);
                if (entry == null) {
                    continue;
                }
                indices.add(entry.getValue());
                break;
            }

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

    static double interpolate(double x1, double y1, double x2, double y2, double x) {
        double intercept = (y1 * x2 - x1 * y2) / (x2 - x1);
        double slope = (y1 - intercept) / x1;
        return slope * x + intercept;
    }

}
