package com.intrepiditee;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    int start;
    int end;

    static Segment make(int start, int end) {
        assert start < end;
        return new Segment(start, end);
    }

    private Segment(int start, int end) {
        this.start = start;
        this.end = end;
    }

    boolean contains(int index) {
        return start <= index && index < end;
    }

    /**
     * Merge two intersecting Segments.
     *
     * @param seg1 a Segment
     * @param seg2 another Segment
     * @return a new merged Segment
     */
    static Segment merge(Segment seg1, Segment seg2) {
        return make(
            seg1.start < seg2.start ? seg1.start : seg2.start,
            seg1.end > seg2.end ? seg1.end : seg2.end
        );
    }

    static boolean intersect(Segment seg1, Segment seg2) {
        if (Math.max(seg1.start, seg2.start) >= Math.min(seg1.end, seg2.end)) {
            return false;
        }
        return true;
    }

    static boolean canMerge(Segment seg1, Segment seg2) {
        if (intersect(seg1, seg2)) {
            return true;
        }
        if (Math.max(seg1.start, seg2.start) == Math.min(seg1.end, seg2.end)) {
            return true;
        }
        return false;
    }

    List<Segment> split(List<Integer> excludingIndices) {
        int prevEnd = start;
        List<Segment> splited = new ArrayList<>(excludingIndices.size() + 1);
        for (int i : excludingIndices) {
            splited.add(make(prevEnd, i));
            prevEnd = i + 1;
        }
        // If last i is end - 1, prevEnd is end
        splited.add(make(prevEnd, end));

        return splited;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Segment)) {
            return false;
        }

        Segment o = (Segment) other;
        if (o.start == start && o.end == end) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}
