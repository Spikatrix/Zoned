package com.cg.zoned;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.cg.zoned.Constants.Direction;

public class Player extends InputAdapter {
    public Color color;
    public String name;
    public int score;

    public int[] controls;
    public Direction direction;
    public Direction updatedDirection;

    public Vector2 position;
    public Vector2 prevPosition;
    public Vector2 targetPosition;

    public boolean dummyMoving;
    private Vector2 dummyPosition;

    private float timeElapsed;

    public Player(Color color, String name) {
        this.color = color;
        this.name = name;
        this.score = 0;

        this.position = new Vector2();
        this.prevPosition = null;
        this.targetPosition = null;

        this.dummyPosition = new Vector2(0, 0);
        this.dummyMoving = false;

        this.direction = this.updatedDirection = null;

        this.controls = Constants.PLAYER_CONTROLS[0]; // Default is the first control scheme
    }

    public void setStartPos(GridPoint2 pos) {
        position = new Vector2(pos.x, pos.y);
    }

    public void setControlIndex(int index) {
        controls = Constants.PLAYER_CONTROLS[index];
    }

    public void moveTo(Vector2 targetPosition, float delta) {
        if (this.targetPosition == null) {
            this.targetPosition = targetPosition;
        }

        timeElapsed += delta;
        this.position.lerp(this.targetPosition, timeElapsed / Constants.PLAYER_MOVEMENT_LERP_VALUE);

        if (timeElapsed >= Constants.PLAYER_MOVEMENT_MAX_TIME) {
            timeElapsed = 0;
            this.position.x = Math.round(this.targetPosition.x);
            this.position.y = Math.round(this.targetPosition.y);
            this.targetPosition = null;
            this.direction = null;
        }
    }

    public void dummyMoveTo(Vector2 targetPosition, float delta) { // Simulate a fake movement for timed movement from all players
        if (this.targetPosition == null) {
            this.targetPosition = targetPosition;
            this.dummyMoving = true;
        }

        timeElapsed += delta;
        this.dummyPosition.lerp(this.targetPosition, timeElapsed / Constants.PLAYER_MOVEMENT_LERP_VALUE);

        if (timeElapsed >= Constants.PLAYER_MOVEMENT_MAX_TIME) {
            timeElapsed = 0;
            this.dummyMoving = false;
            this.dummyPosition.set(0, 0);
            this.targetPosition = null;
            this.direction = null;
        }
    }

    public void render(Rectangle userViewRect, Batch batch, TextureRegion playerTexture, float playerTextureRegionScale) {
        float startX = (this.position.x * Constants.CELL_SIZE);
        float startY = (this.position.y * Constants.CELL_SIZE);

        if (userViewRect.contains(startX, startY)) {
            batch.draw(playerTexture, startX, startY,
                    playerTexture.getRegionWidth() / playerTextureRegionScale,
                    playerTexture.getRegionHeight() / playerTextureRegionScale);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == controls[Direction.UP.ordinal()]) {
            updatedDirection = Direction.UP;
        } else if (keycode == controls[Direction.RIGHT.ordinal()]) {
            updatedDirection = Direction.RIGHT;
        } else if (keycode == controls[Direction.DOWN.ordinal()]) {
            updatedDirection = Direction.DOWN;
        } else if (keycode == controls[Direction.LEFT.ordinal()]) {
            updatedDirection = Direction.LEFT;
        }

        return false;
    }
}
