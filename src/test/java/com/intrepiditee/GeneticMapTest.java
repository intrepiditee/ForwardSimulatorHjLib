package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.*;

import static com.intrepiditee.GeneticMap.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class GeneticMapTest {

    @Test
    void testGetRecombinationIndices() {
        GeneticMap m = makeFromFilename("mualtor/ForwardSimulatorHjLib/target/testGeneticMap.gz");
        m.parse();
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            List<Integer> indices = m.getRecombinationIndices();
            freq.merge(indices.size(), 1, (oldCount, one) -> oldCount + one);
            List<Integer> expected = new ArrayList<>(indices);
            Collections.sort(expected);
            assertEquals(expected, indices);
            if (indices.size() > 0) {
                assertTrue(indices.get(0) > 0);
                assertTrue(indices.get(indices.size() - 1) < Configs.chromosomeLength);
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
