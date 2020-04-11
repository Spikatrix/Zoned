package com.cg.zoned.ui;

import com.badlogic.gdx.math.Rectangle;
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

    private float scrollPaneHeight;

    private float buttonHeight;
    private float buttonWidth;
    private int buttonStepCount = 10;

    public Spinner(Skin skin, float scrollPaneHeight) {
        this.scrollPaneHeight = scrollPaneHeight;
        init(skin);
    }

    private void init(Skin skin) {
        this.leftButton = new TextButton("+", skin);
        this.stepScrollPane = new StepScrollPane(skin);
        this.rightButton = new TextButton("-", skin);

        this.leftButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stepScrollPane.snapToStep(buttonStepCount);
            }
        });

        this.rightButton.addListener(new ClickListener() {
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

                if (Math.abs(touchY - y) >= 10) {
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

        buttonHeight = this.leftButton.getPrefHeight();
        buttonWidth = buttonHeight * .8f;

        addAllComponentsToSpinner();
    }

    private void addAllComponentsToSpinner() {
        add(leftButton).width(buttonWidth).height(buttonHeight);
        add(stepScrollPane).growX().height(scrollPaneHeight);
        add(rightButton).width(buttonWidth).height(buttonHeight);

        stepScrollPane.setHeight(scrollPaneHeight);
        stepScrollPane.setStepHeight(scrollPaneHeight);
        stepScrollPane.layout();
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

    public float getScrollYPos() {
        return stepScrollPane.getDestinationPosition();
    }

    public float getScrollPaneHeight() {
        return scrollPaneHeight;
    }

    public TextButton getLeftButton() {
        return leftButton;
    }

    public TextButton getRightButton() {
        return rightButton;
    }

    @Override
    public float getPrefWidth() {
        return this.leftButton.getWidth() + this.stepScrollPane.getWidth() / 2 + this.rightButton.getWidth();
        // Don't ask me where the magic "/ 2" came from
    }
}
