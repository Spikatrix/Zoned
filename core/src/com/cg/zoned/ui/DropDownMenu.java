package com.cg.zoned.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

public class DropDownMenu extends SelectBox<String> {
    public DropDownMenu(Skin skin) {
        super(skin);

        customSetup();
    }

    private void customSetup() {
        this.getList().setAlignment(Align.center);
        this.setAlignment(Align.center);
    }

    public boolean isExpanded() {
        return this.getList().isTouchable();
    }

    // Decreased fadeIn and fadeOut duration so that the cut-off part is less noticable
    @Override
    protected void onShow(Actor selectBoxList, boolean below) {
        selectBoxList.getColor().a = 0;
        selectBoxList.addAction(fadeIn(.1f));
    }

    @Override
    protected void onHide(Actor selectBoxList) {
        selectBoxList.getColor().a = 1;
        selectBoxList.addAction(sequence(fadeOut(.1f), removeActor()));
    }
}
