package com.cg.zoned.maps;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.SpinnerVars;

public class MapExtraParams {
    public String paramSelectTitle;
    public Array<SpinnerVars> spinnerVars;

    public int[] extraParams;

    public MapExtraParams(String paramSelectTitle, Array<SpinnerVars> spinnerVars) {
        this.paramSelectTitle = paramSelectTitle;
        this.spinnerVars = spinnerVars;

        this.extraParams = new int[spinnerVars.size];
        for (int i = 0; i < spinnerVars.size; i++) {
            this.extraParams[i] = MathUtils.clamp(spinnerVars.get(i).snapValue,
                    spinnerVars.get(i).lowValue,
                    spinnerVars.get(i).highValue);
        }
    }
}
