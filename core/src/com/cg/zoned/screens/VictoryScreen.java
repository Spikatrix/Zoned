package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Sort;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.TeamData;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.PlayerManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

import java.text.DecimalFormat;
import java.util.Comparator;

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
    private double[] capturePercentage;

    private Actor[][] scoreboardActors;
    private Container<Label> scoreBoardTitleContainer;
    private float rowHeightScale = 1.5f;
    private float padding;

    public VictoryScreen(final Zoned game, PlayerManager playerManager, int rows, int cols, int wallCount) {
        this.game = game;
        game.discordRPCManager.updateRPC("Post match");

        this.padding = 16f * game.getScaleFactor();
        this.usedTextures = new Array<>();

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Assets.FontManager.SMALL.getFontName());

        getVictoryStrings(playerManager, rows, cols, wallCount);
    }

    @Override
    public void show() {
        setUpStage();
        showFPSCounter = game.preferences.getBoolean(Preferences.FPS_PREFERENCE, false);

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
                animationManager.startScoreBoardAnimation(stage, scoreBoardTitleContainer, scoreboardActors, rowHeightScale, padding);
            }
        });
    }

    private void setUpStage() {
        Table table = new Table();
        //table.setDebug(true);
        table.setFillParent(true);
        table.center();

        Label gameOver = new Label("GAME OVER", game.skin, Assets.FontManager.STYLED_LARGE.getFontName(), Color.GREEN);
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

        ScoreboardHeaderData[] headers = new ScoreboardHeaderData[]{
                new ScoreboardHeaderData("RANK",  2.5f),
                new ScoreboardHeaderData("TEAM",  4.5f),
                new ScoreboardHeaderData("SCORE", 2.5f),
        };

        RankData[] rankData = new RankData[] {
                new RankData(Color.GOLD,       "icons/rank_icons/ic_no1.png"),
                new RankData(Color.LIGHT_GRAY, "icons/rank_icons/ic_no2.png"),
                new RankData(Color.BROWN,      "icons/rank_icons/ic_no3.png"),
                new RankData(Color.GRAY,       null),
        };

        scoreboardActors = new Actor[capturePercentage.length + 1][];
        float rowHeight = getRowHeight();

        // Setting background drawable on a container since setting it directly to the label means
        // I'll have to reset it otherwise other labels will have it as well since it's the same object
        Label scoreBoardTitle = new Label("SCOREBOARD", game.skin, Assets.FontManager.STYLED_SMALL.getFontName(), Color.WHITE);
        scoreBoardTitle.setAlignment(Align.center);
        scoreBoardTitleContainer = new Container<>(scoreBoardTitle);
        scoreBoardTitleContainer.fill();
        scoreBoardTitleContainer.setBackground(getDrawable(new Color(1, 1, 1, .2f)));
        scoreBoardTitleContainer.getColor().a = 0;
        table.add(scoreBoardTitleContainer).space(padding).growX().uniform().height(rowHeight).colspan(headers.length);
        table.row();

        scoreboardActors[0] = new Actor[headers.length];
        for (int i = 0; i < headers.length; i++) {
            ScoreboardHeaderData headerData = headers[i];

            Label headerLabel = new Label(headerData.headerStr, game.skin);
            headerLabel.setAlignment(Align.center);
            Container<Label> headerContainer = new Container<>(headerLabel);
            headerContainer.fill();
            headerContainer.setBackground(getDrawable(new Color(1, 1, 1, .2f)));
            headerContainer.getColor().a = 0;
            table.add(headerContainer).width((rowHeight * headerData.sizeMultiplier) + padding).uniformY().growY().space(padding);

            scoreboardActors[0][i] = headerContainer;
        }
        table.row();

        int rankIndex = 0;
        for (int i = 1; i < capturePercentage.length + 1; i++, rankIndex++) {
            if (i > 1 && teamData.get(i - 2).getScore() == teamData.get(i - 1).getScore()) {
                rankIndex--;
            }

            int rowIndex = 0;
            int rankDataIndex = Math.min(rankIndex, rankData.length - 1);

            scoreboardActors[i] = new Actor[headers.length];

            // Rank image and label stack
            Stack rankStack;
            Image rankImage = null;
            Label rankLabel = new Label(Integer.toString(rankIndex + 1), game.skin,
                    Assets.FontManager.STYLED_SMALL.getFontName(), rankData[rankDataIndex].rankColor);
            rankLabel.setAlignment(Align.center);
            if (rankData[rankDataIndex].rankTexture != null) {
                rankImage = new Image(rankData[rankDataIndex].rankTexture);
                rankImage.setScaling(Scaling.fit);
                rankImage.getColor().a = .5f;
            }
            if (rankImage != null) {
                rankStack = new Stack(rankImage, rankLabel);
            } else {
                rankStack = new Stack(rankLabel);
            }

            scoreboardActors[i][rowIndex++] = rankStack;

            Label nameLabel = new Label(PlayerColorHelper.getStringFromColor(teamData.get(i - 1).getColor()), game.skin,
                    Assets.FontManager.REGULAR.getFontName(), teamData.get(i - 1).getColor());
            nameLabel.setAlignment(Align.center);

            scoreboardActors[i][rowIndex++] = nameLabel;

            Label victoryLabel = new Label(teamData.get(i - 1).getScore() + " (" + capturePercentage[i - 1] + "%)", game.skin,
                    Assets.FontManager.REGULAR.getFontName(), rankData[rankDataIndex].rankColor);
            victoryLabel.setAlignment(Align.center);

            scoreboardActors[i][rowIndex++] = victoryLabel;

            for (int j = 0; j < headers.length; j++) {
                if (scoreboardActors[i][j] != null) {
                    table.add(scoreboardActors[i][j]).space(padding).uniformY().height(rowHeight).width(rowHeight * headers[j].sizeMultiplier);
                } else {
                    table.add().space(padding).uniformY();
                }
            }
            table.row();
        }

        masterTable.add(screenScrollPane);

        stage.setScrollFocus(screenScrollPane);
        stage.addActor(masterTable);

        setUpNextButton();
    }

    private Drawable getDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA4444);
        pixmap.setColor(color);
        pixmap.drawPixel(0, 0);

        Texture bgTexture = new Texture(pixmap);
        usedTextures.add(bgTexture);

        pixmap.dispose();

        return new TextureRegionDrawable(bgTexture);
    }

    private float getRowHeight() {
        Label dummyLabel = new Label("DUMMY", game.skin);
        return dummyLabel.getPrefHeight() * rowHeightScale;
    }

    private void getVictoryStrings(PlayerManager playerManager, int rows, int cols, int wallCount) {
        teamData = playerManager.getTeamData();
        new Sort().sort(teamData, new TeamDataComparator());
        this.capturePercentage = new double[teamData.size];

        DecimalFormat df = new DecimalFormat("#.##");
        for (int i = 0; i < teamData.size; i++) {
            double capturePercentage = 100 * (teamData.get(i).getScore() / (((double) rows * cols) - wallCount));
            capturePercentage = Double.parseDouble(df.format(capturePercentage));

            this.capturePercentage[i] = capturePercentage;
        }
    }

    private void setUpNextButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton nextButton = uiButtonManager.addNextButtonToStage(game.assets.getBackButtonTexture());
        nextButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.fadeOutStage(stage, VictoryScreen.this, new MainMenuScreen(game));
            }
        });
        stage.addFocusableActor(nextButton);
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

    private static class TeamDataComparator implements Comparator<TeamData> {
        @Override
        public int compare(TeamData t1, TeamData t2) {
            return t2.getScore() - t1.getScore();
        }
    }

    private static class ScoreboardHeaderData {
        private String headerStr;
        private float sizeMultiplier;

        public ScoreboardHeaderData(String headerStr, float sizeMultiplier) {
            this.headerStr = headerStr;
            this.sizeMultiplier = sizeMultiplier;
        }
    }

    private class RankData {
        private Color rankColor;
        private Texture rankTexture;

        public RankData(Color rankColor, String rankImageLocation) {
            this.rankColor = rankColor;

            if (rankImageLocation != null) {
                this.rankTexture = new Texture(Gdx.files.internal(rankImageLocation));
                usedTextures.add(this.rankTexture);
            }
        }
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