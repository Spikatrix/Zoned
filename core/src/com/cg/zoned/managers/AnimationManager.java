package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Zoned;
import com.cg.zoned.ui.FocusableStage;

public class AnimationManager {
    final Zoned game;

    private InputMultiplexer masterInputMultiplexer;
    private AnimationListener animationListener = null;

    public AnimationManager(final Zoned game, InputProcessor inputProcessor) {
        this.game = game;

        this.masterInputMultiplexer = new InputMultiplexer();
        this.masterInputMultiplexer.addProcessor(inputProcessor);
    }

    public void startMainMenuAnimation(final Stage stage, final Array<Actor> mainMenuUIButtons) {
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        stage.getRoot().getColor().a = 1;
        stage.getRoot().setPosition(0, stage.getHeight());
        for (Actor mainMenuUIButton : mainMenuUIButtons) {
            mainMenuUIButton.getColor().a = 0;
        }

        SequenceAction fallFromAboveAnimation = new SequenceAction();
        fallFromAboveAnimation.addAction(Actions.moveTo(0f, 0f, .7f, Interpolation.swingOut));
        fallFromAboveAnimation.addAction(Actions.run(() -> {
            for (int i = 0; i < mainMenuUIButtons.size; i++) {
                mainMenuUIButtons.get(i).addAction(
                        Actions.delay(((i > 0) ? (.3f) : (0)), Actions.fadeIn(0.3f, Interpolation.smooth)));
            }
        }));
        fallFromAboveAnimation.addAction(Actions.run(() -> {
            masterInputMultiplexer.addProcessor(stage);
            Gdx.input.setInputProcessor(masterInputMultiplexer);

            if (animationListener != null) {
                animationListener.animationEnd(stage);
            }
        }));

        stage.addAction(fallFromAboveAnimation);
    }

