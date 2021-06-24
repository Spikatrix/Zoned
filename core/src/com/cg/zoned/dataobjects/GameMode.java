package com.cg.zoned.dataobjects;

import com.badlogic.gdx.Screen;

public class GameMode {
    private String previewLocation;
    private String name;
    private Class<?> targetClass;

    public GameMode(String name, String previewLocation, Class<? extends Screen> targetClass) {
        this.name = name;
        this.previewLocation = previewLocation;
        this.targetClass = targetClass;
    }

    public String getPreviewLocation() {
        return previewLocation;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }
}
