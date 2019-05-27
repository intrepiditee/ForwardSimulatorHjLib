package com.intrepiditee;

import java.util.*;

public class MapReader {
    Scanner sc;

    List<Double> probabilities;
    List<Integer> indices;

    static MapReader geneticMap;

    static MapReader initialize(String filename) {
        geneticMap = new MapReader(filename);
        return geneticMap;
    }

    private MapReader(String filename) {
        sc = Utils.getScanner(filename);
        probabilities = new ArrayList<>();
        indices = new ArrayList<>();
    }

    public MapReader parse() {
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

        return this;
    }
}
