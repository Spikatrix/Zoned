package com.cg.zoned.controls;

public class ControlType {
    public String controlName;
    public String controlTexturePath;
    public Class<?> controlTypeEntity;

    public ControlType(String controlName, String controlTexturePath, Class<? extends ControlTypeEntity> controlTypeEntity) {
        this.controlName = controlName;
        this.controlTexturePath = controlTexturePath;
        this.controlTypeEntity = controlTypeEntity;
    }
}
