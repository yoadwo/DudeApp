package com.gingers.yoad.car_v12;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by yoad on 24/10/2015.
 */
public class MapStateManager {

    private static final String LONG = "longitude";
    private static final String LAT = "latitude";
//    private static final String TITLE = "title";
//    private static final String SNIPPET = "snippet";

    private static final String CHANGED = "changed";

    private static final String PREFS_MAP= "MapState";
    private SharedPreferences mapStatePrefs;

    public MapStateManager (Context context){
        mapStatePrefs = context.getSharedPreferences(PREFS_MAP, Context.MODE_PRIVATE);
    }


    public void saveMapState (Marker marker){
        SharedPreferences.Editor mapEditor = mapStatePrefs.edit();
        if (marker != null) {
            Log.i("yoad", "MapStateManager --> save map state --> marker saved");
            LatLng target = marker.getPosition();
//            String title = marker.getTitle();
//            String snippet = marker.getSnippet();

            mapEditor.putFloat(LAT, (float) target.latitude);
            mapEditor.putFloat(LONG, (float) target.longitude);
//            mapEditor.putString(TITLE, title);
//            mapEditor.putString(SNIPPET, snippet);
            mapEditor.putBoolean(CHANGED, true);
        }
        else{
            Log.i("yoad", "MapStateManager --> save map state --> marker was null");
            mapEditor.putBoolean(CHANGED, false);
        }
        mapEditor.commit();
    }

    public MarkerOptions loadMapState(){
        boolean hasChanged = mapStatePrefs.getBoolean(CHANGED,false);
        if (hasChanged){
            Log.i("yoad", "MapStateManager --> loadMapState --> loading marker");
            double latitude = mapStatePrefs.getFloat(LAT, 0);
            double longitude = mapStatePrefs.getFloat(LONG, 0);
//            String title = mapStatePrefs.getString(TITLE, "");
//            String snippet = mapStatePrefs.getString(SNIPPET, "");

            LatLng target = new LatLng(latitude, longitude);

            MarkerOptions options = new MarkerOptions()
//                    .title(title)
//                    .snippet(snippet)
                    .position(target);
            return options;
        }
        else {
            Log.i("yoad", "MapStateManager --> loadMapState --> no map found");
            return null;
        }

    }
}
