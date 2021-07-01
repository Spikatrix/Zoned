package com.cg.zoned.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;

public class AnimatedDrawable extends BaseDrawable {

    private Animation<TextureRegion> animation;
    private TextureRegion keyFrame;
    private float stateTime = 0;

    private float width;
    private float height;
    private float scaleFactor;

    public AnimatedDrawable(Animation<TextureRegion> animation, float buttonWidth, float buttonHeight, float scaleFactor) {
        this.animation = animation;
        this.width = buttonWidth;
        this.height = buttonHeight;
        this.scaleFactor = scaleFactor;

        TextureRegion key = animation.getKeyFrame(0);

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
        keyFrame = animation.getKeyFrame(stateTime, true);

        batch.draw(keyFrame, x, y, this.width * scaleFactor, this.height * scaleFactor);
    }
}