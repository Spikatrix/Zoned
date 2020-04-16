package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
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
        stage.getRoot().setPosition(0, stage.getHeight());
        for (TextButton mainMenuButton : mainMenuButtons) {
            mainMenuButton.getColor().a = 0;
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

    public void startPlayModeAnimation(Stage mainStage, Stage playModeStage) {
        masterInputMultiplexer.removeProcessor(mainStage);
        masterInputMultiplexer.addProcessor(playModeStage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        mainStage.addAction(Actions.fadeOut(.3f));

        playModeStage.getRoot().setPosition(0, -playModeStage.getHeight());
        playModeStage.addAction(Actions.fadeIn(.3f, Interpolation.fastSlow));
        playModeStage.addAction(Actions.moveTo(0, 0, .3f, Interpolation.fastSlow));
    }

    public void endPlayModeAnimation(Stage mainStage, Stage playModeStage) {
        masterInputMultiplexer.removeProcessor(playModeStage);
        masterInputMultiplexer.addProcessor(mainStage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        playModeStage.addAction(Actions.fadeOut(.3f, Interpolation.fastSlow));
        playModeStage.addAction(Actions.moveTo(0, -playModeStage.getHeight(), .3f, Interpolation.fastSlow));
        mainStage.addAction(Actions.fadeIn(.3f));
    }

    public void startGameOverAnimation(final Stage stage, final ParticleEffect trailEffect) {
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        float screenWidth = stage.getWidth();

        stage.getRoot().getColor().a = 1;
        stage.getRoot().setPosition(screenWidth, 0);

        SequenceAction slideAnimation = new SequenceAction();
        slideAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                trailEffect.start();
            }
        }));
        slideAnimation.addAction(Actions.delay(.4f));
        slideAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                trailEffect.allowCompletion(); // Completion takes time which is why it's done before the move actions
            }
        }));
        slideAnimation.addAction(Actions.moveTo(screenWidth / 8, 0f, .25f, Interpolation.pow2));
        slideAnimation.addAction(Actions.moveTo(-screenWidth / 8, 0f, 1.6f, Interpolation.linear));
        slideAnimation.addAction(Actions.moveTo(-screenWidth, 0f, .25f, Interpolation.pow2));
        slideAnimation.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                masterInputMultiplexer.addProcessor(stage);
                Gdx.input.setInputProcessor(masterInputMultiplexer);

                if (animationListener != null) {
                    animationListener.animationEnd(stage);
                }
            }
        }));

        stage.addAction(slideAnimation);
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
