package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;

public class MapExtraParams {
    public Array<Object> extraParams;

    public String paramSelectTitle;
    public ArrayList<String> paramPrompts;

    MapExtraParams() {
        extraParams = new Array<Object>();
    }
}
