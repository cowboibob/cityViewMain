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
import android.graphics.Paint;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationManager;
import android.widget.ImageView;



import java.util.HashMap;



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements SensorEventListener {
    private static SensorManager sensorManager;
    private static Sensor sensor;
    private View mContentView;
    private ImageView imageView;
    private double earthRadius = 6378.137;
    private double[] selfLocation = new double[]{0,0,1};
    private double[] selfLocationDegrees = new double[]{0,0};
    private HashMap<String, double[]> locationList = new HashMap<String,double[]>();

    //zoomfactor: 1 => 90° ; 2 => 45° FOV (bigger = more zoom)
    float fieldOfView = 2f;


    //used for low pass filter
    double alpha = 0.9;
    double savedYaw;
    double savedPitch;
    double savedRoll;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

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
            sensorManager.registerListener(this,sensor, SensorManager.SENSOR_DELAY_GAME);
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
            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
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

        hide();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startService();
                } else{
                    Toast.makeText(this, "no permission granted",
                            Toast.LENGTH_LONG).show();
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


    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

    }

    @SuppressLint("InlinedApi")
    private void show() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
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
        //double yaw = newAzimuth;
        double yaw = Math.toDegrees(x);
        double pitch = Math.toDegrees(y);
        double roll = Math.toDegrees(z);



        //adjust values if phone is upside down

        if((pitch >= 45 || pitch <= -45) && (roll <= -90 || roll >= 90)) {
            if(yaw <= 0){
                yaw = 180 + yaw;
            }else{
                yaw = yaw - 180;
            }

            pitch = -180 - pitch;
            if(pitch < -180){
                pitch = pitch + 360;
            }

            if(roll <= -90){
                roll = roll + 180;
            }else if(roll >= 90){
                roll = roll - 180;
            }
            if(pitch <= -90){
                roll = roll*-1;
            }

        }


        // draw everything
        Bitmap bitmap =  Bitmap.createBitmap(imageView.getWidth() , imageView.getHeight() ,
                                                Bitmap.Config.ARGB_8888);
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

        coreHeight = imageView.getHeight() / 2f
                + imageView.getHeight() / (90f/fieldOfView) * (pitch*(-1));

        coreWidth = imageView.getWidth() / 2f
                + imageView.getHeight() / (90f/fieldOfView) * roll;


        //draw horizon
        canvas.drawCircle(Math.round(coreWidth), Math.round(coreHeight),
                Math.round(imageView.getHeight()*fieldOfView), paint);

        //draw core as a constant point
        canvas.drawCircle(Math.round(coreWidth), Math.round(coreHeight),7, textPaint);
        canvas.drawText("Erdkern"+ " ("+ earthRadius + "km)" ,Math.round(coreWidth-35) ,
                Math.round(coreHeight-35), textPaint );



        //draw objects from locationList
        for (String key: locationList.keySet()) {
            String name = key;
            double[] vector = locationList.get(key);
            double latitude = vector[0];
            double longitude = vector[1];

            double[] xyzCoordinates = transformLatLongToXYZ(latitude, longitude);
            double[] relativeVector = calculateRelationVector(xyzCoordinates);
            double[] angles = calculateAnglesFromRelationVector(relativeVector);

            //same longitude results in no angle, because both vectors for the calculation of delta,
            // are in the same direction
            if(Double.isNaN(angles[1])){
                if(selfLocationDegrees[1] == longitude || latitude == -90) {
                    if (selfLocationDegrees[0] > latitude){
                        angles[1] = 180;
                    }
                }else{
                    angles[1] = 0;
                }
            }


            double height;
            double width;

            double hypotenuse = angles[0]  * imageView.getHeight() / (90f/fieldOfView);            //hypotenuse
            double offsetHeight;
            double offsetWidth;                                                                           //above the core

            if (isWest(longitude)) { // left of viewing angle
                offsetHeight = hypotenuse*Math.cos(Math.toRadians(angles[1]+yaw));
                height =  coreHeight - offsetHeight;

                offsetWidth = hypotenuse*Math.sin(Math.toRadians(angles[1]+yaw));
                width = coreWidth - offsetWidth;

            } else { // right of viewing angle
                offsetHeight = hypotenuse*Math.cos(Math.toRadians(angles[1]-yaw));
                height =  coreHeight - offsetHeight;

                offsetWidth = hypotenuse*Math.sin(Math.toRadians(angles[1]-yaw));
                width = coreWidth + offsetWidth;
            }


            //distance in KM: Radius = 1  results in earth radius = 6378.137 km
            //length of relativeVector*earthRadius = distance
            double distance = Math.sqrt(relativeVector[0]*relativeVector[0]+relativeVector[1]
                    *relativeVector[1]+relativeVector[2]*relativeVector[2]);
            long distanceInKM = Math.round(distance*earthRadius);

            //resize Text based on distance (far = small)
            textPaint.setTextSize(Math.round(40 - (distance * 8)));
            long dotSize = Math.round(12 - (distance *3));

            canvas.drawCircle(Math.round(width), Math.round(height),dotSize, textPaint);
            canvas.drawText(name + " ("+ distanceInKM + "km)",
                    Math.round(width-35), Math.round(height+35), textPaint);

        }

        imageView.setImageBitmap(bitmap);

        // output all Data for debugging
        TextView t = (TextView) findViewById(R.id.fullscreen_content);
        t.setText("");
        //t.setText("Gier: "+ yaw + "\nNick: " + pitch + "\nRoll: " + roll );

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

        double zx = relVec[0];
        double zy = relVec[1];
        double zz = relVec[2];

        double scalarProduct = x*zx + y*zy + z*zz;
        double vecSumZ = Math.sqrt(zx*zx + zy*zy + zz*zz);
        double vecSumI = Math.sqrt(x*x + y*y + z*z);

        double gamma = Math.toDegrees(Math.acos(scalarProduct / (vecSumI*vecSumZ)));

        if(Double.isNaN(gamma)){
            gamma = 0;
        }
        angles[0] = gamma;

        //yaw angle
        //faktor a
        //transformiere Zielvektor auf die Ebene
        double aNo = (x*zx + y*zy + z*zz) / (x*x + y*y + z*z);
        double px = zx - aNo * x; //pojiziert auf flache erde ebene
        double py = zy - aNo * y;
        double pz = zz - aNo * z;
        double pSum = Math.sqrt(px*px+py*py+pz*pz);

        //projiziere die x-Achse als Referenzvektor auf die Ebene
        double aRef = (x*1+y*0+z*0)/(x*x+y*y+z*z);
        double nx = 1 - aRef * x; // 1 = Norden !!!
        double ny = 0 - aRef * y;
        double nz = 0 - aRef * z;
        double nSum = Math.sqrt(nx*nx + ny*ny + nz*nz);

        double delta = Math.toDegrees(Math.acos((px*nx + py*ny + pz*nz) / (nSum * pSum)));

        angles[1] = delta;
        return angles;
    }

    public void updateLocation(View view) {
        fillLocationList();
    }

    public void fillLocationList(){
        //todo: automated locationlist from database or JSON
        locationList = new HashMap<String,double[]>();

        locationList.put("Northpole" , new double[]{90.0,0});
        locationList.put("Southpole" , new double[]{-90.0,0});
        locationList.put("Mannheim", new double[]{49.5121, 8.5316});
        locationList.put("Berlin", new double[]{52.520008, 13.404954});
        locationList.put("Paris", new double[]{48.864716, 2.349014});
        locationList.put("Karlsruhe", new double[]{49.007, 8.404});
        locationList.put("Hong Kong", new double[]{ 22.302711, 114.177216});
        locationList.put("Washington DC", new double[]{38.889248, -77.050636});
        locationList.put("Sydney", new double[]{-33.865143, 151.209900});
        locationList.put("Rio de Janeiro", new double[]{-22.908333, -43.196388});
        locationList.put("Cape Town", new double[]{-33.918861, 18.423300});
        locationList.put("New York", new double[]{40.712784, -74.005941});
        locationList.put("London", new double[]{51.5073509, -0.1277583});
        locationList.put("Moskau", new double[]{55.755826, 37.617300});
        locationList.put("Tokio", new double[]{35.6894875,	139.6917064});
        locationList.put("Rom", new double[]{41.9027835, 	12.4963655});
        locationList.put("Jerusalem", new double[]{31.768319, 35.21371});
        locationList.put("Mumbai", new double[]{19.0759837, 72.8776559});
        locationList.put("Buenos Aires", new double[]{-34.603684, -58.381559});
        locationList.put("Mexico-Stadt", new double[]{19.3906797, -99.2840399});



        Log.d("mylog","Location List loaded");

    }

    //transform longitude and latitude into x,y,z coordinates on a sphere with radius=1 around the point(0,0,0)
    public double[] transformLatLongToXYZ(double latitude, double longitude){
        double[] xyz = new double[]{0.0,0.0,0.0};
        xyz[0] = Math.sin(Math.toRadians(latitude));
        xyz[1] = Math.sin(Math.toRadians(longitude))*Math.cos(Math.toRadians(latitude));
        xyz[2] = Math.cos(Math.toRadians(longitude))*Math.cos(Math.toRadians(latitude));

        return xyz;
    }

    //calculate a directional Vector from the current position to given coordinates
    public double[] calculateRelationVector(double[] location){
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
        final int worldAxisForDeviceAxisZ;

        worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        worldAxisForDeviceAxisZ = SensorManager.AXIS_Z;

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisZ, adjustedRotationMatrix);

        // Express the updated rotation matrix as three orientation angles.
        final float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        return orientation;
    }

    public void startService(){
        LocationBroadcastReceiver receiver = new LocationBroadcastReceiver();
        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);
        Intent intent = new Intent(FullscreenActivity.this, LocationService.class);
        startService(intent);
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


                // output for debugging
                TextView l = (TextView) findViewById(R.id.location_content);
                l.setText("");
                //l.setText("Lat: "+ lat+ "\nLng: " + lng );

            }
        }
    }
}
