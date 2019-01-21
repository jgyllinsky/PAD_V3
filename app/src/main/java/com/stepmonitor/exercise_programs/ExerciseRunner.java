package com.stepmonitor.exercise_programs;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.stepmonitor.fragments.NavViewFragments.SettingsFragment;
import com.stepmonitor.dataclient.AbstractMultiSourceSensor;
import com.stepmonitor.dataclient.SensorClientManager;
import com.stepmonitor.system.DataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

/**
 * This class creates a new thread and runs an walking exercise in with, updating every second
 * A listener is ued to update the caller on the
 *
 * TODO the fact that this is in a folder called exercise_programs is based on an earlier version of the app where
 * multiple exercises were considered.  This should be changed by making this whole class the exercise thread as
 * the wrapping structure of it is unneeded
 *
 */
public class ExerciseRunner implements SensorClientManager.OnSensorChanged {

    /**
     * -------- Members
     */

    //the sensors used
    public static final ArrayList<Integer> SENSORS = new ArrayList(Arrays.asList(Sensor.TYPE_STEP_COUNTER,Sensor.TYPE_STEP_DETECTOR));

    //the start time
    private Date mStart;

    //the target date at which the exercise will stop
    private Date mTarget;

    //determine if the data should be recorded
    private boolean mRecord;

    //listener to send updates to on main thread
    private OnExerciseChanged mListener;

    //sensor client to record data from
    private SensorClientManager mSensorClient;

    //stop watch thread
    private ExerciseRunnerThread mExerciseRunnerThread;

    //step measurments
    private int mStepsBaseline;
    private int mStepsCurrent;

    //historical measurments per second
    private DataPackage mDataPackage;

    public ExerciseRunner() {
        mDataPackage = new DataPackage();
        mExerciseRunnerThread = new ExerciseRunnerThread();

        mStepsBaseline = -1;
        mStepsCurrent = -1;
    }

    /**
     * Factory for runner, using the provided parameters a exercise runner is generated and properly configured based on the given parameters
     * Currently there is no configuration, so this exists just for future possible use
     * @param target
     * @param record
     * @param listener
     * @return
     */
    public static ExerciseRunner generateExercise(Date target, boolean record, OnExerciseChanged listener, final SensorClientManager sensorClient) {
        ExerciseRunner runner = new ExerciseRunner();
        runner.mTarget = target;
        runner.mRecord = record;
        runner.mListener = listener;
        runner.mSensorClient = sensorClient;

        runner.mStart = new Date();

        return runner;
    }

    /**
     * -------- Methods
     */

