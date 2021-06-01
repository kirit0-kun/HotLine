package com.flowapp.HotLine.Models;

import java.util.List;

public class PressureTraverse {
    private final List<Point> pressureTraverse;
    private final List<Tuple2<Point, Point>> workLines;

    public PressureTraverse(List<Point> pressureTraverse, List<Tuple2<Point, Point>> workLines) {
        this.pressureTraverse = pressureTraverse;
        this.workLines = workLines;
    }

    public List<Point> getPressureTraverse() {
        return pressureTraverse;
    }

    public List<Tuple2<Point, Point>> getWorkLines() {
        return workLines;
    }
}
