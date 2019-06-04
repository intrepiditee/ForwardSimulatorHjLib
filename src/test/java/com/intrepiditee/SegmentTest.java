package com.intrepiditee;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.intrepiditee.Segment.*;
import static org.junit.jupiter.api.Assertions.*;

public class SegmentTest {

    @Test
    void testContains() {
        Segment seg = make(0, 6, 0);
        assertFalse(seg.contains(-10));
        assertTrue(seg.contains(0));
        assertTrue(seg.contains(4));
        assertTrue(seg.contains(5));
        assertFalse(seg.contains(6));
        assertFalse(seg.contains(10));
    }

//    @Test
//    void testIntersect() {
//        Segment seg = make(-10, 4);
//
//        Segment seg1 = make(-10, 5);
//        Segment seg2 = make(-5, 11);
//        Segment seg3 = make(4, 5);
//        Segment seg4 = make(5, 10);
//
//        assertTrue(intersect(seg, seg1));
//        assertTrue(intersect(seg, seg2));
//        assertFalse(intersect(seg, seg3));
//        assertFalse(intersect(seg, seg4));
//    }

//    @Test
//    void testCanMergeAndMerge() {
//        Segment seg = make(1, 2);
//
//        Segment seg1 = make(2, 3);
//        assertTrue(canMerge(seg, seg1));
//        assertEquals(make(1, 3), merge(seg, seg1));
//
//        Segment seg2 = make(3, 4);
//        assertFalse(canMerge(seg, seg2));
//    }

//    @Test
//    void testSplit() {
//        Integer[] indices = new Integer[]{1, 5, 8};
//        Segment seg = make(0, 10);
//        List<Segment> expected = Arrays.asList(make(0, 1), make(2, 5), make(6, 8), make(9, 10));
//        List<Segment> split = seg.split(Arrays.asList(indices));
//        assertEquals(expected, split);
//
//        indices = new Integer[]{4};
//        seg = make(4, 10);
//        expected = Arrays.asList(make(5, 10));
//        split = seg.split(Arrays.asList(indices));
//        assertEquals(expected, split);
//
//        indices = new Integer[]{9};
//        expected = Arrays.asList(make(4, 9));
//        split = seg.split(Arrays.asList(indices));
//        assertEquals(expected, split);
//
//        indices = new Integer[]{4, 9};
//        expected = Arrays.asList(make(5, 9));
//        split = seg.split(Arrays.asList(indices));
//        assertEquals(expected, split);
//
//        indices = new Integer[]{0, 3, 6, 9};
//        seg = make(0, 10);
//        expected = Arrays.asList(make(1, 3), make(4, 6), make(7, 9));
//        split = seg.split(Arrays.asList(indices));
//        assertEquals(expected, split);
//
//    }

}
