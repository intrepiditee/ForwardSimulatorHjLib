package com.intrepiditee;

import edu.rice.hj.runtime.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static edu.rice.hj.runtime.util.Pair.factory;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PedigreeGraphTest {

    @Test
    void test() {
        PedigreeGraph.pathPrefix = "mualtor/ForwardSimulatorHjLib/target/out/";
        PedigreeGraph.main(new String[]{"--pedigree", "0-3", "4", "3"});

        Map<Pair<Integer, Integer>, Integer> degrees = new HashMap<>();
        Scanner sc = Utils.getScannerFromGZip("mualtor/ForwardSimulatorHjLib/target/out/degrees.txt.gz");
        while (sc.hasNextLine()) {
            String[] ids = sc.nextLine().split("\t");
            int id1 = Integer.parseInt(ids[0]);
            int id2 = Integer.parseInt(ids[1]);
            int degree = Integer.parseInt(ids[2]);
            degrees.put(factory(id1, id2), degree);
            degrees.put(factory(id2, id1), degree);
        }

        assertEquals(1, degrees.get(factory(0, 5)));
        assertEquals(1, degrees.get(factory(10, 11)));
        assertEquals(1, degrees.get(factory(18, 13)));
        assertEquals(1, degrees.get(factory(8, 9)));
        assertEquals(1, degrees.get(factory(8, 7)));
        assertEquals(1, degrees.get(factory(7, 9)));
        assertEquals(1, degrees.get(factory(9, 14)));
        assertEquals(1, degrees.get(factory(5, 10)));

        assertEquals(2, degrees.get(factory(0, 10)));
        assertEquals(2, degrees.get(factory(3, 13)));
        assertEquals(2, degrees.get(factory(9, 13)));
        assertEquals(2, degrees.get(factory(15, 16)));
        assertEquals(2, degrees.get(factory(16, 17)));
        assertEquals(2, degrees.get(factory(9, 12)));
        assertEquals(2, degrees.get(factory(15, 17)));
        assertEquals(2, degrees.get(factory(9, 12)));

        assertEquals(3, degrees.get(factory(0, 15)));
        assertEquals(3, degrees.get(factory(9, 18)));
        assertEquals(3, degrees.get(factory(8, 11)));
        assertEquals(3, degrees.get(factory(9, 17)));
        assertEquals(3, degrees.get(factory(0, 15)));
        assertEquals(3, degrees.get(factory(4, 16)));
    }
}
