package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;

public class CustomButtonGroup extends ButtonGroup {
    private OnCheckChangeListener onCheckChangeListener = null;

    @Override
    protected boolean canCheck(Button button, boolean newState) {
        boolean retVal = super.canCheck(button, newState);

        if (newState && onCheckChangeListener != null) {
            onCheckChangeListener.buttonPressed(button);
        }

        return retVal;
    }

    public void setOnCheckChangeListener(OnCheckChangeListener onCheckChangeListener) {
        this.onCheckChangeListener = onCheckChangeListener;
    }

    public interface OnCheckChangeListener {
        void buttonPressed(Button button);
    }
}
