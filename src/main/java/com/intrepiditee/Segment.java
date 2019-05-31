package com.intrepiditee;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    int start;
    int end;


    public static Segment make(int start, int end) {
        return new Segment(start, end);
    }

    private Segment(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean contains(int index) {
        if (start <= index && index < end) {
            return true;
        }
        return false;
    }

    /**
     * Merge two intersecting Segments.
     *
     * @param seg1 a Segment
     * @param seg2 another Segment
     * @return a new merged Segment
     */
    public static Segment merge(Segment seg1, Segment seg2) {
        return make(
            seg1.start < seg2.start ? seg1.start : seg2.start,
            seg1.end > seg2.end ? seg1.end : seg2.end
        );
    }

    public static boolean intersect(Segment seg1, Segment seg2) {
        if (seg1.start < seg2.end -1 || seg2.start < seg1.end - 1) {
            return false;
        }
        return true;
    }

}
