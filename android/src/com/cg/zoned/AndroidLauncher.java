package com.cg.zoned;

import android.os.Build;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.cg.zoned.maps.ExternalMapReader;

public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            /* Creates /storage/emulated/0/Android/data/com.cg.zoned/files/ZonedExternalMaps */
            getExternalFilesDir(ExternalMapReader.mapDirName);
        }

        startGame();
    }

    private void startGame() {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.hideStatusBar = true;
        config.useImmersiveMode = true;
        initialize(new Zoned(), config);
    }
}
