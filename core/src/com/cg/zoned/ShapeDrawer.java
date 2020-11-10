package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

public class ShapeDrawer extends space.earlygrey.shapedrawer.ShapeDrawer {
    public ShapeDrawer(Batch batch, Skin skin) {
        super(batch, skin.getRegion("white"));
    }

    public ShapeDrawer(Batch batch, TextureRegion whiteTextureRegion) {
        super(batch, whiteTextureRegion);
    }

    public ShapeDrawer(Batch batch, Array<Texture> usedTextures) {
        super(batch, ShapeDrawer.get1x1TextureRegion(usedTextures));
    }

    public static TextureRegion get1x1TextureRegion(Array<Texture> usedTextures) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.drawPixel(0, 0);
        Texture texture = new Texture(pixmap);
        usedTextures.add(texture);
        pixmap.dispose();
        return new TextureRegion(texture, 0, 0, 1, 1);
    }
}
