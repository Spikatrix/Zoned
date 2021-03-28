package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;
import com.cg.zoned.maps.NoStartPositionsFound;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;
import com.cg.zoned.ui.StepScrollPane;

public class MapSelector {
    private MapManager mapManager;

    private Skin skin;
    private float scaleFactor;

    private FocusableStage stage;
    private Spinner mapSpinner;
    private Array<Boolean> mapPreviewChecked;
    private Array<Image> mapPreviewImages;
    private boolean extraParamsDialogActive;

    private Assets assets;
    private Array<Texture> usedTextures;

    public MapSelector(FocusableStage stage, float scaleFactor, Assets assets, Skin skin) {
        this.mapManager = new MapManager();

        this.stage = stage;
        this.skin = skin;
        this.assets = assets;
        this.scaleFactor = scaleFactor;

        mapPreviewChecked = new Array<>();
        mapPreviewImages = new Array<>();
    }

    public Spinner loadMapSelectorSpinner(float spinnerWidth, float spinnerHeight) {
        mapSpinner = new Spinner(skin, spinnerHeight, spinnerWidth, false);

        final Array<MapEntity> mapList = mapManager.getMapList();
        for (int i = 0; i < mapList.size; i++) {
            if (i <= 1) {
                // Load first two maps with previews
                mapPreviewChecked.add(true);
                mapSpinner.addContent(getMapStack(mapList.get(i), mapManager.getMapPreview(mapList.get(i).getName())));
            } else {
                // Loading tons of previews causes a major lag
                // So they're loaded when they're scrolled into view
                mapPreviewChecked.add(false);
                mapSpinner.addContent(getMapStack(mapList.get(i), null));
            }
        }

        mapSpinner.setDestinationPositionListener(new StepScrollPane.StepScrollListener() {
            @Override
            public void destinationPositionChanged(float newDestPos, float oldDestPos) {
                updatePreview(mapList, mapSpinner.getPositionIndex());
            }

            @Override
            public void stepChanged(int newStep) {
                if (newStep < mapList.size - 1) {
                    // Lazy load next image, hence the + 1
                    updatePreview(mapList, newStep + 1);
                }
            }
        });

        mapSpinner.getLeftButton().setText(" < ");
        mapSpinner.getRightButton().setText(" > ");
        mapSpinner.setButtonStepCount(1);

        return mapSpinner;
    }

    private void updatePreview(Array<MapEntity> mapList, int index) {
        if (!mapPreviewChecked.get(index)) {
            Texture mapPreviewTexture = mapManager.getMapPreview(mapList.get(index).getName());
            if (mapPreviewTexture != null) {
                if (usedTextures != null) {
                    usedTextures.add(mapPreviewTexture);
                }
                mapPreviewImages.get(index).setDrawable(new TextureRegionDrawable(new TextureRegion(mapPreviewTexture)));
            }
            mapPreviewChecked.set(index, true);
        }
    }

