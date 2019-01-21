package com.wear;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Generic Display for app, shows phone connection status
 */
public class MainActivity extends WearableActivity implements MultiSourceSensorHost.OnSensorChanged {

    /**
     * Permission Identifiers
     */
    private static final int PERMISSION_BODY_SENSOR = 1;

    /**
     * Views
     */
    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusTextView = findViewById(R.id.status_text_view);

        //prevent app from going to sleep
        setAmbientEnabled();

        //bind background service to listen for activity activations
        BackgroundService.startService(this);

        /**
         * Check Permissions
         */
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, PERMISSION_BODY_SENSOR);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        //set text based on phone connection status
        Wearable.getNodeClient(this).getConnectedNodes().addOnCompleteListener(new OnCompleteListener<List<Node>>() {
            @Override
            public void onComplete(@NonNull Task<List<Node>> task) {
                if (task.getResult().size() > 0) {
                    mStatusTextView.setText("Phone Connected");
                } else {
                    mStatusTextView.setText("Phone Not Found");
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    /*
     * Permissions Callback
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_BODY_SENSOR: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.i("Permissions","Body Sensor Permissions Granted!");

                } else {

                    Log.i("Permissions","Failed to obtain permissions for body sensors.");
                }
                return;
            }
        }
    }

}
