package com.cg.zoned.controls;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;

public class ControlTypeEntity extends InputAdapter {
    protected Player[] players;
    protected boolean isSplitScreen;
    protected Stage stage;
    protected float scaleFactor;
    protected Array<Texture> usedTextures;

    public ControlTypeEntity() {}

    public void init(Player[] players, boolean isSplitScreen, Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        this.players = players;
        this.isSplitScreen = isSplitScreen;
        this.stage = stage;
        this.scaleFactor = scaleFactor;
        this.usedTextures = usedTextures;
    }
}
