package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Player;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;
import com.cg.zoned.maps.NoStartPositionsFound;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

// Used for testing purposes, nvm about the crap in here
public class TestScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ScreenViewport viewport;
    private FocusableStage stage;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private BitmapFont font;

    public TestScreen(final Zoned game) {
        this.game = game;

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.batch = new SpriteBatch();
        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();

        InputMultiplexer inputMultiplexer = new InputMultiplexer(stage, this);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setUpStage() {
        Table table = new Table();
        table.center();
        table.setFillParent(true);

        final MapManager mapManager = new MapManager();

        final Spinner spinner = new Spinner(game.skin,
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight() * 3,
                150f * game.getScaleFactor(), false);
        for (final MapEntity map : mapManager.getMapList()) {
            spinner.addContent(getMapStack(mapManager, map));
        }
        spinner.getLeftButton().setText(" < ");
        spinner.getRightButton().setText(" > ");
        spinner.setButtonStepCount(1);
        table.add(spinner);
        table.row();
        stage.addFocusableActor(spinner.getLeftButton());
        stage.addFocusableActor(spinner.getRightButton());
        stage.row();

        TextButton loadButton = new TextButton("Load map", game.skin);
        final DropDownMenu startPositionMenu = new DropDownMenu(game.skin);
        loadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int mapIndex = spinner.getPositionIndex();

                Array<String> buttonTexts = new Array<>();
                buttonTexts.add("OK");

                try {
                    mapManager.prepareMap(mapIndex);
                } catch (InvalidMapCharacter | NoStartPositionsFound | InvalidMapDimensions e) {
                    e.printStackTrace();
                    stage.showDialog("Error: " + e.getMessage(), buttonTexts, false, game.getScaleFactor(), null, game.skin);
                    return;
                }

                Array<GridPoint2> startPositions = mapManager.getPreparedStartPositions();
                for (GridPoint2 startPosition : startPositions) {
                    try {
                        startPositionMenu.append("(" + startPosition.x + ", " + (mapManager.getPreparedMapGrid().length - startPosition.y - 1) + ")");
                    } catch (NullPointerException e) {
                        stage.showDialog("Error: Please define the start positions in order starting from '" + MapManager.VALID_START_POSITIONS.charAt(0) + "'", buttonTexts, false, game.getScaleFactor(), null, game.skin);
                        return;
                    }
                }
            }
        });
        table.add(loadButton);
        table.row();
        stage.addFocusableActor(loadButton, 2);
        stage.row();

        TextButton startGameButton = new TextButton("Start Game", game.skin);
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Player p1 = new Player(Constants.PLAYER_COLORS.get("GREEN"), "p1");
                p1.setControlIndex(0);
                Player p2 = new Player(Constants.PLAYER_COLORS.get("RED"), "p2");
                Player p3 = new Player(Constants.PLAYER_COLORS.get("PINK"), "p3");
                Player p4 = new Player(Constants.PLAYER_COLORS.get("BLUE"), "p4");
                Player p5 = new Player(Constants.PLAYER_COLORS.get("YELLOW"), "p5");
                p2.setControlIndex(1);
                dispose();
                /*game.setScreen(
                        new GameScreen(game,
                                mapManager,
                                new Player[]{p1, p2},
                                null, null));*/
                game.setScreen(new MapStartPosScreen(game, mapManager,
                        new Player[]{p1, p2}, 2, false));
            }
        });
        table.add(startGameButton);
        table.row();
        stage.addFocusableActor(startGameButton, 2);
        stage.row();

        table.add(startPositionMenu);
        table.row();

        stage.addActor(table);

        checkForExternalMaps(mapManager, spinner);
    }

    private Stack getMapStack(MapManager mapManager, final MapEntity map) {
        Stack stack = new Stack();

        Label mapNameLabel = new Label(map.getName(), game.skin);
        mapNameLabel.setAlignment(Align.center);
        Texture mapPreviewTexture = mapManager.getMapPreview(map.getName());
        if (mapPreviewTexture != null) {
            usedTextures.add(mapPreviewTexture);
            Image mapPreviewImage = new Image(mapPreviewTexture);
            mapPreviewImage.getColor().a = .2f;
            mapPreviewImage.setScaling(Scaling.fit);
            stack.add(mapPreviewImage);
            stack.add(mapNameLabel);
        } else {
            stack.add(mapNameLabel);
        }

        final MapExtraParams extraParams = map.getExtraParamPrompts();
        if (extraParams != null) {
            Table innerTable = new Table();
            innerTable.setFillParent(true);
            innerTable.top().right();

            Texture texture = new Texture(Gdx.files.internal("icons/ui_icons/ic_settings.png"));
            usedTextures.add(texture);
            HoverImageButton hoverImageButton = new HoverImageButton(new TextureRegionDrawable(texture));
            hoverImageButton.getImage().setScaling(Scaling.fit);
            innerTable.add(hoverImageButton)
                    .width(hoverImageButton.getPrefWidth() * .6f * game.getScaleFactor())
                    .height(hoverImageButton.getPrefHeight() * 0.6f * game.getScaleFactor());
            stack.add(innerTable);

            hoverImageButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showExtraParamDialog(extraParams, map);
                }
            });
        }

        return stack;
    }

    private void checkForExternalMaps(final MapManager mapManager, final Spinner spinner) {
        mapManager.loadExternalMaps(new MapManager.OnExternalMapLoadListener() {
            @Override
            public void onExternalMapLoaded(final Array<MapEntity> mapList, final int externalMapStartIndex) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = externalMapStartIndex; i < mapList.size; i++) {
                            spinner.addContent(getMapStack(mapManager, mapList.get(i)));
                        }
                    }
                });
            }
        });
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage();
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    private void showExtraParamDialog(final MapExtraParams prompts, final MapEntity map) {
        final Spinner[] spinners = new Spinner[prompts.spinnerVars.size];

        Table contentTable = new Table();
        contentTable.center();
        for (int i = 0; i < prompts.spinnerVars.size + 1; i++) {
            Label label;
            if (i == 0) {
                /* Title */

                label = new Label(prompts.paramSelectTitle, game.skin, "themed");
                label.setAlignment(Align.center);
                contentTable.add(label).colspan(2).padBottom(10f * game.getScaleFactor());
            } else {
                /* Params */

                int lowValue = prompts.spinnerVars.get(i - 1).lowValue;
                int highValue = prompts.spinnerVars.get(i - 1).highValue;
                int snapValue = prompts.spinnerVars.get(i - 1).snapValue;

                label = new Label(prompts.spinnerVars.get(i - 1).prompt, game.skin);
                spinners[i - 1] = new Spinner(game.skin, game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight(),
                        64f * game.getScaleFactor(), true);
                spinners[i - 1].generateValueRange(lowValue, highValue, game.skin);
                spinners[i - 1].snapToStep(snapValue - lowValue);
                contentTable.add(label).left();
                contentTable.add(spinners[i - 1]);
            }
            contentTable.row();
        }

        Array<String> buttonTexts = new Array<>();
        buttonTexts.add("Cancel");
        buttonTexts.add("Set");

        stage.showDialog(contentTable, buttonTexts, false, game.getScaleFactor(),
                new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(String buttonText) {
                        if (buttonText.equals("Set")) {
                            for (int i = 0; i < prompts.spinnerVars.size; i++) {
                                prompts.extraParams[i] = spinners[i].getPositionIndex() + prompts.spinnerVars.get(i).lowValue;
                            }
                            map.applyExtraParams();
                        }
                    }
                }, game.skin);
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.viewport.apply(true);

        stage.act(delta);
        stage.draw();

        renderer.setProjectionMatrix(this.viewport.getCamera().combined);
        batch.setProjectionMatrix(this.viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.end();

        batch.begin();
        batch.end();

        FPSDisplayer.displayFPS(viewport, batch, font);
    }

    @Override
    public void dispose() {
        stage.dispose();
        renderer.dispose();
        batch.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        dispose();
        game.setScreen(new MainMenuScreen(game));
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
