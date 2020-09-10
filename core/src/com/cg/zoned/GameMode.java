package com.cg.zoned;

public class GameMode {
    public String previewLocation;
    public String name;
    public Class targetClass;

    public GameMode(String name, String previewLocation, Class targetClass) {
        this.name = name;
        this.previewLocation = previewLocation;
        this.targetClass = targetClass;
    }
}
