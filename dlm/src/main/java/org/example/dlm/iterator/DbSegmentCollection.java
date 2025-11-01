package org.example.dlm.iterator;

import org.example.dlm.domain.Segment;
import org.example.dlm.domain.SegmentStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DbSegmentCollection implements SegmentIterable {

    private final List<Segment> source;
    private final SegmentOrder order;
    private final boolean onlyPending;

    public DbSegmentCollection(List<Segment> segmentsFromDb,
                               SegmentOrder order,
                               boolean onlyPending) {
        this.source = segmentsFromDb != null ? segmentsFromDb : List.of();
        this.order = order != null ? order : SegmentOrder.BY_INDEX_ASC;
        this.onlyPending = onlyPending;
    }

    @Override
    public SegmentIterator iterator() {
        List<Segment> prepared = new ArrayList<>(source);

        if (onlyPending) {
            prepared.removeIf(s -> s.getStatus() != SegmentStatus.PENDING);
        }

        prepared.sort(selectComparator(order));

        return new SegmentIterator() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < prepared.size();
            }

            @Override
            public Segment next() {
                return prepared.get(idx++);
            }
        };
    }

    private Comparator<Segment> selectComparator(SegmentOrder order) {
        return switch (order) {
            case BY_INDEX_ASC -> Comparator.comparingInt(Segment::getIdx);

            case BY_LEFTMOST_GAP -> Comparator
                    .comparingLong(Segment::getStartByte)
                    .thenComparingLong(Segment::getReceivedBytes);

            case BY_SMALLEST_REMAINING -> Comparator
                    .comparingLong(this::remainingBytes)
                    .thenComparingInt(Segment::getIdx);
        };
    }

    private long remainingBytes(Segment s) {
        long total = Math.max(0, (s.getEndByte() - s.getStartByte() + 1));
        long rem = total - Math.max(0, s.getReceivedBytes());
        return Math.max(0, rem);
    }
}
