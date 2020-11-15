package com.cg.zoned.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Window extends com.badlogic.gdx.scenes.scene2d.ui.Window {
    public Window(String title, Skin skin) {
        super(title, skin);
    }

    public Window(String title, Skin skin, String styleName) {
        super(title, skin, styleName);
    }

    public Window(String title, WindowStyle style) {
        super(title, style);
    }

    // Stage background should not depend on the Window positions as in scene2d's Window class IMO
    @Override
    protected void drawStageBackground(Batch batch, float parentAlpha, float x, float y, float width, float height) {
        Color color = getColor();
        Stage stage = getStage();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        getStyle().stageBackground.draw(batch, 0, 0, stage.getWidth(), stage.getHeight());
    }
}
