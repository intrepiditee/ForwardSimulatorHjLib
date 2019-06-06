package com.intrepiditee;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Segment implements Serializable {

    final int start;
    final int end;
    final int founderID;
    final byte whichChromosome;

    static Segment make(int start, int end, int founderID, byte whichChromosome) {
        assert start < end;
        return new Segment(start, end, founderID, whichChromosome);
    }

    private Segment(int start, int end, int founderID, byte whichChromosome) {
        this.start = start;
        this.end = end;
        this.founderID = founderID;
        this.whichChromosome = whichChromosome;
    }

    boolean contains(int index) {
        return start <= index && index < end;
    }

    // Merge two intersecting Segments. canMerge must first be called.
    static Segment merge(Segment seg1, Segment seg2) {
        return make(
            seg1.start < seg2.start ? seg1.start : seg2.start,
            seg1.end > seg2.end ? seg1.end : seg2.end,
            seg1.founderID,
            seg1.whichChromosome
        );
    }

    static boolean intersect(Segment seg1, Segment seg2) {
        return Math.max(seg1.start, seg2.start) < Math.min(seg1.end, seg2.end);
    }

    static boolean canMerge(Segment seg1, Segment seg2) {
        if (seg1.founderID != seg2.founderID || seg1.whichChromosome != seg2.whichChromosome) {
            return false;
        }

        return intersect(seg1, seg2) ||
            Math.max(seg1.start, seg2.start) == Math.min(seg1.end, seg2.end);
    }

    List<Segment> split(List<Integer> excludingIndices) {
        int prevEnd = start;
        List<Segment> split = new ArrayList<>(excludingIndices.size() + 1);
        for (int i : excludingIndices) {
            if (prevEnd < i) {
                split.add(make(prevEnd, i, founderID, whichChromosome));
            }
            prevEnd = i + 1;
        }
        // If last i is end - 1, prevEnd is end
        if (prevEnd < end) {
            split.add(make(prevEnd, end, founderID, whichChromosome));
        }

        return split;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Segment)) {
            return false;
        }

        Segment o = (Segment) other;
        return o.start == start &&
            o.end == end &&
            o.founderID == founderID &&
            o.whichChromosome == whichChromosome;

    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + ", " + founderID + ", " + whichChromosome + "]";
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

    static int[] getFounderArray(List<Segment> segments) {
        int[] founderIDs = new int[segments.size()];
        int j = 0;
        for (Segment seg : segments) {
            founderIDs[j] = seg.founderID;
        }
        return founderIDs;
    }

    static String segmentsToString(List<Segment> segs) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < segs.size(); i++) {
            Segment seg = segs.get(i);
            s.append(seg.start);
            s.append(",");
            s.append(seg.end);
            s.append(",");
            s.append(seg.founderID);
            s.append(",");
            s.append(seg.whichChromosome);
            if (i != segs.size() - 1) {
                s.append(" ");
            }
        }
        return s.toString();
    }
}
