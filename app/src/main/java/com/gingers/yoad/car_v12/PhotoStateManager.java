package com.gingers.yoad.car_v12;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by yoad on 24/10/2015.
 */
public class PhotoStateManager {

    private static final String PREFS_PHOTO = "photoState";
    private static final String CHANGED = "changed";
    private static final String LastPath = "LastPath";
    private SharedPreferences photoStatePrefs;

    public PhotoStateManager(Context context){
        photoStatePrefs = context.getSharedPreferences(PREFS_PHOTO, Context.MODE_PRIVATE);
    }

    public void savePhotoState(String path){
        SharedPreferences.Editor photoEditor = photoStatePrefs.edit();
        if (path != null) {
            Log.i("yoad", "save photo state--> path was " + path);
            photoEditor.putString(LastPath, path);
            photoEditor.putBoolean(CHANGED, true);
        }
        else {
            Log.i("yoad", "save photo state--> path was null");
            photoEditor.putString(LastPath, null);
            photoEditor.putBoolean(CHANGED, false);
        }
        photoEditor.commit();
    }

    public String LoadPhotoState(){
        boolean hasChanged = photoStatePrefs.getBoolean(CHANGED, false);
        if (!hasChanged) {
            Log.i("yoad", "load photo state--> path was null, nothing to load");
            return null;
        }else{
            String path = photoStatePrefs.getString(LastPath, null);
            Log.i("yoad", "load photo state--> path found, loading " + path);
            return path;
        }
    }
}
