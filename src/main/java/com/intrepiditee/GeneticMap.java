package com.intrepiditee;

import java.util.*;

public class GeneticMap {
    static Scanner sc;

    static List<Double> probabilities;
    static List<Integer> indices;

    public static void initialize(String filename) {
        sc = Utils.getScanner(filename);
        probabilities = new ArrayList<>();
        indices = new ArrayList<>();
    }

    public static void parse() {
        sc.nextLine();
        Double prevGeneticDistance = 0.0;
        Double geneticDistance;
        Integer physicalDistance;
        while (sc.hasNext()) {
            geneticDistance = sc.nextDouble();
            physicalDistance = sc.nextInt();

            // Map is cumulative, so have minus the previous genetic distance.
            probabilities.add(geneticDistance - prevGeneticDistance);
            indices.add(physicalDistance);

            sc.next();

            prevGeneticDistance = geneticDistance;
        }

        sc.close();
    }

}
