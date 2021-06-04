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

    private int[] controls;
    public Direction direction;
    public Direction updatedDirection;

    private final Vector2 position;
    public final GridPoint2 roundedPosition;
    public GridPoint2 prevPosition;
    private Vector2 targetPosition;

    private float timeElapsed;

    public Player(Color color, String name) {
        this.color = color;
        this.name = name;

        this.position = new Vector2();
        this.roundedPosition = new GridPoint2();
        initPrevPosition();

        this.controls = Constants.PLAYER_CONTROLS[0]; // Default is the first control scheme
    }

    public void setPosition(GridPoint2 pos) {
        setPosition(pos.x, pos.y);
    }

    public void setPosition(Vector2 pos) {
        // Nope, you're not allowed to place the player in between cells
        setPosition(Math.round(pos.x), Math.round(pos.y));
    }

    public void setPosition(int x, int y) {
        if (prevPosition != null) {
            prevPosition.set(roundedPosition);
        }
        position.set(x, y);
        roundedPosition.set(x, y);
    }

    public void initPrevPosition() {
        this.prevPosition = new GridPoint2(this.roundedPosition);
    }

    public void resetPrevPosition() {
        this.prevPosition = null;
    }

    public void setControlScheme(int index) {
        this.controls = Constants.PLAYER_CONTROLS[index];
    }

    public void move(float delta) {
        if (isMoving()) {
            // Move to the target position if it is available
            this.moveTo(this.targetPosition, delta);
        }
    }

    public void fakeMove(float delta) {
        if (isMoving()) {
            // Not allowed fake a movement when the player is already moving
            return;
        }

        this.moveTo(new Vector2(-1, -1), delta);
    }

    public void moveTo(Direction direction, float delta) {
        if (isMoving()) {
            // Not allowed to change direction when the player is already moving
            return;
        }

        this.targetPosition = new Vector2(this.position);
        if (direction == Direction.LEFT) {
            this.targetPosition.x--;
        } else if (direction == Direction.RIGHT) {
            this.targetPosition.x++;
        } else if (direction == Direction.DOWN) {
            this.targetPosition.y--;
        } else if (direction == Direction.UP) {
            this.targetPosition.y++;
        }

        this.move(delta);
    }

    private void moveTo(Vector2 targetPosition, float delta) {
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

        boolean fakeMovement = this.targetPosition.x < 0 || this.targetPosition.y < 0;
        if (fakeMovement) {
            setPosition(this.position);
        } else {
            setPosition(this.targetPosition);
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

    public float getPositionX() {
        return position.x;
    }

    public float getPositionY() {
        return position.y;
    }

    public Vector2 getPosition() {
        // New object so that the caller can't modify the position object content
        return new Vector2(this.position);
    }

    public int[] getControls() {
        return controls;
    }

    public enum Direction {UP, LEFT, DOWN, RIGHT}
}
