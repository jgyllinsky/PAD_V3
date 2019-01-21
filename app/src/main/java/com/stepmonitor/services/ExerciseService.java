package com.stepmonitor.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.stepmonitor.R;
import com.stepmonitor.activites.SurveyActivity;
import com.stepmonitor.dataclient.AbstractMultiSourceSensor;
import com.stepmonitor.dataclient.DataClientManager;
import com.stepmonitor.dataclient.SensorClientManager;
import com.stepmonitor.exercise_programs.ExerciseRunner;
import com.stepmonitor.fragments.NavViewFragments.SettingsFragment;
import com.stepmonitor.wearable_activities.WearableConfirmationLauncher;
import com.stepmonitor.wearable_activities.WearableContinueLauncher;
import com.stepmonitor.wearable_activities.WearableProgressLauncher;

import java.util.Date;
import java.util.UUID;

/**
 * A background service which handles iteraction between user driven events and exercises
 * This includes a broadcast receiver register which notifies on certain exercise events
 *
 * exercise lifetime is automatic from this service as long as its kept alive
 */
public class ExerciseService extends Service implements
        DataClientManager.OnDataChangedListener,
        SensorClientManager.OnSensorChanged,
        ExerciseRunner.OnExerciseChanged {

    private static final String TAG = "ExerciseService";
    private static void LOG(String msg) { Log.d(TAG,msg); }
    private static void LOGE(String msg) { Log.e(TAG,msg); }

    public enum MODE {
        EXCERCISE(0x00),
        EXERCISE_CONFIRMATION(0x01);

        private int mode;

        MODE(int mode) {
            this.mode = mode;
        }

    }

    //notification channel for the exercise survey
    public static final String NOTIFICATION_CHANNEL = "channel.survey";

    //list of actions and passed arguments for the service
    public static final String ACTION_CONFIRMATION_EXERCISE = "action.confirmation_exercise";
    public static final String ACTION_EXERCISE = "action.exercise";
    public static final String EXTRA_DURATION = "extra.duration";
    public static final String EXTRA_UUID = "extra.uuid";

    //BroadcastReceiver channel with actions and extras
    public static final String BROADCAST_STATE_CHANGE = "broadcast.exercise_state";
    public static final String EXTRA_STATE_CHANGE_UUID = "broadcast.exercise_state.extra.uuid";
    public static final String ACTION_ON_START = "action.start";
    public static final String ACTION_ON_DELAY = "action.cancel";
    public static final String ACTION_ON_END = "action.end";

    //service statuses
    //TODO: this is likely a bad way to store state, fix this by using system to check service status
    private static boolean mStatusExercise = false;

    //dataclients
    private DataClientManager mDataClient;
    private SensorClientManager mSensorClient;

    //exercise runner which performs the actual exercise in another thread and runs callsbacks on the service thread
    private ExerciseRunner mRunner;

    //wearable activities which we use to interact with the watch
    private WearableConfirmationLauncher mWearableConfirmation; //get confirmation of user desire to exercise
    private WearableContinueLauncher mWearableContinueExerciseInstructions; //tell the user instructions are on the phone
    private WearableProgressLauncher mWearableProgress; //show progress during the exercise
    private WearableContinueLauncher mWearableContinueExerciseEnd; //tell the user they did a good job and to take the survey

    //service ID
    private int mServiceID;
    private UUID mExerciseID;

    //Service Binder
    private final IBinder mBinder = new LocalBinder();

    /**
     * starts an exercise at the current time with the given duration
     * @param context - program context
     * @param duration - the duration to perform the exercise in minutes
     */
    public static boolean startActionExercise(Context context, int duration, boolean confirmation) {

        LOG("startActionExerciseCalled");

        if (!mStatusExercise) {
            Intent intent = new Intent(context, ExerciseService.class);
            intent.setAction(confirmation ? ACTION_CONFIRMATION_EXERCISE : ACTION_EXERCISE);
            intent.putExtra(EXTRA_DURATION, duration);
            intent.putExtra(EXTRA_UUID, UUID.randomUUID().toString());

            //set status to launched
            mStatusExercise = true;
            context.startService(intent);
            return true;
        } else {
            throw new RuntimeException("Exercise Already Started");
            //return false;
        }

    }

    public static boolean stopActionExercise(Context context) {

        LOG("stopActionExerciseCalled");

        if (mStatusExercise) {
            Intent intent = new Intent(context, ExerciseService.class);

            //set status to launched
            mStatusExercise = false;
            context.stopService(intent);
            return true;
        } else {
            throw new RuntimeException("Exercise Not Started");
            //return false;
        }

    }

    /**
     * Registers a broadcast receiver to the services broadcasts
     * @param context
     * @param listener
     * @return
     */
    public static ExerciseStateReceiver registerBroadcastReceiver(Context context, ExerciseStateListener listener) {
        ExerciseStateReceiver receiver = new ExerciseStateReceiver(listener);

        IntentFilter filter = new IntentFilter(ExerciseService.BROADCAST_STATE_CHANGE);
        filter.addAction(ExerciseService.ACTION_ON_START);  //for some reason you need to set these action on the phone but not the wearable
        filter.addAction(ExerciseService.ACTION_ON_DELAY);  //this was a massive headache
        filter.addAction(ExerciseService.ACTION_ON_END);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,filter);

        return receiver;
    }

    /**
     * Unregisters a broadcast receiver, should be done before receiver destruction
     * @param context
     * @param receiver
     */
    public static void unregisterBroadcastReceiver(Context context, ExerciseStateReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public static boolean getExerciseStatus() {
        return mStatusExercise;
    }

    @Override
    public void onCreate() {

        LOG("onCreate");

        //client to listen for activity start requests
        mDataClient = new DataClientManager(ExerciseService.this,ExerciseService.this);
        mDataClient.bindClient();

        //client to send sensor data to phone
        mSensorClient = new SensorClientManager(ExerciseService.this,ExerciseService.this);
        mSensorClient.useWearable();
        mSensorClient.bindClient();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        LOG("onStartCommand");

        if (mWearableContinueExerciseEnd != null) {
            LOGE("Something is wrong, wearable launcher already declared");
        }

        mServiceID = startId;
        mExerciseID = UUID.fromString(intent.getStringExtra(EXTRA_UUID));

        String action = intent.getAction();
        final int duration = intent.getIntExtra(EXTRA_DURATION, 0);
        switch (action) {
            case ACTION_CONFIRMATION_EXERCISE:
                stopSelf();
                handleActionConfirmationExercise(duration);
                break;
            case ACTION_EXERCISE:
                stopSelf();
                handleActionExercise(duration);
                break;
            default:
                break;
        }

        // issue broadcast of exercise start
        Intent localIntent = new Intent(BROADCAST_STATE_CHANGE);
        localIntent.setAction(ACTION_ON_START);
        localIntent.putExtra(EXTRA_STATE_CHANGE_UUID,mExerciseID.toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ExerciseService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ExerciseService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }

    @Override
    public void onDestroy() {

        LOG("onDestroy");

        //force quit the exercise, this will also call onExerciseFinished
        if (mRunner != null)
            mRunner.quit();

        //make sure wearable activities are dead
        if (mWearableConfirmation != null)
            mWearableConfirmation.end();

        if (mWearableProgress != null)
            mWearableProgress.end();

        if (mWearableContinueExerciseEnd != null)
            mWearableContinueExerciseEnd.end();

        if (mWearableContinueExerciseInstructions != null)
            mWearableContinueExerciseInstructions.end();

        super.onDestroy();
    }

    public void handleActionConfirmationExercise(final int duration) {

        //first ensure that the wearable is started at a clean slate
        WearableConfirmationLauncher.killAllWearableActivites(mDataClient);

        //gain confirmation through wearable confirmation
        //TODO - make some kind of wearable sequence builder to prevent these nested changes of callbacks
        mWearableConfirmation = new WearableConfirmationLauncher(
            mDataClient,
            "Start Exercise?",
            "Start Now",
            "Wait",
            new WearableConfirmationLauncher.OnConfirmationResult() {
                @Override
                public void onConfirmationResult(boolean result) {
                    if (result) {

                        mWearableConfirmation.end();

                        //start new wearable activity to ask if the user is ready and knows how to do the exercise.
                        mWearableContinueExerciseInstructions = new WearableContinueLauncher(
                                mDataClient,
                                "Are you ready?\nExercise Instructions\nare available\non the phone app.",
                                "Okay",
                                new WearableContinueLauncher.OnContinueResult() {
                                    @Override
                                    public void onContinueResult() {
                                        mWearableContinueExerciseInstructions.end();
                                        handleActionExercise(duration);
                                    }
                                }
                        );

                        mWearableContinueExerciseInstructions.start();

                    } else {

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        final int delay = Integer.parseInt(sharedPreferences.getString(SettingsFragment.PREF_KEY_EXERCISE_DELAY,"0"));
                        AlarmManagerService.delayExerciseAlarm(getContext(),delay,duration);

                        //end the current activity
                        mWearableConfirmation.end();
                        mStatusExercise = false; // put service in finished state

                        //broadcast the delay
                        Intent localIntent = new Intent(BROADCAST_STATE_CHANGE);
                        localIntent.setAction(ACTION_ON_DELAY);
                        localIntent.putExtra(EXTRA_STATE_CHANGE_UUID,mExerciseID.toString());
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);

                        //user requested to wait for next exercise
                        stopSelf(mServiceID);

                    }
                }
            }
        );

        //start the confirmation
        mWearableConfirmation.start();

        Log.d("Exercise Service","Wearable Confirmation Started");

    }

    /**
     * Handle action Exercise in the provided background thread for duration minutes
     */
    public void handleActionExercise(final int duration) {

        //initialize runner
        Date target = new Date();
        target.setTime( target.getTime() + 60 * 1000 * duration );
        mRunner = ExerciseRunner.generateExercise(target,true,ExerciseService.this, mSensorClient);

        //start runner
        mRunner.start();

    }

    @Override
    public void onDataChanged(DataMap map, String channel) {

    }

    @Override
    public void onSensorChanged(AbstractMultiSourceSensor.PackagedSensorEvent event) {

    }

    @Override
    public void onExerciseStarted() {
        LOG("onExerciseStarted");

        //initialize wearable progress
        int amount = (int)(mRunner.getEndTime().getTime() - mRunner.getStartTime().getTime()) / 1000;
        mWearableProgress = new WearableProgressLauncher(mDataClient,amount,"Starting Exercise ...");

        mWearableProgress.start();
    }

    @Override
    public void onExerciseUpdated() {
        LOG("onExerciseUpdated");

        //update watch
        String info = mRunner.getDurationTimestamp(mRunner.getEndTime(),new Date());
        int remaining = (int)(mRunner.getEndTime().getTime() - (new Date()).getTime()) / 1000;

        int motivation_interval = getResources().getInteger(R.integer.motivation_interval);
        float motivation_threshold = motivation_interval * getResources().getInteger(R.integer.motivation_percentage) / 100.f;

        if ( remaining % motivation_interval < motivation_threshold ) {
            info = "Good Job!\nKeep it up!";
        }

        if ( remaining % ( (int)motivation_threshold + motivation_interval ) == 0 ) {
            mWearableProgress.vibrate(new long[] {0,100,200,300,400});
        }

        mWearableProgress.update(remaining,info);
    }

    @Override
    public void onExerciseFinished() {
        LOG("onExerciseFinished");

        //end the wearable activity
        mWearableProgress.end();

        //let the user know the exercise is done
        mWearableContinueExerciseEnd = new WearableContinueLauncher(
                mDataClient,
                "Great Job!\nCheck your phone\nto tell us how it was.",
                "Okay",
                new WearableContinueLauncher.OnContinueResult() {
                    @Override
                    public void onContinueResult() {

                        mWearableContinueExerciseEnd.end();

                        //create a notification on the phone so that the user can perform a survey
                        Intent intent = new Intent(ExerciseService.this, SurveyActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(ExerciseService.this, 0 /* Request code */, intent,
                                PendingIntent.FLAG_ONE_SHOT);

                        String channelId = NOTIFICATION_CHANNEL;
                        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        NotificationCompat.Builder notificationBuilder =
                                new NotificationCompat.Builder(ExerciseService.this, channelId)
                                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                                        .setContentTitle("Your Exercise Survey")
                                        .setContentText("Click here to fill out your exercise survey.")
                                        .setAutoCancel(false)
                                        .setSound(defaultSoundUri)
                                        .setContentIntent(pendingIntent);

                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        // Since android Oreo notification channel is needed.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel(channelId,
                                    "Channel Exercise",
                                    NotificationManager.IMPORTANCE_HIGH);
                            notificationManager.createNotificationChannel(channel);
                        }

                        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build());



                        // issue broadcast of exercise end
                        Intent localIntent = new Intent(BROADCAST_STATE_CHANGE);
                        localIntent.setAction(ACTION_ON_END);
                        localIntent.putExtra(EXTRA_STATE_CHANGE_UUID,mExerciseID.toString());
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);

                        stopSelf(mServiceID);
                        mStatusExercise = false; // put service in finished state
                    }
                }
        );

        mWearableContinueExerciseEnd.start();

    }

    @Override
    public Context getContext() {
        return getBaseContext();
    }

    public interface ExerciseStateListener {
        void onStart(final Context context);
        void onDelay(final Context context);
        void onEnd(final Context context);
    }

    //TODO: find a way to implement an interface which automatically binds,unbinds the reciever
    public static class ExerciseStateReceiver extends BroadcastReceiver {
        ExerciseStateListener mListener;
        ExerciseStateReceiver(ExerciseStateListener listener) {
            mListener = listener;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("Received Message","Message Received");
            switch (intent.getAction()) {
                case ACTION_ON_START:
                    mListener.onStart(context);
                    break;
                case ACTION_ON_DELAY:
                    mListener.onDelay(context);
                    break;
                case ACTION_ON_END:
                    mListener.onEnd(context);
                    break;
                default:
                    break;
            }
        }
    }
}
