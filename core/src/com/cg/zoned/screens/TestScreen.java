package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.AnimatedDrawable;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Player;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

// Used for testing purposes, nvm about the crap in here
public class TestScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<Texture>();

    private ScreenViewport viewport;
    private Stage stage;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private BitmapFont font;

    public TestScreen(final Zoned game) {
        this.game = game;

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.batch = new SpriteBatch();
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport);
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
        if (mapManager.getErrorMessage() != null) {
            Label errorMessage = new Label(mapManager.getErrorMessage(), game.skin, "themed");
            table.add(errorMessage);
        } else {
            final Spinner spinner = new Spinner(game.skin,
                    game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight() * 3,
                    150f * game.getScaleFactor(), false);
            for (MapEntity map : mapManager.getMapList()) {
                Label mapNameLabel = new Label(map.getName(), game.skin);
                mapNameLabel.setAlignment(Align.center);
                Texture mapPreviewTexture = mapManager.getMapPreview(map.getName());
                Stack stack = new Stack();
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
                if (map.getName().equals("Rectangle")) {
                    Table innerTable = new Table();
                    innerTable.setFillParent(true);
                    innerTable.top().right();

                    HoverImageButton hoverImageButton = new HoverImageButton(new AnimatedDrawable(getAnimationSettingsDrawable()));
                    hoverImageButton.getImage().setScaling(Scaling.fit);
                    hoverImageButton.setPosition(0, 0);
                    innerTable.add(hoverImageButton).width(20).height(20);
                    stack.add(innerTable);
                }
                spinner.addContent(stack);
            }
            spinner.getLeftButton().setText(" < ");
            spinner.getRightButton().setText(" > ");
            spinner.setButtonStepCount(1);
            table.add(spinner);
            table.row();

            TextButton loadButton = new TextButton("Load map", game.skin);
            final DropDownMenu startPositionMenu = new DropDownMenu(game.skin);
            loadButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    int mapIndex = spinner.getPositionIndex();
                    try {
                        mapManager.prepareMap(mapIndex, null);
                    } catch (InvalidMapCharacter e) {
                        e.printStackTrace();
                        return;
                    }
                    Array<GridPoint2> startPositions = mapManager.getPreparedStartPositions();
                    for (GridPoint2 startPosition : startPositions) {
                        startPositionMenu.append("(" + startPosition.x + ", " + startPosition.y + ")");
                    }
                }
            });
            table.add(loadButton);
            table.row();

            TextButton startGameButton = new TextButton("Start Game", game.skin);
            startGameButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Player p1 = new Player(Constants.PLAYER_COLORS.get("GREEN"), "p1");
                    p1.setControlIndex(0);
                    Player p2 = new Player(Constants.PLAYER_COLORS.get("RED"), "p2");
                    p2.setControlIndex(1);
                    dispose();
                    game.setScreen(
                            new GameScreen(game,
                                    mapManager.getPreparedMapGrid(),
                                    mapManager.getPreparedStartPositions(),
                                    mapManager.getWallCount(),
                                    new Player[]{p1, p2},
                                    null, null));
                }
            });
            table.add(startGameButton);
            table.row();

            table.add(startPositionMenu);
            table.row();
        }

        stage.addActor(table);
    }

    private Animation getAnimationSettingsDrawable() {
        Texture settingsSheet = new Texture(Gdx.files.internal("icons/ui_icons/ic_settings_sheet.png"));
        usedTextures.add(settingsSheet);
        int rowCount = 10, colCount = 3;

        TextureRegion[][] tmp = TextureRegion.split(settingsSheet,
                settingsSheet.getWidth() / colCount,
                settingsSheet.getHeight() / rowCount);

        TextureRegion[] playFrames = new TextureRegion[rowCount * colCount];
        int index = 0;
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                playFrames[index++] = tmp[i][j];
            }
        }

        return new Animation<TextureRegion>(1 / 40f, playFrames);
    }

    private void setUpBackButton() {
        HoverImageButton backButton = UIButtonManager.addBackButtonToStage(stage, game.getScaleFactor(), usedTextures);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        this.viewport.update(width, height, true);
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
