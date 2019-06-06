package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.*;

import static com.intrepiditee.GeneticMap.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneticMapTest {

    @Test
    void testGetRecombinationIndices() {
        int[] testChromosomeNumbers = new int[]{1, 22};

        GeneticMap.pathPrefix = "mualtor/ForwardSimulatorHjLib/target/decode_maps_hg19_filtered/";
        GeneticMap.parseLengths();
        GeneticMap.makeFromChromosomeNumbers(testChromosomeNumbers);
        GeneticMap.parseAllMaps(GENETIC_TO_PHYSICAL);

        for (int c : testChromosomeNumbers) {
            Map<Integer, Integer> freq = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                List<Integer> indices = GeneticMap
                    .chromosomeNumberToGeneticMap
                    .get(c)
                    .getRecombinationIndices(c);
                freq.merge(indices.size(), 1, (oldCount, one) -> oldCount + one);
                List<Integer> expected = new ArrayList<>(indices);
                Collections.sort(expected);
                assertEquals(expected, indices);
            }
            System.out.print("chromosome: " + c);
            System.out.print("\n");
            System.out.println(freq);
            System.out.println();
        }

    }

    @Test
    void testGetPossion() {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            freq.merge(getPoisson(1), 1, (oldCount, one) -> oldCount + one);
        }
        System.out.println(freq);
    }
}
