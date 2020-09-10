package com.cg.zoned.controls;

public class ControlType {
    public String controlName;
    public String controlOffTexturePath;
    public String controlOnTexturePath;
    public ControlTypeEntity controlTypeEntity;

    public ControlType(String controlName, String controlOffTexturePath, String controlOnTexturePath, ControlTypeEntity controlTypeEntity) {
        this.controlName = controlName;
        this.controlOffTexturePath = controlOffTexturePath;
        this.controlOnTexturePath = controlOnTexturePath;
        this.controlTypeEntity = controlTypeEntity;
    }
}
