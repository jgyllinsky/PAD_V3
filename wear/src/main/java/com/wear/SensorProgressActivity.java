package com.wear;

import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

/**
 * This phone launched activity gives a way to provide progress on the watch
 * progress is sent from the phone to the watch via the CHANNEL_PROGRESS channel
 */
public class SensorProgressActivity extends BackgroundService.ServiceLaunchedActivity implements
        DataClientManager.OnDataChangedListener,
        MultiSourceSensorHost.OnSensorChanged {

    /**
     * Channels to use
     * progress if for phone to send progress to the wearable
     */
    public static String CHANNEL_PROGRESS = "/exercise_progress";

    /**
     * Dataclient tags for progress
     */
    public static String TAG_STATUS = "status";         //string
    public static String TAG_PROGRESS = "progress";     //int



    /**
     * Dataclient tags for activity creation
     */
    public static String TAG_INTENT_TITLE = "title";    //string
    public static String TAG_INTENT_RANGE = "range";    //int

    /**
     * Views
     */
    private TextView mTextViewStatus;
    private ProgressBar mProgressBarProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_progress);

        mTextViewStatus = findViewById(R.id.exercise_status);
        mProgressBarProgress = findViewById(R.id.exercise_progress);
        mProgressBarProgress.setIndeterminate(false);

    }

    @Override
    protected void onResume() {

        mDataClientManager.addChannel(CHANNEL_PROGRESS);

        Bundle map = getIntent().getBundleExtra(BackgroundService.EXTRA_DATA);

        //set initial status message
        mTextViewStatus.setText(map.getString(TAG_INTENT_TITLE));

        //set progress bar range
        mProgressBarProgress.setMax(map.getInt(TAG_INTENT_RANGE));

        super.onResume();
    }

    @Override
    public void onDataChanged(DataMap map, String channel) {

        if (channel.equals(CHANNEL_PROGRESS)) {
            if (map.getInt(TAG_PROGRESS) < mProgressBarProgress.getMax()) {
                mTextViewStatus.setText(map.getString(TAG_STATUS));
                mProgressBarProgress.setProgress(map.getInt(TAG_PROGRESS));
            }
        }

        super.onDataChanged(map,channel);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //do nothing
    }

}
