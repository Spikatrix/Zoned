package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.ClickPoint;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;

public class PlayerTouchManager extends InputAdapter {
    private Player[] players;
    private boolean isSplitScreenMultiplayer;

    private Array<ClickPoint> clickPoints;

    public PlayerTouchManager(Player[] players, boolean isSplitScreen) {
        this.clickPoints = new Array<ClickPoint>();
        this.players = players;
        this.isSplitScreenMultiplayer = isSplitScreen;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            clickPoints.add(new ClickPoint(screenX, screenY, pointer));

            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        for (ClickPoint clickPoint : clickPoints) {
            if (clickPoint.pointer == pointer) {
                GridPoint2 endPoint = new GridPoint2(screenX, screenY);
                GridPoint2 subPoint = endPoint.sub(clickPoint.point);

                Player player = this.players[0];
                if (isSplitScreenMultiplayer) {
                    int index = 0;
                    for (int i = 1; i < this.players.length; i++) {
                        if (clickPoint.point.x > ((Gdx.graphics.getWidth() / this.players.length) * i)) {
                            index++;
                        } else {
                            break;
                        }
                    }

                    player = this.players[index];
                }

                if (Math.abs(subPoint.x) > Math.abs(subPoint.y)) {
                    if (subPoint.x > 0) {
                        player.updatedDirection = Constants.Direction.RIGHT;
                    } else {
                        player.updatedDirection = Constants.Direction.LEFT;
                    }
                } else {
                    if (subPoint.y > 0) {
                        player.updatedDirection = Constants.Direction.DOWN;
                    } else {
                        player.updatedDirection = Constants.Direction.UP;
                    }
                }

                clickPoints.removeValue(clickPoint, true);
                return true;
            }
        }

        return false;
    }
}
