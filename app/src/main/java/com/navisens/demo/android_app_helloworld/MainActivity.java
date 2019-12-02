package com.navisens.demo.android_app_helloworld;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaApplication;
import com.navisens.motiondnaapi.MotionDnaInterface;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static android.os.SystemClock.elapsedRealtime;

/*
 * For complete documentation on Navisens SDK API
 * Please go to the following link:
 * https://github.com/navisens/NaviDocs/blob/master/API.Android.md
 */

public class MainActivity extends AppCompatActivity implements MotionDnaInterface{

    MotionDnaApplication motionDnaApplication;
    Hashtable<String, MotionDna> networkUsers = new Hashtable<String, MotionDna>();
    Hashtable<String, Double> networkUsersTimestamps = new Hashtable<String, Double>();
    TextView textView;
    TextView networkTextView;
    private static final int REQUEST_MDNA_PERMISSIONS=1;
    AppCompatImageButton startbtn = null;
    AppCompatImageButton stopbtn = null;
    AppCompatImageButton pausebtn = null;
    AppCompatImageButton viewlogsbtn = null;
    Spinner intervalSpinner = null;
    String devKey = "rrDwDxQdUMHccxfWy3oR394PdWHpSsiMfDiNSCtQZKxzmXrDL7mc21o7kprCZe2c";
    LogFileUtility logFileUtility = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    int REQUEST_WRITE_STORAGE_REQUEST_CODE = 1001;
    double selectedinterval = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAppPermissions();

        textView = (TextView) findViewById(R.id.HELLO);
        networkTextView = (TextView) findViewById(R.id.network);
        intervalSpinner = (Spinner) findViewById(R.id.intervalSpinner);

