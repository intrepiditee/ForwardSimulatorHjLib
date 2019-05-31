package com.intrepiditee;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    int start;
    int end;


    public static Segment make(int start, int end) {
        assert start < end;
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
        if (Math.max(seg1.start, seg2.start) > Math.min(seg1.end, seg2.end)) {
            return false;
        }
        return true;
    }

    public List<Segment> split(List<Integer> excludingIndices) {
        int prevEnd = start;
        List<Segment> splited = new ArrayList<>(excludingIndices.size() + 1);
        for (int i : excludingIndices) {
            splited.add(make(prevEnd, i));
            prevEnd = i + 1;
        }
        splited.add(make(prevEnd, end));

        return splited;
    }
}
