package com.cg.zoned.dataobjects;

public class SpinnerVars {
    public String prompt;
    public int lowValue;
    public int highValue;
    public int snapValue;

    public SpinnerVars(String prompt, int lowValue, int highValue, int snapValue) {
        this.prompt = prompt;
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.snapValue = snapValue;
    }
}
