package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class Spinner extends Table {
    private TextButton leftButton;
    private StepScrollPane stepScrollPane;
    private TextButton rightButton;

    private boolean isVerticalSpinner;

    private float scrollPaneHeight;
    private float scrollPaneWidth;
    private float buttonHeight;
    private float buttonWidth;
    private int buttonStepCount = 10;

    public Spinner(Skin skin, float scrollPaneHeight, float scrollPaneWidth, boolean isVerticalSpinner) {
        this.scrollPaneHeight = scrollPaneHeight;
        this.scrollPaneWidth = scrollPaneWidth;
        this.isVerticalSpinner = isVerticalSpinner;
        init(skin);
    }

    private void init(Skin skin) {
        this.leftButton = new TextButton("+", skin);
        this.stepScrollPane = new StepScrollPane(skin, isVerticalSpinner);
        this.rightButton = new TextButton("-", skin);

        this.leftButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                snapToStep(buttonStepCount);
            }
        });

        this.rightButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                snapToStep(-buttonStepCount);
            }
        });

        this.addListener(new ClickListener() {
            final float thresholdY = scrollPaneHeight / 2;
            final float thresholdX = scrollPaneWidth / 2;
            float touchPos;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (isVerticalSpinner) {
                    touchPos = y;
                } else {
                    touchPos = x;
                }
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (isVerticalSpinner) {
                    if (Math.abs(touchPos - y) >= thresholdY) {
                        if (touchPos > y) {
                            snapToStep(-1);
                        } else {
                            snapToStep(+1);
                        }
                        touchPos = y;
                    }
                } else {
                    if (Math.abs(touchPos - x) >= thresholdX) {
                        if (touchPos > x) {
                            snapToStep(-1);
                        } else {
                            snapToStep(+1);
                        }
                        touchPos = x;
                    }
                }
            }
        });
        this.setTouchable(Touchable.enabled);

        buttonHeight = this.leftButton.getPrefHeight();
        buttonWidth = buttonHeight * .8f;

        addAllComponentsToSpinner();
    }

    private void addAllComponentsToSpinner() {
        add(leftButton).width(buttonWidth).height(buttonHeight);
        add(stepScrollPane).growX().height(scrollPaneHeight).width(scrollPaneWidth);
        add(rightButton).width(buttonWidth).height(buttonHeight);

        stepScrollPane.setHeight(scrollPaneHeight);
        stepScrollPane.setWidth(scrollPaneWidth);
        if (isVerticalSpinner) {
            stepScrollPane.setStepSize(scrollPaneHeight);
        } else {
            stepScrollPane.setStepSize(scrollPaneWidth);
        }
        stepScrollPane.layout();
    }

    public void generateValueRange(int valLowLimit, int valHighLimit, Skin skin) {
        Label valueLabel = new Label("", skin);
        valueLabel.setAlignment(Align.center);
        StringBuilder sb = new StringBuilder();
        for (int i = valLowLimit; i <= valHighLimit; i++) {
            sb.append(i);
            if (i != valHighLimit) {
                sb.append('\n');
            }
        }
        valueLabel.setText(sb.toString());

        stepScrollPane.add(valueLabel);
        stepScrollPane.layout();
    }

    public void addContent(Actor actor) {
        stepScrollPane.add(actor);
        stepScrollPane.layout();
    }

    public void setButtonStepCount(int buttonStepCount) {
        this.buttonStepCount = buttonStepCount;
    }

    public void snapToStep(int stepOffset) {
        stepScrollPane.snapToStep(stepOffset);
    }

    public int getPositionIndex() {
        if (isVerticalSpinner) {
            return Math.round(stepScrollPane.getDestinationPosition() / scrollPaneHeight);
        } else {
            return Math.round(stepScrollPane.getDestinationPosition() / scrollPaneWidth);
        }
    }

    public TextButton getLeftButton() {
        return leftButton;
    }

    public TextButton getRightButton() {
        return rightButton;
    }
}
