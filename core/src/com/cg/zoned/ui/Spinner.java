package com.cg.zoned.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class Spinner extends Table {
    private TextButton plusButton;
    private StepScrollPane stepScrollPane;
    private TextButton minusButton;

    private float scrollPaneHeight;

    private float buttonHeight;
    private float buttonWidth = 60f;
    private int buttonStepCount = 10;

    public Spinner(Skin skin) {
        init(skin);
    }

    public Spinner(Skin skin, float scrollPaneHeight) {
        init(skin);
        this.scrollPaneHeight = scrollPaneHeight;
        addIntoTable();
    }

    private void init(Skin skin) {
        this.plusButton = new TextButton("+", skin);
        this.stepScrollPane = new StepScrollPane(skin);
        this.minusButton = new TextButton("-", skin);

        this.plusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stepScrollPane.snapToStep(buttonStepCount);
            }
        });

        this.minusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stepScrollPane.snapToStep(-buttonStepCount);
            }
        });

        this.addListener(new ClickListener() {
            final int RESET_VALUE = 100000;
            int touchY = RESET_VALUE;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Rectangle rectangle = new Rectangle(stepScrollPane.getX(), stepScrollPane.getY(),
                        stepScrollPane.getWidth(), stepScrollPane.getHeight());
                if (!rectangle.contains(x, y)) {
                    touchY = (int) y;
                }
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (touchY == RESET_VALUE) {
                    super.touchDragged(event, x, y, pointer);
                    return;
                }

                if (Math.abs(touchY - y) >= 5) {
                    if (touchY > y) {
                        stepScrollPane.snapToStep(-1);
                    } else {
                        stepScrollPane.snapToStep(+1);
                    }
                    touchY = (int) y;
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                touchY = RESET_VALUE;
                super.touchUp(event, x, y, pointer, button);
            }
        });
        this.setTouchable(Touchable.enabled);

        buttonHeight = this.plusButton.getPrefHeight();
    }

    public void generateValueLabel(int valLowLimit, int valHighLimit, Skin skin) {
        Label valueLabel = new Label("", skin);
        valueLabel.setAlignment(Align.center);
        valueLabel.setWrap(true);
        StringBuilder sb = new StringBuilder();
        for (int i = valLowLimit; i <= valHighLimit; i++) {
            sb.append(i);
            if (i != valHighLimit) {
                sb.append('\n');
            }
        }
        valueLabel.setText(sb.toString());

        this.scrollPaneHeight = (valueLabel.getPrefHeight() / (valHighLimit - valLowLimit + 1));
        stepScrollPane.add(valueLabel);
        stepScrollPane.setStepHeight(this.scrollPaneHeight);
        addIntoTable();
    }

    private void addIntoTable() {
        add(this.plusButton).width(buttonWidth).height(buttonHeight);
        add(this.stepScrollPane).growX().height(scrollPaneHeight);
        add(this.minusButton).width(buttonWidth).height(buttonHeight);

        this.stepScrollPane.setHeight(scrollPaneHeight);
        this.stepScrollPane.layout();
    }

    public void setButtonStepCount(int buttonStepCount) {
        this.buttonStepCount = buttonStepCount;
    }

    public StepScrollPane getStepScrollPane() {
        return stepScrollPane;
    }

    public float getScrollYPos() {
        return stepScrollPane.getDestinationPosition();
    }

    public float getScrollPaneHeight() {
        return scrollPaneHeight;
    }

    public TextButton getPlusButton() {
        return plusButton;
    }

    public TextButton getMinusButton() {
        return minusButton;
    }

    @Override
    public float getPrefWidth() {
        return this.plusButton.getWidth() + this.stepScrollPane.getWidth() / 2 + this.minusButton.getWidth();
    }
}
