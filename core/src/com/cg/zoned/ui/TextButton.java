package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class TextButton extends com.badlogic.gdx.scenes.scene2d.ui.TextButton {
    public TextButton(String text, Skin skin) {
        super(text, skin);
    }

    public TextButton(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }

    public TextButton(String text, TextButtonStyle style) {
        super(text, style);
    }

    public void performClick() {
        InputEvent touchDownEvent = new InputEvent();
        touchDownEvent.setType(InputEvent.Type.touchDown);
        this.fire(touchDownEvent);

        InputEvent touchUpEvent = new InputEvent();
        touchUpEvent.setType(InputEvent.Type.touchUp);
        this.fire(touchUpEvent);
    }
}
