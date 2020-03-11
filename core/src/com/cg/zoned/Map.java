package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants.Direction;

public class Map {

    private Cell[][] mapGrid;
    public int rows;
    public int cols;
    public int coloredCells;

    public Map(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.coloredCells = 0;

        this.mapGrid = new Cell[this.rows][this.cols];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                mapGrid[i][j] = new Cell();
            }
        }
    }

    public static Vector2[] getStartPositions(int rows, int cols) {
        return new Vector2[]{
                new Vector2(0, rows - 1),
                new Vector2(cols - 1, 0),
                new Vector2(0, 0),
                new Vector2(cols - 1, rows - 1),
        };
    }

    public void update(Player[] players, int[] playerScores, float delta) {
        boolean waitForMovementCompletion = false; // Used to synchronize movement of all players
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
            setMapWeights(players);
            setMapColors(players, playerScores);
        }
    }

    private void setMapWeights(Player[] players) {
        for (Player player : players) {
            if (player.prevPosition != player.position) {
                if (player.prevPosition != null) {
                    mapGrid[Math.round(player.prevPosition.y)][Math.round(player.prevPosition.x)].noOfPlayers--;
                } else {
                    player.prevPosition = new Vector2();
                }

                mapGrid[Math.round(player.position.y)][Math.round(player.position.x)].noOfPlayers++;

                player.prevPosition.x = Math.round(player.position.x);
                player.prevPosition.y = Math.round(player.position.y);
            }
        }

    }

    private void setMapColors(Player[] players, int[] playerScores) {
        for (int i = 0; i < players.length; i++) {
            int posX = Math.round(players[i].position.x);
            int posY = Math.round(players[i].position.y);
            if (mapGrid[posY][posX].cellColor == null && mapGrid[posY][posX].noOfPlayers == 1) {
                mapGrid[posY][posX].cellColor = new Color(players[i].color.r, players[i].color.g, players[i].color.b, 0.1f);
                playerScores[i]++;
                coloredCells++;
            }
        }

        fillSurroundedCells(players, playerScores);
    }

    private void fillSurroundedCells(Player[] players, int[] playerScores) {
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
                            playerScores[index]++;
                            coloredCells++;
                        }
                    } else {
                        fillPositions.clear();
                    }
                }
            }
        }
    }

    public void render(Player[] players, ShapeRenderer renderer, float delta) {
        drawColors(renderer, delta);
        drawPlayers(players, renderer);
        drawGrid(renderer);
    }

    private void drawPlayers(Player[] players, ShapeRenderer renderer) {
        Gdx.gl.glLineWidth(Constants.PLAYER_CIRCLE_WIDTH);
        renderer.set(ShapeRenderer.ShapeType.Line);
        for (Player player : players) {
            player.render(renderer);
        }
        renderer.set(ShapeRenderer.ShapeType.Filled);
    }

    private void drawColors(ShapeRenderer renderer, float delta) {
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                if (mapGrid[i][j].cellColor != null) {
                    renderer.setColor(mapGrid[i][j].cellColor);
                    renderer.rect(j * Constants.CELL_SIZE, i * Constants.CELL_SIZE,
                            Constants.CELL_SIZE, Constants.CELL_SIZE);

                    mapGrid[i][j].cellColor.add(0, 0, 0, 2f * delta);

                    // Use the constant color object to avoid too many redundant color objects
                    Color constColor = PlayerColorHelper.getConstantColor(mapGrid[i][j].cellColor);
                    if (constColor != null) {
                        mapGrid[i][j].cellColor = constColor;
                    }
                } else if (!mapGrid[i][j].isMovable) {
                    renderer.setColor(Constants.MAP_GRID_COLOR);
                    renderer.rect(j * Constants.CELL_SIZE, i * Constants.CELL_SIZE,
                            Constants.CELL_SIZE, Constants.CELL_SIZE);
                }
            }
        }
    }

    private void drawGrid(ShapeRenderer renderer) {
        renderer.setColor(Constants.MAP_GRID_COLOR);
        for (int i = 0; i < this.rows + 1; i++) {
            renderer.rectLine(0, i * Constants.CELL_SIZE,
                    this.cols * Constants.CELL_SIZE, i * Constants.CELL_SIZE, Constants.MAP_GRID_LINE_WIDTH);
        }
        for (int i = 0; i < this.cols + 1; i++) {
            renderer.rectLine(i * Constants.CELL_SIZE, 0,
                    i * Constants.CELL_SIZE, this.rows * Constants.CELL_SIZE, Constants.MAP_GRID_LINE_WIDTH);
        }
    }

    /**
     * Flood fill the unvisited cells
     *
     * @param gridState    2D array depicting the VISITED or UNVISITED state of the grid
     * @param startPos     Position to start flood filling from
     * @param fillPosStack Array that stores the positions of cells in the grid that were flood filled
     * @return A Color which depicts which color can be filled in the flood filled locations.
     *         If 'null' or 'Color.BLACK', multiple or no colors were present along the edges, or a wall was in between
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
                if (!mapGrid[pos.x - 1][pos.y].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x - 1][pos.y].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x - 1][pos.y].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x - 1][pos.y].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x - 1][pos.y].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x - 1][pos.y].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x - 1, pos.y));
                }
            }
            if (pos.x < rows - 1) {
                if (!mapGrid[pos.x + 1][pos.y].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x + 1][pos.y].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x + 1][pos.y].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x + 1][pos.y].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x + 1][pos.y].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x + 1][pos.y].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x + 1, pos.y));
                }
            }
            if (pos.y > 0) {
                if (!mapGrid[pos.x][pos.y - 1].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x][pos.y - 1].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x][pos.y - 1].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x][pos.y - 1].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x][pos.y - 1].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x][pos.y - 1].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x, pos.y - 1));
                }
            }
            if (pos.y < cols - 1) {
                if (!mapGrid[pos.x][pos.y + 1].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x][pos.y + 1].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x][pos.y + 1].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x][pos.y + 1].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x][pos.y + 1].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x][pos.y + 1].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x, pos.y + 1));
                }
            }
            if (pos.x > 0 && pos.y > 0) {
                if (!mapGrid[pos.x - 1][pos.y - 1].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x - 1][pos.y - 1].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x - 1][pos.y - 1].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x - 1][pos.y - 1].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x - 1][pos.y - 1].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x - 1][pos.y - 1].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x - 1, pos.y - 1));
                }
            }
            if (pos.x > 0 && pos.y < cols - 1) {
                if (!mapGrid[pos.x - 1][pos.y + 1].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x - 1][pos.y + 1].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x - 1][pos.y + 1].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x - 1][pos.y + 1].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x - 1][pos.y + 1].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x - 1][pos.y + 1].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x - 1, pos.y + 1));
                }
            }
            if (pos.x < rows - 1 && pos.y > 0) {
                if (!mapGrid[pos.x + 1][pos.y - 1].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x + 1][pos.y - 1].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x + 1][pos.y - 1].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x + 1][pos.y - 1].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x + 1][pos.y - 1].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x + 1][pos.y - 1].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x + 1, pos.y - 1));
                }
            }
            if (pos.x < rows - 1 && pos.y < cols - 1) {
                if (!mapGrid[pos.x + 1][pos.y + 1].isMovable) {
                    fillColor = Color.BLACK;
                } else if (mapGrid[pos.x + 1][pos.y + 1].cellColor != null) {
                    if (fillColor == null) {
                        fillColor = mapGrid[pos.x + 1][pos.y + 1].cellColor;
                    } else if (!equalColors(fillColor, mapGrid[pos.x + 1][pos.y + 1].cellColor)) {
                        fillColor = Color.BLACK;
                    }
                }

                if (gridState[pos.x + 1][pos.y + 1].state == FloodFillGridState.State.UNVISITED) {
                    gridState[pos.x + 1][pos.y + 1].state = FloodFillGridState.State.VISITED;
                    stack.add(new GridPoint2(pos.x + 1, pos.y + 1));
                }
            }

            if (fillPosStack != null) {
                fillPosStack.add(pos);
            }
        }

        return fillColor;
    }

    public boolean gameComplete() {
        //TODO: Account for walls and 50% completion for two player games
        return this.rows * this.cols == this.coloredCells;
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
