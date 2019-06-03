package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.*;

import static com.intrepiditee.GeneticMap.getPoisson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class GeneticMapTest {

    @Test
    void testGetRecombinationIndices() {
        GeneticMap.initialize("mualtor/ForwardSimulatorHjLib/target/testGeneticMap.gz");
        GeneticMap.parse();
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            List<Integer> indices = GeneticMap.getRecombinationIndices();
            freq.merge(indices.size(), 1, (oldCount, one) -> oldCount + one);
            List<Integer> expected = new ArrayList<>(indices);
            Collections.sort(expected);
            assertEquals(expected, indices);
            if (indices.size() > 0) {
                assertTrue(indices.get(0) > 0);
                assertTrue(indices.get(indices.size() - 1) < Configs.genomeLength);
            }
        }
        System.out.println(freq);

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
