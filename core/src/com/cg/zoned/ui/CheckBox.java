package com.cg.zoned.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

/**
 * A checkbox is a button that contains an image indicating the checked or unchecked state and a label.
 *
 * @author Nathan Sweet
 * <p>
 * Slightly modified from the official implementation by Nathan Sweet.
 * Has a <code>reversed</code> parameter for RTL implementation rather than LTR
 */
public class CheckBox extends TextButton {
    private Image image;
    private Cell imageCell;
    private com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle style;

    public CheckBox(String text, Skin skin, boolean reversed) {
        this(text, skin.get(com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle.class), reversed);
    }

    public CheckBox(String text, Skin skin, String styleName, boolean reversed) {
        this(text, skin.get(styleName, com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle.class), reversed);
    }

    public CheckBox(String text, com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle style, boolean reversed) {
        super(text, style);
        clearChildren();
        Label label = getLabel();
        image = new Image(style.checkboxOff, Scaling.none);
        if (!reversed) {
            imageCell = add(image);
            add(label);
            label.setAlignment(Align.left);
        } else {
            add(label);
            imageCell = add(image);
            label.setAlignment(Align.right);
        }
        setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Returns the checkbox's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is
     * called.
     */
    public com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle getStyle() {
        return style;
    }

    public void setStyle(ButtonStyle style) {
        if (!(style instanceof com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle))
            throw new IllegalArgumentException("style must be a CheckBoxStyle.");
        super.setStyle(style);
        this.style = (com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle) style;
    }

    public void draw(Batch batch, float parentAlpha) {
        Drawable checkbox = null;
        if (isDisabled()) {
            if (isChecked() && style.checkboxOnDisabled != null)
                checkbox = style.checkboxOnDisabled;
            else
                checkbox = style.checkboxOffDisabled;
        }
        if (checkbox == null) {
            boolean over = isOver() && !isDisabled();
            if (isChecked() && style.checkboxOn != null)
                checkbox = over && style.checkboxOnOver != null ? style.checkboxOnOver : style.checkboxOn;
            else if (over && style.checkboxOver != null)
                checkbox = style.checkboxOver;
            else
                checkbox = style.checkboxOff;
        }
        image.setDrawable(checkbox);
        super.draw(batch, parentAlpha);
    }

    public Image getImage() {
        return image;
    }

    public Cell getImageCell() {
        return imageCell;
    }

    /**
     * The style for a select box, see {@link com.badlogic.gdx.scenes.scene2d.ui.CheckBox}.
     *
     * @author Nathan Sweet
     */
    static public class CheckBoxStyle extends TextButtonStyle {
        public Drawable checkboxOn, checkboxOff;
        /**
         * Optional.
         */
        public Drawable checkboxOnOver, checkboxOver, checkboxOnDisabled, checkboxOffDisabled;

        public CheckBoxStyle() {
        }

        public CheckBoxStyle(Drawable checkboxOff, Drawable checkboxOn, BitmapFont font, Color fontColor) {
            this.checkboxOff = checkboxOff;
            this.checkboxOn = checkboxOn;
            this.font = font;
            this.fontColor = fontColor;
        }

        public CheckBoxStyle(com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle style) {
            super(style);
            this.checkboxOff = style.checkboxOff;
            this.checkboxOn = style.checkboxOn;
            this.checkboxOver = style.checkboxOver;
            this.checkboxOffDisabled = style.checkboxOffDisabled;
            this.checkboxOnDisabled = style.checkboxOnDisabled;
        }
    }
}
