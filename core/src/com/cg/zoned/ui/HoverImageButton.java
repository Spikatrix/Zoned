package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class HoverImageButton extends ImageButton {
    private static final float normalAlpha = .8f;
    private static final float hoverAlpha = .5f;
    private static final float clickAlpha = .25f;

    private static final float animationDuration = .15f;

    public HoverImageButton(Drawable imageUp, Drawable imageDown, Drawable imageChecked) {
        super(imageUp, imageDown, imageChecked);

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

            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggle();
                super.clicked(event, x, y);
            }
        });
    }
}
