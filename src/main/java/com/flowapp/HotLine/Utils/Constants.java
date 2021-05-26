package com.flowapp.HotLine.Utils;

import kotlin.ranges.IntRange;

import java.util.Map;

public class Constants {

    public static final int MmInMeter = 1000;
    public static final int MmInCMeter = 10;
    public static final float CmInInch = 2.54f;
    public static final float MmInInch = CmInInch * MmInCMeter;
    public static final float AllowedErrorInDeltaT = 0.5f;
    public static final float ZeroCInKelvin = 273f;
    public static final int LaminarFlowMaxNre = 2100;
    public static final int TurbulentFlowMaxNre = 4000;

}
