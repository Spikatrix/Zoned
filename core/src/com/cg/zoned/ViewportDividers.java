package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ViewportDividers {
    private int dividerCount;
    private Color[] leftDividerColors;
    private Color[] rightDividerColors;

    public ViewportDividers(Viewport[] playerViewports, Player[] players) {
        this(playerViewports.length, players);
    }

    public ViewportDividers(int viewportCount, Player[] players) {
        this.dividerCount = viewportCount - 1;
        if (this.dividerCount < 1) {
            // No dividers needed since there's only one player viewport (Networked multiplayer)
            return;
        }

        leftDividerColors = new Color[this.dividerCount];
        rightDividerColors = new Color[this.dividerCount];

        updateDividerColors(players);
    }

    public void updateDividerColors(Player[] players) {
        updateDividerColors(players, 0);
    }

    /**
     * Updates the divider colors as per the supplied player list taking those from
     * playerIndex onwards into account. The divider color will be set to black if the index
     * goes out of bounds.
     *
     * @param players The list of players to update divider colors with
     * @param playerIndex The index of the players list to start from
     */
    public void updateDividerColors(Player[] players, int playerIndex) {
        if (this.dividerCount < 1) {
            return;
        }

        for (int i = 0; i < dividerCount; i++) {
            // This points to the same color reference, there's no need for a new color object clone
            if (i + playerIndex < players.length) {
                leftDividerColors[i] = players[i + playerIndex].color;
            } else {
                leftDividerColors[i] = Color.BLACK;
            }

            if (i + playerIndex + 1 < players.length) {
                rightDividerColors[i] = players[i + playerIndex + 1].color;
            } else {
                rightDividerColors[i] = Color.BLACK;
            }
        }
    }

    /**
     * Draws viewport dividers with a solid part and fade on both it's sides.
     * See {@link Constants#VIEWPORT_DIVIDER_FADE_WIDTH}, {@link Constants#VIEWPORT_DIVIDER_SOLID_WIDTH} and
     * {@link Constants#VIEWPORT_DIVIDER_FADE_COLOR}
     *
     * @param shapeDrawer Used to draw the divider lines with fade on both sides
     * @param screenStage Used to get the height and width of the screen to draw dividers at proper positions
     */
    public void render(ShapeDrawer shapeDrawer, Stage screenStage) {
        if (dividerCount < 1) {
            return;
        }

        float height = screenStage.getViewport().getWorldHeight();
        float dividerFadeWidth = Math.max(Constants.VIEWPORT_DIVIDER_FADE_WIDTH / dividerCount, 3f);
        float dividerSolidWidth = Math.max(Constants.VIEWPORT_DIVIDER_SOLID_WIDTH / dividerCount, 1f);
        for (int i = 0; i < dividerCount; i++) {
            float startX = (screenStage.getViewport().getWorldWidth() / (float) (dividerCount + 1)) * (i + 1);

            // Draws the solid rectangle
            shapeDrawer.filledRectangle(startX - dividerSolidWidth, 0,
                    dividerSolidWidth * 2, height,
                    rightDividerColors[i], leftDividerColors[i], leftDividerColors[i], rightDividerColors[i]);
            // Draws the fade effect on the right side
            shapeDrawer.filledRectangle(startX + dividerSolidWidth, 0,
                    dividerFadeWidth, height,
                    Constants.VIEWPORT_DIVIDER_FADE_COLOR, rightDividerColors[i], rightDividerColors[i], Constants.VIEWPORT_DIVIDER_FADE_COLOR);
            // Draws the fade effect on the left side
            shapeDrawer.filledRectangle(startX - dividerSolidWidth, 0,
                    -dividerFadeWidth, height,
                    Constants.VIEWPORT_DIVIDER_FADE_COLOR, leftDividerColors[i], leftDividerColors[i], Constants.VIEWPORT_DIVIDER_FADE_COLOR);
        }
    }
}
