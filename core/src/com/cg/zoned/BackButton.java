package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.cg.zoned.ui.HoverImageButton;

public class BackButton {
    public static HoverImageButton addBackButtonToStage(Stage stage, float scaleFactor) {
        Table table = new Table();
        table.setFillParent(true);
        table.left().top();

        Drawable backImageDrawable = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_back.png"))));
        final HoverImageButton backButton = new HoverImageButton(backImageDrawable);
        Image backImage = backButton.getImage();
        backImage.setOrigin(backImage.getPrefWidth() / 2, backImage.getPrefHeight() / 2);
        backImage.setScale(scaleFactor);
        backButton.setNormalAlpha(1f);
        backButton.setHoverAlpha(.75f);
        backButton.setClickAlpha(.5f);

        table.add(backButton).padLeft(24f * scaleFactor).padTop(24f * scaleFactor)
                .width(backImage.getPrefWidth() * scaleFactor)
                .height(backImage.getPrefHeight() * scaleFactor);

        stage.addActor(table);

        return backButton;
    }
}
