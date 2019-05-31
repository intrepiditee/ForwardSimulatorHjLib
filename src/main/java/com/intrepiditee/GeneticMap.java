package com.intrepiditee;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticMap {

    // Both are inclusive
    static double minGeneticDistance;
    static double maxGeneticDistance;

    static Scanner sc;

    static TreeMap<Double, Integer> geneticToPhysicalDistance;

    public static void initialize(String filename) {
        sc = Utils.getScanner(filename);
        geneticToPhysicalDistance = new TreeMap<>();
    }

    public static void parse() {
        sc.nextLine();

        double geneticDistance = 0.0;
        int physicalDistance;

        boolean isFirst = true;

        while (sc.hasNext()) {
            geneticDistance = sc.nextDouble();
            physicalDistance = sc.nextInt();

            if (isFirst) {
                minGeneticDistance = geneticDistance;
                isFirst = false;
            }

            // Map is cumulative
            geneticToPhysicalDistance.put(geneticDistance, physicalDistance);
            sc.next();
        }

        maxGeneticDistance = geneticDistance;

        sc.close();
    }

    /**
     *
     * @return a List of inclusive indices where recombinations should begin
     */
    public static List<Integer> getRecombinationIndices() {
        double origin = minGeneticDistance;
        double bound = maxGeneticDistance + 1.2 * (maxGeneticDistance - origin);

        List<Integer> indices = new ArrayList<>();

        while (true) {
            double prob = ThreadLocalRandom.current().nextDouble(origin, bound);
            Map.Entry<Double, Integer> entry = geneticToPhysicalDistance.floorEntry(prob);
            if (entry == null) {
                break;
            }

            indices.add(entry.getValue());

            origin = prob;
        }

        return indices;
    }

}
