package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.intrepiditee.Segment.intersect;
import static com.intrepiditee.Segment.make;
import static com.intrepiditee.Segment.merge;
import static org.junit.jupiter.api.Assertions.*;

public class SegmentTest {

    @Test
    public void testContains() {
        Segment seg = make(0, 6);
        assertFalse(seg.contains(-10));
        assertTrue(seg.contains(0));
        assertTrue(seg.contains(4));
        assertTrue(seg.contains(5));
        assertFalse(seg.contains(6));
        assertFalse(seg.contains(10));
    }

    @Test
    public void testIntersectAndMerge() {
        Segment seg = make(-10, 4);

        Segment seg1 = make(-10, 5);
        Segment seg2 = make(-5, 11);
        Segment seg3 = make(4, 5);
        Segment seg4 = make(5, 10);

        assertTrue(intersect(seg, seg1));
        assertTrue(intersect(seg, seg2));
        assertFalse(intersect(seg, seg3));
        assertFalse(intersect(seg, seg4));

        assertEquals(make(-10, 5), merge(seg, seg1));
        assertEquals(make(-10, 11), merge(seg, seg2));
        assertEquals(make(-10, 5), merge(seg1, seg));
        assertEquals(make(-10, 11), merge(seg2, seg));
    }

    @Test
    public void testSplit() {
        Integer[] indices1 = new Integer[]{1, 5, 8};
        Segment seg1 = make(0, 10);
        List<Segment> splited = seg1.split(Arrays.asList(indices1));
        List<Segment> expected = Arrays.asList(make(0, 1), make(2, 5), make(6, 8), make(9, 10));
        assertEquals(expected, splited);

    }

}
