package com.gingers.yoad.car_v12;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float distanceToDestination;
    private Location LocationObj;
    private Location destinationObj;
    private TextView direction_tv;
    private TextView distance_tv;
    private ImageView arrow;
    private Bitmap bitmap;
    private GPSTracker gps;
    private String photoPath;


    private static final int GPS_ERRORDIALOG_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);    // Register the sensor listeners
        init();

    }
    private void init() {
        distanceToDestination = -1;
        Button beginNav = (Button) findViewById(R.id.id_compass_button);
        beginNav.setOnClickListener(beginNavListener);

        direction_tv = (TextView) findViewById(R.id.id_compass_direction_tv);
        direction_tv.setVisibility(View.INVISIBLE);
        distance_tv = (TextView) findViewById(R.id.id_compass_distance);
        distance_tv.setVisibility(View.INVISIBLE);
        arrow = (ImageView) findViewById(R.id.id_compass_arrow);
        arrow.setVisibility(View.INVISIBLE);
        destinationObj = (Location) getIntent().getExtras().get("location");

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        LocationObj = null;
        photoPath = null;
        initMap();
        decodePhoto();
    }
    private void initMap() {
        Log.i("yoad", "CompassActivity--> Destination --> " +
                destinationObj.getLatitude() + ", " + destinationObj.getLongitude());
        if (servicesOK()) {
            Log.i("yoad", "CompassActivity--> Services OK");
            gps = new GPSTracker(CompassActivity.this);
            LocationObj = gps.getLocation();
        }
        else{
            Log.i("yoad", "CompassActivity--> Services Not OK");
        }
    }

    private void decodePhoto(){
        photoPath = (String) getIntent().getExtras().get("photo");
        if (photoPath!=null){
            Point size = getScreenSize();
            Log.i("yoad", "CompassActivity-->decodePhoto (imageview)-->W: " +size.x/2 +", H: " + size.y/2);
            bitmap = decodeSampledBitmapFromFile(photoPath, size.x/2, size.y/2);
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
                distance_tv.setVisibility(View.VISIBLE);
                arrow.setVisibility(View.VISIBLE);
                Log.i("yoad", "CompassActivity--> currentLocation: " + LocationObj.getLatitude() + ", " + LocationObj.getLongitude());
                if (distanceToDestination !=-1)
                    Log.i("yoad", "CompassActivity-->Distance: " + distanceToDestination);
            }
            else
                Toast.makeText(CompassActivity.this, "Please Hold, Locating GPS..", Toast.LENGTH_SHORT).show();

        }
    };
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

    float[] mGravity;
    float[] mGeomagnetic;
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuthInRadians = orientation[0]; // get azimuth from the orientation sensor
                //azimuth = orientation[0];
                float azimuthInDegress = (float)Math.toDegrees(azimuthInRadians); // convert radians to degrees
                if (azimuthInDegress < 0.0f) {
                    azimuthInDegress += 360.0f;
                }

                azimuthInDegress+=getDeclination(); //converts magnetic north returned from magnetic sensor to true north;
                //azimuthToDirectionText(azimuthInDegress); //points to north
                // Store the bearingTo in the bearTo variable
                //Returns the approximate initial bearing in degrees East of true North
                if (gps.canGetLocation()) {
                    LocationObj = gps.getLocation(); // get location from GPS or network
                }
                else
                    Log.i("yoad", "CompassActivity-->sensorChanged-->Couldn't get gpsLocation!");
                float bearTo = LocationObj.bearingTo( destinationObj );

                // If the bearTo is smaller than 0, add 360 to get the rotation clockwise.
                if (bearTo < 0) {
                    bearTo = bearTo + 360;
                }
                //This is where we choose to point it
                //TrueNorth + Bearing - TrueNorth = Bearing (=direction)
                float direction = bearTo - azimuthInDegress;
                // If the direction is smaller than 0, add 360 to get the rotation clockwise.
                if (direction < 0) {
                    direction = direction + 360;
                }
                distanceToDestination = LocationObj.distanceTo(destinationObj);
                //azimuthToDirectionText(direction);
                if (distanceToDestination >=10)
                    setOnRoute(direction, distanceToDestination);
                else
                    setOnDestination();
                //Log.i("yoad", "CompassACtivity-->orientation(Degrees): " + azimuthInDegress);
            }
        }
    }

    private void setPic(){
        arrow.setBackgroundResource(0);
        arrow.setImageBitmap(bitmap);
        arrow.setVisibility(View.VISIBLE);
    }
    private Bitmap decodeSampledBitmapFromFile(String photoPath, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(photoPath, options);
    }
    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        Log.i("yoad", "CompassActivity-->decodeSampled (photo? options)-->W: " + width + ", H: " + height);
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void setOnRoute(float direction, float distanceToDestination) {
        if (distanceToDestination > 1000) {
            distanceToDestination/=1000;
            distanceToDestination = (float)(Math.floor(distanceToDestination * 1e2) / 1e2);
            distance_tv.setText(String.valueOf(distanceToDestination + " km"));
        }
        else {
            distanceToDestination = (float)(Math.floor(distanceToDestination * 1e2) / 1e2);
            distance_tv.setText(String.valueOf(distanceToDestination+" m"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            arrow.setBackgroundResource(0);
            arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow, getApplicationContext().getTheme()));
        } else {
            arrow.setBackgroundResource(0);
            arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow));
        }
        arrow.setRotation(direction);  //arrow will point to said direction
    }
    private void setOnDestination() {
        distance_tv.setText("Right On Target");
        if (photoPath == null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                arrow.setBackgroundResource(0);
                arrow.setImageDrawable(getResources().getDrawable(R.drawable.green_check, getApplicationContext().getTheme()));
            } else {
                arrow.setBackgroundResource(0);
                arrow.setImageDrawable(getResources().getDrawable(R.drawable.green_check));
            }
        else
            setPic();
    }
    private float getDeclination(){
        GeomagneticField geoField = new GeomagneticField( Double
                .valueOf( LocationObj.getLatitude() ).floatValue(), Double
                .valueOf( LocationObj.getLongitude() ).floatValue(),
                Double.valueOf( LocationObj.getAltitude() ).floatValue(),
                System.currentTimeMillis() );
        return  geoField.getDeclination();
    }
    private void azimuthToDirectionText(float azimuth) {
        String bearingText = "N";

        if ( (360 >= azimuth && azimuth >= 337.5) || (0 <= azimuth && azimuth <= 22.5) ) bearingText = "N";
        else if (azimuth > 22.5 && azimuth < 67.5) bearingText = "NE";
        else if (azimuth >= 67.5 && azimuth <= 112.5) bearingText = "E";
        else if (azimuth > 112.5 && azimuth < 157.5) bearingText = "SE";
        else if (azimuth >= 157.5 && azimuth <= 202.5) bearingText = "S";
        else if (azimuth > 202.5 && azimuth < 247.5) bearingText = "SW";
        else if (azimuth >= 247.5 && azimuth <= 292.5) bearingText = "W";
        else if (azimuth > 292.5 && azimuth < 337.5) bearingText = "NW";
        else bearingText = "?";

        direction_tv.setText(bearingText);
    }
    private Point getScreenSize(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compass, menu);
        return true;
    }
    @Override
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

    /*public void onSensorChanged2(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuthInRadians = orientation[0];
                azimuth = orientation[0];
                float azimuthInDegress = (float)Math.toDegrees(azimuthInRadians);
                if (azimuthInDegress < 0.0f) {
                    azimuthInDegress += 360.0f;
                }
                //mCustomDrawableView.azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
                azimuthToDirectionText(azimuthInDegress);
                //arrow.setRotation(azimuthInDegress);  //arrow will point to said direction
                arrow.setRotation(-azimuth*360/(2*(float)Math.PI)); //arrow will point to north

                //rotateImageView(arrow, com.gingers.yoad.car_v12.R.id.id_compass_arrow, azimuthInDegress);
                //Log.i("yoad", "CompassACtivity-->orientation(Degrees): " + azimuthInDegress);
            }
        }
        //mCustomDrawableView.invalidate();

        TODO: not sure if magnetic reading is really needed. perhaps all i need is simple reading of bearing like example bellow.
        TODO: for now, arrow always points to north (normal compass)
        TODO: switch orientation[0] with bearing! maybe combine below code inside code above, not instead
        float azimuth = // get azimuth from the orientation sensor (it's quite simple)
                Location currentLoc = // get location from GPS or network
        // convert radians to degrees
                azimuth = Math.toDegrees(azimuth);
        GeomagneticField geoField = new GeomagneticField(
                (float) currentLoc.getLatitude(),
                (float) currentLoc.getLongitude(),
                (float) currentLoc.getAltitude(),
                System.currentTimeMillis());
        azimuth += geoField.getDeclination(); // converts magnetic north into true north
        float bearing = currentLoc.bearingTo(target); // (it's already in degrees)
        float direction = azimuth - bearing;

    }*/

    /*private void rotateImageView( ImageView imageView, int drawable, float rotate ) {
        Log.i("yoad", "CompassActivity--> rotateImageview--> drawableId= " + drawable);
        // Decode the drawable into a bitmap
        Bitmap bitmapOrg = android.graphics.BitmapFactory.decodeResource( getResources(),
                drawable );

        // Get the width/height of the drawable
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = bitmapOrg.getWidth(), height = bitmapOrg.getHeight();

        // Initialize a new Matrix
        android.graphics.Matrix matrix = new android.graphics.Matrix();

        // Decide on how much to rotate
        rotate = rotate % 360;

        // Actually rotate the image
        matrix.postRotate( rotate, width, height );

        // recreate the new Bitmap via a couple conditions
        Bitmap rotatedBitmap = Bitmap.createBitmap( bitmapOrg, 0, 0, width, height, matrix, true );
        //BitmapDrawable bmd = new BitmapDrawable( rotatedBitmap );

        //imageView.setImageBitmap( rotatedBitmap );
        imageView.setImageDrawable(new android.graphics.drawable.BitmapDrawable(getResources(), rotatedBitmap));
        imageView.setScaleType( ImageView.ScaleType.CENTER );
    }*/


}
