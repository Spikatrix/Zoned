package com.cg.zoned.controls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;
import com.payne.games.piemenu.PieMenu;

import java.util.Arrays;

public class PieMenuControlManager extends InputAdapter {
    private Stage stage;
    private Player[] players;
    private boolean isSplitScreenMultiplayer;

    private int piemenuRadius = 80;
    private float scaleFactor;
    private PieMenu[] menus;
    private int[] pointers;
    private Vector2[] coords;

    public PieMenuControlManager(Player[] players, boolean isSplitScreen, Stage stage, float scaleFactor, Array<Texture> usedTextures) {
        this.players = players;
        this.isSplitScreenMultiplayer = isSplitScreen;
        this.stage = stage;
        this.menus = new PieMenu[players.length];
        this.pointers = new int[players.length];
        this.coords = new Vector2[players.length];
        this.scaleFactor = scaleFactor;
        Arrays.fill(pointers, -1);
        Arrays.fill(coords, new Vector2());

        setUpPieMenus(usedTextures);
    }

    private void setUpPieMenus(Array<Texture> usedTextures) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        Texture tmpTex = new Texture(pixmap);
        usedTextures.add(tmpTex);
        pixmap.dispose();
        TextureRegion whitePixel = new TextureRegion(tmpTex);

        Texture arrowTexture = new Texture(Gdx.files.internal("icons/control_icons/ic_arrow.png"));
        usedTextures.add(arrowTexture);
        final Drawable arrow = new TextureRegionDrawable(arrowTexture);

        for (int i = 0; i < menus.length; i++) {
            final PieMenu.PieMenuStyle style = new PieMenu.PieMenuStyle();
            style.separatorWidth = 1;
            style.backgroundColor = Color.BLACK;
            style.separatorColor = Color.BLACK;
            style.downColor = Color.WHITE;
            style.sliceColor = players[i].color;
            // Multiply by scaleFactor? Size kinda gets messed up when doing it
            menus[i] = new PieMenu(whitePixel, style, piemenuRadius);

            final Image[] arrowImages = new Image[]{
                    new Image(arrow),
                    new Image(arrow),
                    new Image(arrow),
                    new Image(arrow),
            };
            for (int j = 0; j < arrowImages.length; j++) {
                arrowImages[j].setOrigin(arrowImages[j].getWidth() / 2, arrowImages[j].getHeight() / 2);
                arrowImages[j].setScaling(Scaling.fit);
                arrowImages[j].setColor(Color.WHITE);
                if (j == 0) {
                    arrowImages[j].setRotation(-45f);
                } else if (j == 1) {
                    arrowImages[j].setRotation(45f);
                } else if (j == 2) {
                    arrowImages[j].setRotation(135f);
                } else if (j == 3) {
                    arrowImages[j].setRotation(-135f);
                }
                menus[i].addActor(arrowImages[j]);
            }

            menus[i].setRotation(45f);
            menus[i].setInfiniteSelectionRange(true);
            menus[i].setVisible(false);
            final int finalI = i;
            menus[i].addListener(new PieMenu.PieMenuCallbacks() {
                @Override
                public void onHighlightChange(int highlightedIndex) {
                    for (Image arrowImage : arrowImages) {
                        arrowImage.setColor(Color.WHITE);
                    }
                    arrowImages[highlightedIndex].setColor(Color.BLACK);
                    if (highlightedIndex == 0) {
                        players[finalI].updatedDirection = Constants.Direction.UP;
                    } else if (highlightedIndex == 1) {
                        players[finalI].updatedDirection = Constants.Direction.LEFT;
                    } else if (highlightedIndex == 2) {
                        players[finalI].updatedDirection = Constants.Direction.DOWN;
                    } else if (highlightedIndex == 3) {
                        players[finalI].updatedDirection = Constants.Direction.RIGHT;
                    }
                }
            });
            menus[i].addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    menus[finalI].setVisible(false);
                    menus[finalI].remove();
                }
            });

            menus[i].setPieMenuListener(null);
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            int playerIndex = 0;
            if (isSplitScreenMultiplayer) {
                for (int i = 1; i < this.players.length; i++) {
                    if (screenX > ((stage.getWidth() / this.players.length) * i)) {
                        playerIndex++;
                    } else {
                        break;
                    }
                }
            }

            if (!menus[playerIndex].isVisible() && pointers[playerIndex] == -1) {
                stage.addActor(menus[playerIndex]);
                menus[playerIndex].setPosition(screenX, stage.getHeight() - screenY, Align.center);
                menus[playerIndex].setVisible(true);
                coords[playerIndex].x = screenX;
                coords[playerIndex].y = screenY;
                pointers[playerIndex] = pointer;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        for (int i = 0; i < pointers.length; i++) {
            if (pointers[i] == pointer) {
                coords[i].x = screenX;
                coords[i].y = screenY;
                Vector2 coord = stage.screenToStageCoordinates(coords[i]);
                menus[i].highlightSliceAtStage(coord.x, coord.y);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        for (int i = 0; i < pointers.length; i++) {
            if (pointers[i] == pointer && button == menus[i].getSelectionButton()) {
                coords[i].x = screenX;
                coords[i].y = screenY;
                Vector2 coord = stage.screenToStageCoordinates(coords[i]);
                if (menus[i].getHighlightedIndex() != -1) {
                    menus[i].selectSliceAtStage(coord.x, coord.y);
                } else {
                    menus[i].setVisible(false);
                    menus[i].remove();
                }
                pointers[i] = -1;

                return true;
            }
        }

        return false;
    }
}
