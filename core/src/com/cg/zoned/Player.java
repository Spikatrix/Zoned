package com.cg.zoned;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player extends InputAdapter {
    public Color color;
    public String name;

    private int[] controls;
    public Direction direction;
    public Direction updatedDirection;

    private Vector2 position;
    private Vector2 targetPosition;
    private GridPoint2 prevPosition;
    private GridPoint2 roundedPosition;

    private float timeElapsed;

    public Player(Color color, String name) {
        this.color = color;
        this.name = name;

        this.position = new Vector2();
        this.roundedPosition = new GridPoint2();
        this.prevPosition = new GridPoint2(-1, -1);

        this.controls = Constants.PLAYER_CONTROLS[0]; // Default is the first control scheme
    }

    /**
     * Copy constructor used for cloning the player object
     *
     * @param player The player object to clone
     */
    public Player(Player player) {
        this.color = new Color(player.color);
        this.name = player.name;
        this.controls = player.controls;
        this.direction = player.direction;
        this.updatedDirection = player.updatedDirection;
        this.position = new Vector2(player.position);
        if (player.targetPosition != null) {
            this.targetPosition = new Vector2(player.targetPosition);
        }
        this.prevPosition = new GridPoint2(player.prevPosition);
        this.roundedPosition = new GridPoint2(player.roundedPosition);
        this.timeElapsed = player.timeElapsed;
    }

    public void setPosition(GridPoint2 pos) {
        setPosition(pos.x, pos.y);
    }

    public void setPosition(Vector2 pos) {
        // Nope, you're not allowed to place the player in between cells
        setPosition(Math.round(pos.x), Math.round(pos.y));
    }

    public void setPosition(int x, int y) {
        prevPosition.set(roundedPosition);
        position.set(x, y);
        roundedPosition.set(x, y);
    }

    public void resetPrevPosition() {
        this.prevPosition.set(-1, -1);
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

    public void moveTo(Direction direction, float delta) {
        if (isMoving()) {
            // Not allowed to change direction when the player is already moving
            return;
        }

        if (direction == null) {
            // Run a fake movement
            this.moveTo(new Vector2(-1, -1), delta);
            return;
        }

        this.targetPosition = new Vector2(this.position);
        updatePosition(this.targetPosition, direction);

        this.move(delta);
    }

    private void moveTo(Vector2 targetPosition, float delta) {
        this.targetPosition = targetPosition;

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

    private void updatePosition(Vector2 pos, Direction direction) {
        if (direction == Direction.LEFT) {
            pos.x--;
        } else if (direction == Direction.RIGHT) {
            pos.x++;
        } else if (direction == Direction.DOWN) {
            pos.y--;
        } else if (direction == Direction.UP) {
            pos.y++;
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

    public int getRoundedPositionX() {
        return roundedPosition.x;
    }

    public int getRoundedPositionY() {
        return roundedPosition.y;
    }

    public GridPoint2 getRoundedPosition() {
        // New object so that the caller can't modify the roundedPosition object content
        return new GridPoint2(this.roundedPosition);
    }

    public int getPreviousPositionX() {
        return prevPosition.x;
    }

    public int getPreviousPositionY() {
        return prevPosition.y;
    }

    public GridPoint2 getPreviousPosition() {
        // New object so that the caller can't modify the previousPosition object content
        return new GridPoint2(this.prevPosition);
    }

    public boolean hasPreviousPosition() {
        return getPreviousPositionX() > -1 && getPreviousPositionY() > -1;
    }

    public int[] getControls() {
        return controls;
    }

    public enum Direction {UP, LEFT, DOWN, RIGHT}
}
