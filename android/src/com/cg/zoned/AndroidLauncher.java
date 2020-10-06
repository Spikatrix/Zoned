package com.cg.zoned;

import android.os.Bundle;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

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

        startGame();
    }

    private void startGame() {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.hideStatusBar = true;
        config.useImmersiveMode = true;
        initialize(new Zoned(null, new AndroidDoubleFormatter()), config); // Discord RPC is not available on Android, hence null
    }
}
