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
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.managers.PlayerManager;

import space.earlygrey.shapedrawer.JoinType;

public class Map {
    private Cell[][] mapGrid;
    public int wallCount;

    public int rows;
    public int cols;
    private int coloredCells = 0;

    private boolean mapColorUpdated = false;

    private Rectangle userViewRect;

    private FrameBuffer playerLabelFbo = null;
    public static float playerLabelRegionScale = 2f; // Rendered at higher (2x rn) res, downscaled to required size
    private TextureRegion[] playerLabels = null;

    private FrameBuffer playerFbo = null;
    public static float playerTextureRegionScale = 2f; // Rendered at higher (2x rn) res, downscaled to required size
    private TextureRegion playerTextureRegion = null;

    private FrameBuffer mapFbo = null;
    private TextureRegion mapTextureRegion = null;

    private FloodFillGridState[][] gridState;
    private GridPoint2[] gridPointPool;
    private int nextFreeIndex = 0;
    private Array<GridPoint2> helperStack;
    private Array<GridPoint2> fillPositions;

    /**
     * Creates the map object with features like processing each turn and managing score, rendering
     * the grid and more
     *
     * @param mapGrid     Array of {@link Cell}s of the map grid
     * @param wallCount   Amount of walls in the grid; used for determining winner early in 2 player games
     * @param shapeDrawer Used to prepare grid and player textures for rendering
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

    public void initFloodFillVars() {
        gridState = new FloodFillGridState[this.rows][this.cols];
        for (int i = 0; i < gridState.length; i++) {
            for (int j = 0; j < gridState[i].length; j++) {
                gridState[i][j] = new FloodFillGridState();
            }
        }

        gridPointPool = new GridPoint2[rows * cols];
        for (int i = 0; i < rows * cols; i++) {
            gridPointPool[i] = new GridPoint2();
        }
        helperStack = new Array<>();
        fillPositions = new Array<>();
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
        boolean waitForMovementCompletion = false;
        // Used to synchronize movement of all players
        // so that every one of them moves together

        for (Player player : players) {
            if (player.direction != null) {
                if (waitForMovementCompletion && player.targetPosition == null) {
                    continue;
                } else if (player.targetPosition != null) {
                    waitForMovementCompletion = true;
                    if (player.dummyMoving) {
                        player.dummyMoveTo(player.targetPosition, delta);
                    } else {
                        player.moveTo(player.targetPosition, delta);
                    }
                    continue;
                }

                mapColorUpdated = false;

                Player.Direction direction = player.direction;
                if (direction == Player.Direction.UP && player.position.y < rows - 1 &&
                        mapGrid[Math.round(player.position.y) + 1][Math.round(player.position.x)].isMovable) {

                    player.moveTo(new Vector2(player.position.x, player.position.y + 1), delta);

                } else if (direction == Player.Direction.RIGHT && player.position.x < cols - 1 &&
                        mapGrid[Math.round(player.position.y)][Math.round(player.position.x) + 1].isMovable) {

                    player.moveTo(new Vector2(player.position.x + 1, player.position.y), delta);

                } else if (direction == Player.Direction.DOWN && player.position.y > 0 &&
                        mapGrid[Math.round(player.position.y) - 1][Math.round(player.position.x)].isMovable) {

                    player.moveTo(new Vector2(player.position.x, player.position.y - 1), delta);

                } else if (direction == Player.Direction.LEFT && player.position.x > 0 &&
                        mapGrid[Math.round(player.position.y)][Math.round(player.position.x) - 1].isMovable) {

                    player.moveTo(new Vector2(player.position.x - 1, player.position.y), delta);

                } else {
                    player.dummyMoveTo(new Vector2(1, 0), delta);
                }
            }
        }

        if (!waitForMovementCompletion) { // If movement(s) are completed
            if (!mapColorUpdated) {
                mapColorUpdated = true;

                setMapWeights(players);
                setMapColors(playerManager, players);
                updateCapturePercentage(playerManager);
            }
        }
    }

    private void setMapWeights(Player[] players) {
        for (Player player : players) {
            if (player.prevPosition != player.position) {
                if (player.prevPosition != null) {
                    mapGrid[Math.round(player.prevPosition.y)][Math.round(player.prevPosition.x)].playerCount--;
                } else {
                    player.prevPosition = new Vector2();
                }

                mapGrid[Math.round(player.position.y)][Math.round(player.position.x)].playerCount++;

                player.prevPosition.x = Math.round(player.position.x);
                player.prevPosition.y = Math.round(player.position.y);
            }
        }

    }

    private void setMapColors(PlayerManager playerManager, Player[] players) {
        for (Player player : players) {
            int posX = Math.round(player.position.x);
            int posY = Math.round(player.position.y);
            if (mapGrid[posY][posX].cellColor == null && mapGrid[posY][posX].playerCount == 1) {
                // TODO: Should we allow multiple players of the same team in the same location capture the cell?
                mapGrid[posY][posX].cellColor = new Color(player.color.r, player.color.g, player.color.b, 0.1f);
                if (playerManager != null) {
                    playerManager.incrementScore(player);
                }
                coloredCells++;
            }
        }

        if (gridState != null) {
            fillSurroundedCells(playerManager, players);
        }
    }

    private void updateCapturePercentage(PlayerManager playerManager) {
        if (playerManager == null) {
            return;
        }

        Array<TeamData> teamData = playerManager.getTeamData();
        for (TeamData td : teamData) {
            td.setCapturePercentage((this.rows * this.cols) - this.wallCount);
        }
    }

    private void fillSurroundedCells(PlayerManager playerManager, Player[] players) {
        nextFreeIndex = 0;
        helperStack.clear();
        fillPositions.clear();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (mapGrid[i][j].cellColor != null) {
                    gridState[i][j].state = FloodFillGridState.State.VISITED;
                } else {
                    gridState[i][j].state = FloodFillGridState.State.UNVISITED;
                }
            }
        }

        for (int i = 0; i < cols; i++) {
            if (gridState[0][i].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(0, i);
                floodFill(gridState, gridPoint2, null, helperStack);
            }
            if (gridState[rows - 1][i].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(rows - 1, i);
                floodFill(gridState, gridPoint2, null, helperStack);
            }
        }
        for (int i = 0; i < rows; i++) {
            if (gridState[i][0].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(i, 0);
                floodFill(gridState, gridPoint2, null, helperStack);
            }
            if (gridState[i][cols - 1].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(i, cols - 1);
                floodFill(gridState, gridPoint2, null, helperStack);
            }
        }

        Color fillColor;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (gridState[i][j].state == FloodFillGridState.State.UNVISITED) {
                    GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(i, j);
                    fillColor = floodFill(gridState, gridPoint2, fillPositions, helperStack);

                    if (fillColor != null && fillColor != Color.BLACK) {
                        int index = -1;
                        for (int k = 0; k < players.length; k++) {
                            if (equalColors(players[k].color, fillColor)) {
                                index = k;
                                break;
                            }
                        }

                        while (!fillPositions.isEmpty()) {
                            GridPoint2 pos = fillPositions.pop();

                            mapGrid[pos.x][pos.y].cellColor = new Color(fillColor.r, fillColor.g, fillColor.b, 0.1f);
                            if (playerManager != null) {
                                playerManager.incrementScore(players[index]);
                            }
                            coloredCells++;
                        }
                    } else {
                        fillPositions.clear();
                    }
                }
            }
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

        drawColors(shapeDrawer, playerIndex, userViewRect, delta);
        drawGrid(batch);
        drawPlayers(players, userViewRect, batch);
        drawPlayerLabels(players, playerIndex, userViewRect, batch);
    }

    private void drawPlayers(Player[] players, Rectangle userViewRect, Batch batch) {
        for (Player player : players) {
            player.render(userViewRect, batch, playerTextureRegion, playerTextureRegionScale);
        }
    }

    private void drawColors(ShapeDrawer shapeDrawer, int playerIndex, Rectangle userViewRect, float delta) {
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                float startX = j * Constants.CELL_SIZE;
                float startY = i * Constants.CELL_SIZE;

                if (mapGrid[i][j].cellColor != null) {
                    if (playerIndex == 0 && mapGrid[i][j].cellColor.a != 1f) {
                        mapGrid[i][j].cellColor.add(0, 0, 0, 2f * delta);
                    }

                    if (userViewRect.contains(startX, startY)) {
                        shapeDrawer.setColor(mapGrid[i][j].cellColor);
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
        float posX = (player.position.x * Constants.CELL_SIZE) - (playerLabel.getRegionWidth() / (playerLabelRegionScale * 2f)) + (Constants.CELL_SIZE / 2);
        float posY = (player.position.y * Constants.CELL_SIZE) + Constants.CELL_SIZE + (Constants.MAP_GRID_LINE_WIDTH / 2);
        if (userViewRect.contains(posX + playerLabel.getRegionWidth(), posY - (Constants.CELL_SIZE / 2)) ||
                userViewRect.contains(posX, posY - (Constants.CELL_SIZE / 2))) {
            batch.draw(playerLabel, posX, posY,
                    playerLabel.getRegionWidth() / playerLabelRegionScale, playerLabel.getRegionHeight() / playerLabelRegionScale);
        }
    }

    /**
     * Flood fill the unvisited cells
     *
     * @param gridState    2D array depicting the VISITED or UNVISITED state of the grid
     * @param startPos     Position to start flood filling from
     * @param fillPosStack Array that stores the positions of cells in the grid that were flood filled
     * @param helperStack  Stack used for storing GridPoints in the algo
     * @return A Color which depicts which color can be filled in the flood filled locations.
     * If 'null' or 'Color.BLACK', multiple or no colors were present along the edges, or a wall was in between
     */
    private Color floodFill(FloodFillGridState[][] gridState, GridPoint2 startPos,
                            Array<GridPoint2> fillPosStack, Array<GridPoint2> helperStack) {
        Color fillColor = null;
        helperStack.clear();

        gridState[startPos.x][startPos.y].state = FloodFillGridState.State.VISITED;
        if (!mapGrid[startPos.x][startPos.y].isMovable) {
            fillColor = Color.BLACK;
        }

        helperStack.add(startPos);
        while (!helperStack.isEmpty()) {
            GridPoint2 pos = helperStack.pop();

            if (pos.x > 0) {
                fillColor = fillColorHelper(pos.x - 1, pos.y, fillColor, gridState, helperStack);
            }
            if (pos.x < rows - 1) {
                fillColor = fillColorHelper(pos.x + 1, pos.y, fillColor, gridState, helperStack);
            }
            if (pos.y > 0) {
                fillColor = fillColorHelper(pos.x, pos.y - 1, fillColor, gridState, helperStack);
            }
            if (pos.y < cols - 1) {
                fillColor = fillColorHelper(pos.x, pos.y + 1, fillColor, gridState, helperStack);
            }
            if (pos.x > 0 && pos.y > 0) {
                fillColor = fillColorHelper(pos.x - 1, pos.y - 1, fillColor, gridState, helperStack);
            }
            if (pos.x > 0 && pos.y < cols - 1) {
                fillColor = fillColorHelper(pos.x - 1, pos.y + 1, fillColor, gridState, helperStack);
            }
            if (pos.x < rows - 1 && pos.y > 0) {
                fillColor = fillColorHelper(pos.x + 1, pos.y - 1, fillColor, gridState, helperStack);
            }
            if (pos.x < rows - 1 && pos.y < cols - 1) {
                fillColor = fillColorHelper(pos.x + 1, pos.y + 1, fillColor, gridState, helperStack);
            }

            if (fillPosStack != null) {
                fillPosStack.add(pos);
            }
        }

        return fillColor;
    }

