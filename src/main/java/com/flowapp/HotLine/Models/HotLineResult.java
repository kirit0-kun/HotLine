package com.flowapp.HotLine.Models;

import java.util.List;

public class HotLineResult {
    private final PhysicalProperties physicalProperties;
    private final List<HotTableRow> hotTableRows;
    private final PressureTraverse pressureTraverse;
    private final Point[] temperatureTraverse;
    private final List<HotTableRow> simplifiedFordRows;
    private final String steps;

    public HotLineResult(PhysicalProperties physicalProperties, List<HotTableRow> hotTableRows, PressureTraverse pressureTraverse, Point[] temperatureTraverse, List<HotTableRow> simplifiedFordRows, String steps) {
        this.physicalProperties = physicalProperties;
        this.hotTableRows = hotTableRows;
        this.pressureTraverse = pressureTraverse;
        this.temperatureTraverse = temperatureTraverse;
        this.simplifiedFordRows = simplifiedFordRows;
        this.steps = steps;
    }

    public PhysicalProperties getPhysicalProperties() {
        return physicalProperties;
    }

    public List<HotTableRow> getHotTableRows() {
        return hotTableRows;
    }

    public PressureTraverse getPressureTraverse() {
        return pressureTraverse;
    }

    public Point[] getTemperatureTraverse() {
        return temperatureTraverse;
    }

    public List<HotTableRow> getSimplifiedFordRows() {
        return simplifiedFordRows;
    }

    public String getSteps() {
        return steps;
    }
}
