package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.ui.HoverImageButton;

public class UIButtonManager {
    public static HoverImageButton addBackButtonToStage(Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        return addButtonToStage(stage, scaleFactor, 24f, 24f, 0, Position.TOP_LEFT, Gdx.files.internal("icons/ui_icons/ic_back.png"), usedTextures);
    }

    public static HoverImageButton addHideButtonToStage(Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        return addButtonToStage(stage, scaleFactor, 24f, 24f, 90f, Position.TOP_LEFT, Gdx.files.internal("icons/ui_icons/ic_back.png"), usedTextures);
    }

    public static HoverImageButton addSettingsButtonToStage(Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        return addButtonToStage(stage, scaleFactor, 24f, 24f, 0, Position.TOP_RIGHT, Gdx.files.internal("icons/ui_icons/ic_settings.png"), usedTextures);
    }

    public static HoverImageButton addTestingButtonToStage(Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        return addButtonToStage(stage, scaleFactor, 24f, 48f + 64f, 0, Position.TOP_RIGHT, Gdx.files.internal("icons/ui_icons/ic_testing.png"), usedTextures);
    }

    public static HoverImageButton addExitButtonToStage(Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        return addButtonToStage(stage, scaleFactor, 24f, 24f, 0, Position.TOP_LEFT, Gdx.files.internal("icons/ui_icons/ic_cross.png"), usedTextures);
    }

    private static HoverImageButton addButtonToStage(Stage stage, float scaleFactor,
                                                     float paddingSide, float paddingTop,
                                                     float rotateDegrees, Position position,
                                                     FileHandle buttonImageLocation, Array<Texture> usedTextures) {
        Table table = new Table();
        table.setFillParent(true);
        if (position == Position.TOP_LEFT) {
            table.left().top();
        } else if (position == Position.TOP_RIGHT) {
            table.right().top();
        }

        Texture buttonImageTexture = new Texture(buttonImageLocation);
        usedTextures.add(buttonImageTexture);
        Drawable backImageDrawable = new TextureRegionDrawable(buttonImageTexture);
        final HoverImageButton button = new HoverImageButton(backImageDrawable);
        Image buttonImage = button.getImage();
        buttonImage.setOrigin(buttonImage.getPrefWidth() / 2, buttonImage.getPrefHeight() / 2);
        buttonImage.rotateBy(rotateDegrees);
        buttonImage.setScale(scaleFactor);
        button.setNormalAlpha(1f);
        button.setHoverAlpha(.65f);
        button.setClickAlpha(.5f);

        table.add(button)
                .padLeft(paddingSide * scaleFactor).padRight(paddingSide * scaleFactor)
                .padTop(paddingTop * scaleFactor)
                .width(buttonImage.getPrefWidth() * scaleFactor)
                .height(buttonImage.getPrefHeight() * scaleFactor);

        stage.addActor(table);

        return button;
    }

    private enum Position {
        TOP_LEFT,
        TOP_RIGHT,
    }
}
