package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Player;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.PlayerManager;
import com.cg.zoned.ui.FocusableStage;

import java.text.DecimalFormat;

public class VictoryScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ParticleEffect trailEffect;

    private String victoryString;

    public VictoryScreen(final Zoned game, PlayerManager playerManager, int rows, int cols) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        getVictoryString(playerManager, rows, cols);
    }

    @Override
    public void show() {
        setUpStage();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        trailEffect = new ParticleEffect();
        trailEffect.setPosition(0, Gdx.graphics.getHeight() / 2f);
        trailEffect.load(Gdx.files.internal("particles/trails.p"), Gdx.files.internal("particles"));

        animationManager.startGameOverAnimation(stage, trailEffect);
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                stage.clear();
                setUpVictoryUI();
                stage.getRoot().setPosition(0, 0);
                animationManager.setAnimationListener(null);
                animationManager.fadeInStage(stage);
            }
        });
    }

    private void setUpStage() {
        Table table = new Table();
        //table.setDebug(true);
        table.setFillParent(true);
        table.center();

        Label gameOver = new Label("GAME OVER", game.skin, Constants.FONT_MANAGER.LARGE.getName(), Color.GREEN);
        table.add(gameOver);

        stage.addActor(table);
    }

    private void setUpVictoryUI() {
        Table table = new Table();
        //table.setDebug(true);
        table.setFillParent(true);
        table.center();

        Label victoryLabel = new Label(victoryString, game.skin, "themed");
        victoryLabel.setAlignment(Align.center);
        TextButton returnToMainMenuButton = new TextButton("Return to the main menu", game.skin);
        returnToMainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.fadeOutStage(stage, new MainMenuScreen(game));
            }
        });
        table.add(victoryLabel);
        table.row();
        table.add(returnToMainMenuButton).pad(10 * game.getScaleFactor()).width(350 * game.getScaleFactor());

        stage.addActor(table);
        stage.addFocusableActor(returnToMainMenuButton);
        stage.setFocusedActor(returnToMainMenuButton);
    }

    private void getVictoryString(PlayerManager playerManager, int rows, int cols) {
        Player[] players = playerManager.getPlayers();
        StringBuilder stringBuilder = new StringBuilder();

        int highScore = 0;
        String winner = "";
        for (int i = 0; i < players.length; i++) {
            int score = playerManager.getPlayerScore(i);

            if (score > highScore) {
                highScore = score;
                winner = players[i].name;
            }

            double capturePercentage = 100 * (score / ((double) rows * cols));
            DecimalFormat df = new DecimalFormat("#.##");
            capturePercentage = Double.parseDouble(df.format(capturePercentage));

            stringBuilder.append(players[i].name).append(": ")
                    .append(score)
                    .append(" (").append(capturePercentage).append(" %)");
            if (i != players.length - 1) {
                stringBuilder.append('\n');
            }
        }

        victoryString = winner + " won with a score of " + highScore + "\n" + stringBuilder.toString();
        Gdx.app.log(Constants.LOG_TAG, winner + " won with a score of " + highScore);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        trailEffect.setPosition(0, height / 2f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);

        stage.getBatch().begin();
        trailEffect.draw(stage.getBatch(), delta);
        stage.getBatch().end();

        if (showFPSCounter) {
            FPSDisplayer.displayFPS(stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void dispose() {
        stage.dispose();
        trailEffect.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            animationManager.fadeOutStage(stage, new MainMenuScreen(game));
            return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
