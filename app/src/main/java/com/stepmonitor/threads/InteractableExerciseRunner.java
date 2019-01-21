package com.stepmonitor.threads;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.stepmonitor.services.ExerciseService;

import java.lang.ref.WeakReference;

/**
 * This class allows a interactive ExerciseService to be run in the background
 * This thread handles the service lifetime and closes on exercise delay or completion
 *
 * The reason we use a thread is because the exercise service itself contains a thread
 * we need to keep the service alive so that it can communicate with the exercise thread via callbacks
 *
 * By using a thread we make it so an interactable exercise can be launched with ease
 * anywhere in the program.  There is no need to implement binder methods and callbacks
 * It also ensure the exercise stays alive even when its caller is destroyed
 *
 */
public class InteractableExerciseRunner extends Thread {

    private static String TAG = "IERunner";
    private static void LOG(String msg) { Log.d(TAG,msg); }
    private static void LOGE(String msg) { Log.e(TAG,msg); }

    //local variables, please note volatile is required to avoid race conditions and ensure atomic operation
    private volatile ExerciseService mService;
    private ExerciseService.ExerciseStateReceiver mReceiver;
    private volatile boolean isDone = false;

    //program context
    WeakReference<Context> mContext;

    //the duration of the exercise
    final int mDuration;

    public InteractableExerciseRunner(Context context, int duration) {
        mContext = new WeakReference<>(context);
        mDuration = duration;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LOG("onServiceConnected");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ExerciseService.LocalBinder binder = (ExerciseService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    private void end() {
        isDone = true;
    }

    @Override
    public void run() {

        //TODO
        //replace thread polling with callbacks
        //ex connectioncallback runs exercise
        //exerciser receiver manages thread finishing

        if (ExerciseService.getExerciseStatus())
            throw new RuntimeException("Exercise is already running!");

        LOG("Starting IERunner");

        try {
            Intent intent = new Intent(mContext.get(), ExerciseService.class);
            if (!mContext.get().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                LOGE("Failed to bind service!");
            }

            // wait to get our service
            while (mService == null) {
                try {
                    Thread.sleep(1000);
                    LOG("Waiting for Service");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            LOGE(e.getMessage());
            e.printStackTrace();
        }

        LOG("Starting Exercise");

        try {
            // loop while exercise is performed, this is basically thread.loop but with broadcast receivers
            mReceiver = ExerciseService.registerBroadcastReceiver(mContext.get(), new ExerciseService.ExerciseStateListener() {
                @Override
                public void onStart(Context context) {
                    LOG("onStart");
                }
                @Override
                public void onDelay(Context context) {
                    LOG("onDelay");
                    end();
                }
                @Override
                public void onEnd(Context context) {
                    LOG("onEnd");
                    end();
                }
            });
        } catch (Exception e) {
            LOGE(e.getMessage());
            e.printStackTrace();
        }

        ExerciseService.startActionExercise(mContext.get(),mDuration,true);

        while (!isDone);
        LOG("Ending IERunner");

        ExerciseService.unregisterBroadcastReceiver(mContext.get(),mReceiver);
        mContext.get().unbindService(mConnection);

    }

}
