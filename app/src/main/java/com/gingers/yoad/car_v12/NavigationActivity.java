package com.gingers.yoad.car_v12;

import android.app.Dialog;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;


public class NavigationActivity extends AppCompatActivity implements SensorEventListener {

    private TextView fieldBearing;
    private TextView distance;
    private Location LocationObj;
    private Location destinationObj;
    private ImageView mPointer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private GPSTracker gps;

    private static final int GPS_ERRORDIALOG_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        initialize();
    }

    private void initialize() {
        Button b_begin = (Button) findViewById(R.id.id_button_beginNav);
        b_begin.setOnClickListener(beginNavListener);

        fieldBearing = (TextView) findViewById(R.id.id_nav_info_tmp);
        distance = (TextView) findViewById(R.id.id_nav_distance);
        destinationObj = (Location) getIntent().getExtras().get("location");
        initMap();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


    }

    private void initMap() {
        Log.i("yoad", "NavigationActivity--> Destination --> " +
                "Lat: " + destinationObj.getLatitude() + ", Lng: " + destinationObj.getLongitude());
        if (servicesOK()) {
            Log.i("yoad", "NavigationActivity--> Services OK");
            gps = new GPSTracker(NavigationActivity.this);
        }
        else{
            Log.i("yoad", "NavigationActivity--> Services Not OK");
        }
    }

    public boolean servicesOK(){
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS)
            return true;
        else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)){
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, GPS_ERRORDIALOG_REQUEST);
            dialog.show();
        }
        else{
            Toast.makeText(this, "Can't Connect To Google Play Services", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private View.OnClickListener beginNavListener = new View.OnClickListener() {
        public void onClick(View v) {
            // add geo code label
            if (gps.canGetLocation()) {
                LocationObj = gps.getLocation();
                Log.i("yoad", "NavigationActivity--> CurrentLocation --> " +
                        "Lat: " + LocationObj.getLatitude() + ", Lng: " + LocationObj.getLongitude());
            }

        }
    };

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_navigation, menu);
        return true;
    }

    // http://www.techrepublic.com/article/pro-tip-create-your-own-magnetic-compass-using-androids-internal-sensors/
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }   //added because of guide
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        gps.stopUsingGPS();
    }    //added because of guide
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }   //added because of guide

    // http://stackoverflow.com/questions/7978618/rotating-an-imageview-like-a-compass-with-the-north-pole-set-elsewhere
    public void onSensorChanged( SensorEvent event ) {

        // If we don't have a Location, we break out
        if ( LocationObj == null ) return;

        float azimuth = event.values[0];
        float baseAzimuth = azimuth;
        Log.i("yoad", "azimuth = " + azimuth +", base azimuth (degrees) = " + Math.toDegrees(baseAzimuth));
        GeomagneticField geoField = new GeomagneticField( Double
                .valueOf( LocationObj.getLatitude() ).floatValue(), Double
                .valueOf( LocationObj.getLongitude() ).floatValue(),
                Double.valueOf( LocationObj.getAltitude() ).floatValue(),
                System.currentTimeMillis() );

        azimuth += geoField.getDeclination(); // converts magnetic north into true north

        // Store the bearingTo in the bearTo variable
        //Returns the approximate initial bearing in degrees East of true North
        float bearTo = LocationObj.bearingTo( destinationObj );

        // If the bearTo is smaller than 0, add 360 to get the rotation clockwise.
        if (bearTo < 0) {
            bearTo = bearTo + 360;
        }

        //This is where we choose to point it
        //TrueNorth + Bearing - TrueNorth = Bearing (=direction)
        float direction = bearTo - azimuth;

        // If the direction is smaller than 0, add 360 to get the rotation clockwise.
        if (direction < 0) {
            direction = direction + 360;
        }

        //rotateImageView( arrow, R.drawable.arrow, direction );

        //Set the field
        String bearingText = "N";

        if ( (360 >= baseAzimuth && baseAzimuth >= 337.5) || (0 <= baseAzimuth && baseAzimuth <= 22.5) ) bearingText = "N";
        else if (baseAzimuth > 22.5 && baseAzimuth < 67.5) bearingText = "NE";
        else if (baseAzimuth >= 67.5 && baseAzimuth <= 112.5) bearingText = "E";
        else if (baseAzimuth > 112.5 && baseAzimuth < 157.5) bearingText = "SE";
        else if (baseAzimuth >= 157.5 && baseAzimuth <= 202.5) bearingText = "S";
        else if (baseAzimuth > 202.5 && baseAzimuth < 247.5) bearingText = "SW";
        else if (baseAzimuth >= 247.5 && baseAzimuth <= 292.5) bearingText = "W";
        else if (baseAzimuth > 292.5 && baseAzimuth < 337.5) bearingText = "NW";
        else bearingText = "?";

        fieldBearing.setText(bearingText);

    }

    public void onSensorChanged2( SensorEvent event ) {
        /*
        // If we don't have a Location, we break out
        if ( LocationObj == null ) {
            if (gps.canGetLocation()) {
                LocationObj = gps.getLocation();
                if (LocationObj == null) {
                    Log.w("yoad", "NavigationActivity-> onSensorChanged->failed to retrieve location");
                    return;
                }
            }
            else {
                Log.i("yoad", "NavigationActivity-> onSensorChanged-> cannot get Location, check GPS");
                return;
            }
        }*/

        if (gps.canGetLocation())
            LocationObj = gps.getLocation();
        else {
            Log.w("yoad", "NavigationActivity-->OnSensorChange--> cannot get GPS location");
            return;
        }
        if (LocationObj == null){
            Log.w("yoad", "NavigationActivity-->OnSensorChange--> Location is Null");
            return;
        }


        distance.setText( ( Double.toString(LocationObj.distanceTo(destinationObj))  ));
        float azimuth = event.values[0];
        float baseAzimuth = azimuth;

        GeomagneticField geoField = new GeomagneticField(
                Double.valueOf(LocationObj.getLatitude()).floatValue(),
                Double.valueOf(LocationObj.getLongitude()).floatValue(),
                Double.valueOf( LocationObj.getAltitude() ).floatValue(),
                System.currentTimeMillis() );
        azimuth -= geoField.getDeclination(); // converts magnetic north into true north

        float bearTo = LocationObj.bearingTo( destinationObj );
        // If the bearTo is smaller than 0, add 360 to get the rotation clockwise.
        if (bearTo < 0) {
            bearTo = bearTo + 360;
        }
        //This is where we choose to point it
        float direction = bearTo - azimuth;

        // If the direction is smaller than 0, add 360 to get the rotation clockwise.
        if (direction < 0) {
            direction = direction + 360;
        }

        Log.i("yoad", "BASE AZIMUTH: " + baseAzimuth + ", AZIMUTH: " + azimuth);
        //rotateImageView( arrow, R.drawable.arrow, direction );

        //Set the field
        String bearingText = "N";
        if ( (360 >= baseAzimuth && baseAzimuth >= 337.5) || (0 <= baseAzimuth && baseAzimuth <= 22.5) ) bearingText = "N";
        else if (baseAzimuth > 22.5 && baseAzimuth < 67.5) bearingText = "NE";
        else if (baseAzimuth >= 67.5 && baseAzimuth <= 112.5) bearingText = "E";
        else if (baseAzimuth > 112.5 && baseAzimuth < 157.5) bearingText = "SE";
        else if (baseAzimuth >= 157.5 && baseAzimuth <= 202.5) bearingText = "S";
        else if (baseAzimuth > 202.5 && baseAzimuth < 247.5) bearingText = "SW";
        else if (baseAzimuth >= 247.5 && baseAzimuth <= 292.5) bearingText = "W";
        else if (baseAzimuth > 292.5 && baseAzimuth < 337.5) bearingText = "NW";
        else bearingText = "?";

        fieldBearing.setText(bearingText);

    }


}
