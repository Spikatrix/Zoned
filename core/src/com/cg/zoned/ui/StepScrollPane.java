package com.cg.zoned.ui;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import static java.lang.Math.abs;

public class StepScrollPane extends ScrollPane {
    private Table content;

    private boolean isVerticalScrollPane;

    private boolean wasPanDragFling = false;
    private float destinationPosition = 0f;
    private float stepSize = 0;

    public StepScrollPane(Skin skin, boolean isVerticalScrollPane) {
        super(null, skin);
        this.isVerticalScrollPane = isVerticalScrollPane;
        disableScrollBars();
        setUpContentTable();

        removeDefaultScrollListener();
        addCustomScrollListener();
    }

    private void removeDefaultScrollListener() {
        EventListener eventListener = null;
        for (EventListener listener : getListeners()) {
            if (listener instanceof InputListener) {
                eventListener = listener;
                break;
            }
        }

        if (eventListener != null) {
            removeListener(eventListener);
        }
    }

    private void addCustomScrollListener() {
        addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                /* TODO: libGDX 1.9.12 issues I've noticed:
                          1. ScrollPane not using its full height
                          2. Test and integrate scrollpane horizontal scroll
                          3. ShapeDrawer issues
                 */
                com.badlogic.gdx.Gdx.app.log("test", "AmountX: " + amountX + " AmountY: " + amountY);
                snapToStep((int) amountY);
                return true;
            }
        });
    }

    private void disableScrollBars() {
        this.setScrollBarTouch(false);
        this.setScrollbarsOnTop(false);
        this.setScrollbarsVisible(false);
        this.setScrollingDisabled(isVerticalScrollPane, !isVerticalScrollPane);
        this.setupFadeScrollBars(0f, 0f);
    }

    private void setUpContentTable() {
        content = new Table();
        this.setActor(content);
    }

    public void add(Actor actor) {
        content.add(actor).width(getWidth());
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (wasPanDragFling && !isPanning() && !isDragging() && !isFlinging()) {
            wasPanDragFling = false;
            snapToStep(0); // Snap to closest step
        } else {
            if (isPanning() || isDragging() || isFlinging()) {
                wasPanDragFling = true;
            }
        }
    }

    public void snapToStep(int stepOffset) {
        if (isVerticalScrollPane) {
            snapToStepY(stepOffset);
        } else {
            snapToStepX(-stepOffset); // Have to invert, otherwise the direction is opposite
        }
    }

    private void snapToStepY(int stepOffset) {
        if (stepSize == 0) {
            // Use the scrollpane's height as the stepHeight if it was not set
            setStepSize(this.getHeight());
        }

        float scrollYPos;
        if (stepOffset == 0) {
            scrollYPos = this.getScrollY();
        } else {
            scrollYPos = this.getScrollY() + stepSize * stepOffset;
        }

        float offset = scrollYPos % stepSize;
        if (offset > stepSize - offset) {
            offset = stepSize - offset;
        } else {
            offset = -offset;
        }

        float centerOffset = abs(this.getHeight() - stepSize) / 2;

        destinationPosition = MathUtils.clamp(scrollYPos + offset + centerOffset, 0, getMaxY());
        setScrollY(destinationPosition);
    }

    private void snapToStepX(int stepOffset) {
        if (stepSize == 0) {
            // Use the scrollpane's width as the stepSize if it was not set
            setStepSize(this.getWidth());
        }

        float scrollXPos;
        if (stepOffset == 0) {
            scrollXPos = this.getScrollX();
        } else {
            scrollXPos = this.getScrollX() + stepSize * stepOffset;
        }

        float offset = scrollXPos % stepSize;
        if (offset > stepSize - offset) {
            offset = stepSize - offset;
        } else {
            offset = -offset;
        }

        float centerOffset = abs(this.getWidth() - stepSize) / 2;

        destinationPosition = MathUtils.clamp(scrollXPos + offset + centerOffset, 0, getMaxX());
        setScrollX(destinationPosition);
    }

    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        this.stepSize = stepSize;
    }

    public float getDestinationPosition() {
        return destinationPosition;
    }
}
