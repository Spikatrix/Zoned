package com.cg.zoned.managers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.Overlay;
import com.cg.zoned.Player;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.controls.ControlType;
import com.cg.zoned.controls.ControlTypeEntity;
import com.cg.zoned.controls.FlingControlManager;
import com.cg.zoned.controls.PieMenuControlManager;

public class ControlManager {
    // Specify at least one control mechanism. Otherwise, it will crash and burn.
    public static final ControlType[] CONTROL_TYPES = {
            new ControlType("D-Pad",
                    "images/control_icons/ic_control_piemenu_off.png",
                    "images/control_icons/ic_control_piemenu_on.png",
                    new PieMenuControlManager()),
            new ControlType("Fling",
                    "images/control_icons/ic_control_fling_off.png",
                    "images/control_icons/ic_control_fling_on.png",
                    new FlingControlManager()),
    };

    private ControlTypeEntity currentControls = null;

    private Player[] players;
    private Stage stage;
    private Overlay[] overlays;
    private Table[] controlTables;

    public ControlManager(Player[] players, Stage stage) {
        this.stage = stage;
        this.players = players;
    }

    public void setUpControls(int controlIndex, boolean isSplitScreen, float scaleFactor, Array<Texture> usedTextures) {
        currentControls = CONTROL_TYPES[controlIndex].controlTypeEntity;
        currentControls.init(players, isSplitScreen, stage, scaleFactor, usedTextures);
    }

    public void setUpOverlay(boolean isSplitScreen, int controlIndex, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        setUpControls(controlIndex, isSplitScreen, scaleFactor, usedTextures);

        FileHandle controlImagePath = Gdx.files.internal(CONTROL_TYPES[controlIndex].controlOffTexturePath);

        Table masterTable = new Table();
        masterTable.setFillParent(true);

        int splitScreenCount = isSplitScreen ? players.length : 1;
        Image[] controlImages = new Image[splitScreenCount];
        overlays = new Overlay[splitScreenCount];
        controlTables = new Table[splitScreenCount];
        Label[] controlLabels = null;
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            controlLabels = new Label[splitScreenCount];
        }

        Texture controlImageTexture = new Texture(controlImagePath);
        usedTextures.add(controlImageTexture);

        float splitScreenWidth = stage.getWidth() / overlays.length;
        for (int i = 0; i < overlays.length; i++) {
            overlays[i] = new Overlay(new Color(0, 0, 0, 0.8f), 6.0f);
            controlImages[i] = new Image(controlImageTexture);
            controlImages[i].setScaling(Scaling.fit);
            controlImages[i].setOrigin(Align.center);
            if (splitScreenCount == 2 && Gdx.app.getType() == Application.ApplicationType.Android) {
                if (i == 0) {
                    controlImages[i].setRotation(-90f);
                } else {
                    controlImages[i].setRotation(90f);
                }
            }
            if (controlLabels != null) {
                String controlString = Input.Keys.toString(players[i].controls[0]) + '\n' +
                        Input.Keys.toString(players[i].controls[1]) +
                        "  " +
                        Input.Keys.toString(players[i].controls[2]) +
                        "  " +
                        Input.Keys.toString(players[i].controls[3]);
                controlLabels[i] = new Label(controlString, skin);
                Color labelColor = new Color(players[i].color);
                controlLabels[i].setColor(labelColor);
                controlLabels[i].setAlignment(Align.center);
            }

            controlTables[i] = new Table();
            controlTables[i].center();
            controlTables[i].setSize(splitScreenWidth, stage.getHeight());
            controlTables[i].add(controlImages[i]);
            if (controlLabels != null) {
                controlTables[i].row();
                controlTables[i].add(controlLabels[i]);
            }

            masterTable.add(controlTables[i]).uniformX().expand();
        }

        stage.addActor(masterTable);
    }

    public void renderPlayerControlPrompt(ShapeDrawer shapeDrawer, float delta) {
        float splitScreenWidth = stage.getWidth() / overlays.length;

        for (int i = 0; i < overlays.length; i++) {
            overlays[i].render(shapeDrawer,
                    i * splitScreenWidth, 0, splitScreenWidth, stage.getHeight(), delta);

            overlays[i].drawOverlay(players[i].updatedDirection == null);
            controlTables[i].getColor().a = overlays[i].getOverlayAlpha();
        }
    }

    public InputAdapter getControls() {
        return currentControls;
    }
}
