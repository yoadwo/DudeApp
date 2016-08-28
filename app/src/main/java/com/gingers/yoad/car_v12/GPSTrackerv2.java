package com.gingers.yoad.car_v12;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class GPSTrackerv2  implements ConnectionCallbacks, OnConnectionFailedListener{

    private final Context context;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;


    public GPSTrackerv2(Context context){
        this.context = context;
        buildGoogleApiClient();
        mLastLocation = null;

    }

    public double getLatitude() {
        if(mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            return latitude;
        }
        Log.i("yoad", this.getClass().toString() + "--> getLat failed (lastLoc == null ");
        return -360;
    }
    public double getLongitude() {
        if(mLastLocation != null) {
            double longitude = mLastLocation.getLongitude();
            return longitude;
        }
        Log.i("yoad", this.getClass().toString() + "--> getLng failed (lastLoc == null ");
        return -360;
    }
    public Location getLocation(){
        if(mLastLocation != null) {
            return mLastLocation;
        }
        Log.i("yoad", this.getClass().toString() + "--> getLocation failed (lastLoc == null ");
        return null;
    }
    /*
        public boolean canGetLocation(){

            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGPSEnabled){
                Toast.makeText(context, "GPSTrackerV2--> GPS Services Off or Unavailable", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!isNetworkEnabled){
                Toast.makeText(context, "GPSTrackerV2--> Network Services Off or Unavailable", Toast.LENGTH_SHORT).show();
                return false;
            }
            Log.i("yoad", this.getClass().toString() + " canGetLocation --> gps & network ok");
            return true;

        }
        */
    public boolean canGetLocation() {
        boolean isGPSEnabled = false;
        boolean isNetworkEnabled = false;
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.w("yoad", this.getClass().toString() + " --> canGetLocation is false");
            return false;

        } else {
            Log.w("yoad", this.getClass().toString() + " --> canGetLocation is true");
            return true;
        }
    }

    public void connect(){
        mGoogleApiClient.connect();
    }
    public void disconnect(){
        mGoogleApiClient.disconnect();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.i("yoad", this.getClass().toString() + "onConnected --> Connected");
        Toast.makeText(context, "Ready To Use!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w("yoad", this.getClass().toString() +" OnConnectionFailed " + connectionResult.toString());
    }
}
