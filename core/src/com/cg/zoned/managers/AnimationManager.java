package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.cg.zoned.Zoned;

public class AnimationManager {
    final Zoned game;

    private InputMultiplexer masterInputMultiplexer;
    private AnimationListener animationListener = null;

    public AnimationManager(final Zoned game, InputProcessor inputProcessor) {
        this.game = game;

        this.masterInputMultiplexer = new InputMultiplexer();
        this.masterInputMultiplexer.addProcessor(inputProcessor);
    }

    public void startMainMenuAnimation(final Stage stage, final TextButton[] mainMenuButtons) {
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        stage.getRoot().getColor().a = 1;
        stage.getRoot().setPosition(0, Gdx.graphics.getHeight());
        for (int i = 0; i < mainMenuButtons.length; i++) {
            mainMenuButtons[i].getColor().a = 0;
        }

        SequenceAction fallFromAboveAnimation = new SequenceAction();
        fallFromAboveAnimation.addAction(Actions.moveTo(0f, 0f, .6f, Interpolation.swingOut));
        fallFromAboveAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mainMenuButtons.length; i++) {
                    mainMenuButtons[i].addAction(Actions.delay(.3f * i, Actions.fadeIn(0.3f, Interpolation.smooth)));
                }
            }
        }));
        fallFromAboveAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                masterInputMultiplexer.addProcessor(stage);
                Gdx.input.setInputProcessor(masterInputMultiplexer);

                if (animationListener != null) {
                    animationListener.animationEnd(stage);
                }
            }
        }));

        stage.addAction(fallFromAboveAnimation);
    }

    public void fadeInStage(final Stage stage) {
        SequenceAction fadeInAnimation = new SequenceAction();
        fadeInAnimation.addAction(Actions.fadeIn(.3f, Interpolation.smooth));
        fadeInAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                masterInputMultiplexer.addProcessor(stage);
                Gdx.input.setInputProcessor(masterInputMultiplexer);

                if (animationListener != null) {
                    animationListener.animationEnd(stage);
                }
            }
        }));

        stage.getRoot().getColor().a = 0;
        stage.addAction(fadeInAnimation);
    }

    public void fadeOutStage(final Stage stage, final Screen toScreen) {
        masterInputMultiplexer.removeProcessor(stage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        SequenceAction fadeOutAnimation = new SequenceAction();
        fadeOutAnimation.addAction(Actions.alpha(0f, .45f, Interpolation.smooth));
        fadeOutAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                Gdx.input.setInputProcessor(null);

                game.setScreen(toScreen);
                if (animationListener != null) {
                    animationListener.animationEnd(stage);
                }
            }
        }));

        stage.addAction(fadeOutAnimation);
    }

    public void setAnimationListener(AnimationListener animationListener) {
        this.animationListener = animationListener;
    }

    public interface AnimationListener {
        void animationEnd(Stage stage);
    }
}
