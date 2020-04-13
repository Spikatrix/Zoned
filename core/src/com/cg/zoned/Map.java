package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants.Direction;
import com.cg.zoned.managers.PlayerManager;

public class Map {

    private Cell[][] mapGrid;
    public int coloredCells = 0;
    public int rows;
    public int cols;
    public int wallCount = 0;
    private Array<GridPoint2> startPositions;

    public Map(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        this.mapGrid = new Cell[this.rows][this.cols];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                mapGrid[i][j] = new Cell();

                if (!mapGrid[i][j].isMovable) {
                    wallCount++;
                }
            }
        }

        startPositions = new Array<GridPoint2>();
        startPositions.add(new GridPoint2(0, rows - 1));
        startPositions.add(new GridPoint2(cols - 1, 0));
        startPositions.add(new GridPoint2(0, 0));
        startPositions.add(new GridPoint2(cols - 1, rows - 1));
    }

    public Map(Cell[][] mapGrid, Array<GridPoint2> startPositions, int wallCount) {
        this.rows = mapGrid.length;
        this.cols = mapGrid[0].length;
        this.mapGrid = mapGrid;
        this.startPositions = startPositions;
        this.wallCount = wallCount;
    }

    public Array<GridPoint2> getStartPositions() {
        return startPositions;
    }

    public void update(PlayerManager playerManager, float delta) {
        boolean waitForMovementCompletion = false; // Used to synchronize movement of all players
        // so that every one of them moves together

        for (Player player : playerManager.getPlayers()) {
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

                Direction direction = player.direction;
                if (direction == Direction.UP && player.position.y < rows - 1 && mapGrid[Math.round(player.position.y) + 1][Math.round(player.position.x)].isMovable) {
                    player.moveTo(new Vector2(player.position.x, player.position.y + 1), delta);
                } else if (direction == Direction.RIGHT && player.position.x < cols - 1 && mapGrid[Math.round(player.position.y)][Math.round(player.position.x) + 1].isMovable) {
                    player.moveTo(new Vector2(player.position.x + 1, player.position.y), delta);
                } else if (direction == Direction.DOWN && player.position.y > 0 && mapGrid[Math.round(player.position.y) - 1][Math.round(player.position.x)].isMovable) {
                    player.moveTo(new Vector2(player.position.x, player.position.y - 1), delta);
                } else if (direction == Direction.LEFT && player.position.x > 0 && mapGrid[Math.round(player.position.y)][Math.round(player.position.x) - 1].isMovable) {
                    player.moveTo(new Vector2(player.position.x - 1, player.position.y), delta);
                } else {
                    player.dummyMoveTo(new Vector2(1, 0), delta);
                }
            }
        }

        if (!waitForMovementCompletion) { // If movement(s) have completed
            setMapWeights(playerManager.getPlayers());
            setMapColors(playerManager);
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

    private void setMapColors(PlayerManager playerManager) {
        Player[] players = playerManager.getPlayers();
        for (int i = 0; i < players.length; i++) {
            int posX = Math.round(players[i].position.x);
            int posY = Math.round(players[i].position.y);
            if (mapGrid[posY][posX].cellColor == null && mapGrid[posY][posX].playerCount == 1) {
                mapGrid[posY][posX].cellColor = new Color(players[i].color.r, players[i].color.g, players[i].color.b, 0.1f);
                playerManager.incrementScore(players[i]);
                coloredCells++;
            }
        }

        fillSurroundedCells(playerManager);
    }

    private void fillSurroundedCells(PlayerManager playerManager) {
        Player[] players = playerManager.getPlayers();

        FloodFillGridState[][] gridState = new FloodFillGridState[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                gridState[i][j] = new FloodFillGridState();
                if (mapGrid[i][j].cellColor != null) {
                    gridState[i][j].state = FloodFillGridState.State.VISITED;
                }
            }
        }

        for (int i = 0; i < cols; i++) {
            if (gridState[0][i].state == FloodFillGridState.State.UNVISITED) {
                floodFill(gridState, new GridPoint2(0, i), null);
            }
            if (gridState[rows - 1][i].state == FloodFillGridState.State.UNVISITED) {
                floodFill(gridState, new GridPoint2(rows - 1, i), null);
            }
        }
        for (int i = 0; i < rows; i++) {
            if (gridState[i][0].state == FloodFillGridState.State.UNVISITED) {
                floodFill(gridState, new GridPoint2(i, 0), null);
            }
            if (gridState[i][cols - 1].state == FloodFillGridState.State.UNVISITED) {
                floodFill(gridState, new GridPoint2(i, cols - 1), null);
            }
        }

        Array<GridPoint2> fillPositions = new Array<GridPoint2>();
        Color fillColor;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (gridState[i][j].state == FloodFillGridState.State.UNVISITED) {
                    fillColor = floodFill(gridState, new GridPoint2(i, j), fillPositions);

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
                            playerManager.incrementScore(players[index]);
                            coloredCells++;
                        }
                    } else {
                        fillPositions.clear();
                    }
                }
            }
        }
    }

    public void render(Player[] players, ShapeRenderer renderer, OrthographicCamera camera, float delta) {
        drawColors(renderer, camera, delta);
        drawPlayers(players, camera, renderer);
        drawGrid(camera, renderer);
    }

    private void drawPlayers(Player[] players, OrthographicCamera camera, ShapeRenderer renderer) {
        Gdx.gl.glLineWidth(Constants.PLAYER_CIRCLE_WIDTH);
        renderer.set(ShapeRenderer.ShapeType.Line);
        for (Player player : players) {
            player.render(camera, renderer);
        }
        renderer.set(ShapeRenderer.ShapeType.Filled);
    }

    private void drawColors(ShapeRenderer renderer, OrthographicCamera camera, float delta) {
        float x = camera.position.x;
        float y = camera.position.y;
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;

        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                float startX = j * Constants.CELL_SIZE;
                float startY = i * Constants.CELL_SIZE;

                if ((startX >= x - width) && (startX + Constants.CELL_SIZE <= x + width) &&
                        (startY >= y - height) && (startY + Constants.CELL_SIZE <= y + height)) {
                    if (mapGrid[i][j].cellColor != null) {
                        renderer.setColor(mapGrid[i][j].cellColor);
                        renderer.rect(startX, startY,
                                Constants.CELL_SIZE, Constants.CELL_SIZE);

                        mapGrid[i][j].cellColor.add(0, 0, 0, 2f * delta);

                        // Use the constant color object to avoid too many redundant color objects
                        Color constColor = PlayerColorHelper.getConstantColor(mapGrid[i][j].cellColor);
                        if (constColor != null) {
                            mapGrid[i][j].cellColor = constColor;
                        }
                    } else if (!mapGrid[i][j].isMovable) {
                        renderer.setColor(Constants.MAP_GRID_COLOR);
                        renderer.rect(startX, startY,
                                Constants.CELL_SIZE, Constants.CELL_SIZE);
                    }
                }
            }
        }
    }

    private void drawGrid(OrthographicCamera camera, ShapeRenderer renderer) {
        float x = camera.position.x;
        float y = camera.position.y;
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;

        renderer.setColor(Constants.MAP_GRID_COLOR);
        for (int i = 0; i < this.rows + 1; i++) {
            if (i * Constants.CELL_SIZE >= y - height && i * Constants.CELL_SIZE <= y + height) {
                renderer.rectLine(Math.max(0, x - width), i * Constants.CELL_SIZE,
                        Math.min(this.cols * Constants.CELL_SIZE, x + width), i * Constants.CELL_SIZE, Constants.MAP_GRID_LINE_WIDTH);
            }
        }
        for (int i = 0; i < this.cols + 1; i++) {
            if (i * Constants.CELL_SIZE >= x - width && i * Constants.CELL_SIZE <= x + width) {
                renderer.rectLine(i * Constants.CELL_SIZE, Math.max(0, y - height),
                        i * Constants.CELL_SIZE, Math.min(this.rows * Constants.CELL_SIZE, y + height), Constants.MAP_GRID_LINE_WIDTH);
            }
        }
    }

    /**
     * Flood fill the unvisited cells
     *
     * @param gridState    2D array depicting the VISITED or UNVISITED state of the grid
     * @param startPos     Position to start flood filling from
     * @param fillPosStack Array that stores the positions of cells in the grid that were flood filled
     * @return A Color which depicts which color can be filled in the flood filled locations.
     * If 'null' or 'Color.BLACK', multiple or no colors were present along the edges, or a wall was in between
     */
    private Color floodFill(FloodFillGridState[][] gridState, GridPoint2 startPos, Array<GridPoint2> fillPosStack) {
        Array<GridPoint2> stack = new Array<GridPoint2>();
        Color fillColor = null;

        gridState[startPos.x][startPos.y].state = FloodFillGridState.State.VISITED;
        if (!mapGrid[startPos.x][startPos.y].isMovable) {
            fillColor = Color.BLACK;
        }

        stack.add(startPos);
        while (!stack.isEmpty()) {
            GridPoint2 pos = stack.pop();

            if (pos.x > 0) {
                fillColor = fillColorHelper(pos.x - 1, pos.y, fillColor, gridState, stack);
            }
            if (pos.x < rows - 1) {
                fillColor = fillColorHelper(pos.x + 1, pos.y, fillColor, gridState, stack);
            }
            if (pos.y > 0) {
                fillColor = fillColorHelper(pos.x, pos.y - 1, fillColor, gridState, stack);
            }
            if (pos.y < cols - 1) {
                fillColor = fillColorHelper(pos.x, pos.y + 1, fillColor, gridState, stack);
            }
            if (pos.x > 0 && pos.y > 0) {
                fillColor = fillColorHelper(pos.x - 1, pos.y - 1, fillColor, gridState, stack);
            }
            if (pos.x > 0 && pos.y < cols - 1) {
                fillColor = fillColorHelper(pos.x - 1, pos.y + 1, fillColor, gridState, stack);
            }
            if (pos.x < rows - 1 && pos.y > 0) {
                fillColor = fillColorHelper(pos.x + 1, pos.y - 1, fillColor, gridState, stack);
            }
            if (pos.x < rows - 1 && pos.y < cols - 1) {
                fillColor = fillColorHelper(pos.x + 1, pos.y + 1, fillColor, gridState, stack);
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
            stack.add(new GridPoint2(x, y));
        }

        return fillColor;
    }

    public boolean gameComplete(Player[] players) {
        if (players.length == 2) {
            for (Player player : players) {
                // For two player games, end the game when a player has captured more than 50% of the cells
                if (100 * (player.score / (((double) this.rows * this.cols) - this.wallCount)) > 50.0) {
                    return true;
                }
            }
        }
        return ((this.rows * this.cols) - this.wallCount) == this.coloredCells;
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

        public FloodFillGridState() {
            state = State.UNVISITED;
        }
    }
}
