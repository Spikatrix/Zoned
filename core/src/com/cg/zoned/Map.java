package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.TeamData;
import com.cg.zoned.managers.PlayerManager;

public class Map {
    private final Cell[][] mapGrid;
    public int wallCount;

    public int rows;
    public int cols;
    private int coloredCellCount;

    private boolean playerMoved;
    private FloodFiller floodfiller;
    private final MapRenderer mapRenderer;

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

        this.mapRenderer = new MapRenderer(mapGrid, shapeDrawer);
    }

    public Map(Cell[][] mapGrid, ShapeDrawer shapeDrawer) {
        this(mapGrid, 0, shapeDrawer);
    }

    public void createPlayerLabelTextures(PlayerEntity[] players, ShapeDrawer shapeDrawer, BitmapFont playerLabelFont) {
        mapRenderer.createPlayerLabelTextures(players, shapeDrawer, playerLabelFont);
    }

    public void createMapTexture(ShapeDrawer shapeDrawer) {
        mapRenderer.createMapTexture(shapeDrawer);
    }

    public void initFloodFillVars() {
        this.floodfiller = new FloodFiller(this.rows, this.cols);
    }

    public void update(PlayerManager playerManager, float delta) {
        update(playerManager, playerManager.getPlayers(), delta);
    }

    public void update(PlayerManager playerManager, PlayerEntity[] players, float delta) {
        // If movement(s) have been completed and at least one player moved
        if (updatePlayerPositions(players, delta) && playerMoved) {
            playerMoved = false;
            // Update the map grid data
            updateMap(players, playerManager);
        }
    }

    /**
     * Starts and continues player movements
     *
     * @param players The list of players
     * @param delta   The time passed between two successive frames
     * @return        true if all player movements have been completed, false if not
     */
    public boolean updatePlayerPositions(PlayerEntity[] players, float delta) {
        boolean completedMovement = false;
        for (PlayerEntity player : players) {
            // If true, the player is in the middle of a movement
            if (player.isMoving()) {
                // Continue the current movement
                player.move(delta);
            } else {
                // Start a new movement with the player's set direction
                if (beginPlayerMovement(player, delta)) {
                    playerMoved = true;
                }
            }

            // True if the current movement has been completed
            if (!player.isMoving()) {
                completedMovement = true;
            }
        }

        return completedMovement;
    }

    /**
     * Starts the player movement based on their set direction
     *
     * @param player The player to start movement
     * @param delta  The time passed between two successive frames
     * @return true if the movement was not a fake one, false otherwise
     */
    private boolean beginPlayerMovement(PlayerEntity player, float delta) {
        if (isValidMovement(player, player.direction)) {
            player.moveTo(player.direction, delta);
            return true;
        } else {
            // Simulate a fake movement
            player.moveTo(null, delta);
            return false;
        }
    }

    public boolean isValidMovement(PlayerEntity player, Player.Direction direction) {
        int posX = player.getRoundedPositionX();
        int posY = player.getRoundedPositionY();

        boolean atLeftEdge = posX == 0;
        boolean atRightEdge = posX == cols - 1;
        boolean atTopEdge = posY == rows - 1;
        boolean atBottomEdge = posY == 0;

        return (direction == Player.Direction.UP    && !atTopEdge    && mapGrid[posY + 1][posX].isMovable) ||
               (direction == Player.Direction.RIGHT && !atRightEdge  && mapGrid[posY][posX + 1].isMovable) ||
               (direction == Player.Direction.DOWN  && !atBottomEdge && mapGrid[posY - 1][posX].isMovable) ||
               (direction == Player.Direction.LEFT  && !atLeftEdge   && mapGrid[posY][posX - 1].isMovable);
    }

    public void updateMap(PlayerManager playerManager) {
        updateMap(playerManager.getPlayers(), playerManager);
    }

    public void updateMap(PlayerEntity[] players, PlayerManager playerManager) {
        // `playerManager` can be null (Like in the tutorial)
        setMapWeights(players);
        setMapColors(playerManager, players);
        updateCapturePercentage(playerManager);
    }

    private void setMapWeights(PlayerEntity[] players) {
        for (PlayerEntity player : players) {
            // If false, previous position is unavailable (Should be the very first turn)
            if (player.hasPreviousPosition()) {
                mapGrid[player.getPreviousPositionY()][player.getPreviousPositionX()].playerCount--;
            }

            mapGrid[player.getRoundedPositionY()][player.getRoundedPositionX()].playerCount++;
        }
    }

    private void setMapColors(PlayerManager playerManager, PlayerEntity[] players) {
        for (PlayerEntity player : players) {
            int rPosX = player.getRoundedPositionX();
            int rPosY = player.getRoundedPositionY();

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


    public void render(PlayerEntity[] players, int playerIndex, ShapeDrawer shapeDrawer, OrthographicCamera camera, float delta) {
        mapRenderer.render(players, playerIndex, shapeDrawer, camera, delta);
    }

    public void render(PlayerEntity[] players, ShapeDrawer shapeDrawer, OrthographicCamera camera, float delta) {
        render(players, 0, shapeDrawer, camera, delta);
    }


    public void clearGrid() {
        this.coloredCellCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                resetCell(i, j);
            }
        }
    }

    public void clearGrid(PlayerEntity[] players) {
        this.coloredCellCount = 0;
        for (PlayerEntity player : players) {
            resetCell(player.getRoundedPositionY(), player.getRoundedPositionX());
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
        mapRenderer.dispose();
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
