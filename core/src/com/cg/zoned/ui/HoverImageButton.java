package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class HoverImageButton extends ImageButton {
    private float normalAlpha = 1f;
    private float hoverAlpha = .5f;
    private float clickAlpha = .25f;

    private float animationDuration = .15f;

    public HoverImageButton(Drawable imageUp, Drawable imageDown, Drawable imageChecked) {
        super(imageUp, imageDown, imageChecked);
        setupImageButton();
    }

    public HoverImageButton(Drawable image, Drawable imageChecked) {
        super(image, null, imageChecked);
        setupImageButton();
    }

    public HoverImageButton(Drawable image) {
        super(image, null, null);
        setupImageButton();
    }

    private void setupImageButton() {
        getColor().a = normalAlpha;
        getImage().getColor().a = normalAlpha;

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

        getColor().a = normalAlpha;
        getImage().getColor().a = normalAlpha;
    }

    public void setHoverAlpha(float hoverAlpha) {
        this.hoverAlpha = hoverAlpha;
    }

    public void setClickAlpha(float clickAlpha) {
        this.clickAlpha = clickAlpha;
    }


}
