package com.intrepiditee;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticMap {

    // Both are inclusive
    static double minGeneticDistance;
    static double maxGeneticDistance;

    static Scanner sc;

    static TreeMap<Double, Integer> geneticToPhysicalDistance;

    static void initialize(String filename) {
        sc = Utils.getScanner(filename);
        geneticToPhysicalDistance = new TreeMap<>();
    }

    static void parse() {
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
    static List<Integer> getRecombinationIndices() {
        int numIndices = getPoisson(Configs.genomeLength / 50000000.0);
        List<Integer> indices = new ArrayList<>(numIndices);
        if (numIndices == 0) {
            return indices;
        }

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
            origin = bound;
            bound += range;

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

}
