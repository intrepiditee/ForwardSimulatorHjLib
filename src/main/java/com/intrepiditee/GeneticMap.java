package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class GeneticMap {

    // Both are inclusive
    double minGeneticDistance;
    double maxGeneticDistance;

    int minPhysicalDistance;
    int maxPhysicalDistance;

    Scanner sc;

    TreeMap<Double, Integer> geneticToPhysicalDistance;
    TreeMap<Integer, Double> physicalToGeneticDistance;

    static Map<Integer, Double> chromosomeNumberToGeneticLength;
    static Map<Integer, Integer> chromosomeNumberToPhysicalLength;

    static Map<Integer, Map<Byte, GeneticMap>> chromosomeNumberToSexToMap;

    static String prefix = "decode_map/";
    static String summaryFilename = "male.gmap.summary";
    static String malePrefix = "male.gmap.cumulative.chr";
    static String femalePrefix = "female.gmap.cumulative.chr";

    static byte MALE = 1;
    static byte FEMALE = 0;
    static byte BOTH = 2;

    static byte GENETIC_TO_PHYSICAL = 3;
    static byte PHYSICAL_TO_GENETIC = 4;


    public static void main(String[] args) {
        System.out.println();

        Map<Integer, Byte> chromosomeNumberToSex = getChromosomeNumberToSex();
        makeFromMap(chromosomeNumberToSex);

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
            for (Map.Entry<Integer, Map<Byte, GeneticMap>> e : chromosomeNumberToSexToMap.entrySet()) {
                for (byte sex : new byte[]{MALE, FEMALE}) {
                    GeneticMap map = e.getValue().get(sex).parseDirection(PHYSICAL_TO_GENETIC);

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


    static Map<Integer, Byte> getChromosomeNumberToSex() {
        Map<Integer, Byte> chromosomeNumberToSex = new HashMap<>();
        for (int i = 1; i <= 22; i++) {
            chromosomeNumberToSex.put(i, BOTH);
        }
        return chromosomeNumberToSex;
    }

    static void parseSummary() {
        if (chromosomeNumberToGeneticLength != null &&
            chromosomeNumberToPhysicalLength != null) {
            return;
        }

        chromosomeNumberToGeneticLength = new HashMap<>();
        chromosomeNumberToPhysicalLength = new HashMap<>();

        Scanner sc = Utils.getScanner(prefix + summaryFilename);
        sc.nextLine();
        while (sc.hasNext()) {
            String line = sc.nextLine();
            String[] split = line.split("\t");
            int chromosomeNumber = Integer.parseInt(split[0].substring("chr".length()));
            chromosomeNumberToGeneticLength.put(
                chromosomeNumber,
                Double.parseDouble(split[2])
            );
            chromosomeNumberToPhysicalLength.put(
                chromosomeNumber,
                Integer.parseInt(split[3])
            );
        }
    }

    static void makeFromMap(Map<Integer, Byte> chromosomeNumberToSex) {
        Map<Integer, Map<Byte, GeneticMap>> maps = new HashMap<>(chromosomeNumberToSex.size());
        for (Map.Entry<Integer, Byte> e : chromosomeNumberToSex.entrySet()) {
            int chromosomeNumber = e.getKey();
            byte sex = e.getValue();
            Map<Byte, GeneticMap> byteToMap = maps.getOrDefault(chromosomeNumber, new HashMap<>());
            if (sex == BOTH) {
                byteToMap.put(MALE, make(chromosomeNumber, MALE));
                byteToMap.put(FEMALE, make(chromosomeNumber, FEMALE));
            } else {
                byteToMap.put(sex, make(chromosomeNumber, sex));
            }
            maps.put(chromosomeNumber, byteToMap);

        }

        chromosomeNumberToSexToMap = maps;
    }

    static GeneticMap make(int chromosomeNumber, byte sex) {
        return new GeneticMap(
            sex == MALE ?
                malePrefix + chromosomeNumber :
                femalePrefix + chromosomeNumber
        );
    }

    static GeneticMap makeFromFilename(String filename) {
        return new GeneticMap(filename);
    }

    private GeneticMap(String filename) {
        sc = Utils.getScanner(prefix + filename);
    }

    GeneticMap parseBothDirections() {
        return parseDirection(BOTH);
    }

    @SuppressWarnings("unchecked")
    GeneticMap parseDirection(byte direction) {
        TreeMap mapGeneticToPhysical = new TreeMap<>();
        TreeMap mapPhysicalToGenetic = new TreeMap<>();

        double geneticDistance = 0.0;
        int physicalDistance = 0;
        boolean isFirst = true;

        sc.nextLine();
        while (sc.hasNext()) {
            geneticDistance = sc.nextDouble();
            physicalDistance = sc.nextInt();
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

            sc.next();
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

    List<Integer> getRecombinationIndices(int chromosomeNumber) {
        int numIndices = getPoisson(chromosomeNumberToGeneticLength.get(chromosomeNumber) / 50);
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


        if (indices.get(indices.size() - 1) < Configs.chromosomeLength - 1) {
            // Add end of chromosome as a recombination index to allow all segments be added
            indices.add(Configs.chromosomeLength - 1);
        }

        return indices;
    }

    /**
     *
     * @return a List of inclusive indices where recombinations should begin
     */
    List<Integer> getRecombinationIndices() {
        return getRecombinationIndices(22);
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
