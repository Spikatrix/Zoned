package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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

public class MapSelector {
    private MapManager mapManager;

    private Skin skin;
    private float scaleFactor;

    private FocusableStage stage;
    private Spinner mapSpinner;

    private Assets assets;
    private Array<Texture> usedTextures;

    public MapSelector(FocusableStage stage, float scaleFactor, Assets assets, Skin skin) {
        this.mapManager = new MapManager();
        this.stage = stage;
        this.skin = skin;
        this.assets = assets;
        this.scaleFactor = scaleFactor;
    }

    public Spinner loadMapSelectorSpinner(float spinnerWidth, float spinnerHeight) {
        mapSpinner = new Spinner(skin, spinnerHeight, spinnerWidth, false);

        for (final MapEntity map : mapManager.getMapList()) {
            mapSpinner.addContent(getMapStack(map, mapManager.getMapPreview(map.getName())));
        }

        mapSpinner.getLeftButton().setText(" < ");
        mapSpinner.getRightButton().setText(" > ");
        mapSpinner.setButtonStepCount(1);

        return mapSpinner;
    }

    private Stack getMapStack(final MapEntity map, Texture mapPreviewTexture) {
        Stack stack = new Stack();

        Label mapNameLabel = new Label(map.getName(), skin);
        mapNameLabel.setAlignment(Align.center);
        if (mapPreviewTexture != null) {
            if (usedTextures != null) {
                usedTextures.add(mapPreviewTexture);
            }
            Image mapPreviewImage = new Image(mapPreviewTexture);
            mapPreviewImage.getColor().a = .2f;
            mapPreviewImage.setScaling(Scaling.fit);
            stack.add(mapPreviewImage);
            stack.add(mapNameLabel);
        } else {
            stack.add(mapNameLabel);
        }

        final MapExtraParams extraParams = map.getExtraParams();
        if (extraParams != null) {
            Table innerTable = new Table();
            innerTable.setFillParent(true);
            innerTable.top().right();

            Texture texture = assets.getSettingsButtonTexture();

            HoverImageButton hoverImageButton = new HoverImageButton(new TextureRegionDrawable(texture));
            hoverImageButton.getImage().setScaling(Scaling.fit);
            innerTable.add(hoverImageButton)
                    .width(mapSpinner.getSpinnerHeight() / 3)
                    .height(mapSpinner.getSpinnerHeight() / 3);
            stack.add(innerTable);

            hoverImageButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showExtraParamDialog(extraParams, map);
                }
            });
        }

        return stack;
    }

    public void loadExternalMaps() {
        mapManager.loadExternalMaps(new MapManager.OnExternalMapLoadListener() {
            @Override
            public void onExternalMapLoaded(final Array<MapEntity> mapList, final int externalMapStartIndex) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = externalMapStartIndex; i < mapList.size; i++) {
                            MapEntity map = mapList.get(i);
                            mapSpinner.addContent(getMapStack(map, mapManager.getMapPreview(map.getName())));
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
                spinners[i - 1] = new Spinner(skin, skin.getFont(Constants.FONT_MANAGER.REGULAR.getFontName()).getLineHeight(),
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

        Array<String> buttonTexts = new Array<>();
        buttonTexts.add("Cancel");
        buttonTexts.add("Set");

        stage.showDialog(contentTable, focusableDialogButtons, buttonTexts, false, scaleFactor,
                new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(String buttonText) {
                        if (buttonText.equals("Set")) {
                            for (int i = 0; i < prompts.spinnerVars.size; i++) {
                                prompts.extraParams[i] = spinners[i].getPositionIndex() + prompts.spinnerVars.get(i).lowValue;
                            }
                            map.applyExtraParams();
                        }
                    }
                }, skin);
    }

    public boolean loadSelectedMap() {
        int mapIndex = mapSpinner.getPositionIndex();

        Array<String> buttonTexts = new Array<>();
        buttonTexts.add("OK");

        try {
            mapManager.prepareMap(mapIndex);
        } catch (InvalidMapCharacter | NoStartPositionsFound | InvalidMapDimensions e) {
            e.printStackTrace();
            stage.showDialog("Error: " + e.getMessage(), buttonTexts, false, scaleFactor, null, skin);
            return false;
        }

        return true;
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
}
