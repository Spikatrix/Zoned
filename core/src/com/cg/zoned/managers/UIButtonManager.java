package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.ui.HoverImageButton;

public class UIButtonManager {
    private Stage stage;
    private float scaleFactor;
    private Array<Texture> usedTextures;

    private Array<Table> buttonPositionTables;

    public UIButtonManager(Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        this.stage = stage;
        this.scaleFactor = scaleFactor;
        this.usedTextures = usedTextures;

        this.buttonPositionTables = new Array<>();
        for (Position position : Position.values()) {
            Table table = new Table();
            table.setFillParent(true);

            if (position == Position.TOP_LEFT) {
                table.top().left();
            } else if (position == Position.TOP_CENTER) {
                table.center().top();
            } else if (position == Position.TOP_RIGHT) {
                table.top().right();
            } else {
                throw new IllegalArgumentException("Unknown position table in UIButtonManager");
            }

            buttonPositionTables.add(table);
        }
    }

    public HoverImageButton addBackButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 0, Position.TOP_LEFT, Gdx.files.internal("icons/ui_icons/ic_back.png"), null);
    }

    public HoverImageButton addHideButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 90f, Position.TOP_LEFT, Gdx.files.internal("icons/ui_icons/ic_back.png"), null);
    }

    public HoverImageButton addSettingsButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 0, Position.TOP_RIGHT, Gdx.files.internal("icons/ui_icons/ic_settings.png"), null);
    }

    public HoverImageButton addPauseButtonToStage() {
        return addButtonToStage(.8f, .65f, .5f, 20f, 0, Position.TOP_CENTER, Gdx.files.internal("icons/ui_icons/ic_pause.png"), null);
    }

    public HoverImageButton addZoomButtonToStage() {
        return addButtonToStage(.8f, .65f, .5f, 20f, 0, Position.TOP_CENTER, Gdx.files.internal("icons/ui_icons/ic_zoom_out.png"), Gdx.files.internal("icons/ui_icons/ic_zoom_in.png"));
    }

    public HoverImageButton addTutorialButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 0, Position.TOP_RIGHT, Gdx.files.internal("icons/ui_icons/ic_tutorial.png"), null);
    }

    public HoverImageButton addDevButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 0, Position.TOP_RIGHT, Gdx.files.internal("icons/ui_icons/ic_dev.png"), null);
    }

    public HoverImageButton addCreditsButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 0, Position.TOP_RIGHT, Gdx.files.internal("icons/ui_icons/ic_credits.png"), null);
    }

    public HoverImageButton addExitButtonToStage() {
        return addButtonToStage(1f, .65f, .5f, 24f, 0, Position.TOP_LEFT, Gdx.files.internal("icons/ui_icons/ic_cross.png"), null);
    }

    private HoverImageButton addButtonToStage(float normalAlpha, float hoverAlpha, float clickAlpha,
                                              float paddingTop,
                                              float rotateDegrees, Position position,
                                              FileHandle buttonImageLocation1,
                                              FileHandle buttonImageLocation2) {
        Table table = buttonPositionTables.get(position.ordinal());

        HoverImageButton button;
        if (buttonImageLocation2 == null) {
            Texture buttonImageTexture = new Texture(buttonImageLocation1);
            usedTextures.add(buttonImageTexture);
            button = new HoverImageButton(new TextureRegionDrawable(buttonImageTexture));
        } else {
            Texture buttonImageTexture1 = new Texture(buttonImageLocation1);
            Texture buttonImageTexture2 = new Texture(buttonImageLocation2);
            usedTextures.add(buttonImageTexture1);
            usedTextures.add(buttonImageTexture2);
            button = new HoverImageButton(new TextureRegionDrawable(buttonImageTexture1),
                    new TextureRegionDrawable(buttonImageTexture2));
        }

        float buttonWidth = 64f;
        float buttonHeight = 64f;

        Image buttonImage = button.getImage();
        buttonImage.setOrigin(buttonWidth * scaleFactor / 2, buttonHeight * scaleFactor / 2);
        buttonImage.rotateBy(rotateDegrees);
        button.setNormalAlpha(normalAlpha);
        button.setHoverAlpha(hoverAlpha);
        button.setClickAlpha(clickAlpha);

        table.add(button)
                .padLeft(24f).padRight(24f)
                .padTop(paddingTop * scaleFactor)
                .width(buttonWidth * scaleFactor)
                .height(buttonHeight * scaleFactor);
        table.row();

        stage.addActor(table);

        return button;
    }

    private enum Position {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
    }
}