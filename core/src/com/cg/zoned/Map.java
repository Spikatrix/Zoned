package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.TeamData;
import com.cg.zoned.managers.PlayerManager;

import space.earlygrey.shapedrawer.JoinType;

public class Map {
    private final Cell[][] mapGrid;
    public int wallCount;

    public int rows;
    public int cols;
    private int coloredCellCount = 0;

    private boolean playerMoved = true;

    private Rectangle userViewRect;

    private FrameBuffer playerLabelFbo = null;
    public static float playerLabelRegionScale = 2f; // Rendered at higher (2x rn) res, downscaled to required size
    private TextureRegion[] playerLabels = null;

    private FrameBuffer playerFbo = null;
    public static float playerTextureRegionScale = 2f; // Rendered at higher (2x rn) res, downscaled to required size
    private TextureRegion playerTextureRegion = null;

    private FrameBuffer mapFbo = null;
    private TextureRegion mapTextureRegion = null;
    private FloodFiller floodfiller = null;

    /**
     * Creates the map object with features like processing each turn and managing score, rendering
     * the grid and more
     *
     * @param mapGrid     2D array of {@link Cell}s of the map grid
     * @param wallCount   Amount of walls in the grid; used for determining the winner early in 2 player games
     * @param shapeDrawer Used to prepare the grid and player textures for rendering
     */
    public Map(Cell[][] mapGrid, int wallCount, ShapeDrawer shapeDrawer) {
        this.rows = mapGrid.length;
        this.cols = mapGrid[0].length;
        this.mapGrid = mapGrid;
        this.wallCount = wallCount;

        userViewRect = new Rectangle();

        createPlayerTexture(shapeDrawer);
        createMapTexture(shapeDrawer);
    }

    public Map(Cell[][] mapGrid, ShapeDrawer shapeDrawer) {
        this(mapGrid, 0, shapeDrawer);
    }

    public void initFloodFillVars() {
        this.floodfiller = new FloodFiller(this.rows, this.cols);
    }

