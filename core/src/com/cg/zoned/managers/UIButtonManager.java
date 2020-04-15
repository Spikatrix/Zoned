package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.cg.zoned.ui.HoverImageButton;

public class UIButtonManager {
    public static HoverImageButton addBackButtonToStage(Stage stage, float scaleFactor) {
        return addButtonToStage(stage, scaleFactor, Gdx.files.internal("icons/ui_icons/ic_back.png"));
    }

    public static HoverImageButton addExitButtonToStage(Stage stage, float scaleFactor) {
        return addButtonToStage(stage, scaleFactor, Gdx.files.internal("icons/ui_icons/ic_cross.png"));
    }

    private static HoverImageButton addButtonToStage(Stage stage, float scaleFactor, FileHandle buttonImageLocation) {
        Table table = new Table();
        table.setFillParent(true);
        table.left().top();

        Drawable backImageDrawable = new TextureRegionDrawable(new Texture(buttonImageLocation));
        final HoverImageButton button = new HoverImageButton(backImageDrawable);
        Image buttonImage = button.getImage();
        buttonImage.setOrigin(buttonImage.getPrefWidth() / 2, buttonImage.getPrefHeight() / 2);
        buttonImage.setScale(scaleFactor);
        button.setNormalAlpha(1f);
        button.setHoverAlpha(.75f);
        button.setClickAlpha(.5f);

        table.add(button).padLeft(24f * scaleFactor).padTop(24f * scaleFactor)
                .width(buttonImage.getPrefWidth() * scaleFactor)
                .height(buttonImage.getPrefHeight() * scaleFactor);

        stage.addActor(table);

        return button;
    }
}
