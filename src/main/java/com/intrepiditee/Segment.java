package com.intrepiditee;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    int start;
    int end;
    int founderID;

    static Segment make(int start, int end, int founderID) {
        assert start < end;
        return new Segment(start, end, founderID);
    }

    private Segment(int start, int end, int founderID) {
        this.start = start;
        this.end = end;
        this.founderID = founderID;
    }

    boolean contains(int index) {
        return start <= index && index < end;
    }

//    /**
//     * Merge two intersecting Segments.
//     *
//     * @param seg1 a Segment
//     * @param seg2 another Segment
//     * @return a new merged Segment
//     */
//    static Segment merge(Segment seg1, Segment seg2) {
//        return make(
//            seg1.start < seg2.start ? seg1.start : seg2.start,
//            seg1.end > seg2.end ? seg1.end : seg2.end
//        );
//    }

//    static boolean intersect(Segment seg1, Segment seg2) {
//        return Math.max(seg1.start, seg2.start) < Math.min(seg1.end, seg2.end);
//    }

//    static boolean canMerge(Segment seg1, Segment seg2) {
//        if (intersect(seg1, seg2)) {
//            return true;
//        }
//        return Math.max(seg1.start, seg2.start) == Math.min(seg1.end, seg2.end);
//    }

//    List<Segment> split(List<Integer> excludingIndices) {
//        int prevEnd = start;
//        List<Segment> split = new ArrayList<>(excludingIndices.size() + 1);
//        for (int i : excludingIndices) {
//            if (prevEnd < i) {
//                split.add(make(prevEnd, i));
//            }
//            prevEnd = i + 1;
//        }
//        // If last i is end - 1, prevEnd is end
//        if (prevEnd < end) {
//            split.add(make(prevEnd, end));
//        }
//
//        return split;
//    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Segment)) {
            return false;
        }

        Segment o = (Segment) other;
        return o.start == start && o.end == end && o.founderID == founderID;

    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }


    static List<Integer> segmentsToList(List<Segment> segs) {
        List<Integer> indices = new ArrayList<>(2 * segs.size());
        for (Segment seg: segs) {
            indices.add(seg.start);
            indices.add(seg.end);
        }
        return indices;
    }

    static int[] segmentsToArray(List<Segment> segments) {
        int[] indices = new int[segments.size() * 2];
        int j = 0;
        for (Segment seg : segments) {
            indices[j] = seg.start;
            indices[j + 1] = seg.end;
            j += 2;
        }
        return indices;
    }

    static String segmentsToString(List<Segment> segs) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < segs.size(); i++) {
            Segment seg = segs.get(i);
            s.append(seg.start);
            s.append(",");
            s.append(seg.end);
            if (i != segs.size() - 1) {
                s.append(" ");
            }
        }
        return s.toString();
    }
}
