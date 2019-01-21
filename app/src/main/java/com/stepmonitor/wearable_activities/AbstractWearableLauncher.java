package com.stepmonitor.wearable_activities;

import android.support.annotation.CallSuper;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.stepmonitor.dataclient.DataClientManager;

public abstract class AbstractWearableLauncher implements DataClientManager.OnDataChangedListener {

    /**
     * Channels for activity life cycles
     */
    public static final String CHANNEL_START_ACTIVITY = "/start_activity";
    public static final String CHANNEL_START_CALLBACK_ACTIVITY = "/callback_activity";
    public static final String CHANNEL_FINISH_ACTIVITY = "/finish_activity";
    public static final String CHANNEL_FINISH_CALLBACK_ACTIVITY = "/finish_activity_callback";;

    //tags for activity start package
    public static final String TAG_START_ACTIVITY = "activity_type";
    public static final String TAG_START_DATA = "data";

    //channel to trigger a vibrate on the watch
    public static final String CHANNEL_VIBRATE = "/activity_functions/vibrate";
    public static final String TAG_VIBRATE_DATA = "vibrate_data";

    //channel to trigger kill all action on the watch
    public static final String CHANNEL_KILL_ALL = "/channel_kill_all";

    protected DataClientManager mDataClient;
    protected boolean mState;
    protected DataMap mPackage;

    private OnActivityStarted mOnActivityStartedCallback;
    private OnActivityEnded mOnActivityEndedCallback;

    /**
     * Role: used to end all remote activties on the watch
     * This should typically be called before starting a wearable activity session.
     * That way if a session fails a blank session can be made
     * @param dataClient
     */
    public static void killAllWearableActivites(DataClientManager dataClient) {
        DataMap wearableUpdate = new DataMap();
        dataClient.sendMessage(wearableUpdate, CHANNEL_KILL_ALL);
    }

    AbstractWearableLauncher(DataClientManager dataClient) {
        this(dataClient,null,null);
    }

    AbstractWearableLauncher(DataClientManager dataClient, OnActivityStarted onStarted, OnActivityEnded onEnded) {
        mDataClient = dataClient;
        mDataClient.addChannel(CHANNEL_START_CALLBACK_ACTIVITY);
        mDataClient.addChannel(CHANNEL_FINISH_CALLBACK_ACTIVITY);
        mOnActivityStartedCallback = onStarted;
        mOnActivityEndedCallback = onEnded;
        mState = false;
    }

    @CallSuper
    public boolean start() {
        //start only if off, return true for successful start
        if (!mState) {
            mState = true;
            return true;
        } else {
            return false;
        }
    }

    @CallSuper
    public boolean end() {
        //end only if on, return true for successful end
        if (mState) {
            DataMap wearableUpdate = new DataMap();
            mDataClient.sendMessage(wearableUpdate, CHANNEL_FINISH_ACTIVITY);
            mState = false;
            return true;
        } else {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @CallSuper
    public void onDataChanged(DataMap map, String channel) {
        Log.i("Function Call","Wearable Abstract Launcher : onDataChanged | Channel : " + channel);

        //make approriate callback call
        switch (channel) {
            case CHANNEL_START_CALLBACK_ACTIVITY:
                if (mOnActivityStartedCallback != null) {
                    mOnActivityStartedCallback.onActivityStarted();

                }
                break;
            case CHANNEL_FINISH_CALLBACK_ACTIVITY:
                if (mOnActivityEndedCallback != null) {
                    mOnActivityEndedCallback.onActivityEnded();

                }
                break;
            default:
                break;
        }
    }

    /**
     * Role: Trigger a vibration sequence on the watch
     * @param sequence - the vibration sequence to send to the watch
     */
    public void vibrate(long[] sequence) {
        if (mState) {
            DataMap wearableUpdate = new DataMap();
            wearableUpdate.putLongArray(TAG_VIBRATE_DATA,sequence);
            mDataClient.sendMessage(wearableUpdate, CHANNEL_VIBRATE);
        }
    }

    /**
     * -------- Interfaces
     */

    /**
     * Role: callback for use when an wearable activity has started
     */
    public interface OnActivityStarted {
        void onActivityStarted();
    }

    /**
     * Role: callback for use when an wearable activity has ended
     */
    public interface OnActivityEnded {
        void onActivityEnded();
    }

}
