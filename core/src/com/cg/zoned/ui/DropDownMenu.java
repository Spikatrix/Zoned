package com.cg.zoned.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

public class DropDownMenu extends SelectBox<String> {
    private float moveAmount = 8f;
    private float padding = 16f;

    public DropDownMenu(Skin skin) {
        this(skin, 1f);
    }

    public DropDownMenu(Skin skin, float scaleFactor) {
        super(skin);

        this.padding = this.padding * scaleFactor;
        this.moveAmount = this.moveAmount * scaleFactor;

        customSetup();
    }

    private void customSetup() {
        this.getList().setAlignment(Align.center);
        this.setAlignment(Align.center);
    }

    public void append(String s) {
        Array<String> itemList = getItems();
        itemList.add(s);
        setItems(itemList);
    }

    public boolean isExpanded() {
        return this.getList().isTouchable();
    }

    // Custom animations on show and hide
    @Override
    protected void onShow(Actor selectBoxList, boolean below) {
        selectBoxList.getColor().a = 0;
        selectBoxList.moveBy(0, moveAmount);
        selectBoxList.addAction(Actions.parallel(
                Actions.moveBy(0, -moveAmount, .2f, Interpolation.smooth),
                Actions.fadeIn(.2f, Interpolation.smooth))
        );
    }

    @Override
    protected void onHide(Actor selectBoxList) {
        selectBoxList.getColor().a = 1;
        selectBoxList.addAction(Actions.sequence(
                Actions.parallel(
                    Actions.moveBy(0, moveAmount, .2f, Interpolation.smooth),
                    Actions.fadeOut(.2f)
                ),
                Actions.removeActor())
        );
    }

    @Override
    public float getPrefWidth() {
        return super.getPrefWidth() + padding;
    }

}
