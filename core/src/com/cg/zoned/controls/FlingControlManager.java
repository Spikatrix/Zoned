package com.cg.zoned.controls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.dataobjects.GameTouchPoint;

public class FlingControlManager extends ControlTypeEntity {
    private Image[] arrowImages;
    private GameTouchPoint[] clickPoints;

    public FlingControlManager(Player[] players, boolean isSplitScreen, Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        super(players, isSplitScreen, stage, scaleFactor, usedTextures);

        this.clickPoints = new GameTouchPoint[players.length];
        this.arrowImages = new Image[players.length];

        Texture arrowTexture = new Texture(Gdx.files.internal("images/control_icons/ic_arrow.png"));
        for (int i = 0; i < players.length; i++) {
            clickPoints[i] = new GameTouchPoint();

            arrowImages[i] = new Image(arrowTexture);
            arrowImages[i].setColor(players[i].color);
            arrowImages[i].setOrigin(Align.center);
            arrowImages[i].setScale(scaleFactor);
            stage.addActor(arrowImages[i]);
        }

        usedTextures.add(arrowTexture);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            int playerIndex = getPlayerIndex(screenX);

            if (clickPoints[playerIndex].pointer != -1) {
                // There's already another pointer in use
                return false;
            }

            Image arrowImage = arrowImages[playerIndex];
            arrowImage.clearActions();
            arrowImage.getColor().a = 1f;
            arrowImage.setPosition(screenX, stage.getHeight() - screenY, Align.center);
            clickPoints[playerIndex].point.set(screenX, screenY);
            clickPoints[playerIndex].pointer = pointer;

            return true;
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        for (int i = 0; i < clickPoints.length; i++) {
            GameTouchPoint gameTouchPoint = clickPoints[i];
            if (gameTouchPoint.pointer == pointer) {
                Image arrowImage = arrowImages[i];

                GridPoint2 endPoint = new GridPoint2(screenX, screenY);
                GridPoint2 subPoint = endPoint.sub(gameTouchPoint.point);

                if (Math.abs(subPoint.x) > Math.abs(subPoint.y)) {
                    if (subPoint.x > 0) { // RIGHT
                        arrowImage.setRotation(-90f);
                    } else {              // LEFT
                        arrowImage.setRotation(90f);
                    }
                } else {
                    if (subPoint.y > 0) { // DOWN
                        arrowImage.setRotation(-180f);
                    } else {              // UP
                        arrowImage.setRotation(0f);
                    }
                }

                arrowImage.setPosition(screenX, stage.getHeight() - screenY, Align.center);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        for (int i = 0; i < clickPoints.length; i++) {
            GameTouchPoint gameTouchPoint = clickPoints[i];
            if (gameTouchPoint.pointer == pointer) {
                final Image arrowImage = arrowImages[i];
                float arrowImageRotation = arrowImage.getRotation();

                Player player = players[i];

                float moveAnimationDistance = 60f * scaleFactor;
                float moveAnimationDuration = .2f;
                Action moveAction = null;
                if (arrowImageRotation == -90f) {
                    player.updatedDirection = Player.Direction.RIGHT;
                    moveAction = Actions.moveBy(moveAnimationDistance, 0f, moveAnimationDuration);
                } else if (arrowImageRotation == 90f){
                    player.updatedDirection = Player.Direction.LEFT;
                    moveAction = Actions.moveBy(-moveAnimationDistance, 0f, moveAnimationDuration);
                } else if (arrowImageRotation == -180f) {
                    player.updatedDirection = Player.Direction.DOWN;
                    moveAction = Actions.moveBy(0f, -moveAnimationDistance, moveAnimationDuration);
                } else if (arrowImageRotation == 0f){
                    player.updatedDirection = Player.Direction.UP;
                    moveAction = Actions.moveBy(0f, moveAnimationDistance, moveAnimationDuration);
                }

                arrowImage.addAction(Actions.parallel(moveAction, Actions.fadeOut(.2f)));
                clickPoints[i].point.set(-1, -1);
                clickPoints[i].pointer = -1;

                return true;
            }
        }

        return false;
    }
}