    private void createPlayerTexture(ShapeDrawer shapeDrawer) {
        int size = (int) (((int) Constants.CELL_SIZE) * playerTextureRegionScale);

        Batch batch = shapeDrawer.getBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, size, size));

        if (playerFbo != null) {
            playerFbo.dispose();
        }

        playerFbo = new FrameBuffer(Pixmap.Format.RGBA4444, size, size, false);
        playerFbo.begin();
        batch.begin();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeDrawer.setColor(Constants.PLAYER_CIRCLE_COLOR);
        shapeDrawer.circle(size / 2f, size / 2f, size / 3f,
                Constants.PLAYER_CIRCLE_WIDTH * playerTextureRegionScale, JoinType.SMOOTH);

        batch.end();
        playerFbo.end();

        playerTextureRegion = new TextureRegion(playerFbo.getColorBufferTexture(), size, size);
        playerTextureRegion.flip(false, true);
    }

    public void createMapTexture(ShapeDrawer shapeDrawer) {
        int width = (int) ((cols * Constants.CELL_SIZE) + Constants.MAP_GRID_LINE_WIDTH);
        int height = (int) ((rows * Constants.CELL_SIZE) + Constants.MAP_GRID_LINE_WIDTH);

        Batch batch = shapeDrawer.getBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(
                -Constants.MAP_GRID_LINE_WIDTH / 2, -Constants.MAP_GRID_LINE_WIDTH / 2, width, height));

        if (mapFbo != null) {
            mapFbo.dispose();
        }

        mapFbo = new FrameBuffer(Pixmap.Format.RGBA4444, width, height, false);
        mapFbo.begin();
        batch.begin();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawGrid(shapeDrawer);

        batch.end();
        mapFbo.end();

        mapTextureRegion = new TextureRegion(mapFbo.getColorBufferTexture(), width, height);
        mapTextureRegion.flip(false, true);
    }

    public void createPlayerLabelTextures(Player[] players, ShapeDrawer shapeDrawer, BitmapFont playerLabelFont) {
        playerLabels = new TextureRegion[players.length];

        int totalHeight = (((int) (playerLabelFont.getLineHeight() - (Constants.MAP_GRID_LINE_WIDTH / 2))) * players.length);
        int height = totalHeight / players.length;
        int width = (int) (((int) (Constants.CELL_SIZE * 3f)) * playerLabelRegionScale);
        float radius = height / 2f;

        Batch batch = shapeDrawer.getBatch();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, totalHeight));

        if (playerLabelFbo != null) {
            playerLabelFbo.dispose();
        }

        playerLabelFbo = new FrameBuffer(Pixmap.Format.RGBA4444, width, totalHeight, false);
        playerLabelFbo.begin();
        batch.begin();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (int i = 0; i < playerLabels.length; i++) {
            int yOffset = i * height;
            players[i].color.a = .7f;

            shapeDrawer.setColor(players[i].color);
            shapeDrawer.filledRectangle(radius, yOffset, width - (2 * radius), height);
            shapeDrawer.sector(radius, yOffset + height - radius, radius,
                    (float) Math.toRadians(90f), (float) Math.toRadians(180f));
            shapeDrawer.sector(width - radius, yOffset + height - radius, radius,
                    (float) Math.toRadians(270f), (float) Math.toRadians(180));

            playerLabelFont.setColor(Color.WHITE);
            playerLabelFont.draw(batch, players[i].name,
                    0.75f * radius, yOffset + (height / 2f) + (playerLabelFont.getLineHeight() / 4f),
                    0, players[i].name.length(),
                    width - (1.5f * radius), Align.center, false, "...");

            players[i].color.a = 1f;
        }
        batch.end();
        playerLabelFbo.end();

        TextureRegion textureRegion = new TextureRegion(playerLabelFbo.getColorBufferTexture(), width, totalHeight);
        for (int i = 0; i < playerLabels.length; i++) {
            int yOffset = i * height;
            playerLabels[i] = new TextureRegion(textureRegion, 0, yOffset, width, height);
            playerLabels[i].flip(false, true);
        }
    }

    private void drawGrid(ShapeDrawer shapeDrawer) {
        shapeDrawer.setColor(Constants.MAP_GRID_COLOR);

        for (int i = 0; i < rows + 1; i++) {
            float rowLineY = (i * Constants.CELL_SIZE);
            shapeDrawer.line(-Constants.MAP_GRID_LINE_WIDTH / 2, rowLineY,
                    (cols * Constants.CELL_SIZE) + Constants.MAP_GRID_LINE_WIDTH, rowLineY, Constants.MAP_GRID_LINE_WIDTH);
        }
        for (int i = 0; i < cols + 1; i++) {
            float colLineX = (i * Constants.CELL_SIZE);
            shapeDrawer.line(colLineX, -Constants.MAP_GRID_LINE_WIDTH / 2,
                    colLineX, (rows * Constants.CELL_SIZE) + Constants.MAP_GRID_LINE_WIDTH, Constants.MAP_GRID_LINE_WIDTH);
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float startX = j * Constants.CELL_SIZE;
                float startY = i * Constants.CELL_SIZE;

                if (!mapGrid[i][j].isMovable) {
                    shapeDrawer.filledRectangle(startX, startY, Constants.CELL_SIZE, Constants.CELL_SIZE);
                }
            }
        }
    }

    public void update(PlayerManager playerManager, float delta) {
        update(playerManager, playerManager.getPlayers(), delta);
    }

    public void update(PlayerManager playerManager, Player[] players, float delta) {
        // Used to synchronize movement of all players so that every one of them moves in sync
        boolean waitForMovementCompletion = false;

        for (Player player : players) {
            if (player.direction != null) {
                if (waitForMovementCompletion && !player.isMoving()) {
                    continue;
                } else if (player.isMoving()) {
                    waitForMovementCompletion = true;
                    player.move(delta);
                    continue;
                }

                Player.Direction direction = player.direction;
                int rPosX = player.roundedPosition.x;
                int rPosY = player.roundedPosition.y;

                boolean atLeftEdge = rPosX == 0;
                boolean atRightEdge = rPosX == cols - 1;
                boolean atTopEdge = rPosY == rows - 1;
                boolean atBottomEdge = rPosY == 0;

                playerMoved = true;

                if ((direction == Player.Direction.UP    && !atTopEdge    && mapGrid[rPosY + 1][rPosX].isMovable) ||
                    (direction == Player.Direction.RIGHT && !atRightEdge  && mapGrid[rPosY][rPosX + 1].isMovable) ||
                    (direction == Player.Direction.DOWN  && !atBottomEdge && mapGrid[rPosY - 1][rPosX].isMovable) ||
                    (direction == Player.Direction.LEFT  && !atLeftEdge   && mapGrid[rPosY][rPosX - 1].isMovable)) {

                    player.moveTo(direction, delta);
                } else {
                    // Simulate a fake movement
                    player.fakeMove(delta);
                }
            }
        }

        if (!waitForMovementCompletion && playerMoved) {
            // If movement(s) are completed and
            // If at least one player moved, update the map grid
            playerMoved = false;
            updateMap(players, playerManager);
        }
    }

    public void updateMap(Player[] players, PlayerManager playerManager) {
        setMapWeights(players);
        setMapColors(playerManager, players);
        updateCapturePercentage(playerManager);
    }

    private void setMapWeights(Player[] players) {
        for (Player player : players) {
            mapGrid[player.roundedPosition.y][player.roundedPosition.x].playerCount++;

            if (player.prevPosition != null) {
                mapGrid[player.prevPosition.y][player.prevPosition.x].playerCount--;
            } else {
                player.initPrevPosition();
            }
        }
    }

    private void setMapColors(PlayerManager playerManager, Player[] players) {
        for (Player player : players) {
            int rPosX = player.roundedPosition.x;
            int rPosY = player.roundedPosition.y;
            if (mapGrid[rPosY][rPosX].cellColor == null && mapGrid[rPosY][rPosX].playerCount == 1) {
                // TODO: Should we allow multiple players of the same team in the same location capture the cell?
                mapGrid[rPosY][rPosX].cellColor = new Color(player.color.r, player.color.g, player.color.b, 0.1f);
                if (playerManager != null) {
                    playerManager.incrementScore(player);
                }
                coloredCellCount++;
            }
        }

        if (floodfiller != null) {
            coloredCellCount += floodfiller.fillSurroundedCells(mapGrid, playerManager, players);
        }
    }

    private void updateCapturePercentage(PlayerManager playerManager) {
        if (playerManager == null) {
            return;
        }

        Array<TeamData> teamData = playerManager.getTeamData();
        for (TeamData td : teamData) {
            td.setCapturePercentage(getMovableCellCount());
        }
    }

    private Rectangle calcUserViewRect(OrthographicCamera camera) {
        float x = camera.position.x;
        float y = camera.position.y;
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;

        userViewRect.set(x - (width / 2) - Constants.CELL_SIZE, y - (height / 2) - Constants.CELL_SIZE,
                width + Constants.CELL_SIZE, height + Constants.CELL_SIZE);

        return userViewRect;
    }

    public void render(Player[] players, ShapeDrawer shapeDrawer, OrthographicCamera camera, float delta) {
        render(players, 0, shapeDrawer, camera, delta);
    }

    public void render(Player[] players, int playerIndex, ShapeDrawer shapeDrawer, OrthographicCamera camera, float delta) {
        Rectangle userViewRect = calcUserViewRect(camera);
        Batch batch = shapeDrawer.getBatch();

        drawColors(shapeDrawer, batch, playerIndex, userViewRect, delta);
        drawGrid(batch);
        drawPlayers(players, userViewRect, batch);
        drawPlayerLabels(players, playerIndex, userViewRect, batch);
    }

    private void drawPlayers(Player[] players, Rectangle userViewRect, Batch batch) {
        for (Player player : players) {
            player.render(userViewRect, batch, playerTextureRegion, playerTextureRegionScale);
        }
    }

    private void drawColors(ShapeDrawer shapeDrawer, Batch batch, int playerIndex, Rectangle userViewRect, float delta) {
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                float startX = j * Constants.CELL_SIZE;
                float startY = i * Constants.CELL_SIZE;

                if (mapGrid[i][j].cellColor != null) {
                    if (playerIndex == 0 && mapGrid[i][j].cellColor.a != 1f) {
                        mapGrid[i][j].cellColor.add(0, 0, 0, 2f * delta);
                    }

                    if (userViewRect.contains(startX, startY)) {
                        if (!equalColors(batch.getColor(), mapGrid[i][j].cellColor)) {
                            shapeDrawer.setColor(mapGrid[i][j].cellColor);
                        }
                        shapeDrawer.filledRectangle(startX, startY, Constants.CELL_SIZE, Constants.CELL_SIZE);
                    }
                }
            }
        }
    }

    private void drawGrid(Batch batch) {
        batch.draw(mapTextureRegion, -Constants.MAP_GRID_LINE_WIDTH / 2, -Constants.MAP_GRID_LINE_WIDTH / 2);
    }

    public void drawPlayerLabels(Player[] players, int playerIndex, Rectangle userViewRect, Batch batch) {
        if (playerLabels != null) {
            for (int i = 0; i < players.length; i++) {
                if (i == playerIndex) {
                    continue;
                }
                renderPlayerLabel(players[i], playerLabels[i], userViewRect, batch);
            }

            // Render the current player's label on top of other labels
            renderPlayerLabel(players[playerIndex], playerLabels[playerIndex], userViewRect, batch);
        }
    }

    private void renderPlayerLabel(Player player, TextureRegion playerLabel, Rectangle userViewRect, Batch batch) {
        float posX = (player.getPositionX() * Constants.CELL_SIZE) - (playerLabel.getRegionWidth() / (playerLabelRegionScale * 2f)) + (Constants.CELL_SIZE / 2);
        float posY = (player.getPositionY() * Constants.CELL_SIZE) + Constants.CELL_SIZE + (Constants.MAP_GRID_LINE_WIDTH / 2);
        if (userViewRect.contains(posX + playerLabel.getRegionWidth(), posY - (Constants.CELL_SIZE / 2)) ||
                userViewRect.contains(posX, posY - (Constants.CELL_SIZE / 2))) {
            batch.draw(playerLabel, posX, posY,
                    playerLabel.getRegionWidth() / playerLabelRegionScale, playerLabel.getRegionHeight() / playerLabelRegionScale);
        }
    }

    public void clearGrid() {
        this.coloredCellCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                resetCell(i, j);
            }
        }
    }

    public void clearGrid(Player[] players) {
        this.coloredCellCount = 0;
        for (Player player : players) {
            resetCell(player.roundedPosition.y, player.roundedPosition.x);
        }
    }

    private void resetCell(int i, int j) {
        mapGrid[i][j].cellColor = null;
        mapGrid[i][j].playerCount = 0;
    }

    public boolean gameComplete(Array<TeamData> teamData) {
        if (teamData.size == 2) {
            for (TeamData td : teamData) {
                // For two team games, end the game when a team has captured more than 50% of the cells
                if (td.getCapturePercentage() > 50.0) {
                    return true;
                }
            }
        }
        return getMovableCellCount() == this.coloredCellCount;
    }

    public void dispose() {
        if (playerLabelFbo != null) {
            playerLabelFbo.dispose();
            playerLabelFbo = null;
        }
        if (playerFbo != null) {
            playerFbo.dispose();
            playerFbo = null;
        }
        if (mapFbo != null) {
            mapFbo.dispose();
            mapFbo = null;
        }
    }

    /**
     * Returns the total number of cells in the map excluding cells with walls
     *
     * @return The count of cells that players can move into
     */
    private int getMovableCellCount() {
        return (this.rows * this.cols) - this.wallCount;
    }

    /**
     * Compares only the rgb components of Colors, ignoring its alpha component
     *
     * @param c1 The first Color to compare against
     * @param c2 The second Color to compare against
     * @return 'true' if the rgb components of 'c1' and 'c2' are equal
     * 'false' if not
     */
    public static boolean equalColors(Color c1, Color c2) {
        return c1.r == c2.r && c1.g == c2.g && c1.b == c2.b;
    }
}
