package com.flowapp.HotLine.Models;

import com.flowapp.HotLine.Utils.Constants;

import java.util.concurrent.Flow;

public class PhysicalProperties {
    private final float c;
    private final float bT;
    private final float lambdaF;
    private final float pT;
    private final float nu;
    private final float alpha1;
    private final float alpha2;
    private final float k;
    private final float nre;
    private final FlowType flowType;
    private float deltaT;

    public PhysicalProperties(float c, float bT, float lambdaF, float pT, float nu, float alpha1, float alpha2, float k, float deltaT, float nre) {
        this.c = c;
        this.bT = bT;
        this.lambdaF = lambdaF;
        this.pT = pT;
        this.nu = nu;
        this.alpha1 = alpha1;
        this.alpha2 = alpha2;
        this.k = k;
        this.deltaT = deltaT;
        this.nre = nre;
        if (nre <= Constants.LaminarFlowMaxNre) {
            flowType = FlowType.LAMINAR;
        } else if (nre < Constants.TurbulentFlowMaxNre) {
            flowType = FlowType.TRANSITIONAL;
        } else {
            flowType = FlowType.TURBULENT;
        }
    }

    public float getC() {
        return c;
    }

    public float getBT() {
        return bT;
    }

    public float getLambdaF() {
        return lambdaF;
    }

    public float getPT() {
        return pT;
    }

    public float getNu() {
        return nu;
    }

    public float getAlpha1() {
        return alpha1;
    }

    public float getAlpha2() {
        return alpha2;
    }

    public float getK() {
        return k;
    }

    public float getNre() {
        return nre;
    }

    public FlowType getFlowType() {
        return flowType;
    }

    public float getDeltaT() {
        return deltaT;
    }

    public void setDeltaT(float deltaT) {
        this.deltaT = deltaT;
    }
}