    /**
     * Role: Starts the exercise
     * Exceptions: Throws RunTimeException if runner thread is already started or null
     */
    public void start() throws RuntimeException {
        if (mExerciseRunnerThread == null || mExerciseRunnerThread.isStarted())
            throw new RuntimeException("Exercise runner already started!");

        mSensorClient.setOnSensorChangedListener(this);

        //bind sensors
        //TODO: send in array
        (new Thread(new Runnable() {
            @Override
            public void run() {
                for (Integer sensor : SENSORS) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mSensorClient.bindSensor(sensor);
                }
                mExerciseRunnerThread.start();
            }
        })).start();

    }

    /**
     * Role: Uses App parameters to convert step count to distance
     * @param steps the number of steps
     * @return a double representing the distance traveld in the same units as those for the persons height
     */
    private double getDistance(int steps) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mListener.getContext());
        double height = Double.parseDouble(sharedPref.getString(SettingsFragment.PREF_KEY_USER_HEIGHT, "0"));
        return 0.45f * height * steps;
    }

    /**
     * Role: converts two dates into a formatted string indicating the time between them
     * @param a start date
     * @param b end date
     * @return string in HH:MM:SS format //TODO i think java dateformat can do this natively
     */
    public String getDurationTimestamp(Date a, Date b) {
        int difference = (int)(a.getTime() - b.getTime()) / 1000;

        String info = "";
        int hour = difference / 3600;
        info += (hour < 10) ? "0" + Integer.toString(hour) : Integer.toString(hour);
        int minutes = (difference % 3600) / 60;
        info += ":" + ((minutes < 10) ? "0" + Integer.toString(minutes) : Integer.toString(minutes));
        int seconds = difference % 60;
        info += ":" + ((seconds < 10) ? "0" + Integer.toString(seconds) : Integer.toString(seconds));

        return info;
    }

    /**
     * Role: return a fractional percentage of current progress
     * @return a double between 0 and 1
     */
    public double getProgress() {
        return ((double)((new Date()).getTime() - mStart.getTime())) / ((double)(mTarget.getTime() - mStart.getTime()));
    }

    public Date getStartTime() {
        return mStart;
    }

    public Date getEndTime() {
        return mTarget;
    }

    @Deprecated
    public String getUpdateText() {
        //get textual description of exercise status
        String result = "";
        result += "Time Remaining : " + getDurationTimestamp(mTarget,new Date()) + "\n";
        result += "Steps : " + Integer.toString( mStepsCurrent - mStepsBaseline) + "\n";
        double distance = getDistance(mStepsCurrent - mStepsBaseline);
        result += "Distance in Meters : " + String.format("%.2f", distance) + "\n";
        return result;
    }

    /**
     * Role: forcefully quit the exercise
     */
    public void quit() {
        mExerciseRunnerThread.quit();
    }

    /**
     * -------- Overrides
     */

    /**
     * Role: Retrieves data from SensorClientManager and appends result to current data
     * @param event sensor manager passed event
     */
    @Override
    public void onSensorChanged(AbstractMultiSourceSensor.PackagedSensorEvent event) {
        //If the event is not step counter or step detector then the wearable app must not be compatible with the application
        assert(event.type == Sensor.TYPE_STEP_COUNTER || event.type == Sensor.TYPE_STEP_DETECTOR);
        switch (event.type) {
            //A step was detected, just increment
            case Sensor.TYPE_STEP_DETECTOR:
                mStepsCurrent++; //a step was detected, just increment
                break;
            //The step counter was updated, update steps to new value
            case Sensor.TYPE_STEP_COUNTER:
                //if not started set mStepsBaseline
                if (mStepsBaseline == -1) {
                    mStepsBaseline = (int)event.values[0] - mStepsCurrent;
                }
                mStepsCurrent = (int)event.values[0];
                break;
            default:
                break;
        }
    }

    /**
     * -------- Interfaces
     */

    /**
     * This interfaces is used to grabs the context from the exercise create as well
     * as provide callbacks so that the caller can interact with the exercise
     */
    public interface OnExerciseChanged {
        void onExerciseStarted();
        void onExerciseUpdated();
        void onExerciseFinished();
        Context getContext();
    }

    /**
     * -------- Classes
     */

    /**
     * This thread is the core of the exercise functionality
     * Each second it the data buffers for new data from the wear sensors
     * then it pushes the data to a DataPackage, updates wear UI and calls caller callback
     */
    private class ExerciseRunnerThread extends Thread {

        //allows to forcefully quit
        private boolean mQuit;

        //determine if started
        private boolean mStarted;

        //main thread access
        private Handler mMainThread;

        ExerciseRunnerThread() {
            mQuit = false;
            mStarted = false;
            mMainThread = new Handler(Looper.getMainLooper());
        }

        /**
         * Role: this method provides a way to exit the thread forcefully
         * //TODO: replace this with a proper messager or BroadcastReceiver
         */
        public void quit() {
            mQuit = true;
        }

        /**
         * Role: tests if the exercise has started
         * @return true if the exercise has started, false otherwise
         */
        public boolean isStarted() {
            return mStarted;
        }

        /**
         * -------- Overrides
         */

        /**
         * Role: Calls onStart callbacks and starts thread
         */
        @Override
        public void start() {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onExerciseStarted();
                    ExerciseRunnerThread.super.start();
                    mStarted = true;
                }
            });
        }

        /**
         * Role: Each second updates data from sensors and calls onUpdate callbacks on listener
         */
        @Override
        public void run() {
            //date to keep track of current time
            Date time = new Date();

            while ( !mQuit && time.compareTo(mTarget) < 0 ) {

                //fetch values
                final int steps = mStepsCurrent - mStepsBaseline;
                final double distance = getDistance(steps);
                final Date current = time;

                //update listener
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onExerciseUpdated();
                    }
                });

                //record data
                mDataPackage.push(current,steps);

                //update watch every second
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                time = new Date();  //update time

            }

            Log.e("ExerciseRunner","Exercise Finished");

            //export data
            String json = mDataPackage.export();

            if (mRecord)
                DataManager.writeToSystem("/walking",Long.toString(time.getTime())+".csv",json,false);

            //unbind all sensors
            //TODO unbind in array
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    for (Integer sensor : SENSORS) {
                        try {
                            Thread.sleep(50);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mSensorClient.unbindSensor(sensor);
                    }
                }
            })).start();

            //call onfinished callback
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onExerciseFinished();
                }
            });

        }

    }

    /**
     * This class is used to easily manage data from the watch by storing and exporting data for the current session
     */
    private class DataPackage {

        //time and dates of each step taken
        private ArrayList<Date> mTimes;
        private ArrayList<Integer> mSteps;

        DataPackage() {
            mTimes = new ArrayList<>();
            mSteps = new ArrayList<>();
        }

        /**
         * Role: add an entry to the data package
         * Side Effects: None
         * @param date the time of the entry
         * @param steps the entry for current number of steps
         */
        public void push(Date date, int steps) {
            mTimes.add(date);
            mSteps.add(steps);
        }

        /**
         * Role: take this data package and convert to json string
         * @return JSON String representing the recorded data of the exercise
         */
        public String export() {

            //csv package all data
            ArrayList<String[]> data = new ArrayList<>();
            data.add(new String[] {"timestamp","steps","distance"});
            for (int i = 0; i < mTimes.size(); i++) {
                data.add(new String[]{mTimes.get(i).toString(),mSteps.get(i).toString(),Double.toString(getDistance(mSteps.get(i)))});
            }
            String csv = DataManager.convertToCSV(data);

            //convert to json
            HashMap<String,String> map = new HashMap();
            map.put("step_data",csv);
            String json = DataManager.covertToJson(map);

            return json;
        }

    }

}
