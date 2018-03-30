package io.aniruddh.edison;

import android.content.Context;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Request;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    MjpegView fpv;
    Spinner pilotMode;
    Spinner throttleMode;
    Spinner maxThrottle;

    TextView angleText;
    TextView throttleText;
    TextView preferenceText;

    String pilot_mode;
    String throttle_mode;
    float max_throttle = 1;

    LinearLayout controlView;
    LinearLayout errorView;


    SharedPreferences sharedPreferences;

    Boolean debug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        goFullScreen();
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/GoogleSans-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );



        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);

        pilotMode = (Spinner) findViewById(R.id.pilotModeSpinner);
        throttleMode = (Spinner) findViewById(R.id.throttleModeSpinner);
        maxThrottle = (Spinner) findViewById(R.id.throttleMaxSpinner);

        angleText = (TextView) findViewById(R.id.angleView);
        throttleText = (TextView) findViewById(R.id.throttleView);
         // Initial Values
        pilot_mode = getPilotMode(pilotMode.getSelectedItem().toString());
        throttle_mode = getThrottleMode(throttleMode.getSelectedItem().toString());
        max_throttle = 1;

        pilotMode.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            Object item = parent.getItemAtPosition(pos);
                            System.out.println(item.toString());     //prints the text in spinner item.
                            pilot_mode = getPilotMode(item.toString());
                            sendData(0, 0, pilot_mode, "false");
                            goFullScreen();
                        }
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });


            throttleMode.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            Object item = parent.getItemAtPosition(pos);
                            System.out.println(item.toString());     //prints the text in spinner item.
                            throttle_mode = getThrottleMode(item.toString());
                            goFullScreen();
                        }
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

            maxThrottle.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            Object item = parent.getItemAtPosition(pos);
                            System.out.println(item.toString());     //prints the text in spinner item.
                            max_throttle = getMaxThrottle(item.toString());
                            goFullScreen();
                        }
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });


            int TIMEOUT = 5; //seconds
            fpv = (MjpegView) findViewById(R.id.fpv);
            Mjpeg.newInstance()
                    .open(Constants.EDISON_VIDEO, TIMEOUT)
                    .subscribe(inputStream -> {
                        fpv.setSource(inputStream);
                        fpv.setDisplayMode(DisplayMode.BEST_FIT);
                        fpv.setTransparentBackground();
                        fpv.showFps(true);
                    });

            Joystick joystick = (Joystick) findViewById(R.id.joystick);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            joystick.setJoystickListener(new JoystickListener() {
                @Override
                public void onDown() {
                    // ..
                }

                @Override
                public void onDrag(float degrees, float offset) {
                    // ..
                    Log.d("ANGLE", String.valueOf(degrees));
                    float angle = 0, throttle = 0;
                    if (degrees > 90 && degrees < 180 || degrees > -180 && degrees < -90){
                        // 2nd and 3rd quadrant - car is going LEFT
                        if (degrees > 90 && degrees < 180) {
                            // 2nd quadrant - car is going FORWARD
                            if (throttle_mode.contentEquals("user")){
                                throttle = reMap(offset, 0, 1, 0, max_throttle);
                            } else if (throttle_mode.contentEquals("c_control")){
                                throttle = max_throttle;
                            }

                            //angle = (float) (offset/Math.tan(degrees));
                            angle = -1*(degrees - 90) / 90;

                        } else if (degrees > -180 && degrees < -90){
                            // 3rd quadrant - car is going BACK

                            if (throttle_mode.contentEquals("user")){
                                throttle = reMap(offset, 0, 1, 0, -1*max_throttle);
                            } else if (throttle_mode.contentEquals("c_control")){
                                throttle = -1*max_throttle;
                            }
                            angle = 1*(1*degrees + 90) / 90;

                        }
                    } else if (degrees > -90 && degrees < - 0 || degrees > 0 && degrees < 90){
                        // 1st and 4th quadrant - car is going RIGHT
                        if (degrees > -90 && degrees < - 0) {
                            // 4th quadrant - car is going BACK

                            if (throttle_mode.contentEquals("user")){
                                throttle = reMap(offset, 0, 1, 0, -1*max_throttle);
                            } else if (throttle_mode.contentEquals("c_control")){
                                throttle = -1*max_throttle;
                            }

                            angle = 1*(1*degrees + 90) / 90;


                        } else if (degrees > 0 && degrees < 90) {
                            // 1st quadrant - car is going FORWARD

                            if (throttle_mode.contentEquals("user")){
                                throttle = reMap(offset, 0, 1, 0, max_throttle);
                            } else if (throttle_mode.contentEquals("c_control")){
                                throttle = max_throttle;
                            }
                            angle = -1 * (degrees - 90) / 90;

                        }

                    }

                    if (debug){
                        Log.d("OFFSET", String.valueOf(offset));
                        Log.d("THROTTLE_RANGE", String.valueOf(throttle));
                        Log.d("ANGLE_RANGE", String.valueOf(angle));
                    }

                    String a_text = "A : " + String.format("%.2f", angle);
                    String t_text = "T : " + String.format("%.2f", throttle);

                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);

                    sendData(angle, throttle, pilot_mode, "true");
                    angleText.setText(a_text);
                    throttleText.setText(t_text);

                }

                @Override
                public void onUp() {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);

                /*JSONObject drive = new JSONObject();
                try {
                    drive.put("angle", 0);
                    drive.put("throttle", 0);
                    drive.put("drive_mode", "user");
                    drive.put("recording", "false");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    Request request = Bridge
                            .post(Constants.EDISON_SERVER)
                            .body(drive)
                            .request();
                } catch (BridgeException e) {
                    e.printStackTrace();
                }*/

                    sendData(0, 0, pilot_mode, "false");

                    String a_text = "A : " + String.format("%.2f", 0.00000);
                    String t_text = "T : " + String.format("%.2f", 0.00000);
                    angleText.setText(a_text);
                    throttleText.setText(t_text);

                }
            });
    }

    public static float reMap(float value, float from1, float to1, float from2, float to2){
        return (value-from1) / (to1 -from1)*(to2 - from2) + from2;

    }


    public void openSettings(View view){

    }

    public Boolean isConnected(String url, int timeout){
        try {
            URL myUrl = new URL(url);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void sendData(float angle, float throttle, String driving_mode, String recording_mode){
        JSONObject drive = new JSONObject();
        try {
            drive.put("angle", angle);
            drive.put("throttle", throttle);
            drive.put("drive_mode", driving_mode);
            drive.put("recording", recording_mode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Request request = Bridge
                    .post(Constants.EDISON_SERVER)
                    .body(drive)
                    .request();
        } catch (BridgeException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void goFullScreen(){
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }
    public String getPilotMode(String spinner_value){
        if (spinner_value.contentEquals("Manual Drive")){
            return "user";
        } else if (spinner_value.contentEquals("Full Autopilot")){
            return "local";
        } else if (spinner_value.contentEquals("Steer Assist")){
            return "local_angle";
        }
        return "user";
    }

    public String getThrottleMode(String spinner_value){
        if (spinner_value.contentEquals("Manual Mode")){
            return "user";
        } else if (spinner_value.contentEquals("Cruise Control")){
            return "c_control";
        }
        return "user";
    }

    public float getMaxThrottle(String spinner_value){
        return Float.valueOf(spinner_value)/100;
    }

    public void onResume() {
        super.onResume();
        goFullScreen();
    }
}
