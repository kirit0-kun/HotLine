package com.flowapp.HotLine.Models;

import com.flowapp.HotLine.Utils.Constants;

public class HotTableRow {

    private final float tf1;
    private final float tf2;
    private final float tfBar;
    private final float ti;
    private final float visAtI;
    private final float c;
    private final Float pT;
    private final Float alpha;
    private final float l;
    private final float sumL;
    private final float nRe;
    private final float f;
    private final float k;
    private final Float hF;
    private final Float deltaP;
    private final Float sumP;
    private final FlowType flowType;

    public HotTableRow(float tf1, float tf2, float tfBar, float ti, float visAtI, float c, Float pT, Float alpha, float l, float sumL, float nRe, float f, float k, Float hF, Float deltaP, Float sumP) {
        this.tf1 = tf1;
        this.tf2 = tf2;
        this.tfBar = tfBar;
        this.ti = ti;
        this.visAtI = visAtI;
        this.c = c;
        this.pT = pT;
        this.alpha = alpha;
        this.l = l;
        this.sumL = sumL;
        this.nRe = nRe;
        this.f = f;
        this.k = k;
        this.hF = hF;
        this.deltaP = deltaP;
        this.sumP = sumP;
        if (nRe <= Constants.LaminarFlowMaxNre) {
            flowType = FlowType.LAMINAR;
        } else if (nRe < Constants.TurbulentFlowMaxNre) {
            flowType = FlowType.TRANSITIONAL;
        } else {
            flowType = FlowType.TURBULENT;
        }
    }

    public float getTf1() {
        return tf1;
    }

    public float getTf2() {
        return tf2;
    }

    public float getTfBar() {
        return tfBar;
    }

    public float getTi() {
        return ti;
    }

    public float getVisAtI() {
        return visAtI;
    }

    public float getC() {
        return c;
    }

    public Float getPt() {
        return pT;
    }

    public Float getAlpha() {
        return alpha;
    }

    public float getL() {
        return l;
    }

    public float getSumL() {
        return sumL;
    }

    public float getNre() {
        return nRe;
    }

    public float getF() {
        return f;
    }

    public float getK() {
        return k;
    }

    public Float getHf() {
        return hF;
    }

    public Float getDeltaP() {
        return deltaP;
    }

    public Float getSumP() {
        return sumP;
    }

    public FlowType getFlowType() {
        return flowType;
    }
}
