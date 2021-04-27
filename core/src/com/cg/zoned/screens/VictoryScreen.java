package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import com.cg.zoned.Assets;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.TeamData;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.HoverImageButton;

import java.text.DecimalFormat;
import java.util.Comparator;

public class VictoryScreen extends ScreenObject implements InputProcessor {
    private ParticleEffect trailEffect;

    private Array<TeamData> teamData;

    private Actor[][] scoreboardActors;
    private Container<Label> scoreBoardTitleContainer;
    private float rowHeightScale = 1.5f;
    private float padding;

    public VictoryScreen(final Zoned game, Array<TeamData> teamData) {
        super(game);
        game.discordRPCManager.updateRPC("Post match");

        this.padding = 16f * game.getScaleFactor();

        this.animationManager = new AnimationManager(this.game, this);
        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        this.teamData = teamData;
        finalizeTeamData();
    }

    private void finalizeTeamData() {
        new Sort().sort(teamData, new TeamDataComparator());

        DecimalFormat df = new DecimalFormat("#.##");
        for (TeamData td : teamData) {
            td.roundCapturePercentage(df);
        }
    }

    @Override
    public void show() {
        setUpStage();

        trailEffect = new ParticleEffect();
        trailEffect.setPosition(0, screenStage.getHeight() / 2f);
        trailEffect.load(Gdx.files.internal("particles/trails.p"), Gdx.files.internal("particles"));

        animationManager.startGameOverAnimation(screenStage, trailEffect);
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

        screenStage.addActor(table);
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

        RankData[] rankData = new RankData[]{
                new RankData(Color.GOLD,       "images/rank_icons/ic_no1.png"),
                new RankData(Color.LIGHT_GRAY, "images/rank_icons/ic_no2.png"),
                new RankData(Color.BROWN,      "images/rank_icons/ic_no3.png"),
                new RankData(Color.GRAY,       null),
        };

        scoreboardActors = new Actor[teamData.size + 1][];
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
        for (int i = 1; i < teamData.size + 1; i++, rankIndex++) {
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

            Label victoryLabel = new Label(teamData.get(i - 1).getScore() + " (" + teamData.get(i - 1).getCapturePercentage() + "%)", game.skin,
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

        screenStage.setScrollFocus(screenScrollPane);
        screenStage.addActor(masterTable);

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

    private void setUpNextButton() {
        HoverImageButton nextButton = uiButtonManager.addNextButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        nextButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.fadeOutStage(screenStage, VictoryScreen.this, new MainMenuScreen(game));
            }
        });
        screenStage.addFocusableActor(nextButton);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        trailEffect.setPosition(0, height / 2f);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        screenStage.getBatch().begin();
        trailEffect.draw(screenStage.getBatch(), delta);
        screenStage.getBatch().end();

        screenStage.draw();
        screenStage.act(delta);

        displayFPS();
    }

    @Override
    public void dispose() {
        super.dispose();
        trailEffect.dispose();
    }

    /**
     * Actions to do when the back/escape button is pressed
     *
     * @return true if the action has been handled from this screen
     *         false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (screenStage.dialogIsActive()) {
            return false;
        }

        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
        return true;
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
            return onBackPressed();
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
            return onBackPressed();
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}