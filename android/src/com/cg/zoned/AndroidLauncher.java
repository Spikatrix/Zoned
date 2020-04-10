package com.cg.zoned;

import android.os.Build;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    private final static int WRITE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPerms();
    }

    private void checkPerms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startGame();
            // TODO: Perms cleanup (Might come in handy for importing external maps)
            // Do we need this? Bit tedious. Try /storage/emulated/0/Android/data/com.cg.zoned/files instead
        } else {
            /*if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
            } else {*/
            startGame();
            /*}*/
        }
    }

    private void startGame() {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.hideStatusBar = true;
        config.useImmersiveMode = true;
        initialize(new Zoned(), config);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_REQUEST_CODE) {
            //if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startGame();
        }
    }
}