    /**
     * Helper method for flood fill
     */
    private Color fillColorHelper(int x, int y, Color fillColor, FloodFillGridState[][] gridState, Array<GridPoint2> stack) {
        if (!mapGrid[x][y].isMovable) {
            fillColor = Color.BLACK;
        } else if (mapGrid[x][y].cellColor != null) {
            if (fillColor == null) {
                fillColor = mapGrid[x][y].cellColor;
            } else if (!equalColors(fillColor, mapGrid[x][y].cellColor)) {
                fillColor = Color.BLACK;
            }
        }

        if (gridState[x][y].state == FloodFillGridState.State.UNVISITED) {
            gridState[x][y].state = FloodFillGridState.State.VISITED;
            stack.add(gridPointPool[nextFreeIndex++].set(x, y));
        }

        return fillColor;
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
        return ((this.rows * this.cols) - this.wallCount) == this.coloredCells;
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
     * Compares only the rgb components of Colors, ignoring its alpha component
     *
     * @param c1 The first Color to compare against
     * @param c2 The second Color to compare against
     * @return 'true' if the rgb components of 'c1' and 'c2' are equal
     * 'false' if not
     */
    private boolean equalColors(Color c1, Color c2) {
        return c1.r == c2.r && c1.g == c2.g && c1.b == c2.b;
    }

    static class FloodFillGridState {
        private enum State {UNVISITED, VISITED}

        private State state;

        FloodFillGridState() {
            state = State.UNVISITED;
        }
    }
}
