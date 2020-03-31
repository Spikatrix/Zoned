package com.cg.zoned.managers;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;

public class ControlManager {
    private FlingControlManager flingControlManager = null;
    private PieMenuControlManager pieMenuControlManager = null;

    public ControlManager(Player[] players, boolean isSplitScreen, Stage stage, int controls) {
        if (controls == Constants.PIE_MENU_CONTROL) {
            pieMenuControlManager = new PieMenuControlManager(players, isSplitScreen, stage);
        } else if (controls == Constants.FLING_CONTROL) {
            flingControlManager = new FlingControlManager(players, isSplitScreen, stage);
        }
    }

    public InputAdapter getControls() {
        if (pieMenuControlManager != null) {
            return pieMenuControlManager;
        } else if (flingControlManager != null) {
            return flingControlManager;
        } else {
            return null;
        }
    }
}
