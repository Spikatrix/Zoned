package com.cg.zoned;

import java.text.DecimalFormat;

public class AndroidDoubleFormatter implements com.cg.zoned.DoubleFormatter {
    private DecimalFormat df;

    public AndroidDoubleFormatter() {
        df = new DecimalFormat("#.##");
    }

    @Override
    public double formatDouble(double doubleToFormat) {
        return Double.parseDouble(df.format(doubleToFormat));
    }
}
