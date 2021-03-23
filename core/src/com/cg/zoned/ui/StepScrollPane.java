package com.cg.zoned.ui;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class StepScrollPane extends ScrollPane {
    private Table content;

    private final boolean isVerticalScrollPane;

    private boolean wasPanDragFling = false;
    private float destinationPosition = 0f;
    private int prevStepIndex;
    private float stepSize = 0;
    private StepScrollListener stepScrollListener;

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

    public void setStepScrollListener(StepScrollListener stepScrollListener) {
        this.stepScrollListener = stepScrollListener;
    }

    private void addCustomScrollListener() {
        addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                // `amountX` is always 0 for me on Linux even though horizontal scrolling is enabled via `synclient` ¯\_(ツ)_/¯
                snapToStep((int) amountY);
                return true;
            }
        });
    }

    private void disableScrollBars() {
        this.setScrollBarTouch(false);
        this.setScrollbarsVisible(false);
        this.setScrollbarsOnTop(true);
        this.setScrollingDisabled(isVerticalScrollPane, !isVerticalScrollPane);
        this.setupFadeScrollBars(0f, 0f);
    }

    private void setUpContentTable() {
        content = new Table();
        this.setActor(content);
    }

    public void add(Actor actor) {
        // Might have bugs, hmm
        if (isVerticalScrollPane) {
            content.add(actor).width(getWidth());
        } else {
            content.add(actor).width(getWidth()).height(getHeight());
        }
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

                if (stepScrollListener != null) {
                    float scroll = isVerticalScrollPane ? getScrollY() : getScrollX();
                    int currStepIndex = (int) (scroll / stepSize);
                    if (prevStepIndex != currStepIndex) {
                        stepScrollListener.stepChanged(currStepIndex);
                        prevStepIndex = currStepIndex;
                    }
                }
            }
        }
    }

    public void snapToStep(int stepOffset) {
        if (isVerticalScrollPane) {
            snapToStepY(stepOffset);
        } else {
            snapToStepX(stepOffset);
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

        float oldDestPos = destinationPosition;

        destinationPosition = MathUtils.clamp(scrollYPos + offset, 0, getMaxY());
        setScrollY(destinationPosition);

        if (oldDestPos != destinationPosition && stepScrollListener != null) {
            stepScrollListener.destinationPositionChanged(destinationPosition, oldDestPos);
        }
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

        float oldDestPos = destinationPosition;

        destinationPosition = MathUtils.clamp(scrollXPos + offset, 0, getMaxX());
        setScrollX(destinationPosition);

        if (oldDestPos != destinationPosition && stepScrollListener != null) {
            stepScrollListener.destinationPositionChanged(destinationPosition, oldDestPos);
        }
    }

    public int getPositionIndex() {
        return Math.round(destinationPosition / stepSize);
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

    public interface StepScrollListener {
        void destinationPositionChanged(float newDestPos, float oldDestPos);

        void stepChanged(int newStep);
    }
}
