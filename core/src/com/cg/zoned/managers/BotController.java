package com.cg.zoned.managers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

public class BotController {
    public BotController() {
        Gdx.app.log("Ext", "External file dir: " + Gdx.files.getExternalStoragePath());
        // On Android: /storage/emulated/0 (Need permission first)
        // On Desktop: /home/username/
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            File f = new File(Gdx.files.getExternalStoragePath() + "/Zoned/Bots");
            try {
                Gdx.app.log("File", "Starting file list: ");
                for (File file : f.listFiles()) {
                    if (!file.isDirectory())
                        Gdx.app.log("File", "Filename: " + file.getName());
                }
            } catch (NullPointerException e) {
                Gdx.app.log("Fail", "Npe " + e.getMessage());
            }
        }
    }

    public void isThisEvenGonnaWork() {
        File f = new File(Gdx.files.getExternalStoragePath());
        URI u = f.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> urlClass = URLClassLoader.class;
        Method method = null;
        try {
            method = urlClass.getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        try {
            method.setAccessible(true);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        try {
            method.invoke(urlClassLoader, new Object[]{u.toURL()});
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            for (URL url : urlClassLoader.getURLs())
                Gdx.app.log("testing", url.toString());

            ClassReflection.forName("bot.Temp");
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        // Not working (yet?)

        // For Android, I think you need to get the DexClassLoader via Context
        /*
         * DexClassLoader dexClassLoader = new DexClassLoader("path/to/someApkfile.apk", myOptimizedDirectory, null, myContext.getClassLoader());
            Class<?> MyClass =  dexClassLoader.loadClass("its.package.name.MyClass");
            Object myInstance = MyClass.newInstance();
            MyClass.getDeclaredMethod("foo").invoke(myInstance);
         *
         * I have no idea if this is gonna work soon ;-; I doubt it tho :/
         */
    }
}
