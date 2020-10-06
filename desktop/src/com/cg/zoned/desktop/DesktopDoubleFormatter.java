package com.cg.zoned.desktop;

import java.text.DecimalFormat;

public class DesktopDoubleFormatter implements com.cg.zoned.DoubleFormatter {
    private DecimalFormat df;

    public DesktopDoubleFormatter() {
        df = new DecimalFormat("#.##");
    }

    @Override
    public double formatDouble(double doubleToFormat) {
        return Double.parseDouble(df.format(doubleToFormat));
    }
}
