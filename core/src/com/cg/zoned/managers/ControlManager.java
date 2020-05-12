package com.cg.zoned.managers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;
import com.cg.zoned.controls.FlingControlManager;
import com.cg.zoned.controls.PieMenuControlManager;

public class ControlManager {
    private FlingControlManager flingControlManager = null;
    private PieMenuControlManager pieMenuControlManager = null;

    private Player[] players;
    private Stage stage;
    private Color[] overlayColors;
    private Table[] controlTables;

    public ControlManager(Player[] players, boolean isSplitScreen, Stage stage, int controls, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        this.stage = stage;
        this.players = players;

        FileHandle controlImagePath = null;
        if (controls == Constants.PIE_MENU_CONTROL) {
            pieMenuControlManager = new PieMenuControlManager(players, isSplitScreen, stage, scaleFactor, usedTextures);
            controlImagePath = Gdx.files.internal("icons/control_icons/ic_control_piemenu_off.png");
        } else if (controls == Constants.FLING_CONTROL) {
            flingControlManager = new FlingControlManager(players, isSplitScreen, stage, scaleFactor, usedTextures);
            controlImagePath = Gdx.files.internal("icons/control_icons/ic_control_fling_off.png");
        }

        Table masterTable = new Table();
        masterTable.setFillParent(true);

        int splitScreenCount = isSplitScreen ? players.length : 1;
        Image[] controlImages = new Image[splitScreenCount];
        overlayColors = new Color[splitScreenCount];
        controlTables = new Table[splitScreenCount];
        Label[] controlLabels = null;
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            controlLabels = new Label[splitScreenCount];
        }

        Texture controlImageTexture = new Texture(controlImagePath);
        usedTextures.add(controlImageTexture);

        float splitScreenWidth = stage.getWidth() / overlayColors.length;
        for (int i = 0; i < overlayColors.length; i++) {
            overlayColors[i] = new Color(0, 0, 0, 0);
            controlImages[i] = new Image(controlImageTexture);
            controlImages[i].setScaling(Scaling.fit);
            controlImages[i].setOrigin(controlImages[i].getWidth() / 2, controlImages[i].getHeight() / 2);
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
                labelColor.mul(10);
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

    public void renderPlayerControlPrompt(ShapeRenderer renderer, float delta) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        float splitScreenWidth = stage.getWidth() / overlayColors.length;

        for (int i = 0; i < overlayColors.length; i++) {
            if (overlayColors[i].a > 0) {
                renderer.setColor(overlayColors[i]);
                renderer.rect(i * splitScreenWidth, 0, splitScreenWidth, stage.getHeight());
            }

            if (players[i].updatedDirection == null) {
                overlayColors[i].a += delta * 2.5f * (2f - overlayColors[i].a);
                overlayColors[i].a = Math.min(overlayColors[i].a, .8f);
            } else {
                overlayColors[i].a -= delta * 2.5f * (2f - overlayColors[i].a);
                overlayColors[i].a = Math.max(overlayColors[i].a, 0f);
            }

            controlTables[i].getColor().a = overlayColors[i].a;
        }

        renderer.end();
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
