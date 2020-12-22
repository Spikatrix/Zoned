package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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

    private final float buttonHeight = 64f;
    private final float buttonWidth = 64f;

    private final float buttonPadding = 24f;

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
            } else if (position == Position.BOTTOM_RIGHT) {
                table.bottom().right();
            } else {
                throw new IllegalArgumentException("Unknown position table in UIButtonManager");
            }

            buttonPositionTables.add(table);
        }
    }

    public HoverImageButton addBackButtonToStage(Texture backTexture) {
        return addButtonToStage(1f, .65f, .5f, 0,
                Position.TOP_LEFT, backTexture, null);
    }

    public HoverImageButton addNextButtonToStage(Texture backTexture) {
        return addButtonToStage(1f, .65f, .5f, 180f,
                Position.BOTTOM_RIGHT, backTexture, null);
    }

    public HoverImageButton addHideButtonToStage(Texture backTexture) {
        return addButtonToStage(1f, .65f, .5f, 90f,
                Position.TOP_LEFT, backTexture, null);
    }

    public HoverImageButton addSettingsButtonToStage(Texture settingsTexture) {
        return addButtonToStage(1f, .65f, .5f, 0,
                Position.TOP_RIGHT, settingsTexture, null);
    }

    public HoverImageButton addPauseButtonToStage() {
        return addButtonToStage(.8f, .65f, .5f, 0,
                Position.TOP_CENTER, getTexture(Gdx.files.internal("images/ui_icons/ic_pause.png")), null);
    }

    public HoverImageButton addZoomButtonToStage() {
        return addButtonToStage(.8f, .65f, .5f, 0,
                Position.TOP_CENTER, getTexture(Gdx.files.internal("images/ui_icons/ic_zoom_out.png")), getTexture(Gdx.files.internal("images/ui_icons/ic_zoom_in.png")));
    }

    public HoverImageButton addTutorialButtonToStage(Texture tutorialTexture) {
        return addButtonToStage(1f, .65f, .5f, 0,
                Position.TOP_RIGHT, tutorialTexture, null);
    }

    public HoverImageButton addDevButtonToStage(Texture devTexture) {
        return addButtonToStage(1f, .65f, .5f, 0,
                Position.TOP_RIGHT, devTexture, null);
    }

    public HoverImageButton addCreditsButtonToStage(Texture creditsTexture) {
        return addButtonToStage(1f, .65f, .5f, 0,
                Position.TOP_RIGHT, creditsTexture, null);
    }

    public HoverImageButton addExitButtonToStage(Texture crossTexture) {
        return addButtonToStage(1f, .65f, .5f, 0,
                Position.TOP_LEFT, crossTexture, null);
    }

    private Texture getTexture(FileHandle fileHandle) {
        Texture texture = new Texture(fileHandle);
        usedTextures.add(texture);
        return texture;
    }

    private HoverImageButton addButtonToStage(float normalAlpha, float hoverAlpha, float clickAlpha,
                                              float rotateDegrees, Position position,
                                              Texture texture1,
                                              Texture texture2) {
        Table table = buttonPositionTables.get(position.ordinal());

        HoverImageButton button;
        if (texture2 == null) {
            button = new HoverImageButton(new TextureRegionDrawable(texture1));
        } else {
            button = new HoverImageButton(new TextureRegionDrawable(texture1), new TextureRegionDrawable(texture2));
        }

        Image buttonImage = button.getImage();
        buttonImage.setOrigin(buttonWidth * scaleFactor / 2, buttonHeight * scaleFactor / 2);
        buttonImage.rotateBy(rotateDegrees);
        button.setNormalAlpha(normalAlpha);
        button.setHoverAlpha(hoverAlpha);
        button.setClickAlpha(clickAlpha);

        Cell<HoverImageButton> cell = table.add(button)
                .padLeft(buttonPadding).padRight(buttonPadding)
                .width(buttonWidth * scaleFactor)
                .height(buttonHeight * scaleFactor);
        if (position.name().startsWith("TOP")) {
            cell.padTop(buttonPadding * scaleFactor);
        } else if (position.name().startsWith("BOTTOM")) {
            cell.padBottom(buttonPadding * scaleFactor);
        }
        table.row();

        stage.addActor(table);

        return button;
    }

    public float getHeaderPad(float givenHeight) {
        return (buttonPadding * scaleFactor) + (((buttonHeight * scaleFactor) - givenHeight) / 2);
    }

    private enum Position {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_RIGHT,
    }
}
