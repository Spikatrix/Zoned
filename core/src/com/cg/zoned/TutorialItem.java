package com.cg.zoned;

public class TutorialItem {
    public String mainItem;
    public String subItem;
    public boolean enablePlayerInteraction;

    public TutorialItem(String mainItem, String subItem, boolean enablePlayerInteraction) {
        this.mainItem = mainItem;
        this.subItem = subItem;
        this.enablePlayerInteraction = enablePlayerInteraction;
    }
}
