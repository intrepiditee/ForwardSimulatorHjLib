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

    static String prefix = "decode_map/";
    static String summaryFilename = "male.gmap.summary";
    static String malePrefix = "male.gmap.cumulative.chr";
    static String femalePrefix = "female.gmap.cumulative.chr";
    static int numChromosomes = 22;

    static byte MALE = 1;
    static byte FEMALE = 0;
    static byte BOTH = 2;

    static byte GENETIC_TO_PHYSICAL = 3;
    static byte PHYSICAL_TO_GENETIC = 4;

    public static void main(String[] args) {
        GeneticMap map = makeFromFilename("male.gmap.cumulative.chr22.gz").parseDirection(PHYSICAL_TO_GENETIC);
        ObjectInputStream in = Utils.getBufferedObjectInputStream("variantSiteIndices");
        try {
            in.readInt();
            in.readInt();
            int[] indices = (int[]) in.readUnshared();
            BufferedWriter w = Utils.getBufferedWriter("map.chr22.gz");
            for (int index : indices) {
                StringBuilder s = new StringBuilder();
                s.append(index);
                s.append("\t");

                if (index < map.minPhysicalDistance) {
                    s.append(map.minGeneticDistance);
                } else if (index > map.maxPhysicalDistance) {
                    s.append(map.maxGeneticDistance);
                } else {
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
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }


    }


    static Map<Integer, Map<Byte, GeneticMap>> makeFromMap(Map<Integer, Byte> chromosomeNumberToSex) {
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
        return maps;
    }

    static GeneticMap make(int chromosomeNumber, byte sex) {
        return new GeneticMap(
            sex == MALE ?
                prefix + malePrefix + chromosomeNumber :
                prefix + femalePrefix + chromosomeNumber
        );
    }

    static GeneticMap makeFromFilename(String filename) {
        return new GeneticMap(filename);
    }

    private GeneticMap(String filename) {
        sc = Utils.getScanner(filename);
    }

    GeneticMap parse() {
        return parseDirection(GENETIC_TO_PHYSICAL);
    }

    @SuppressWarnings("unchecked")
    GeneticMap parseDirection(byte direction) {
        TreeMap map = new TreeMap<>();

        double geneticDistance = 0.0;
        int physicalDistance = 0;
        boolean isFirst = true;

        sc.nextLine();
        while (sc.hasNext()) {
            geneticDistance = sc.nextDouble();
            physicalDistance = sc.nextInt();
            if (direction == GENETIC_TO_PHYSICAL) {
                map.put(geneticDistance, physicalDistance);
            } else if (direction == PHYSICAL_TO_GENETIC) {
                map.put(physicalDistance, geneticDistance);
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
        if (direction == GENETIC_TO_PHYSICAL) {
            geneticToPhysicalDistance = map;
        } else if (direction == PHYSICAL_TO_GENETIC) {
            physicalToGeneticDistance = map;
        }

        return this;
    }

    List<Integer> getRecombinationIndices(int chromosomeNumber) {
        int numIndices = getPoisson(Configs.chromosomeLength / 50000000.0);
        // At least one recombination
        numIndices = Math.max(numIndices, 1);
        List<Integer> indices = new ArrayList<>(numIndices);

        double range = (maxGeneticDistance - minGeneticDistance) / (numIndices + 1);
        double origin = minGeneticDistance;
        double bound = minGeneticDistance + range;

        for (int i = 0; i < numIndices; i++) {
            double prob = ThreadLocalRandom.current().nextDouble(origin, bound);
            Map.Entry<Double, Integer> entry = geneticToPhysicalDistance.ceilingEntry(prob);
            if (entry == null) {
                break;
            }
            indices.add(entry.getValue());
            origin = prob + 1e-10;
            bound += range;

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
