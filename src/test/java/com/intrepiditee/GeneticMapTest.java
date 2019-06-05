package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.*;

import static com.intrepiditee.GeneticMap.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class GeneticMapTest {

    @Test
    void testGetRecombinationIndices() {
        GeneticMap.prefix = "";
        GeneticMap.summaryFilename = "mualtor/ForwardSimulatorHjLib/target/decode_map/" + summaryFilename;
        GeneticMap.parseSummary();
        GeneticMap m = makeFromFilename(
            "mualtor/ForwardSimulatorHjLib/target/decode_map/testGeneticMap"
        );
        m.parseDirection(GENETIC_TO_PHYSICAL);
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            List<Integer> indices = m.getRecombinationIndices();
            freq.merge(indices.size(), 1, (oldCount, one) -> oldCount + one);
            List<Integer> expected = new ArrayList<>(indices);
            Collections.sort(expected);
            assertEquals(expected, indices);
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
