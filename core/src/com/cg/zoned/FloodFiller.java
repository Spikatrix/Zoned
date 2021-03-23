package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.managers.PlayerManager;

public class FloodFiller {
    private FloodFillGridState[][] gridState;
    private GridPoint2[] gridPointPool;
    private int nextFreeIndex = 0;
    private GridPoint2[] helperStack;
    private int helperStackTop = -1;
    private Array<GridPoint2> fillPositions;

    private final int rows;
    private final int cols;

    public FloodFiller(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        initFloodFillVars();
    }

    private void initFloodFillVars() {
        gridState = new FloodFillGridState[rows][cols];
        for (int i = 0; i < gridState.length; i++) {
            for (int j = 0; j < gridState[i].length; j++) {
                gridState[i][j] = new FloodFillGridState();
            }
        }

        gridPointPool = new GridPoint2[rows * cols];
        helperStack = new GridPoint2[rows * cols];
        for (int i = 0; i < rows * cols; i++) {
            gridPointPool[i] = new GridPoint2();
        }
        fillPositions = new Array<>();
    }

    public int fillSurroundedCells(Cell[][] mapGrid, PlayerManager playerManager, Player[] players) {
        int coloredCells = 0;

        nextFreeIndex = 0;
        helperStackTop = -1;
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
                floodFill(mapGrid, gridState, gridPoint2, null, helperStack);
            }
            if (gridState[rows - 1][i].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(rows - 1, i);
                floodFill(mapGrid, gridState, gridPoint2, null, helperStack);
            }
        }
        for (int i = 0; i < rows; i++) {
            if (gridState[i][0].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(i, 0);
                floodFill(mapGrid, gridState, gridPoint2, null, helperStack);
            }
            if (gridState[i][cols - 1].state == FloodFillGridState.State.UNVISITED) {
                GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(i, cols - 1);
                floodFill(mapGrid, gridState, gridPoint2, null, helperStack);
            }
        }

        Color fillColor;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (gridState[i][j].state == FloodFillGridState.State.UNVISITED) {
                    GridPoint2 gridPoint2 = gridPointPool[nextFreeIndex++].set(i, j);
                    fillColor = floodFill(mapGrid, gridState, gridPoint2, fillPositions, helperStack);

                    if (fillColor != null && fillColor != Color.BLACK) {
                        int index = -1;
                        for (int k = 0; k < players.length; k++) {
                            if (Map.equalColors(players[k].color, fillColor)) {
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

        return coloredCells;
    }

    /**
     * Flood fill the unvisited cells
     *
     *
     * @param mapGrid      2D array depicting the map grid cells
     * @param gridState    2D array depicting the VISITED or UNVISITED state of the grid
     * @param startPos     Position to start flood filling from
     * @param fillPosStack Array that stores the positions of cells in the grid that were flood filled
     * @param helperStack  Stack used for storing GridPoints in the algorithm
     * @return A Color which depicts which color can be filled in the flood filled locations.
     * If 'null' or 'Color.BLACK', multiple or no colors were present along the edges, or a wall was in between
     */
    private Color floodFill(Cell[][] mapGrid, FloodFillGridState[][] gridState, GridPoint2 startPos,
                            Array<GridPoint2> fillPosStack, GridPoint2[] helperStack) {
        Color fillColor = null;
        helperStackTop = -1;

        gridState[startPos.x][startPos.y].state = FloodFillGridState.State.VISITED;
        if (!mapGrid[startPos.x][startPos.y].isMovable) {
            fillColor = Color.BLACK;
        }

        helperStack[++helperStackTop] = startPos;
        while (helperStackTop != -1) {
            GridPoint2 pos = helperStack[helperStackTop--];

            if (pos.x > 0) {
                fillColor = fillColorHelper(mapGrid, pos.x - 1, pos.y, fillColor, gridState, helperStack);
            }
            if (pos.x < rows - 1) {
                fillColor = fillColorHelper(mapGrid, pos.x + 1, pos.y, fillColor, gridState, helperStack);
            }
            if (pos.y > 0) {
                fillColor = fillColorHelper(mapGrid, pos.x, pos.y - 1, fillColor, gridState, helperStack);
            }
            if (pos.y < cols - 1) {
                fillColor = fillColorHelper(mapGrid, pos.x, pos.y + 1, fillColor, gridState, helperStack);
            }
            if (pos.x > 0 && pos.y > 0) {
                fillColor = fillColorHelper(mapGrid, pos.x - 1, pos.y - 1, fillColor, gridState, helperStack);
            }
            if (pos.x > 0 && pos.y < cols - 1) {
                fillColor = fillColorHelper(mapGrid, pos.x - 1, pos.y + 1, fillColor, gridState, helperStack);
            }
            if (pos.x < rows - 1 && pos.y > 0) {
                fillColor = fillColorHelper(mapGrid, pos.x + 1, pos.y - 1, fillColor, gridState, helperStack);
            }
            if (pos.x < rows - 1 && pos.y < cols - 1) {
                fillColor = fillColorHelper(mapGrid, pos.x + 1, pos.y + 1, fillColor, gridState, helperStack);
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
    private Color fillColorHelper(Cell[][] mapGrid, int x, int y, Color fillColor, FloodFillGridState[][] gridState, GridPoint2[] helperStack) {
        if (!mapGrid[x][y].isMovable) {
            fillColor = Color.BLACK;
        } else if (mapGrid[x][y].cellColor != null) {
            if (fillColor == null) {
                fillColor = mapGrid[x][y].cellColor;
            } else if (!Map.equalColors(fillColor, mapGrid[x][y].cellColor)) {
                fillColor = Color.BLACK;
            }
        }

        if (gridState[x][y].state == FloodFillGridState.State.UNVISITED) {
            gridState[x][y].state = FloodFillGridState.State.VISITED;
            helperStack[++helperStackTop] = gridPointPool[nextFreeIndex++].set(x, y);
        }

        return fillColor;
    }

    private static class FloodFillGridState {
        private enum State {UNVISITED, VISITED}

        private State state;

        FloodFillGridState() {
            state = State.UNVISITED;
        }
    }
}
