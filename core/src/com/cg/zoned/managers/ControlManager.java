package com.cg.zoned.managers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;
import com.cg.zoned.controls.FlingControlManager;
import com.cg.zoned.controls.PieMenuControlManager;

public class ControlManager {
    private FlingControlManager flingControlManager = null;
    private PieMenuControlManager pieMenuControlManager = null;

    private Player[] players;
    private Stage stage;
    private Image[] controlImage;
    private Color[] overlayColors;
    private Label[] controlLabels = null;

    public ControlManager(Player[] players, boolean isSplitScreen, Stage stage, int controls, Skin skin) {
        this.stage = stage;
        this.players = players;

        FileHandle controlImagePath = null;
        if (controls == Constants.PIE_MENU_CONTROL) {
            pieMenuControlManager = new PieMenuControlManager(players, isSplitScreen, stage);
            controlImagePath = Gdx.files.internal("icons/ic_control_piemenu_off.png");
        } else if (controls == Constants.FLING_CONTROL) {
            flingControlManager = new FlingControlManager(players, isSplitScreen, stage);
            controlImagePath = Gdx.files.internal("icons/ic_control_fling_off.png");
        }

        int splitScreenCount = isSplitScreen ? players.length : 1;
        overlayColors = new Color[splitScreenCount];
        controlImage = new Image[splitScreenCount];
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            controlLabels = new Label[splitScreenCount];
        }

        float colorWidth = stage.getWidth() / overlayColors.length;
        for (int i = 0; i < overlayColors.length; i++) {
            overlayColors[i] = new Color(0, 0, 0, 0);
            controlImage[i] = new Image(new TextureRegionDrawable(new TextureRegion(new Texture(controlImagePath))));
            controlImage[i].setPosition(i * colorWidth + colorWidth / 2 - controlImage[i].getWidth() / 2, stage.getHeight() / 2 - controlImage[i].getHeight() / 2);
            if (controlLabels != null) {
                StringBuilder controlString = new StringBuilder();
                controlString.append(Input.Keys.toString(players[i].controls[0])).append('\n');
                controlString.append(Input.Keys.toString(players[i].controls[3]))
                        .append("  ")
                        .append(Input.Keys.toString(players[i].controls[2]))
                        .append("  ")
                        .append(Input.Keys.toString(players[i].controls[1]));
                controlLabels[i] = new Label(controlString.toString(), skin);
                controlLabels[i].setAlignment(Align.center);
                controlLabels[i].setPosition(i * colorWidth + colorWidth / 2 - controlLabels[i].getPrefWidth() / 2, stage.getHeight() / 2 - controlImage[i].getHeight());
                stage.addActor(controlLabels[i]);
            }
            stage.addActor(controlImage[i]);
        }
    }

    public void resize() {
        float colorWidth = stage.getWidth() / overlayColors.length;
        for (int i = 0; i < controlImage.length; i++) {
            controlImage[i].setPosition(i * colorWidth + colorWidth / 2 - controlImage[i].getWidth() / 2, stage.getHeight() / 2 - controlImage[i].getHeight() / 2);
            if (controlLabels != null) {
                controlLabels[i].setPosition(i * colorWidth + colorWidth / 2 - controlLabels[i].getPrefWidth() / 2, stage.getHeight() / 2 - controlImage[i].getHeight());
            }
        }
    }

    public void renderPlayerControlPrompt(ShapeRenderer renderer, float delta) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        float colorWidth = stage.getWidth() / overlayColors.length;

        for (int i = 0; i < overlayColors.length; i++) {
            if (overlayColors[i].a > 0) {
                renderer.setColor(overlayColors[i]);
                renderer.rect(i * colorWidth, 0, colorWidth, stage.getHeight());
            }

            if (players[i].updatedDirection == null) {
                overlayColors[i].a += delta * 2.5f * (2f - overlayColors[i].a);
                overlayColors[i].a = Math.min(overlayColors[i].a, .8f);
            } else {
                overlayColors[i].a -= delta * 2.5f * (2f - overlayColors[i].a);
                overlayColors[i].a = Math.max(overlayColors[i].a, 0f);
            }
            controlImage[i].getColor().a = overlayColors[i].a;

            if (controlLabels != null) {
                controlLabels[i].getColor().a = overlayColors[i].a;
            }
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
