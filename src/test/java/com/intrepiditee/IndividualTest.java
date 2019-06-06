package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intrepiditee.Individual.mergeOneChromosome;
import static com.intrepiditee.Individual.recombineMutationIndices;
import static com.intrepiditee.Individual.recombineOneChromosome;
import static com.intrepiditee.Segment.make;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndividualTest {

    @Test
    public void testMergeOneChromosome() {
        List<Segment> segments = Arrays.asList(
            make(0, 1, 1)
        );
        segments = mergeOneChromosome(segments);
        List<Segment> expected = Arrays.asList(
            make(0, 1, 1)
        );
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, 2),
            make(1, 2, 2),
            make(2, 4, 2));
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(make(0, 4, 2));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 5, 3),
            make(4, 10, 3)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(make(0, 10, 3));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, -1),
            make(1, 2, -1),
            make(2, 5, -1),
            make(4, 10, -1)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(make(0, 10, -1));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, 7),
            make(2, 5, 7),
            make(4, 10, 7),
            make(8, 14, 7),
            make(15, 16, 7)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(
            make(0, 1, 7),
            make(2, 14, 7),
            make(15, 16, 7)
        );
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, 8),
            make(2, 5, 8),
            make(4, 10, 8),
            make(8, 14, 8),
            make(13, 16, 9),
            make(15, 20, 0),
            make(18, 21, 0)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(
            make(0, 1, 8),
            make(2, 14, 8),
            make(13, 16, 9),
            make(15, 21, 0)
        );
        assertEquals(expected, segments);
    }

    @Test
    void testRecombineOneChromosome() {
        List<Segment> segs1 = Arrays.asList(
            make(0, 5, 0),
            make(10, 15, 1)
        );
        List<Segment> segs2 = Arrays.asList(
            make(3, 4, 2),
            make(12, 14, 3)
        );
        List<Integer> indices = new ArrayList<>(Arrays.asList(2, 6, 10, Integer.MAX_VALUE));
        List<Segment> expected = Arrays.asList(
            make(0, 2, 0),
            make(3, 4, 2),
            make(12, 14, 3)
        );
        List<Segment> recombined = recombineOneChromosome(segs1, segs2, indices);
        assertEquals(expected, recombined);

        segs1 = Arrays.asList(make(0, 10, 0));
        segs2 = Arrays.asList(make(0, 10, 0));
        indices = new ArrayList<>(Arrays.asList(0, 4, 6, 9, 10, Integer.MAX_VALUE));
        expected = Arrays.asList(
            make(0, 4, 0),
            make(4, 6, 0),
            make(6, 9, 0),
            make(9, 10, 0)
        );
        recombined = recombineOneChromosome(segs1, segs2, indices);
        assertEquals(expected, recombined);

        segs1 = Arrays.asList(
            make(4, 5, 5),
            make(20, 40, 6),
            make(43, 60, 7),
            make(61, 70, 7)
        );
        segs2 = Arrays.asList(
            make(1, 3, 3),
            make(10, 20, 1),
            make(30, 33, 0),
            make(80, 90, 4)
        );
        indices = new ArrayList<>(Arrays.asList(0, 4, 20, 44, 68, 91, 102, Integer.MAX_VALUE));
        expected = Arrays.asList(
            make(10, 20, 1),
            make(20, 40, 6),
            make(43, 44, 7),
            make(68, 70, 7)
        );
        recombined = recombineOneChromosome(segs2, segs1, indices);
        assertEquals(expected, recombined);
    }


    @Test
    void testRecombineMutationIndices() {
        List<Integer> oneMutationIndices = Arrays.asList(10);
        List<Integer> anotherMutationIndices = Arrays.asList(20);
        List<Integer> recombinationIndices = Arrays.asList(15, Integer.MAX_VALUE);
        List<Integer> expected = Arrays.asList(10, 20);
        List<Integer> recombined = recombineMutationIndices(
            oneMutationIndices, anotherMutationIndices, recombinationIndices
        );
        assertEquals(expected, recombined);

        expected = Arrays.asList();
        recombined = recombineMutationIndices(
            anotherMutationIndices, oneMutationIndices, recombinationIndices
        );
        assertEquals(expected, recombined);

        oneMutationIndices = Arrays.asList();
        anotherMutationIndices = Arrays.asList();
        recombinationIndices = Arrays.asList(Integer.MAX_VALUE);
        expected = Arrays.asList();
        recombined = recombineMutationIndices(
            oneMutationIndices, anotherMutationIndices, recombinationIndices
        );
        assertEquals(expected, recombined);

        oneMutationIndices = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        recombinationIndices = Arrays.asList(1, 3, 4, 6, 7, 8, Integer.MAX_VALUE);
        expected = Arrays.asList(3, 6);
        recombined = recombineMutationIndices(
            oneMutationIndices, anotherMutationIndices, recombinationIndices
        );
        assertEquals(expected, recombined);


        oneMutationIndices = Arrays.asList(10, 24, 243, 625, 920, 1505, 10240, 510523);
        anotherMutationIndices = Arrays.asList(105, 115, 205, 506, 802, 999, 10424, 49522, 109999);
        recombinationIndices = Arrays.asList(
            24, 57, 92, 235, 643, 952, 1063, 3055, 9032, 105235, Integer.MAX_VALUE
        );
        expected = Arrays.asList(24, 506, 920, 999, 1505, 10240, 109999);
        recombined = recombineMutationIndices(
            anotherMutationIndices, oneMutationIndices, recombinationIndices
        );
        assertEquals(expected, recombined);
    }

}
