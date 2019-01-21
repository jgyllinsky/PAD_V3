package com.stepmonitor.wearable_activities;

import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.stepmonitor.dataclient.DataClientManager;

public class WearableConfirmationLauncher extends AbstractWearableLauncher implements
        DataClientManager.OnDataChangedListener {

    //activity id
    static int ACTIVITY_ID = 0x01;

    //channels to recieve on
    public static String CHANNEL_REQUEST_RESPONSE = "/exercise_request_response";

    /**
     * Dataclient tags for activity creation
     */
    public static String TAG_INTENT_TITLE = "title";    //string
    public static String TAG_INTENT_REQYES = "yes";        //string
    public static String TAG_INTENT_REQNO = "no";          //string

    /**
     * Dataclient tags for sending data back to phone
     */
    public static String TAG_RESPONSE = "response";     //boolean

    private String mInfo;
    private String mYesText;
    private String mNoText;

    private OnConfirmationResult mListener;

    public WearableConfirmationLauncher(DataClientManager dataClient,
                                        String title, String yes, String no,
                                        OnConfirmationResult listener) {
        this(dataClient,title,yes,no,listener,null,null);
    }

    public WearableConfirmationLauncher(DataClientManager dataClient,
                                        String title, String yes, String no,
                                        OnConfirmationResult listener,
                                        OnActivityStarted onStarted,
                                        OnActivityEnded onEnded) {
        super(dataClient,onStarted,onEnded);

        //channel to get response on
        mDataClient.setOnDataChangedListener(this);
        mDataClient.addChannel(CHANNEL_REQUEST_RESPONSE);

        mInfo = title;
        mYesText = yes;
        mNoText = no;

        mListener = listener;
    }

    public boolean start() {
        if (super.start()) {
            //package starting data
            DataMap exerciseInitData = new DataMap();
            exerciseInitData.putString(TAG_INTENT_TITLE,mInfo);
            exerciseInitData.putString(TAG_INTENT_REQYES,mYesText);
            exerciseInitData.putString(TAG_INTENT_REQNO,mNoText);

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


    /**
     * {@inheritDoc}
     */
    @Override
    public void onDataChanged(DataMap map, String channel) {
        super.onDataChanged(map,channel);

        if (mState) {
            Log.i("Function Call","Wearable Confirmation Launcher : onDataChanged | Channel : " + channel);
            if (channel.equals(CHANNEL_REQUEST_RESPONSE)) {
                mListener.onConfirmationResult(map.getBoolean(TAG_RESPONSE));
            }
        }
    }

    public interface OnConfirmationResult {
        void onConfirmationResult(boolean result);
    }

}
