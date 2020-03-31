package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class HoverCheckBox extends CheckBox {
    private float normalAlpha = 1f;
    private float hoverAlpha = .5f;
    private float clickAlpha = .5f;

    private float animationDuration = .15f;

    public HoverCheckBox(String text, Skin skin) {
        super(text, skin);

        applyMouseEffects();
    }

    private void applyMouseEffects() {
        this.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                getImage().clearActions();
                getImage().addAction(Actions.alpha(hoverAlpha, animationDuration));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                getImage().clearActions();
                getImage().addAction(Actions.alpha(normalAlpha, animationDuration));
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                clearActions();
                addAction(Actions.alpha(clickAlpha, animationDuration));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                clearActions();
                addAction(Actions.alpha(normalAlpha, animationDuration));
            }
        });
    }

    public void setAnimationDuration(float animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void setNormalAlpha(float normalAlpha) {
        this.normalAlpha = normalAlpha;
    }

    public void setHoverAlpha(float hoverAlpha) {
        this.hoverAlpha = hoverAlpha;
    }

    public void setClickAlpha(float clickAlpha) {
        this.clickAlpha = clickAlpha;
    }
}
