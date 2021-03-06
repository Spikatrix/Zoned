package com.cg.zoned.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.cg.zoned.MapSelector;

public class Spinner extends Table {
    private TextButton leftButton;
    private StepScrollPane stepScrollPane;
    private TextButton rightButton;

    private boolean isVerticalSpinner;

    private float scrollPaneHeight;
    private float scrollPaneWidth;
    private int buttonStepCount = 10;

    public Spinner(Skin skin, float scrollPaneHeight, float scrollPaneWidth, boolean isVerticalSpinner) {
        this.scrollPaneHeight = scrollPaneHeight;
        this.scrollPaneWidth = scrollPaneWidth;
        this.isVerticalSpinner = isVerticalSpinner;
        init(skin);
    }

    private void init(Skin skin) {
        this.leftButton = new TextButton("  -  ", skin);
        this.stepScrollPane = new StepScrollPane(skin, isVerticalSpinner);
        this.rightButton = new TextButton("  +  ", skin);

        this.leftButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                snapToStep(-buttonStepCount);
            }
        });

        this.rightButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                snapToStep(buttonStepCount);
            }
        });

        addAllComponentsToSpinner();
    }

    private void addAllComponentsToSpinner() {
        add(leftButton).growY();
        add(stepScrollPane).height(scrollPaneHeight).width(scrollPaneWidth);
        add(rightButton).growY();

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

    public void replaceContent(int index, Actor actor) {
        ((Table) stepScrollPane.getActor()).getCells().get(index).setActor(actor);
        stepScrollPane.layout();
    }

    public void setButtonStepCount(int buttonStepCount) {
        this.buttonStepCount = buttonStepCount;
    }

    public void snapToStep(int stepOffset) {
        stepScrollPane.snapToStep(stepOffset);
    }

    public int getPositionIndex() {
        return stepScrollPane.getPositionIndex();
    }

    public void setExtendedListener(final MapSelector.ExtendedMapSelectionListener extendedMapSelectionListener) {
        stepScrollPane.addListener(new ActorGestureListener() {
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {
                if (count >= 2 && button == Input.Buttons.LEFT) {
                    extendedMapSelectionListener.onExtendedMapSelectorOpened();
                }
            }
        });
    }

    public void setDestinationPositionListener(StepScrollPane.StepScrollListener stepScrollListener) {
        stepScrollPane.setStepScrollListener(stepScrollListener);
    }

    public TextButton getLeftButton() {
        return leftButton;
    }

    public TextButton getRightButton() {
        return rightButton;
    }

    public float getSpinnerHeight() {
        return scrollPaneHeight;
    }

    public float getSpinnerWidth() {
        return scrollPaneWidth;
    }
}
