package com.wear;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.CallSuper;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;

public class BackgroundService extends JobIntentService implements
        DataClientManager.OnDataChangedListener,
        MultiSourceSensorHost.OnSensorChanged {

    private static final String TAG = "BackgroundService";
    private static void LOG(String msg) { Log.d(TAG,msg); }
    private static void LOGE(String msg) { Log.e(TAG,msg); }

    /**
     * Channels To Broadcast Over and bundle key
     */
    public static final String BROADCAST_START_ACTIVITY = "com.steptracker.broadcast.start_activity";
    public static final String RECEIVER_DATA_MAP = "com.steptracker.broadcast.data.map";

    public static final String BROADCAST_KILL_ALL = "com.steptracker.broadcast.kill_all";

    /**
     * JobIntentService Identifiers and actions
     */
    public static final int JOB_START_ACTIVITY_ID = 1000;
    public static final String ACTION_START_ACTIVITY = "com.steptracker.action.wait"; // we want to start an activity
    public static final String ACTION_KILL_ALL = "com.steptracker.kill_all"; //we want to end all wearable launched activites

    //TODO remove this its redundant with bottom stuff
    /**
     * channels to listen over for activity starts with bundle tags
     */
    public static final String CHANNEL_START_ACTIVITY = "/start_activity";
    public static final String CHANNEL_START_CALLBACK_ACTIVITY = "/callback_activity";
    public static final String CHANNEL_FINISH_ACTIVITY = "/finish_activity";
    public static final String CHANNEL_FINISH_CALLBACK_ACTIVITY = "/finish_activity_callback";

    //channel to trigger kill all action on the watch
    public static final String CHANNEL_KILL_ALL = "/channel_kill_all";

    //abstract call which remote launched wearable activities will be launched from
    public static abstract class ServiceLaunchedActivity extends WearableActivity implements
            DataClientManager.OnDataChangedListener {

        /**
         * Channels to listen over for activity starts with bundle tags
         */
        public static final String CHANNEL_START_ACTIVITY = "/start_activity";
        public static final String CHANNEL_START_CALLBACK_ACTIVITY = "/callback_activity";
        public static final String CHANNEL_FINISH_ACTIVITY = "/finish_activity";
        public static final String CHANNEL_FINISH_CALLBACK_ACTIVITY = "/finish_activity_callback";

        /**
         * Channels for auxillary phone to watch functionality for every LaunchedWearableActivity
         */
        //channel to trigger a vibrate on the watch
        public static final String CHANNEL_VIBRATE = "/activity_functions/vibrate";
        public static final String TAG_VIBRATE_DATA = "vibrate_data";

        /**
         * Receiver to listen to BackgroundService
         * currently only used to activate kill all functions
         */
        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_KILL_ALL)) {
                    finish();
                }
            }
        };

        protected DataClientManager mDataClientManager;
        @CallSuper
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDataClientManager = new DataClientManager(this,this);
            mDataClientManager.bindClient();

            IntentFilter filter = new IntentFilter(BROADCAST_KILL_ALL);
            filter.addAction(ACTION_KILL_ALL);
            LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mReceiver,filter);

            // Enables Always-on
            setAmbientEnabled();
        }
        @CallSuper
        protected void onResume() {
            super.onResume();
            mDataClientManager.addChannel(CHANNEL_FINISH_ACTIVITY);
            mDataClientManager.addChannel(CHANNEL_VIBRATE);

            mDataClientManager.sendMessage(new DataMap(),CHANNEL_START_CALLBACK_ACTIVITY);
        }
        @CallSuper
        protected void onDestroy() {
            super.onDestroy();
            mDataClientManager.sendMessage(new DataMap(),CHANNEL_FINISH_CALLBACK_ACTIVITY);
            mDataClientManager.unbindClient();
            LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(mReceiver);
        }
        @CallSuper
        public void onDataChanged(DataMap map, String channel) {
            if (channel.equals(CHANNEL_FINISH_ACTIVITY)) {
                finish();
            } else if(channel.equals(CHANNEL_VIBRATE)) {
                Vibrator vibrator = (Vibrator) getBaseContext().getSystemService(VIBRATOR_SERVICE);
                long[] vibrationPattern = map.getLongArray(TAG_VIBRATE_DATA);
                final int indexInPatternToRepeat = -1;
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            }
        }
    }

    public static final String TAG_ACTIVITY_TYPE = "activity_type";
    public static final String TAG_ACTIVITY_DATA = "data";

    /**
     * possible activities to start
     */
    public static final int ACTIVITY_SENSOR_PROGRESS = 0;
    public static final int ACTIVITY_CONFORMATION = 1;
    public static final int ACTIVITY_CONTINUE = 2;

    /**
     * bundle key for transaction between service and new activity
     */
    public static final String EXTRA_DATA = "data";

    private DataClientManager mDataClientManager;
    private MultiSourceSensorHost mSensorManager;

    /**
     * broad cast receiver, called when a onDataChanged is called in this class
     */
    static class ActivityStartReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive

        @Override
        public void onReceive(Context context, Intent intent) {

            //vibrate to notify user of app wake up
            Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0, 300, 200, 100, 500};
            //-1 - don't repeat
            final int indexInPatternToRepeat = -1;

            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

            Bundle map = intent.getBundleExtra(RECEIVER_DATA_MAP);

            int activity = map.getInt(TAG_ACTIVITY_TYPE);

            //pass args to correct activity
            Class<?> cls = Activity.class;
            Intent activityIntent;
            switch (activity) {
                case ACTIVITY_SENSOR_PROGRESS:
                    cls = SensorProgressActivity.class;
                    break;
                case ACTIVITY_CONFORMATION:
                    cls = ConfirmationActivity.class;
                    break;
                case ACTIVITY_CONTINUE:
                    cls = ContinueActivity.class;
                    break;
                default:
                    break;
            }

            LOG("Creating Activity ... " + cls.getSimpleName());

            activityIntent = new Intent(context,cls);
            activityIntent.putExtra(EXTRA_DATA,map.getBundle(TAG_ACTIVITY_DATA));
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }

    public BackgroundService() {
        super();
    }

    /**
     * Starts a service to listen to activity initiating messages
     * @param context - used for broadcast creation, not stored after creation
     */
    public static void startService(Context context) {

        //create an intent filter to the broadcasting channel
        IntentFilter statusIntentFilter = new IntentFilter(BackgroundService.BROADCAST_START_ACTIVITY);

        //create an activity starter class
        //TODO: this leaks, please fix
        ActivityStartReceiver activityStartReceiver = new ActivityStartReceiver();

        //register the receiver with the filter
        LocalBroadcastManager.getInstance(context).registerReceiver(
                activityStartReceiver,
                statusIntentFilter);

        //package the intent to start the service
        Intent intent = new Intent(context,BackgroundService.class);
        intent.setAction(ACTION_START_ACTIVITY);
        enqueueWork(context, BackgroundService.class, JOB_START_ACTIVITY_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Called after startService from enqueueWork, this gets the intent passed by enqueueWork
     * @param intent
     */
    @Override
    protected void onHandleWork(Intent intent) {
        mDataClientManager = new DataClientManager(getApplicationContext(),this);
        mSensorManager = new MultiSourceSensorHost(getApplicationContext(),this);

        Log.e("Handle Action","Handle");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START_ACTIVITY.equals(action)) {
                handleActionWait();
            }
        }
    }

    /**
     * Called on service creation when performing action start_activity
     * bind client and listen to activity starting channel
     */
    private void handleActionWait() {

        Log.e("Handle Action","Wait action started");

        //client to listen for activity start requests
        mDataClientManager.bindClient();

        //client to send sensor data to phone
        mSensorManager.bindClient();

        //get ready to listen to activity requests
        mDataClientManager.addChannel(CHANNEL_START_ACTIVITY);

        //get ready to listen to kill requests
        mDataClientManager.addChannel(CHANNEL_KILL_ALL);

    }

    /**
     * Listen to service messages and parse
     * @param map
     * @param channel
     */
    @Override
    public void onDataChanged(DataMap map, String channel) {

        //if starting activity pass data to broadcaster
        if (channel.equals(CHANNEL_START_ACTIVITY)) {

            Intent localIntent = new Intent(BROADCAST_START_ACTIVITY);
            localIntent.putExtra(RECEIVER_DATA_MAP,map.toBundle());

            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        }

        //if killing all simply issue the broadcast
        if (channel.equals(CHANNEL_KILL_ALL)) {
            Intent localIntent = new Intent(BROADCAST_KILL_ALL);
            localIntent.setAction(ACTION_KILL_ALL);

            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }

    }

    /**
     * OnSensorChangedListener used by multisensor host, not used right now
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        //pass
    }

}
