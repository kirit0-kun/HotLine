package com.flowapp.HotLine.Models;

public class HotTableRow {

    private final float tf1;
    private final float tf2;
    private final float tfBar;
    private final float ti;
    private final float visAtI;
    private final float c;
    private final float pT;
    private final float l;
    private final float sumL;
    private final float nRe;
    private final float f;
    private final float hF;
    private final float deltaP;
    private final float sumP;

    public HotTableRow(float tf1, float tf2, float tfBar, float ti, float visAtI, float c, float pT, float l, float sumL, float nRe, float f, float hF, float deltaP, float sumP) {
        this.tf1 = tf1;
        this.tf2 = tf2;
        this.tfBar = tfBar;
        this.ti = ti;
        this.visAtI = visAtI;
        this.c = c;
        this.pT = pT;
        this.l = l;
        this.sumL = sumL;
        this.nRe = nRe;
        this.f = f;
        this.hF = hF;
        this.deltaP = deltaP;
        this.sumP = sumP;
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

    public float getPt() {
        return pT;
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

    public float getHf() {
        return hF;
    }

    public float getDeltaP() {
        return deltaP;
    }

    public float getSumP() {
        return sumP;
    }
}
