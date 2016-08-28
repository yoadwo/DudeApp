package com.gingers.yoad.car_v12;

/*TODO: find correct use for gps1/2
        design new icons
*/
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    GoogleMap mMap;
    MapView mMapView;
    Marker marker;
    GPSTracker gps;
//    GPSTrackerv2 gps2;

    private ImageView imv_thumb;
    private Bitmap bm_thumb;

    private String PhotoPath;
    private static final String FILE_PREFIX = "Dude_";
    private static final String FILE_SUFFIX = ".jpg";
    private static final int ACTION_TAKE_PHOTO = 1;
    private static final int GPS_ERRORDIALOG_REQUEST = 100;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("yoad", "---- APP CREATED ----");
        initialize();
        LoadPathPreferences();
        if (servicesOK()) {
            if (initMap(savedInstanceState)) {
                Log.i("yoad", "services ok->init map-> Map Available");
                gps = new GPSTracker(MainActivity.this);
//                gps2 = new GPSTrackerv2(MainActivity.this);
                mMap.setMyLocationEnabled(true);
                if (!loadMapPreferences()) {
                    Log.i("yoad", "Main--> no map found");
                    GoToCurrentLocation();
                }
            }
            else
                Log.i("yoad", "services ok->init map-> Map NOT Available");
        }
    }

    private void initialize(){
        imv_thumb = (ImageView) findViewById(R.id.id_imageView);
        bm_thumb = null;

        Button b_takePhoto = (Button) findViewById(R.id.id_button_take);
        Button b_clrPhoto = (Button) findViewById(R.id.id_button_clear_photo);
        Button b_sharePhoto = (Button) findViewById(R.id.id_button_share);
        PhotoPath = null;

        Button b_saveLoc = (Button) findViewById(R.id.id_button_save);
        Button b_clrLoc = (Button) findViewById(R.id.id_button_clear_map);
        Button b_gotoLoc = (Button) findViewById((R.id.id_button_navigate));
        mMap = null;
        marker = null;
        gps = null;
        mMapView = null;
//        gps2 = null;


        b_takePhoto.setOnClickListener(takePhotoListener);
        b_clrPhoto.setOnClickListener(clrPhotoListener);
        b_sharePhoto.setOnClickListener(sharePhotoListener);

        b_saveLoc.setOnClickListener(saveLocationListener);
        b_clrLoc.setOnClickListener(clearLocationListener);
        b_gotoLoc.setOnClickListener(gotoLocationListener);


    }

    private View.OnClickListener takePhotoListener = new View.OnClickListener() {
        public void onClick(View v) {
            dispatchTakePhotoIntent(v);
        }
    };

    private View.OnClickListener clrPhotoListener = new View.OnClickListener() {
        public void onClick(View v) {
            imv_thumb.setImageResource(R.drawable.thumb);
            Log.i("yoad", "Main --> photo cleared");
            PhotoPath = null;
        }
    };

    private View.OnClickListener sharePhotoListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (PhotoPath == null) {
                Toast.makeText(MainActivity.this, "Take Photo First", Toast.LENGTH_LONG).show();
                Log.i("yoad", "Main --> Shared but no photo");
            }
            else{
                Intent sharePhotoIntent = new Intent(Intent.ACTION_SEND);
                sharePhotoIntent.setType("image/jpg");
                sharePhotoIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(PhotoPath));
                startActivity(Intent.createChooser(sharePhotoIntent, "Share Image Using"));
                Log.i("yoad", "Main --> Share button Clicked");
            }
        }
    };

    private View.OnClickListener saveLocationListener = new View.OnClickListener() {
        public void onClick(View v) {
            saveLocation();
        }
    };

    private View.OnClickListener clearLocationListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (marker!= null) {
                mMap.clear();
                marker.remove();
                marker = null;
                Log.i("yoad", "Main --> markers cleared");
            }
        }
    };

    private View.OnClickListener gotoLocationListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (marker!= null) {
                Intent intent = new Intent(MainActivity.this, CompassActivity.class);
                Location loc = latlngToLoc(marker.getPosition());
                Log.i("yoad", "Main-->sending Location: " +loc.getLatitude() +", " + loc.getLongitude());
                intent.putExtra("location", loc);
                intent.putExtra("photo", PhotoPath); //could be null!
                startActivity(intent);
            }

            else {
                Toast.makeText(MainActivity.this, "Please Set Marker First", Toast.LENGTH_LONG ).show();
            }
        }
    };

    private Location latlngToLoc(LatLng position) {
        Location location = new Location("Destination");
        location.setLatitude(position.latitude);
        location.setLongitude(position.longitude);
        location.setTime(new Date().getTime()); //Set time as current Date
        return location;
    }

    private void dispatchTakePhotoIntent(View v) {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = null;
        try {
            f = CreatePhotoFile();
            PhotoPath = f.getAbsolutePath();
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        } catch (IOException e){
            e.printStackTrace();
            f = null;
            PhotoPath = null;
        }
        startActivityForResult(takePhotoIntent, ACTION_TAKE_PHOTO);
    }
    private File CreatePhotoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("dd-MM_HH-mm").format(new Date());
        String imageFileName = FILE_PREFIX + timeStamp + "_";
        File albumDir = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, FILE_SUFFIX, albumDir);
        return imageF;
    }
    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    getString(R.string.album_dir));

            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            setPic();
            Log.i("yoad", "onActivityResult--> visibility:" + imv_thumb.isShown());
            galleryAddPic();
        }
        else {
            Toast.makeText(MainActivity.this, "Error: result code", Toast.LENGTH_LONG).show();
        }
    }
    private void LoadPathPreferences(){
//        SharedPreferences settings = getPreferences(MODE_PRIVATE);
//        String previousPath = settings.getString("LastPath", null);
        PhotoStateManager photoMgr = new PhotoStateManager(this);
        String previousPath = photoMgr.LoadPhotoState();
        if (previousPath == null)
            Toast.makeText(MainActivity.this, "no previous photo exists", Toast.LENGTH_LONG).show();
        else{
            PhotoPath = previousPath;
            setPic();
        }
    }
    private void SavePathPreferences(){
        PhotoStateManager photoMgr = new PhotoStateManager(this);
        photoMgr.savePhotoState(PhotoPath);
    }
    private void SaveMapPreferences() {
        MapStateManager mapMgr = new MapStateManager(this);
        mapMgr.saveMapState(marker);
    }
    private boolean loadMapPreferences(){

        MapStateManager mapMgr = new MapStateManager(this);
        MarkerOptions options = mapMgr.loadMapState();

        if (mMap != null) {
            if (options != null) {
                marker = mMap.addMarker(options);
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(options.getPosition(), 15);
                mMap.moveCamera(update);
                return true;
            }else { // no marker returned
                return false;
            }
        }
        else { // notify mmap not initialized
            return false;
        }

    }

    private void setPic(){
        imv_thumb.setImageBitmap(
                decodeSampledBitmapFromFile(imv_thumb.getWidth(), imv_thumb.getHeight()));
        imv_thumb.setVisibility(View.VISIBLE);
        Log.i("yoad", "setPic --> visibilty: " + imv_thumb.isShown());
    }
    public Bitmap decodeSampledBitmapFromFile(int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(PhotoPath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(PhotoPath, options);
    }
    public static int calculateInSampleSize( BitmapFactory.Options options, int targetW, int targetH) {
        // Raw height and width of image
        final int photoH = options.outHeight;
        final int photoW = options.outWidth;
        int  scaleFactor = 1;

        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

        return scaleFactor;
    }
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(PhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
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
            Toast.makeText(this,"Can't Connect To Google Play Services", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
    private boolean initMap(Bundle savedInstanceState){
        mMapView = (MapView) findViewById(R.id.id_map);
        mMapView.onCreate(savedInstanceState);
        //position is null, mMap not initialized
        if (mMap == null)
            mMap = mMapView.getMap();   //fix to get map async
        return (mMap!=null);
    }
    private void saveLocation(){
        LatLng currentLL = GoToCurrentLocation();
        Log.i("yoad", "saveLocation--> Lat: " + currentLL.latitude +"  Lng:  " + currentLL.longitude);
        if (marker != null){
            marker.remove();
        }
        MarkerOptions options = new MarkerOptions()
                .position(currentLL);
//        mMap.clear(); //?
        marker = mMap.addMarker(options);
    }
    private LatLng GoToCurrentLocation(){
        double latitude =0 , longitude =0;
        if(gps.canGetLocation()) {
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            Log.i("yoad", "GoToCurrentLocation--> got location!");

        } else {
            gps.showSettingsAlert();
        }
        GoToLocation(latitude, longitude);
        return new LatLng(latitude,longitude);
    }
    private void GoToLocation(double lat, double lng){
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
        mMap.animateCamera(update);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_exit)
            finish();
        return super.onOptionsItemSelected(item);
    }
    /*
    Some lifecycle callbacks so that the image can survive orientation change
    couldnt understand what they are for
    */
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putParcelable(BITMAP_STORAGE_KEY, bm_thumb);
        //outState.putBoolean(IMAGEVIEW_VI  SIBILITY_STORAGE_KEY, (bm_thumb != null) );
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        /*bm_thumb = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        imv_thumb.setImageBitmap(bm_thumb);
        imv_thumb.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );*/
    }

    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    protected void onPause() {
        super.onPause();
        Log.i("yoad", "---- APP paused ----");
        if (mMapView!=null)
            mMapView.onPause();
        SavePathPreferences();//was onStop, moved here
        SaveMapPreferences();
    }
    protected void onResume() {
        super.onResume();
        Log.i("yoad", "---- APP resumed ----");
        if (mMapView!=null)
            mMapView.onResume();
    }

    protected void onStart() {
        super.onStart();
        gps.getLocation();
        if (gps.canGetLocation())
            Log.i("yoad", "---- resumed GPS signal ----");
//        gps2.connect();
    }

    protected void onStop() {
        super.onStop();
        gps.stopUsingGPS();
        Log.i("yoad", "---- stopped GPS signal ----");

//        gps2.disconnect();
//        SavePathPreferences();
//        SaveMapPreferences();
    }
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        Log.i("yoad", "---- APP DESTROYED ----");
    }
    public int calculateInSampleSize_org(    //got it from google.dev.android
                                             BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    private void setPic_original() {

        //todo: inBitmap = Bitmap, returned either from decodeFile or CreateBitmap
        /*BitmapFactory.decodeFile(PhotoPath, bmOptions);
        mCurrentBitmap = Bitmap.createBitmap(bmOptions.outWidth, bmOptions.outHeight, Bitmap.Config.ARGB_8888)
        bmOptions.inBitmap = bm_thumb;*/

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */
		/* Get the size of the ImageView */
        int targetW = imv_thumb.getWidth();
        int targetH = imv_thumb.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(PhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        bmOptions.inSampleSize = 1;
		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
		/* Set bitmap options to scale the image decode target */
        bmOptions.inSampleSize = scaleFactor; //was after decode bounds
        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inPurgeable = true; //fix if memory crashes
//        bmOptions.inInputShareable = true;
		/* Decode the JPEG file into a Bitmap */
        //Bitmap bitmap = BitmapFactory.decodeFile(PhotoPath, bmOptions);
        bm_thumb = BitmapFactory.decodeFile(PhotoPath, bmOptions);
//        bmOptions.inBitmap = bm_thumb;
//        bm_thumb = BitmapFactory.decodeFile(PhotoPath, bmOptions);
        /* Associate the Bitmap to the ImageView */
        //imv_thumb.setImageBitmap(bitmap);
        //imv_thumb.setVisibility(View.VISIBLE);
        imv_thumb.setImageBitmap(bm_thumb);
        imv_thumb.setVisibility(View.VISIBLE);
    }
    /*
    private LatLng GoToCurrentLocation(){

        double latitude, longitude;
        latitude = longitude = 0;
        if(gps2.canGetLocation()) {
            latitude = gps2.getLatitude();
            longitude = gps2.getLongitude();
            Log.i("yoad", "GoToCurrentLocation--> got location!");

        } else {
            //gps2.showSettingsAlert();
        }
        GoToLocation(latitude, longitude);
        return new LatLng(latitude,longitude);
    }*/
}
