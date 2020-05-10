package com.cg.zoned.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;

public class AnimatedDrawable extends BaseDrawable {

    private Animation animation;
    private TextureRegion keyFrame;
    private float stateTime = 0;

    public AnimatedDrawable(Animation animation) {

        this.animation = animation;
        TextureRegion key = (TextureRegion) animation.getKeyFrame(0);

        this.setLeftWidth(key.getRegionWidth() / 2f);
        this.setRightWidth(key.getRegionWidth() / 2f);
        this.setTopHeight(key.getRegionHeight() / 2f);
        this.setBottomHeight(key.getRegionHeight() / 2f);
        this.setMinWidth(key.getRegionWidth());
        this.setMinHeight(key.getRegionHeight());
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {

        stateTime += Gdx.graphics.getDeltaTime();
        keyFrame = (TextureRegion) animation.getKeyFrame(stateTime, true);

        batch.draw(keyFrame, x, y, keyFrame.getRegionWidth(), keyFrame.getRegionHeight());
    }
}