    public void startPlayModeAnimation(Stage mainStage, Stage playModeStage, Actor playButton) {
        masterInputMultiplexer.removeProcessor(mainStage);
        masterInputMultiplexer.addProcessor(playModeStage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        mainStage.addAction(Actions.fadeOut(.25f, Interpolation.fastSlow));
        float initScale = playButton.getScaleX();
        playButton.setOrigin(playButton.getWidth() / 2, playButton.getHeight() / 2);
        playButton.addAction(Actions.sequence(
                Actions.scaleTo(2.5f, 2.5f, .2f),
                Actions.scaleTo(initScale, initScale)
        ));

        playModeStage.getRoot().setPosition(0, -playModeStage.getHeight());
        playModeStage.addAction(Actions.fadeIn(.25f, Interpolation.fastSlow));
        playModeStage.addAction(Actions.moveTo(0, 0, .25f, Interpolation.fastSlow));
    }

    public void endPlayModeAnimation(Stage mainStage, Stage playModeStage) {
        masterInputMultiplexer.removeProcessor(playModeStage);
        masterInputMultiplexer.addProcessor(mainStage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        playModeStage.addAction(Actions.fadeOut(.25f, Interpolation.fastSlow));
        playModeStage.addAction(Actions.moveTo(0, -playModeStage.getHeight(), .25f, Interpolation.fastSlow));
        mainStage.addAction(Actions.fadeIn(.25f, Interpolation.fastSlow));
    }

    public void startGameOverAnimation(final Stage stage, final ParticleEffect trailEffect) {
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        float screenWidth = stage.getWidth();

        stage.getRoot().getColor().a = 1;
        stage.getRoot().setPosition(screenWidth, 0);

        SequenceAction slideAnimation = new SequenceAction();
        slideAnimation.addAction(Actions.run(() -> trailEffect.start()));
        slideAnimation.addAction(Actions.delay(.4f));
        slideAnimation.addAction(Actions.run(() -> {
            trailEffect.allowCompletion(); // Completion takes time which is why it's done before the move actions
        }));
        slideAnimation.addAction(Actions.moveTo(screenWidth / 8, 0f, .25f, Interpolation.pow2));
        slideAnimation.addAction(Actions.moveTo(-screenWidth / 8, 0f, 1.6f, Interpolation.linear));
        slideAnimation.addAction(Actions.moveTo(-screenWidth, 0f, .25f, Interpolation.pow2));
        slideAnimation.addAction(Actions.run(() -> {
            masterInputMultiplexer.addProcessor(stage);
            Gdx.input.setInputProcessor(masterInputMultiplexer);

            if (animationListener != null) {
                animationListener.animationEnd(stage);
            }
        }));

        stage.addAction(slideAnimation);
    }

    public void startExtendedMapSelectorAnimation(FocusableStage mainStage, FocusableStage mapSelectorStage, float fadeOutAlpha) {
        masterInputMultiplexer.removeProcessor(mainStage);
        masterInputMultiplexer.addProcessor(mapSelectorStage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        mainStage.addAction(Actions.alpha(fadeOutAlpha, .25f, Interpolation.fastSlow));
        mapSelectorStage.addAction(Actions.fadeIn(.25f, Interpolation.fastSlow));
    }

    public void endExtendedMapSelectorAnimation(FocusableStage mainStage, FocusableStage mapSelectorStage) {
        masterInputMultiplexer.removeProcessor(mapSelectorStage);
        masterInputMultiplexer.addProcessor(mainStage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        mapSelectorStage.addAction(Actions.fadeOut(.25f, Interpolation.fastSlow));
        mainStage.addAction(Actions.fadeIn(.25f, Interpolation.fastSlow));
    }

    public void fadeInStage(final Stage stage) {
        SequenceAction fadeInAnimation = new SequenceAction();
        fadeInAnimation.addAction(Actions.fadeIn(.3f, Interpolation.smooth));
        fadeInAnimation.addAction(Actions.run(() -> {
            masterInputMultiplexer.addProcessor(stage);
            Gdx.input.setInputProcessor(masterInputMultiplexer);

            if (animationListener != null) {
                animationListener.animationEnd(stage);
            }
        }));

        stage.getRoot().getColor().a = 0;
        stage.addAction(fadeInAnimation);
    }

    public void fadeOutStage(final Stage stage, final Screen fromScreen, final Screen toScreen) {
        masterInputMultiplexer.removeProcessor(stage);
        Gdx.input.setInputProcessor(masterInputMultiplexer);

        SequenceAction fadeOutAnimation = new SequenceAction();
        fadeOutAnimation.addAction(Actions.alpha(0f, .45f, Interpolation.smooth));
        // Crashes on GWT without the postRunnable
        fadeOutAnimation.addAction(Actions.run(() -> Gdx.app.postRunnable(() -> {
            Gdx.input.setInputProcessor(null);
            fromScreen.dispose();
            game.setScreen(toScreen);
            if (animationListener != null) {
                animationListener.animationEnd(stage);
            }
        })));

        stage.addAction(fadeOutAnimation);
    }

    public void startScoreBoardAnimation(final Stage stage, Container<Label> scoreBoardTitle, final Actor[][] actors, float rowHeightScale, float padding) {
        for (Actor[] rowActors : actors) {
            for (Actor actor : rowActors) {
                actor.getColor().a = 0f;
            }
        }

        // TODO: Tweak animations to make it more natural

        float moveAmount = ((((scoreBoardTitle.getActor().getPrefHeight() * rowHeightScale) + padding) * actors.length) / 2) + (padding * 2);
        scoreBoardTitle.addAction(Actions.sequence(
                Actions.moveBy(0, -moveAmount, .1f, Interpolation.exp10),
                Actions.fadeIn(.7f, Interpolation.smoother),
                Actions.moveBy(0, moveAmount, .2f * actors.length, Interpolation.smoother),
                Actions.run(() -> {
                    for (int i = 0; i < actors.length; i++) {
                        for (int j = 0; j < actors[i].length; j++) {
                            float moveAmount1 = actors[i][j].getHeight();

                            actors[i][j].addAction(Actions.moveBy(0, moveAmount1, .1f, Interpolation.exp10));

                            ParallelAction fadeFallInAnimation = new ParallelAction();
                            fadeFallInAnimation.addAction(Actions.fadeIn((i + 1) * .2f, Interpolation.smooth));
                            fadeFallInAnimation.addAction(Actions.moveBy(0, -moveAmount1, (i + 1) * .1f, Interpolation.smooth));

                            if (i == actors.length - 1 && j == actors[i].length - 1) {
                                SequenceAction finalFadeZoomOutAnimation = new SequenceAction();
                                finalFadeZoomOutAnimation.addAction(fadeFallInAnimation);
                                finalFadeZoomOutAnimation.addAction(Actions.run(() -> {
                                    if (animationListener != null) {
                                        animationListener.animationEnd(stage);
                                    }
                                }));

                                actors[i][j].addAction(finalFadeZoomOutAnimation);
                            } else {
                                actors[i][j].addAction(fadeFallInAnimation);
                            }
                        }
                    }
                })
        ));
    }

    public void setAnimationListener(AnimationListener animationListener) {
        this.animationListener = animationListener;
    }

    public interface AnimationListener {
        void animationEnd(Stage stage);
    }
}
