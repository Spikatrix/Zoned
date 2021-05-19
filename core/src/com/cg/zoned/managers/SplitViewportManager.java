package com.cg.zoned.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.dataobjects.GameTouchPoint;
import com.cg.zoned.ui.FocusableStage;

public class SplitViewportManager {
    private Viewport[] splitViewports;
    private Vector2[] dragOffset;

    private float targetZoom = Constants.ZOOM_MIN_VALUE;
    private float cameraPosLerpVal = 2.5f;
    private float cameraZoomLerpVal = 5.4f;

    public SplitViewportManager(int splitCount, float worldSize, Vector2 centerPos) {
        splitViewports = new ExtendViewport[splitCount];
        for (int i = 0; i < splitCount; i++) {
            splitViewports[i] = new ExtendViewport(worldSize / splitCount, worldSize);
        }
        setViewportCameraPosition(centerPos);
    }

    public SplitViewportManager(int splitCount, float worldSize, float stretchAspectRatio, Vector2 centerPos) {
        splitViewports = new StretchViewport[splitCount];
        for (int i = 0; i < splitCount; i++) {
            splitViewports[i] = new StretchViewport(worldSize * stretchAspectRatio, worldSize);
        }
        setViewportCameraPosition(centerPos);
    }

    public void setViewportCameraPosition(Vector2 centerPos) {
        if (centerPos == null) {
            return;
        }

        for (Viewport splitViewport : splitViewports) {
            Vector3 cameraPos = splitViewport.getCamera().position;
            cameraPos.set(centerPos.x, centerPos.y, cameraPos.z);
        }
    }

    public void setUpDragOffset(FocusableStage screenStage) {
        int splitCount = splitViewports.length;

        final GameTouchPoint[] touchPoint = new GameTouchPoint[splitCount];
        dragOffset = new Vector2[splitCount];
        for (int i = 0; i < splitCount; i++) {
            dragOffset[i] = new Vector2(0, 0);
            touchPoint[i] = new GameTouchPoint(0, 0, -1, null, -1);
        }

        screenStage.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (screenStage.dialogIsActive()) {
                    return false;
                }

                int splitPaneIndex = 0;
                float width = screenStage.getViewport().getWorldWidth();
                for (int i = 1; i < splitCount; i++) {
                    if (x > ((width / splitCount) * i)) {
                        splitPaneIndex++;
                    } else {
                        break;
                    }
                }

                if (touchPoint[splitPaneIndex].pointer == -1) {
                    touchPoint[splitPaneIndex].pointer = pointer;
                    touchPoint[splitPaneIndex].point.x = (int) x;
                    touchPoint[splitPaneIndex].point.y = (int) y;
                }

                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                for (int i = 0; i < splitCount; i++) {
                    if (touchPoint[i].pointer == pointer) {
                        dragOffset[i].x = touchPoint[i].point.x - x;
                        dragOffset[i].y = touchPoint[i].point.y - y;
                        break;
                    }
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                for (int i = 0; i < splitCount; i++) {
                    if (touchPoint[i].pointer == pointer) {
                        dragOffset[i].set(0, 0);
                        touchPoint[i].pointer = -1;
                        break;
                    }
                }
            }
        });
    }

    public void toggleZoom() {
        if (targetZoom == Constants.ZOOM_MIN_VALUE) {
            targetZoom = Constants.ZOOM_MAX_VALUE;
        } else if (targetZoom == Constants.ZOOM_MAX_VALUE) {
            targetZoom = Constants.ZOOM_MIN_VALUE;
        }
    }

    public void setCameraPosLerpVal(float cameraPosLerpVal) {
        this.cameraPosLerpVal = cameraPosLerpVal;
    }

    public void setCameraZoomLerpVal(float cameraZoomLerpVal) {
        this.cameraZoomLerpVal = cameraZoomLerpVal;
    }

    public void resize(int width, int height) {
        int splitCount = splitViewports.length;
        for (int i = 0; i < splitCount; i++) {
            splitViewports[i].update(width / splitCount, height);
            splitViewports[i].setScreenX(i * width / splitCount);
        }
    }

    public void setScreenPosition(int screenX, int screenY) {
        for (Viewport viewport : splitViewports) {
            viewport.setScreenPosition(screenX, screenY);
        }
    }

    public void render(ShapeDrawer shapeDrawer, SpriteBatch batch, Map map, Player[] players, float delta) {
        render(shapeDrawer, batch, map, players, 0, delta);
    }

    public void render(ShapeDrawer shapeDrawer, Batch batch, Map map,
                        Player[] players, int playerStartIndex, float delta) {
        for (int viewportIndex = 0; viewportIndex < splitViewports.length; viewportIndex++) {
            renderMap(shapeDrawer, batch, map, players, playerStartIndex + viewportIndex, viewportIndex, delta);
        }
    }

    private void renderMap(ShapeDrawer shapeDrawer, Batch batch, Map map,
                            Player[] players, int playerIndex, int viewportIndex, float delta) {
        if (playerIndex >= players.length) {
            return;
        }

        Viewport viewport = splitViewports[viewportIndex];
        Vector2 dragVel = ((dragOffset != null) ? (dragOffset[viewportIndex]) : (null));

        focusCameraOnPlayer(viewport, players[playerIndex], dragVel, delta);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        map.render(players, playerIndex, shapeDrawer, (OrthographicCamera) viewport.getCamera(), delta);
        batch.end();
    }

    private void focusCameraOnPlayer(Viewport viewport, Player player, Vector2 vel, float delta) {
        OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();

        Vector3 position = camera.position;

        float posX = (player.position.x * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;
        float posY = (player.position.y * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;

        if (vel != null) {
            posX += vel.x;
            posY += vel.y;
        }

        position.x += (posX - position.x) * cameraPosLerpVal * delta;
        position.y += (posY - position.y) * cameraPosLerpVal * delta;

        camera.zoom += (targetZoom - camera.zoom) * cameraZoomLerpVal * delta;
    }
}
