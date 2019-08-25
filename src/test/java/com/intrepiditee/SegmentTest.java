package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.intrepiditee.Configs.FEMALE;
import static com.intrepiditee.Configs.MALE;
import static com.intrepiditee.Segment.*;
import static org.junit.jupiter.api.Assertions.*;

public class SegmentTest {


    @Test
    void testComputeIBD() {
        List<Segment> oneSegmentList = Arrays.asList(make(0, 7, 0, (byte) -1));
        List<Segment> anotherSegment = Arrays.asList(make(3, 5, 0, (byte) -1));
        List<Segment> expected = Arrays.asList(make(3, 5, 0, (byte) - 1));
        List<Segment> ibds = computeIBDsFromTwoChromosomes(oneSegmentList, anotherSegment);
        assertEquals(expected, ibds);

        oneSegmentList = Arrays.asList(
            make(0, 5, 1, (byte) -1),
            make(5, 10, 2, (byte) -1),
            make(10, 11, 3, (byte) -1),
            make(11, 19, 4, (byte) -1),
            make(19, 21, 5, (byte) -1),
            make(21, 23, 6, (byte) -1),
            make(23, 25, 7, (byte) -1)
        );
        anotherSegment = Arrays.asList(
            make(0, 3, 2, (byte) -1),
            make(3, 7, 1, (byte) -1),
            make(7, 11, 3, (byte) -1),
            make(11, 17, 4, (byte) -1),
            make(17, 21, 5, (byte) -1),
            make(21, 25, 6, (byte) -1)
        );
        expected = Arrays.asList(
            make(3, 5, 1, (byte) - 1),
            make(10, 11, 3, (byte) -1),
            make(11, 17, 4, (byte) -1),
            make(19, 21, 5, (byte) -1),
            make(21, 23, 6, (byte) -1)
        );
        ibds = computeIBDsFromTwoChromosomes(oneSegmentList, anotherSegment);
        assertEquals(expected, ibds);
    }

    @Test
    void testContains() {
        Segment seg = make(0, 6, 0, MALE);
        assertFalse(seg.contains(-10));
        assertTrue(seg.contains(0));
        assertTrue(seg.contains(4));
        assertTrue(seg.contains(5));
        assertFalse(seg.contains(6));
        assertFalse(seg.contains(10));
    }

    @Test
    void testIntersect() {
        Segment seg = make(-10, 4, 0, FEMALE);

        Segment seg1 = make(-10, 5, 0, FEMALE);
        Segment seg2 = make(-5, 11, 0, FEMALE);
        Segment seg3 = make(4, 5, 0, FEMALE);
        Segment seg4 = make(5, 10, 0, FEMALE);

        assertTrue(intersect(seg, seg1));
        assertTrue(intersect(seg, seg2));
        assertFalse(intersect(seg, seg3));
        assertFalse(intersect(seg, seg4));
    }

    @Test
    void testCanMergeAndMerge() {
        Segment seg = make(1, 2, 5, MALE);

        Segment seg1 = make(2, 3, 5, MALE);
        assertTrue(canMerge(seg, seg1));
        assertEquals(make(1, 3, 5, MALE), merge(seg, seg1));

        Segment seg2 = make(3, 4, 5, MALE);
        assertFalse(canMerge(seg, seg2));

        Segment seg3 = make(2, 3, 4, MALE);
        assertFalse(canMerge(seg, seg3));

        Segment seg4 = make(2, 3, 5, FEMALE);
        assertFalse(canMerge(seg, seg4));
    }

    @Test
    void testSplit() {
        Integer[] indices = new Integer[]{1, 5, 8};
        Segment seg = make(0, 10, 1, MALE);
        List<Segment> expected = Arrays.asList(
            make(0, 1, 1, MALE),
            make(2, 5, 1, MALE),
            make(6, 8, 1, MALE),
            make(9, 10, 1, MALE)
        );
        List<Segment> split = seg.split(Arrays.asList(indices));
        assertEquals(expected, split);

        indices = new Integer[]{4};
        seg = make(4, 10, 3, MALE);
        expected = Arrays.asList(make(5, 10, 3, MALE));
        split = seg.split(Arrays.asList(indices));
        assertEquals(expected, split);

        indices = new Integer[]{9};
        expected = Arrays.asList(make(4, 9, 3, MALE));
        split = seg.split(Arrays.asList(indices));
        assertEquals(expected, split);

        indices = new Integer[]{4, 9};
        expected = Arrays.asList(make(5, 9, 3, MALE));
        split = seg.split(Arrays.asList(indices));
        assertEquals(expected, split);

        indices = new Integer[]{0, 3, 6, 9};
        seg = make(0, 10, 11, MALE);
        expected = Arrays.asList(
            make(1, 3, 11, MALE),
            make(4, 6, 11, MALE),
            make(7, 9, 11, MALE)
        );
        split = seg.split(Arrays.asList(indices));
        assertEquals(expected, split);
    }

}
