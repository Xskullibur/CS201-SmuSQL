package edu.smu.smusql.bplustreeA.helper;

public class Range<K> {
    private K start;
    private K end;

    public Range(K start, K end) {
        this.start = start;
        this.end = end;
    }

    public K getStart() {
        return start;
    }

    public K getEnd() {
        return end;
    }
}