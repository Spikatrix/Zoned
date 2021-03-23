package com.cg.zoned.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

public class DropDownMenu extends SelectBox<String> {
    private final float moveAmount = 8f;

    public DropDownMenu(Skin skin) {
        super(skin);

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
        selectBoxList.moveBy(0, moveAmount * 2);
        selectBoxList.addAction(Actions.parallel(
                Actions.moveBy(0, -moveAmount, .2f, Interpolation.smooth),
                Actions.fadeIn(.15f, Interpolation.smooth))
        );
    }

    @Override
    protected void onHide(Actor selectBoxList) {
        selectBoxList.getColor().a = 1;
        selectBoxList.addAction(Actions.sequence(
                Actions.parallel(
                    Actions.moveBy(0, moveAmount, .2f, Interpolation.smooth),
                    Actions.fadeOut(.15f)
                ),
                Actions.removeActor())
        );
    }
}
