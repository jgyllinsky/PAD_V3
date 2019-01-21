package com.stepmonitor.wearable_activities;

import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.stepmonitor.dataclient.DataClientManager;

public class WearableContinueLauncher extends AbstractWearableLauncher implements
        DataClientManager.OnDataChangedListener {

    //activity id for launch in service
    static int ACTIVITY_ID = 0x02;

    //channels to receive on
    public static String CHANNEL_REQUEST_CONTINUE = "/exercise_request_continue";

    /**
     * Dataclient tags for activity creation
     */
    public static String TAG_INTENT_TITLE = "title";    //string
    public static String TAG_INTENT_REQCONT = "continue";        //string

    /**
     * members for to store transmitting data in
     */
    private String mStringTitle;
    private String mStringContinue;

    private OnContinueResult mListener;

    public WearableContinueLauncher(DataClientManager dataClient,
                                        String title, String cont,
                                        WearableContinueLauncher.OnContinueResult listener) {
        this(dataClient,title,cont,listener,null,null);
    }

    public WearableContinueLauncher(DataClientManager dataClient,
                                    String title, String cont,
                                    WearableContinueLauncher.OnContinueResult listener,
                                    OnActivityStarted onStarted,
                                    OnActivityEnded onEnded) {

        super(dataClient,onStarted,onEnded);

        //channel to get response on
        mDataClient.setOnDataChangedListener(this);
        mDataClient.addChannel(CHANNEL_REQUEST_CONTINUE);

        mStringTitle = title;
        mStringContinue = cont;

        mListener = listener;

    }

    public boolean start() {
        if (super.start()) {
            //package starting data
            DataMap exerciseInitData = new DataMap();
            exerciseInitData.putString(TAG_INTENT_TITLE,mStringTitle);
            exerciseInitData.putString(TAG_INTENT_REQCONT,mStringContinue);

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
            if (channel.equals(CHANNEL_REQUEST_CONTINUE)) {
                mListener.onContinueResult();
            }
        }
    }

    public interface OnContinueResult  {
        void onContinueResult();
    }

}
