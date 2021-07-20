package com.cg.zoned;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;

public class Player extends PlayerEntity implements InputProcessor {
    private int[] controls;

    public Player(Color color, String name) {
        super(color, name);
        this.controls = Constants.PLAYER_CONTROLS[0]; // The first control scheme is used by default
    }

    public Player(Player player) {
        super(player);
        this.controls = player.controls;
    }

    public void setControlScheme(int index) {
        this.controls = Constants.PLAYER_CONTROLS[index];
    }


    public int[] getControls() {
        return controls;
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

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
