package com.cg.zoned.controls;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.dataobjects.GameTouchPoint;

public class FlingControlManager extends ControlTypeEntity {
    private Texture arrowTexture;

    private Array<GameTouchPoint> clickPoints;

    public FlingControlManager() {
    }

    public void init(Player[] players, boolean isSplitScreen, Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        super.init(players, isSplitScreen, stage, scaleFactor, usedTextures);

        this.clickPoints = new Array<>();

        arrowTexture = new Texture(Gdx.files.internal("images/control_icons/ic_arrow.png"));
        usedTextures.add(arrowTexture);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            int playerIndex = 0;
            if (isSplitScreen) {
                for (int i = 1; i < this.players.length; i++) {
                    if (screenX > ((stage.getWidth() / this.players.length) * i)) {
                        playerIndex++;
                    } else {
                        break;
                    }
                }
            }

            Image clickImage = new Image(arrowTexture);
            if (Gdx.app.getType() == Application.ApplicationType.Android) {
                // Scale both X and Y at the same rate or we'll have problems since the code below uses getScaleX only
                clickImage.setScale(scaleFactor, scaleFactor);
            }
            clickImage.setColor(players[playerIndex].color);
            clickImage.setPosition(screenX - clickImage.getWidth() * clickImage.getScaleX() / 2, stage.getHeight() - screenY - clickImage.getHeight() * clickImage.getScaleX() / 2);

            clickPoints.add(new GameTouchPoint(screenX, screenY, pointer, clickImage, playerIndex));
            stage.addActor(clickImage);

            return true;
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        for (GameTouchPoint gameTouchPoint : clickPoints) {
            if (gameTouchPoint.pointer == pointer) {
                Image clickImage = gameTouchPoint.clickImage;
                clickImage.clearActions();

                GridPoint2 endPoint = new GridPoint2(screenX, screenY);
                GridPoint2 subPoint = endPoint.sub(gameTouchPoint.point);

                if (Math.abs(subPoint.x) > Math.abs(subPoint.y)) {
                    if (subPoint.x > 0) { // RIGHT
                        clickImage.setRotation(-90f);
                        clickImage.setPosition(screenX - clickImage.getWidth() * clickImage.getScaleX() / 2, stage.getHeight() - screenY + clickImage.getHeight() * clickImage.getScaleX() / 2);
                    } else {              // LEFT
                        clickImage.setRotation(90f);
                        clickImage.setPosition(screenX + clickImage.getWidth() * clickImage.getScaleX() / 2, stage.getHeight() - screenY - clickImage.getHeight() * clickImage.getScaleX() / 2);
                    }
                } else {
                    if (subPoint.y > 0) { //DOWN
                        clickImage.setRotation(-180f);
                        clickImage.setPosition(screenX + clickImage.getWidth() * clickImage.getScaleX() / 2, stage.getHeight() - screenY + clickImage.getHeight() * clickImage.getScaleX() / 2);
                    } else {              // UP
                        clickImage.setRotation(0f);
                        clickImage.setPosition(screenX - clickImage.getWidth() * clickImage.getScaleX() / 2, stage.getHeight() - screenY - clickImage.getHeight() * clickImage.getScaleX() / 2);
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        for (GameTouchPoint gameTouchPoint : clickPoints) {
            if (gameTouchPoint.pointer == pointer) {
                final Image clickImage = gameTouchPoint.clickImage;

                GridPoint2 endPoint = new GridPoint2(screenX, screenY);
                GridPoint2 subPoint = endPoint.sub(gameTouchPoint.point);

                Player player = players[gameTouchPoint.playerIndex];

                float moveAnimationDistance = 60f * clickImage.getScaleX();
                MoveByAction moveAction;
                if (Math.abs(subPoint.x) > Math.abs(subPoint.y)) {
                    if (subPoint.x > 0) {
                        player.updatedDirection = Player.Direction.RIGHT;
                        moveAction = Actions.moveBy(moveAnimationDistance, 0f, .2f);
                    } else {
                        player.updatedDirection = Player.Direction.LEFT;
                        moveAction = Actions.moveBy(-moveAnimationDistance, 0f, .2f);
                    }
                } else {
                    if (subPoint.y > 0) {
                        player.updatedDirection = Player.Direction.DOWN;
                        moveAction = Actions.moveBy(0f, -moveAnimationDistance, .2f);
                    } else {
                        player.updatedDirection = Player.Direction.UP;
                        moveAction = Actions.moveBy(0f, moveAnimationDistance, .2f);
                    }
                }

                clickImage.addAction(
                        Actions.sequence(
                                Actions.parallel(moveAction, Actions.fadeOut(.2f)),
                                Actions.run(() -> stage.getRoot().removeActor(clickImage))
                        )
                );
                clickPoints.removeValue(gameTouchPoint, true);

                return true;
            }
        }

        return false;
    }
}
