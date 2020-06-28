package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.TeamData;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.PlayerManager;
import com.cg.zoned.ui.FocusableStage;

import java.text.DecimalFormat;

public class VictoryScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ParticleEffect trailEffect;

    private Array<TeamData> teamData;
    private String[] victoryStrings;

    private Table[] tableRows;

    public VictoryScreen(final Zoned game, PlayerManager playerManager, int rows, int cols, int wallCount) {
        this.game = game;
        game.discordRPCManager.updateRPC("Post match");

        this.usedTextures = new Array<>();

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        getVictoryStrings(playerManager, rows, cols, wallCount);
    }

    @Override
    public void show() {
        setUpStage();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        trailEffect = new ParticleEffect();
        trailEffect.setPosition(0, stage.getHeight() / 2f);
        trailEffect.load(Gdx.files.internal("particles/trails.p"), Gdx.files.internal("particles"));

        animationManager.startGameOverAnimation(stage, trailEffect);
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                stage.clear();
                setUpVictoryUI();
                stage.getRoot().setPosition(0, 0);
                animationManager.setAnimationListener(null);
                animationManager.startVictoryAnimation(stage, tableRows);
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
        Table masterTable = new Table();
        //table.setDebug(true);
        masterTable.setFillParent(true);
        masterTable.center();

        Table table = new Table();
        table.center();
        table.pad(20f);
        ScrollPane screenScrollPane = new ScrollPane(table);
        screenScrollPane.setOverscroll(false, true);

        StringBuilder victoryString = new StringBuilder();
        for (String victoryStr : victoryStrings) {
            victoryString.append(victoryStr).append("\n");
        }

        Color[] rankColors = new Color[4];
        rankColors[0] = Color.GOLD;
        rankColors[1] = Color.LIGHT_GRAY;
        rankColors[2] = Color.BROWN;
        rankColors[3] = Color.GRAY;

        String[] rankImageLocations = new String[]{
                "icons/rank_icons/ic_no1.png",
                "icons/rank_icons/ic_no2.png",
                "icons/rank_icons/ic_no3.png",
        };
        Texture[] rankImageTextures = new Texture[rankImageLocations.length];
        for (int i = 0; i < rankImageTextures.length; i++) {
            rankImageTextures[i] = new Texture(Gdx.files.internal(rankImageLocations[i]));
            usedTextures.add(rankImageTextures[i]);
        }
        Image[] rankImages = new Image[victoryStrings.length];
        Label[] victoryLabels = new Label[victoryStrings.length];
        Label[] rankLabels = new Label[victoryStrings.length];
        float rankLabelMaxWidth = 0;
        int rankIndex = 0;
        for (int i = 0; i < victoryStrings.length; i++, rankIndex++) {
            if (i > 0 && teamData.get(i - 1).score == teamData.get(i).score) {
                rankIndex--;
            }
            rankLabels[i] = new Label("#" + (rankIndex + 1), game.skin, Constants.FONT_MANAGER.REGULAR.getName(), rankColors[Math.min(rankIndex, 3)]);
            victoryLabels[i] = new Label(victoryStrings[i], game.skin, Constants.FONT_MANAGER.REGULAR.getName(), rankColors[Math.min(rankIndex, 3)]);
            if (rankIndex < rankImageLocations.length) {
                rankImages[i] = new Image(rankImageTextures[rankIndex]);
            } else {
                rankImages[i] = null;
            }

            if (rankLabels[i].getPrefWidth() > rankLabelMaxWidth) {
                rankLabelMaxWidth = rankLabels[i].getPrefWidth();
            }
        }

        tableRows = new Table[victoryStrings.length];
        for (int i = 0; i < victoryStrings.length; i++) {
            tableRows[i] = new Table();

            tableRows[i].add(rankLabels[i]).space(20f).left().width(rankLabelMaxWidth);
            if (rankImages[i] != null) {
                tableRows[i].add(rankImages[i]).height(victoryLabels[0].getPrefHeight() * 1.5f).width(victoryLabels[0].getPrefHeight() * 1.5f).space(20f);
                tableRows[i].add(victoryLabels[i]).right().space(20f).expandX();
            } else {
                tableRows[i].add(victoryLabels[i]).padLeft(victoryLabels[0].getPrefHeight() + 30f).right().space(20f).expandX();
            }

            table.add(tableRows[i]).space(20f).padLeft(20f).padRight(20f).uniform().grow();
            table.row();
        }

        TextButton returnToMainMenuButton = new TextButton("Return to the main menu", game.skin);
        returnToMainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.fadeOutStage(stage, VictoryScreen.this, new MainMenuScreen(game));
            }
        });
        table.add(returnToMainMenuButton).pad(10 * game.getScaleFactor()).width(350 * game.getScaleFactor());

        masterTable.add(screenScrollPane);

        stage.addFocusableActor(returnToMainMenuButton);
        stage.setScrollFocus(screenScrollPane);

        stage.addActor(masterTable);
    }

    private void getVictoryStrings(PlayerManager playerManager, int rows, int cols, int wallCount) {
        teamData = playerManager.getTeamData();
        this.victoryStrings = new String[teamData.size];

        DecimalFormat df = new DecimalFormat("#.##");
        for (int i = 0; i < teamData.size; i++) {
            double capturePercentage = 100 * (teamData.get(i).score / (((double) rows * cols) - wallCount));
            capturePercentage = Double.parseDouble(df.format(capturePercentage));

            this.victoryStrings[i] = PlayerColorHelper.getStringFromColor(teamData.get(i).color)
                    + " got a score of " + teamData.get(i).score + " (" + capturePercentage + "%)";
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
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
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void dispose() {
        stage.dispose();
        trailEffect.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    public void onBackPressed() {
        animationManager.fadeOutStage(stage, this, new MainMenuScreen(game));
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            onBackPressed();
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
        if (button == Input.Buttons.BACK) {
            onBackPressed();
            return true;
        }

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