    private Stack getMapStack(final MapEntity map, Texture mapPreviewTexture) {
        Stack stack = new Stack();

        Label mapNameLabel = new Label(map.getName(), skin);
        mapNameLabel.setAlignment(Align.center);
        mapNameLabel.setWrap(true);

        if (usedTextures != null && mapPreviewTexture != null) {
            usedTextures.add(mapPreviewTexture);
        }
        Image mapPreviewImage;
        if (mapPreviewTexture != null) {
            mapPreviewImage = new Image(mapPreviewTexture);
        } else {
            mapPreviewImage = new Image();
        }
        mapPreviewImage.getColor().a = .2f;
        mapPreviewImage.setScaling(Scaling.fit);
        mapPreviewImages.add(mapPreviewImage);

        stack.add(mapPreviewImage);
        stack.add(mapNameLabel);

        final MapExtraParams extraParams = map.getExtraParams();
        if (extraParams != null) {
            Table innerTable = new Table();
            innerTable.setFillParent(true);
            innerTable.top().right();

            Texture texture = assets.getTexture(Assets.TextureObject.SETTINGS_TEXTURE);

            HoverImageButton extraParamButton = new HoverImageButton(new TextureRegionDrawable(texture));
            extraParamButton.getImage().setScaling(Scaling.fit);
            innerTable.add(extraParamButton)
                    .width(mapSpinner.getSpinnerHeight() / 3)
                    .height(mapSpinner.getSpinnerHeight() / 3)
                    .padRight(1f);
            stack.add(innerTable);

            extraParamButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showExtraParamDialog(extraParams, map);
                }
            });
        }

        return stack;
    }

    public void loadExternalMaps(final MapManager.OnExternalMapLoadListener externalMapLoadListener) {
        mapManager.loadExternalMaps(new MapManager.OnExternalMapLoadListener() {
            @Override
            public void onExternalMapsLoaded(final Array<MapEntity> mapList, final int externalMapStartIndex) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = externalMapStartIndex; i < mapList.size; i++) {
                            MapEntity map = mapList.get(i);
                            mapPreviewChecked.add(false);
                            mapSpinner.addContent(getMapStack(map, null));
                        }

                        if (externalMapLoadListener != null) {
                            externalMapLoadListener.onExternalMapsLoaded(mapList, externalMapStartIndex);
                        }
                    }
                });
            }
        });
    }

    private void showExtraParamDialog(final MapExtraParams prompts, final MapEntity map) {
        final Spinner[] spinners = new Spinner[prompts.spinnerVars.size];

        Table contentTable = new Table();
        contentTable.center();
        Array<Actor> focusableDialogButtons = new Array<>();
        for (int i = 0; i < prompts.spinnerVars.size + 1; i++) {
            Label label;
            if (i == 0) {
                /* Title */

                label = new Label(prompts.paramSelectTitle, skin, "themed");
                label.setAlignment(Align.center);
                contentTable.add(label).colspan(2).padBottom(10f * scaleFactor);
            } else {
                /* Params */

                int lowValue = prompts.spinnerVars.get(i - 1).lowValue;
                int highValue = prompts.spinnerVars.get(i - 1).highValue;
                int snapValue = prompts.spinnerVars.get(i - 1).snapValue;

                label = new Label(prompts.spinnerVars.get(i - 1).prompt, skin);
                spinners[i - 1] = new Spinner(skin, skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight(),
                        64f * scaleFactor, true);
                spinners[i - 1].generateValueRange(lowValue, highValue, skin);
                spinners[i - 1].snapToStep(snapValue - lowValue);
                contentTable.add(label).left();
                contentTable.add(spinners[i - 1]);

                focusableDialogButtons.add(spinners[i - 1].getLeftButton());
                focusableDialogButtons.add(spinners[i - 1].getRightButton());
                focusableDialogButtons.add(null);
            }
            contentTable.row();
        }

        extraParamsDialogActive = true;
        stage.showDialog(contentTable, focusableDialogButtons,
                new FocusableStage.DialogButton[]{FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.Set},
                false, scaleFactor,
                new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(FocusableStage.DialogButton button) {
                        if (button == FocusableStage.DialogButton.Set) {
                            for (int i = 0; i < prompts.spinnerVars.size; i++) {
                                prompts.extraParams[i] = spinners[i].getPositionIndex() + prompts.spinnerVars.get(i).lowValue;
                            }
                            map.applyExtraParams();
                        }

                        extraParamsDialogActive = false;
                    }
                }, skin);
    }

    public boolean loadSelectedMap() {
        int mapIndex = mapSpinner.getPositionIndex();

        try {
            mapManager.prepareMap(mapIndex);
        } catch (InvalidMapCharacter | NoStartPositionsFound | InvalidMapDimensions e) {
            stage.showOKDialog("Error: " + e.getMessage(), false, scaleFactor, null, skin);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void setUpExtendedSelector(final FocusableStage mapSelectorStage, final ExtendedMapSelectionListener extendedMapSelectionListener) {
        mapSelectorStage.clear();
        mapSelectorStage.clearFocusableArray();

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();

        Table scrollTable = new Table();
        final ScrollPane scrollPane = new ScrollPane(scrollTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setScrollbarsOnTop(true);
        scrollPane.setFadeScrollBars(false);

        Array<MapEntity> mapList = mapManager.getMapList();
        for (int i = 0; i < mapList.size; i++) {
            final Stack stack = new Stack();
            final Image image = new Image(skin.getRegion("white"));
            image.getColor().a = 0;
            stack.add(image);

            Label mapLabel = new Label(mapList.get(i).getName(), skin);
            mapLabel.setAlignment(Align.center);
            mapLabel.setWrap(true);

            stack.add(mapLabel);

            final int mapIndex = i;
            stack.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    image.clearActions();
                    image.addAction(Actions.alpha(.25f, .2f, Interpolation.smooth));
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    image.clearActions();
                    image.addAction(Actions.alpha(0, .2f, Interpolation.smooth));
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (extendedMapSelectionListener != null) {
                        extendedMapSelectionListener.onMapSelect(mapIndex);
                    }
                }
            });

            scrollTable.add(stack)
                    .growX()
                    .height(mapLabel.getPrefHeight() * 2);
            scrollTable.row();

            mapSelectorStage.addFocusableActor(stack);
            mapSelectorStage.setScrollpane(scrollPane);
            mapSelectorStage.row();

            if (i == 0) {
                mapSelectorStage.setFocusedActor(stack);
            }
        }

        mainTable.add(scrollPane).grow();
        mapSelectorStage.addActor(mainTable);

        if (extendedMapSelectionListener != null) {
            mapSpinner.setExtendedListener(extendedMapSelectionListener);
        }
    }

    public boolean extraParamShortcutPressed() {
        if (extraParamsDialogActive) {
            // Dialog is already active
            return false;
        }

        MapEntity map = mapManager.getMapList().get(mapSpinner.getPositionIndex());
        MapExtraParams extraParams = map.getExtraParams();
        if (extraParams != null) {
            showExtraParamDialog(extraParams, map);
            return true;
        }

        return false;
    }

    public boolean extraParamsDialogActive() {
        return extraParamsDialogActive;
    }

    public TextButton getLeftButton() {
        return mapSpinner.getLeftButton();
    }

    public TextButton getRightButton() {
        return mapSpinner.getRightButton();
    }

    public void setUsedTextureArray(Array<Texture> usedTextures) {
        this.usedTextures = usedTextures;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public interface ExtendedMapSelectionListener {
        void onExtendedMapSelectorOpened();
        void onMapSelect(int mapIndex);
    }
}
