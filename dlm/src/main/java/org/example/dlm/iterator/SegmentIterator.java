package org.example.dlm.iterator;

import org.example.dlm.domain.Segment;

public interface SegmentIterator {
    boolean hasNext();
    Segment next();
}
