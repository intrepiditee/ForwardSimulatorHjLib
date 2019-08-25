package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intrepiditee.Configs.FEMALE;
import static com.intrepiditee.Configs.MALE;
import static com.intrepiditee.Individual.mergeOneChromosome;
import static com.intrepiditee.Individual.recombineMutationIndices;
import static com.intrepiditee.Individual.recombineOneChromosome;
import static com.intrepiditee.Segment.make;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndividualTest {

    @Test
    public void testMergeOneChromosome() {
        List<Segment> segments = Arrays.asList(
            make(0, 1, 1, MALE)
        );
        segments = mergeOneChromosome(segments);
        List<Segment> expected = Arrays.asList(
            make(0, 1, 1, MALE)
        );
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, 2, MALE),
            make(1, 2, 2, MALE),
            make(2, 4, 2, MALE));
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(make(0, 4, 2, MALE));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 5, 3, FEMALE),
            make(4, 10, 3, FEMALE)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(make(0, 10, 3, FEMALE));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, -1, MALE),
            make(1, 2, -1, MALE),
            make(2, 5, -1, MALE),
            make(4, 10, -1, MALE)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(make(0, 10, -1, MALE));
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, 7, MALE),
            make(2, 5, 7, MALE),
            make(4, 10, 7, MALE),
            make(8, 14, 7, MALE),
            make(14, 16, 7, FEMALE)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(
            make(0, 1, 7, MALE),
            make(2, 14, 7, MALE),
            make(14, 16, 7, FEMALE)
        );
        assertEquals(expected, segments);

        segments = Arrays.asList(
            make(0, 1, 8, FEMALE),
            make(2, 5, 8, FEMALE),
            make(4, 10, 8, FEMALE),
            make(8, 14, 8, FEMALE),
            make(13, 16, 9, FEMALE),
            make(15, 20, 0, FEMALE),
            make(18, 21, 0, FEMALE)
        );
        segments = mergeOneChromosome(segments);
        expected = Arrays.asList(
            make(0, 1, 8, FEMALE),
            make(2, 14, 8, FEMALE),
            make(13, 16, 9, FEMALE),
            make(15, 21, 0, FEMALE)
        );
        assertEquals(expected, segments);
    }

    @Test
    void testRecombineOneChromosome() {
        List<Segment> segs1 = Arrays.asList(
            make(0, 5, 0, MALE),
            make(10, 15, 1, FEMALE)
        );
        List<Segment> segs2 = Arrays.asList(
            make(3, 4, 2, MALE),
            make(12, 14, 3, FEMALE)
        );
        List<Integer> indices = new ArrayList<>(Arrays.asList(2, 6, 10, Integer.MAX_VALUE));
        List<Segment> expected = Arrays.asList(
            make(0, 2, 0, MALE),
            make(3, 4, 2, MALE),
            make(12, 14, 3, FEMALE)
        );
        List<Segment> recombined = recombineOneChromosome(segs1, segs2, indices);
        assertEquals(expected, recombined);

        segs1 = Arrays.asList(make(0, 10, 0, FEMALE));
        segs2 = Arrays.asList(make(0, 10, 0, MALE));
        indices = new ArrayList<>(Arrays.asList(0, 4, 6, 9, 10, Integer.MAX_VALUE));
        expected = Arrays.asList(
            make(0, 4, 0, MALE),
            make(4, 6, 0, FEMALE),
            make(6, 9, 0, MALE),
            make(9, 10, 0, FEMALE)
        );
        recombined = recombineOneChromosome(segs1, segs2, indices);
        assertEquals(expected, recombined);

        segs1 = Arrays.asList(
            make(4, 5, 5, MALE),
            make(20, 40, 6, MALE),
            make(43, 60, 7, FEMALE),
            make(61, 70, 7, FEMALE)
        );
        segs2 = Arrays.asList(
            make(1, 3, 3, FEMALE),
            make(10, 20, 1, MALE),
            make(30, 33, 0, FEMALE),
            make(80, 90, 4, MALE)
        );
        indices = new ArrayList<>(Arrays.asList(0, 4, 20, 44, 68, 91, 102, Integer.MAX_VALUE));
        expected = Arrays.asList(
            make(10, 20, 1, MALE),
            make(20, 40, 6, MALE),
            make(43, 44, 7, FEMALE),
            make(68, 70, 7, FEMALE)
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
