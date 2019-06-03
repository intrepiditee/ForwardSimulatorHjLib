package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.intrepiditee.Individual.mergeOneGenome;
import static com.intrepiditee.Segment.make;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndividualTest {

    @Test
    public void testMergeOneGenome() {
        List<Segment> segments = Arrays.asList(make(0, 1));
        segments = mergeOneGenome(segments);
        List<Segment> expected = Arrays.asList(make(0, 1));
        assertEquals(expected, segments);

        segments = Arrays.asList(make(0, 1), make(1, 2), make(2, 4));
        segments = mergeOneGenome(segments);
        expected = Arrays.asList(make(0, 4));
        assertEquals(expected, segments);

        segments = Arrays.asList(make(0, 5), make(4, 10));
        segments = mergeOneGenome(segments);
        expected = Arrays.asList(make(0, 10));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1), make(1, 2), make(2, 5), make(4, 10)
        );
        segments = mergeOneGenome(segments);
        expected = Arrays.asList(make(0, 10));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1), make(2, 5), make(4, 10),
            make(8, 14), make(15, 16)
        );
        segments = mergeOneGenome(segments);
        expected = Arrays.asList(make(0, 1), make(2, 14), make(15, 16));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1), make(2, 5), make(4, 10), make(8, 14),
            make(15, 16), make(17, 20), make(18, 21)
        );
        segments = mergeOneGenome(segments);
        expected = Arrays.asList(
            make(0, 1), make(2, 14), make(15, 16), make(17, 21)
        );
        assertEquals(expected, segments);
    }

    @Test
    void testRecombineOneGenome() {

    }

}
