package com.stepmonitor.wearable_activities;

import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.stepmonitor.dataclient.DataClientManager;

/**
 * An object which manages the remove progress activity
 */
public class WearableProgressLauncher extends AbstractWearableLauncher {

    //the activity id for the wearable activity
    static int ACTIVITY_ID = 0x00;

    //channels to send messages to
    static String CHANNEL_PROGRESS = "/exercise_progress";

    /**
     * DataClient tags for initiating an exercise
     */
    static String TAG_INTENT_TITLE = "title";
    static String TAG_INTENT_RANGE = "range";

    /**
     * DataClient tags for monitoring exercise progress
     */
    static String TAG_RUN_PROGRESS = "progress";
    static String TAG_RUN_STATUS = "status";

    private int mAmount;
    private String mInfo;

    public WearableProgressLauncher(DataClientManager dataClient, int amount, String info) {
        this(dataClient,amount,info,null,null);
    }

    public WearableProgressLauncher(DataClientManager dataClient, int amount, String info, OnActivityStarted onStarted, OnActivityEnded onEnded) {
        super(dataClient,onStarted,onEnded);
        mAmount = amount;
        mInfo = info;
    }

    @Override
    public boolean start() {
        if (super.start()) {
            //package starting data
            DataMap exerciseInitData = new DataMap();
            exerciseInitData.putString(TAG_INTENT_TITLE,mInfo);
            exerciseInitData.putInt(TAG_INTENT_RANGE,mAmount);

            //prepare activity start message and package exercise arguments
            final DataMap wearableActivityTrigger = new DataMap();
            wearableActivityTrigger.putInt(TAG_START_ACTIVITY,ACTIVITY_ID);
            wearableActivityTrigger.putDataMap(TAG_START_DATA,exerciseInitData);

            //launch activity on wearable and give some time to add the listener
            mDataClient.sendMessage(wearableActivityTrigger, CHANNEL_START_ACTIVITY);

            return true;
        } else {
            return false;
        }
    }

    public void update(int remaining, String info) {

        if (mState) {
            DataMap wearableUpdate = new DataMap();
            wearableUpdate.putString(TAG_RUN_STATUS,"Remaining:\n"+info);
            wearableUpdate.putInt(TAG_RUN_PROGRESS, remaining);
            mDataClient.sendMessage(wearableUpdate, CHANNEL_PROGRESS);
        }

    }

}