        final String[] menuArray = getResources().getStringArray(R.array.intervals);
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(motionDnaApplication != null) {
                    double selected = 0;
                    if(position == 1) {
                        selected = Double.parseDouble("1") * 1000;
                    } else {
                        selected = Double.parseDouble(menuArray[position]) * 1000;
                    }
                    selectedinterval = selected;
                    motionDnaApplication.setCallbackUpdateRateInMs(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        intervalSpinner.setSelection(1);

        startbtn = (AppCompatImageButton) findViewById(R.id.startbtn);
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(motionDnaApplication != null) {
                    //    This functions starts up the SDK. You must pass in a valid developer's key in order for
                    //    the SDK to function. IF the key has expired or there are other errors, you may receive
                    //    those errors through the reportError() callback route.
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    String strTimestamp = sdf.format(timestamp);
                    logFileUtility.createFolderFile(strTimestamp);

                    motionDnaApplication.runMotionDna(devKey);

                    startbtn.setEnabled(false);
                    logFileUtility.setWriteToFile("***********START MOTION DNA*************\n");
                    motionDnaApplication.setCallbackUpdateRateInMs(selectedinterval);
                }
            }
        });
        stopbtn = (AppCompatImageButton) findViewById(R.id.stopbtn);
        stopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(motionDnaApplication != null) {
                    motionDnaApplication.stop();
                    startbtn.setEnabled(true);
                    logFileUtility.setWriteToFile("***********STOP MOTION DNA*************\n");
                }
            }
        });
        pausebtn = (AppCompatImageButton) findViewById(R.id.pausebtn);
        pausebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(motionDnaApplication != null) {
                    if(v.isSelected()) {
                        motionDnaApplication.resume();
                        v.setSelected(false);
                        logFileUtility.setWriteToFile("***********RESUME MOTION DNA*************\n");
                    } else {
                        motionDnaApplication.pause();
                        v.setSelected(true);
                        logFileUtility.setWriteToFile("***********PAUSE MOTION DNA*************\n");
                    }
                }
            }
        });
        viewlogsbtn = (AppCompatImageButton) findViewById(R.id.viewlogsbtn);
        viewlogsbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });


        logFileUtility = new LogFileUtility(MainActivity.this);

        // Requests app
        ActivityCompat.requestPermissions(this,MotionDnaApplication.needsRequestingPermissions()
                , REQUEST_MDNA_PERMISSIONS);
    }

    Intent motionDnaServiceIntent;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (MotionDnaApplication.checkMotionDnaPermissions(this)) // permissions already requested
        {

            // Starts a foreground service to ensure that the
            // App continues to sample the sensors in background
            motionDnaServiceIntent = new Intent(getAppContext(), MotionDnaForegroundService.class);
            getAppContext().startService(motionDnaServiceIntent);

            // Start the MotionDna Core
            startMotionDna();
        }
    }

    public void startMotionDna() {


        motionDnaApplication = new MotionDnaApplication(this);

        motionDnaApplication.setCallbackUpdateRateInMs(selectedinterval);

        //    Use our internal algorithm to automatically compute your location and heading by fusing
        //    inertial estimation with global location information. This is designed for outdoor use and
        //    will not compute a position when indoors. Solving location requires the user to be walking
        //    outdoors. Depending on the quality of the global location, this may only require as little
        //    as 10 meters of walking outdoors.

        //motionDnaApplication.setLocationNavisens();

        motionDnaApplication.setLocationLatitudeLongitude(1.274138, 103.801573);

        //   Set accuracy for GPS positioning, states :HIGH/LOW_ACCURACY/OFF, OFF consumes
        //   the least battery.

        motionDnaApplication.setExternalPositioningState(MotionDna.ExternalPositioningState.LOW_ACCURACY);

        //    Manually sets the global latitude, longitude, and heading. This enables receiving a
        //    latitude and longitude instead of cartesian coordinates. Use this if you have other
        //    sources of information (for example, user-defined address), and need readings more
        //    accurate than GPS can provide.
        //motionDnaApplication.setLocationLatitudeLongitudeAndHeadingInDegrees(37.787582, -122.396627, 0);

        //    Set the power consumption mode to trade off accuracy of predictions for power saving.

        motionDnaApplication.setPowerMode(MotionDna.PowerConsumptionMode.PERFORMANCE);

        //    Connect to your own server and specify a room. Any other device connected to the same room
        //    and also under the same developer will receive any udp packets this device sends.

        motionDnaApplication.startUDP();

        //    Allow our SDK to record data and use it to enhance our estimation system.
        //    Send this file to support@navisens.com if you have any issues with the estimation
        //    that you would like to have us analyze.

        motionDnaApplication.setBinaryFileLoggingEnabled(true);

        //    Tell our SDK how often to provide estimation results. Note that there is a limit on how
        //    fast our SDK can provide results, but usually setting a slower update rate improves results.
        //    Setting the rate to 0ms will output estimation results at our maximum rate.

        motionDnaApplication.setCallbackUpdateRateInMs(selectedinterval);

        //    When setLocationNavisens is enabled and setBackpropagationEnabled is called, once Navisens
        //    has initialized you will not only get the current position, but also a set of latitude
        //    longitude coordinates which lead back to the start position (where the SDK/App was started).
        //    This is useful to determine which building and even where inside a building the
        //    person started, or where the person exited a vehicle (e.g. the vehicle parking spot or the
        //    location of a drop-off).
        motionDnaApplication.setBackpropagationEnabled(true);

        //    If the user wants to see everything that happened before Navisens found an initial
        //    position, he can adjust the amount of the trajectory to see before the initial
        //    position was set automatically.
        motionDnaApplication.setBackpropagationBufferSize(2000);

        //motionDnaApplication.runMotionDna(devKey);

//        motionDnaApplication.startUDPHostAndPort();
//
    //    Enables AR mode. AR mode publishes orientation quaternion at a higher rate.

//        motionDnaApplication.setARModeEnabled(true);
    }

    //    This event receives the estimation results using a MotionDna object.
    //    Check out the Getters section to learn how to read data out of this object.

    @Override
    public void receiveMotionDna(MotionDna motionDna)
    {

        String str = "**********Navisens MotionDna Location Data***********\n";
        //str += "Lat from getLocation().globallocation : " + motionDna.getLocation().globalLocation.latitude + " Lon from getLocation().longitude: " + motionDna.getLocation().globalLocation.longitude + "\n";
        MotionDna.XYZ location = motionDna.getLocation().localLocation;
        str += "X: " + location.x +  " \n";
        str += "Y: " + location.y +  " \n";
        str += "Z: " + location.z +  " \n";
        //str += String.format(" (%.2f, %.2f, %.2f)\n",location.x, location.y, location.z);
        //str += "Hdg: " + motionDna.getLocation().heading +  " \n";
        str += "motionType: " + motionDna.getMotion().motionType + "\n";

        str += "Lat from getGpsLocation().globallocation : " + motionDna.getGpsLocation().globalLocation.latitude + "\n ";
        str += "Lon from getGpsLocation().longitude: " + motionDna.getGpsLocation().globalLocation.longitude + "\n";

       /* str += "getMotionStatistics dwelling : " + motionDna.getMotionStatistics().dwelling +  " \n";
        str += "getMotionStatistics stationary : " + motionDna.getMotionStatistics().stationary +  " \n";
        str += "getMotionStatistics walking : " + motionDna.getMotionStatistics().walking +  " \n"; */

        str += "getAttitude pitch: " + motionDna.getAttitude().pitch +  " \n";
        str += "getAttitude roll: " + motionDna.getAttitude().roll +  " \n";
        str += "getAttitude yaw: " + motionDna.getAttitude().yaw +  " \n";
        str += "getTimestamp : " + motionDna.getTimestamp() +  " \n";

        str += "getLocation().globalLocation.latitude : " + motionDna.getLocation().globalLocation.latitude +  " \n";
        str += "getLocation().globalLocation.longitude : " + motionDna.getLocation().globalLocation.longitude +  " \n";


        //str += "getGpsLocation().locationStatus : " + motionDna.getGpsLocation().locationStatus +  " \n";
        //str += "getLocation().verticalMotionStatus : " + motionDna.getLocation().verticalMotionStatus +  " \n";
        //str += "getGpsLocation().verticalMotionStatus : " + motionDna.getGpsLocation().verticalMotionStatus +  " \n";

        logFileUtility.setWriteToFile(str);

        textView.setTextColor(Color.BLACK);

        final String fstr = str;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(fstr);
            }
        });

        motionDnaApplication.sendUDPPacket(str);
    }

    //    This event receives estimation results from other devices in the server room. In order
    //    to receive anything, make sure you call startUDP to connect to a room. Again, it provides
    //    access to a MotionDna object, which can be unpacked the same way as above.
    //
    //
    //    If you aren't receiving anything, then the room may be full, or there may be an error in
    //    your connection. See the reportError event below for more information.

    @Override
    public void receiveNetworkData(MotionDna motionDna) {

        networkUsers.put(motionDna.getID(),motionDna);
        double timeSinceBootSeconds = elapsedRealtime() / 1000.0;
        networkUsersTimestamps.put(motionDna.getID(),timeSinceBootSeconds);
        StringBuilder activeNetworkUsersStringBuilder = new StringBuilder();
        List<String> toRemove = new ArrayList();

        activeNetworkUsersStringBuilder.append("Network Shared Devices:\n");
        for (MotionDna user: networkUsers.values()) {
            if (timeSinceBootSeconds - networkUsersTimestamps.get(user.getID()) > 2.0) {
                toRemove.add(user.getID());
            } else {
                activeNetworkUsersStringBuilder.append(user.getDeviceName());
                MotionDna.XYZ location = user.getLocation().localLocation;
                activeNetworkUsersStringBuilder.append(String.format(" (%.2f, %.2f, %.2f)",location.x, location.y, location.z));
                activeNetworkUsersStringBuilder.append("\n");
            }

        }
        for (String key: toRemove) {
            networkUsers.remove(key);
            networkUsersTimestamps.remove(key);
        }

        networkTextView.setText(activeNetworkUsersStringBuilder.toString());
    }

    //    This event receives arbitrary data from the server room. You must have
    //    called startUDP already to connect to the room.

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ?> map) {

    }

    //    Report any errors of the estimation or internal SDK

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
        switch (errorCode) {
            case ERROR_AUTHENTICATION_FAILED:
                System.out.println("Error: authentication failed " + s);
                break;
            case ERROR_SDK_EXPIRED:
                System.out.println("Error: SDK expired " + s);
                break;
            case ERROR_PERMISSIONS:
                System.out.println("Error: permissions not granted " + s);
                break;
            case ERROR_SENSOR_MISSING:
                System.out.println("Error: sensor missing " + s);
                break;
            case ERROR_SENSOR_TIMING:
                System.out.println("Error: sensor timing " + s);
                break;
        }
    }

    //    The two required methods shown below bind
    //    the interface to your application's activity,
    //    so MotionDna is able to retrieve the necessary
    //    permissions and capabilities
    @Override
    public PackageManager getPkgManager() {
        return getPackageManager();
    }

    @Override
    public Context getAppContext() {
        return getApplicationContext();
    }

    protected void onDestroy() {

        // Shuts downs the MotionDna Core
        motionDnaApplication.stop();

        // Handle destruction of the foreground service if
        // it is enabled
        if (motionDnaServiceIntent != null) {
            getAppContext().stopService(motionDnaServiceIntent);
        }
        super.onDestroy();
    }

    private void requestAppPermissions() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (hasReadPermissions() && hasWritePermissions()) {
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_WRITE_STORAGE_REQUEST_CODE); // your request code
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public void showDialog(){

        final Dialog dialog = new Dialog(MainActivity.this);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_listview);

        Button btndialog = (Button) dialog.findViewById(R.id.btndialog);
        btndialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        ListView listView = (ListView) dialog.findViewById(R.id.listview);
        final File[] myFileDirList = logFileUtility.fetchAllFiles();
        final List<String> fileList = new ArrayList<>();
        for(File file : myFileDirList) {
            fileList.add(file.getName());
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(this,R.layout.list_item, R.id.tv, fileList);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //textView.setText("You have clicked : "+fileList.get(position).toString());
                dialog.dismiss();
                File chosenFile = myFileDirList[position];
               // File file = chosenFile.getAbsoluteFile();
               // Uri uri = Uri.fromFile(file);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent.setDataAndType(uri, "text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                Uri apkURI = FileProvider.getUriForFile(
                        MainActivity.this,
                        MainActivity.this.getApplicationContext().getPackageName() + ".provider", chosenFile);
                intent.setDataAndType(apkURI, "text/plain");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                MainActivity.this.startActivity(intent);
            }
        });

        dialog.show();

    }
}
