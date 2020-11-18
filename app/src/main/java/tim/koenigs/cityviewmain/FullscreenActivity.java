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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Surface;
import android.location.LocationManager;
import android.widget.ImageView;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;


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

        //1 = Westen , -1 = Osten
        double a = Math.round((Math.cos(orientation[0])*Math.cos(orientation[1])*Math.sin(orientation[2])+Math.sin(orientation[0])*Math.sin(orientation[1])*Math.cos(orientation[2]))*100);
        //1 = Norden, -1 = Süden
        double b = -Math.round((Math.cos(orientation[0])*Math.sin(orientation[1])*Math.cos(orientation[2])+Math.sin(orientation[0])*Math.cos(orientation[1])*Math.sin(orientation[2]))*100);
        // 1 = Erd Mitte, 0 = Horizont; -1 Zenit
        double c = -Math.round(Math.cos(orientation[1])*Math.cos(orientation[2])*100);


        // orientation values returned by the sensors
        // x = yaw (2*pi) , y = pitch (pi) , z = roll (2*pi)
        float x = Math.round(orientation[0] *100);
        float y = Math.round(orientation[1] *100);
        float z = Math.round(orientation[2] *100);

        a = a/100;
        b = b/100;
        c = c/100;
        x = x/100;
        y = y/100;
        z = z/100;

        //direction in degrees
        long direction = Math.round(Math.sin(x/(2*pi))*360);



        //TODO: draw everything
        Bitmap bitmap =  Bitmap.createBitmap(imageView.getWidth() , imageView.getHeight() , Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30);

        // calculate object locations on screen
        // Direction = yaw - roll
        int fieldOfView = 2;
        double horizon = Math.round(imageView.getHeight()*(c) + imageView.getHeight()/2);
        double north = (imageView.getWidth()/2) - direction*(imageView.getWidth()/100);
        double east = (imageView.getWidth()/2) - (direction-90)*(imageView.getWidth()/100);
        double south = (imageView.getWidth()/2) - (direction-180)*(imageView.getWidth()/100);
        double south2 = (imageView.getWidth()/2) - (direction+180)*(imageView.getWidth()/100); //south x2 to draw S in sinus overflow
        double west = (imageView.getWidth()/2) - (direction+90)*(imageView.getWidth()/100);


        //draw objects into bitmap
        canvas.drawRect(0-imageView.getWidth() , imageView.getHeight() ,imageView.getWidth() , Math.round(horizon), paint);
        canvas.drawText("Horizon" ,imageView.getWidth()/2 , Math.round(horizon), textPaint );
        canvas.drawText("N" ,Math.round(north), Math.round(horizon-40), textPaint );
        canvas.drawText("O" ,Math.round(east) , Math.round(horizon-40), textPaint );
        canvas.drawText("S" ,Math.round(south) , Math.round(horizon-40), textPaint );
        canvas.drawText("S" ,Math.round(south2) , Math.round(horizon-40), textPaint );
        canvas.drawText("W" ,Math.round(west) , Math.round(horizon-40), textPaint );
        imageView.setImageBitmap(bitmap);

        //rotate depending on roll
        imageView.setRotation(0);

        //remove borders wich appear when rotating
        imageView.setScaleX(fieldOfView);
        imageView.setScaleY(fieldOfView);

        // output all Data
        TextView t = (TextView) findViewById(R.id.fullscreen_content);
        t.setText("Richtung: "+ direction + "°\nWinkel: " + y + "\nRolle: " + z+ "\na = " + a +"\nb = "+ b + "\nc= " + c +  "\nsqrt:" + east);
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

                TextView l = (TextView) findViewById(R.id.location_content);
                l.setText("Lat: "+ lat+ "\nLng: " + lng);
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
