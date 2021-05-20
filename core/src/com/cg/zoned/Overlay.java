package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class Overlay {
    private Color overlayColor;
    private Color targetColor;
    private float lerpVal;
    private boolean enableOverlay;

    public Overlay(Color overlayColor) {
        this(overlayColor, 1.8f);
    }

    public Overlay(Color overlayColor, float lerpVal) {
        this(overlayColor, new Color(overlayColor), lerpVal);
    }

    public Overlay(Color overlayColor, Color targetColor, float lerpVal) {
        this.overlayColor = overlayColor;
        this.targetColor = targetColor;
        this.lerpVal = lerpVal;
        this.enableOverlay = true;
    }

    public void setLerpVal(float lerpVal) {
        this.lerpVal = lerpVal;
    }

    public void drawOverlay(boolean enableOverlay) {
        this.enableOverlay = enableOverlay;
    }

    public void toggleOverlay() {
        this.enableOverlay = !this.enableOverlay;
    }

    public void update(float delta) {
        update(getTargetColor(), delta);
    }

    private void update(Color targetColor, float delta) {
        if (targetColor == null) {
            return;
        }
        overlayColor.lerp(targetColor, lerpVal * delta);
    }

    public void completeAnimation() {
        if (getTargetColor() == null) {
            return;
        }

        overlayColor.set(getTargetColor());
    }

    public void render(ShapeDrawer shapeDrawer, Stage screenStage, float delta) {
        float width = screenStage.getViewport().getWorldWidth();
        float height = screenStage.getViewport().getWorldHeight();
        render(shapeDrawer, 0, 0, width, height, delta);
    }

    public void render(ShapeDrawer shapeDrawer, float startX, float startY, float width, float height, float delta) {
        update(delta);

        if (overlayColor.a >= 0.04f) {
            shapeDrawer.setColor(overlayColor);
            shapeDrawer.filledRectangle(startX, startY, width, height);
        }
    }

    public void setTargetColor(Color targetColor) {
        this.targetColor = targetColor;
    }

    public void setTargetColor(float r, float g, float b, float a) {
        if (this.targetColor == null) {
            setTargetColor(new Color(r, g, b, a));
        } else {
            this.targetColor.set(r, g, b, a);
        }
    }

    public float getOverlayAlpha() {
        return overlayColor.a;
    }

    private Color getTargetColor() {
        if (enableOverlay) {
            return targetColor;
        } else {
            return Color.CLEAR;
        }
    }
}
