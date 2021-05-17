package com.cg.zoned;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player extends InputAdapter {
    // TODO: Too many public fields. Reduce access level using getters and setters etc
    public Color color;
    public String name;

    public int[] controls;
    public Direction direction;
    public Direction updatedDirection;

    public Vector2 position;
    public Vector2 prevPosition;
    private Vector2 targetPosition;
    public GridPoint2 roundedPosition;

    private float timeElapsed;

    public Player(Color color, String name) {
        this.color = color;
        this.name = name;

        this.position = new Vector2();
        this.prevPosition = null;
        this.targetPosition = null;
        this.roundedPosition = new GridPoint2();

        this.direction = this.updatedDirection = null;

        this.controls = Constants.PLAYER_CONTROLS[0]; // Default is the first control scheme
    }

    public void setPosition(GridPoint2 pos) {
        position = new Vector2(pos.x, pos.y);
        setRoundedPosition();
    }

    public void setControlIndex(int index) {
        controls = Constants.PLAYER_CONTROLS[index];
    }

    public void setRoundedPosition() {
        this.roundedPosition.x = Math.round(this.position.x);
        this.roundedPosition.y = Math.round(this.position.y);
    }

    public void move(float delta) {
        if (isMoving()) {
            // Move to the target position if it is available
            this.moveTo(this.targetPosition, delta);
        } else {
            // Simulate a fake movement if targetPosition is not available
            this.moveTo(new Vector2(-1, -1), delta);
        }
    }

    public void moveTo(Vector2 targetPosition, float delta) {
        if (this.targetPosition == null) {
            this.targetPosition = targetPosition;
        }

        // Fake movements are done to synchronize movement of all players at discrete intervals
        boolean fakeMovement = targetPosition.x < 0 || targetPosition.y < 0;
        timeElapsed += delta;

        if (!fakeMovement) {
            this.position.lerp(this.targetPosition, timeElapsed / Constants.PLAYER_MOVEMENT_LERP_VALUE);
        }

        if (timeElapsed >= Constants.PLAYER_MOVEMENT_MAX_TIME) {
            completeMovement();
        }
    }

    public void completeMovement() {
        if (!isMoving()) {
            return;
        }

        boolean fakeMovement = targetPosition.x < 0 || targetPosition.y < 0;
        if (!fakeMovement) {
            this.position.x = Math.round(this.targetPosition.x);
            this.position.y = Math.round(this.targetPosition.y);
            setRoundedPosition();
        }

        timeElapsed = 0;
        this.targetPosition = null;
        this.direction = null;
    }

    public boolean isMoving() {
        return this.targetPosition != null;
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

    public enum Direction {UP, LEFT, DOWN, RIGHT}
}
