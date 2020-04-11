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

    private boolean wasPanDragFling = false;
    private float destinationPosition = 0f;
    private float stepHeight = 0;

    public StepScrollPane(Skin skin) {
        super(null, skin);
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
            public boolean scrolled(InputEvent event, float x, float y, int amount) {
                snapToStep(amount);
                return true;
            }
        });
    }

    private void disableScrollBars() {
        this.setScrollbarsVisible(false);
        this.setScrollBarTouch(false);
        this.setScrollbarsOnTop(false);
        this.setScrollingDisabled(true, false);
    }

    private void setUpContentTable() {
        content = new Table();
        this.setActor(content);
    }

    public void add(Actor actor) {
        content.add(actor);
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
        if (stepHeight == 0) {
            // Use the scrollpane's height as the stepHeight if it was not set
            setStepHeight(this.getHeight());
        }

        float scrollYPos;
        if (stepOffset == 0) {
            scrollYPos = this.getScrollY();
        } else {
            scrollYPos = this.getScrollY() + stepHeight * stepOffset;
        }

        float offset = scrollYPos % stepHeight;
        if (offset > stepHeight - offset) {
            offset = stepHeight - offset;
        } else {
            offset = -offset;
        }

        float centerOffset = abs(this.getHeight() - stepHeight) / 2;

        destinationPosition = MathUtils.clamp(scrollYPos + offset + centerOffset, 0, getMaxY());
        setScrollY(destinationPosition);
    }

    public void setStepHeight(float stepHeight) {
        this.stepHeight = stepHeight;
    }

    public float getStepHeight() {
        return stepHeight;
    }

    public float getDestinationPosition() {
        return destinationPosition;
    }
}
