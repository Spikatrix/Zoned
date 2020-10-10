package com.cg.zoned;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.cg.zoned.maps.ExternalMapReader;

public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BuildConfig.VERSION_NAME.equals(Constants.GAME_VERSION)) {
            // Used as a precaution
            Toast.makeText(getApplicationContext(),
                    "GAME VERSION MISMATCH (" + BuildConfig.VERSION_NAME + " and " + Constants.GAME_VERSION + ")",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
        config.numSamples = 4;
        initialize(new Zoned(null), config); // Discord RPC is not available on Android, hence null
    }
}
