package tim.koenigs.cityviewmain;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Surface;
import android.location.LocationManager;
import android.widget.ImageView;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements SensorEventListener {
    private static SensorManager sensorManager;
    private static Sensor sensor;
    private static LocationManager locationManager;
    private View mContentView;
    private ImageView imageView;
    private double pi = Math.PI;
    private double earthRadius = 6378.137;
    private double[] selfLocation = new double[]{0,0,0};
    private double[] selfLocationDegrees = new double[]{0,0};
    private HashMap<String, double[]> locationList = new HashMap<String,double[]>();


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();



    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            //hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
            TextView t = (TextView) findViewById(R.id.fullscreen_content);
            t.setText("Button was Pressed");
            }
            return false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        imageView = findViewById(R.id.imageView);
        sensorManager = (SensorManager)getSystemService((Context.SENSOR_SERVICE));
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        



        if (sensor != null)
        {
            sensorManager.registerListener(this,sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        else
        {
            Toast.makeText(getApplicationContext(),"no Sensor found", Toast.LENGTH_SHORT).show();
        }

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        //GPS
        if (Build.VERSION.SDK_INT >= 23){
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                Log.d("mylog","BUILD VERSION >= 23 & Permission denied");
            }
            else{
                //Request Location Permission
                startService();
                Log.d("mylog","GPS permission granted");
            }
        } else {
            //Start Location Service
            startService();
            Log.d("mylog","BUILD VERSION < 23");
        }

        imageView.setBackgroundColor(Color.CYAN);

        fillLocationList();



    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startService();
                } else{
                    Toast.makeText(this, "give me permissions", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] orientation = new float[3];
        orientation = calculateOrientation(event);

        // orientation values returned by the sensors
        // x = yaw (2*pi) , y = pitch (pi) , z = roll (2*pi)
        double x = orientation[0];
        double y = orientation[1];
        double z = orientation[2];

        //angles in degrees
        double yaw = Math.toDegrees(x);
        double pitch = Math.toDegrees(y);
        double roll = Math.toDegrees(z);

        //adjust values if phone is upside down

        if(roll <= -90 || roll >= 90){
            if(yaw <= 0){
                yaw = 180 + yaw;
            }else{
                yaw = yaw - 180;
            }
            pitch = -180 - pitch;
            if(pitch <-180){
                pitch = pitch+360;
            }

            if(roll <= -90){
                roll = roll + 180;
            }else if(roll >= 90){
                roll = roll -180;
            }

        }



        //1 = Westen , -1 = Osten ueseless
        double a = (Math.cos(x)*Math.cos(y)*Math.sin(z)+Math.sin(x)*Math.sin(y)*Math.cos(z));
        //1 = Norden, -1 = Süden useless
        double b = -(Math.cos(x)*Math.sin(y)*Math.cos(z)+Math.sin(x)*Math.cos(y)*Math.sin(z));
        // 1 = Erd Mitte, 0 = Horizont; -1 Zenit
        double c = -Math.cos(y)*Math.cos(z);


        //TODO: draw everything
        Bitmap bitmap =  Bitmap.createBitmap(imageView.getWidth() , imageView.getHeight() , Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30);


        double coreHeight;
        double coreWidth;

        coreHeight = imageView.getHeight() / 2f + imageView.getHeight() / 90f * (pitch*(-1));
        coreWidth = imageView.getWidth() / 2f + imageView.getHeight() / 90f * roll;


        //draw constant points
        canvas.drawCircle(Math.round(coreWidth), Math.round(coreHeight),Math.round(imageView.getHeight()), paint);
        canvas.drawCircle(Math.round(coreWidth), Math.round(coreHeight),7, textPaint);
        canvas.drawText("CORE"+ " ("+ earthRadius + "km)" ,Math.round(coreWidth-30) , Math.round(coreHeight-30), textPaint );



        //draw objects from locationList
        for (String key: locationList.keySet()) {
            String name = key;
            double[] vector = locationList.get(key);
            double latitude = vector[0];
            double longitude = vector[1];
            double[] xyzCoordinates = transformLatLongToXYZ(latitude, longitude);
            double[] relativeVector = calculateRelationVector(xyzCoordinates);
            double[] angles = calculateAnglesFromRelationVector(relativeVector);

            if(Double.isNaN(angles[1])){
                if(selfLocationDegrees[1] == longitude || latitude == -90) {
                    if (selfLocationDegrees[0] > latitude){
                        angles[1] = 180;
                    }
                }else{
                    angles[1] = 0;
                }
            }
            
            double xHeight;
            double xWidth;


            double offsetHeight = angles[0]  * imageView.getHeight() / 90;          //hypothenuse
            double offset;                                                          //above the core



            if (isWest(longitude)) { // left of viewing angle
                xHeight =  coreHeight - offsetHeight*Math.cos(Math.toRadians(angles[1]+yaw));
                offset = offsetHeight*Math.sin(Math.toRadians(angles[1]+yaw));
                xWidth = coreWidth - offset;

            } else { // right of viewing angle
                xHeight =  coreHeight - offsetHeight*Math.cos(Math.toRadians(angles[1]-yaw));
                offset = offsetHeight*Math.sin(Math.toRadians(angles[1]-yaw));
                xWidth = coreWidth + offset;
            }

            //distance in KM: Radius = 1  results in earth radius = 6378.137 km
            //length of relativeVector*earthRadius = distance
            long distanceInKM = Math.round(Math.sqrt(relativeVector[0]*relativeVector[0]+relativeVector[1]*relativeVector[1]+relativeVector[2]*relativeVector[2])*earthRadius);
            canvas.drawCircle(Math.round(xWidth), Math.round(xHeight),7, textPaint);
            canvas.drawText(name + " ("+ distanceInKM + "km)", Math.round(xWidth-30), Math.round(xHeight+30), textPaint);

        }



        imageView.setImageBitmap(bitmap);

        //rotate depending on roll
        imageView.setRotation(0);

        //remove borders which appear when rotating
        int fieldOfView = 1;
        imageView.setScaleX(fieldOfView);
        imageView.setScaleY(fieldOfView);

        // output all Data
        //todo remove (debugging)
        TextView t = (TextView) findViewById(R.id.fullscreen_content);
        t.setText("Richtung: "+ yaw + "\nNeigung: " +pitch + "\nRolle: " + roll + "\n"+ "\na = " + a +"\nb = "+ b + "\nc= " + c );


    }


    public boolean isWest(double longitude){
        double modulo = ((selfLocationDegrees[1])-longitude +360)%360;

        if(modulo <= 180) {
            return true;
        }else{
            return false;
        }

    }


    public double[] calculateAnglesFromRelationVector(double[] relVec){
        double[] angles = new double[2];
        // pitch angle
        //inverse nullvector
        double x = -selfLocation[0];
        double y = -selfLocation[1];
        double z = -selfLocation[2];

        double vecLength = Math.sqrt(relVec[0]*relVec[0]+relVec[1]*relVec[1]+relVec[2]*relVec[2]);
        double vecLengthSelf = Math.sqrt(x*x+y*y+z*z);
        double scalarProduct = x*relVec[0]+y*relVec[1]+z*relVec[2];
        double gamma = Math.toDegrees(Math.acos(scalarProduct / (vecLengthSelf*vecLength)));

        if(Double.isNaN(gamma)){
            gamma = 0;
        }
        angles[0] = gamma;

        //yaw angle
        //faktor a
        //transformiere zielvektor auf die Ebene
        double aNo = (x*relVec[0]+y*relVec[1]+z*relVec[2])/(x*x+y*y+z*z);
        double zpx = relVec[0]-x*aNo; //pojeziert auf flache erde ebene
        double zpy = relVec[1]-y*aNo;
        double zpz = relVec[2]-z*aNo;
        double betzpNo = Math.sqrt(zpx*zpx+zpy*zpy+zpz*zpz);

        //transformiere referenzvektor auf die Ebene
        double aRef = (x*1+y*0+z*0)/(x*x+y*y+z*z);
        double zpxRef = 1-x*aRef; // 1 = Norden !!!
        double zpyRef = 0-y*aRef;
        double zpzRef = 0-z*aRef;
        double betzpRef = Math.sqrt(zpxRef*zpxRef+zpyRef*zpyRef+zpzRef*zpzRef);

        double alpha = Math.toDegrees(Math.acos((zpx*zpxRef + zpy*zpyRef + zpz*zpzRef) / (betzpNo * betzpRef)));

        angles[1] = alpha;
        return angles;
    }

    public void updateLocation(View view) {
        fillLocationList();

        //TODO remove below
        for (String key: locationList.keySet()) {
            String name = key;
            double[] location = locationList.get(key);
            double latitude = location[0];
            double longitude = location[1];
            double[] xyzLocation = transformLatLongToXYZ(latitude, longitude);
            double[] relativeVector = calculateRelationVector(xyzLocation);
            double[] angles;
            angles = calculateAnglesFromRelationVector(relativeVector);

            Log.d("mylog","gamma "+ name + " : " + angles[0] + " / " + angles[1]  );

        }
    }

    public void fillLocationList(){
        //todo automated locationlist from database ?
        locationList = new HashMap<String,double[]>();

        locationList.put("Northpole" , new double[]{90.0,0});
        locationList.put("Southpole" , new double[]{-90.0,0});
        locationList.put("Mannheim", new double[]{49.5121, 8.5316});
        locationList.put("Ludwigshafen", new double[]{49.489, 8.3791});
        locationList.put("Zweibrücken", new double[]{49.245, 7.3634});
        locationList.put("Karlsruhe", new double[]{49.007, 8.404});



        for (String key: locationList.keySet()) {
            String name = key;
            double[] coordinates = locationList.get(key);
            Log.d("mylog","Item in LocationList: " + name + " (" + coordinates[0]+"°|" + coordinates[1] +"°)");

        }
        Log.d("mylog","Location List loaded");

    }

    public double[] transformLatLongToXYZ(double latitude, double longitude){   //transform longitude and latitude into x,y,z coordinates on a sphere with radius=1 around the point(0,0,0)
        double[] xyz = new double[]{0.0,0.0,0.0};
        xyz[0] = Math.sin(Math.toRadians(latitude));
        xyz[1] = Math.sin(Math.toRadians(longitude))*Math.cos(Math.toRadians(latitude));
        xyz[2] = Math.cos(Math.toRadians(longitude))*Math.cos(Math.toRadians(latitude));

        return xyz;
    }

    public double[] calculateRelationVector(double[] location){ //calculate a directional Vector from the current position to given coordinates
        double[] relationVector = new double[]{0.0,0.0,0.0};
        relationVector[0] = location[0] - selfLocation[0];
        relationVector[1] = location[1] - selfLocation[1];
        relationVector[2] = location[2] - selfLocation[2];

        return relationVector;
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float[] calculateOrientation(SensorEvent event){
        // Rotation matrix based on current readings from accelerometer and magnetometer.
        final float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        worldAxisForDeviceAxisY = SensorManager.AXIS_Z;

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix);

        // Express the updated rotation matrix as three orientation angles.
        final float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        return orientation;
    }

    public class LocationBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACT_LOC")){
                double lat = intent.getDoubleExtra("latitude",0f);
                double lng = intent.getDoubleExtra("longitude",0f);
                selfLocation = transformLatLongToXYZ(lat,lng);
                selfLocationDegrees[0] = lat;
                selfLocationDegrees[1] = lng;



                String s1 = String.valueOf(selfLocation[0]);
                String s2 = String.valueOf(selfLocation[1]);
                String s3 = String.valueOf(selfLocation[2]);


                TextView l = (TextView) findViewById(R.id.location_content);
                l.setText("Lat: "+ lat+ "\nLng: " + lng + "\n" +s1+ "\n" +s2+ "\n"+s3);
            }
        }
    }

    public void startService(){
        LocationBroadcastReceiver receiver = new LocationBroadcastReceiver();
        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);
        Intent intent = new Intent(FullscreenActivity.this, LocationService.class);
        startService(intent);
    }






}
