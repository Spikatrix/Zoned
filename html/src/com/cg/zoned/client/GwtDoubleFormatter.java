package com.cg.zoned.client;

import com.google.gwt.i18n.client.NumberFormat;

public class GwtDoubleFormatter implements com.cg.zoned.DoubleFormatter {
    private NumberFormat nf;

    public GwtDoubleFormatter() {
        nf = NumberFormat.getFormat("#.##");
    }

    @Override
    public double formatDouble(double doubleToFormat) {
        return Double.parseDouble(nf.format(doubleToFormat));
    }
}
