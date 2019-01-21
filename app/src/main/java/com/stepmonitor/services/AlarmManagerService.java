package com.stepmonitor.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.stepmonitor.fragments.NavViewFragments.SettingsFragment;
import com.stepmonitor.threads.InteractableExerciseRunner;

import java.util.Calendar;

import static com.stepmonitor.fragments.NavViewFragments.SettingsFragment.PREF_KEY_EXERCISE_TRIGGERTIME;
import static com.stepmonitor.services.ExerciseService.ACTION_CONFIRMATION_EXERCISE;
import static com.stepmonitor.services.ExerciseService.EXTRA_DURATION;

/**
 * AlarmManagerService has two primary roles
 * 1. host the methods to alter the exercise alarm
 * 2. handle exercise alarm triggers when they occur
 */
public class AlarmManagerService extends JobIntentService {

    private static final String TAG = "AlarmManagerService";
    private static void LOG(String msg) { Log.d(TAG,msg); }
    private static void LOGE(String msg) { Log.e(TAG,msg); }

    private static int JOB_ID = 101;

    public static void setExerciseAlarm(Context context, final int minutes, final int duration) {

        LOG("Set Exercise Alarm");

        clearExerciseAlarm(context);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmManagerService.class);
        intent.setAction(ACTION_CONFIRMATION_EXERCISE);
        intent.putExtra(EXTRA_DURATION,duration);
        PendingIntent alarmIntent = PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //parse minutes to get exact time
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final int start = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_START,"0"));
        final int end = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_END,"0"));
        //if time is not in range set to be at start time

        //if we are not in bounds of valid times to exercise then set to start
        if ( (minutes >= start && minutes <= end) ) {
            calendar.set(Calendar.HOUR_OF_DAY, minutes / 60);
            calendar.set(Calendar.MINUTE, minutes % 60);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, start / 60);
            calendar.set(Calendar.MINUTE, start % 60);
        }

        //if time is in the past then add one day
        if (calendar.getTime().getTime() < System.currentTimeMillis())
            calendar.add(Calendar.DAY_OF_MONTH,1);


        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
//        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);

        //store the time in a triggertime preference for global access
        //TODO maybe find a better solution, global variables are bad
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PREF_KEY_EXERCISE_TRIGGERTIME, calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));
        editor.commit();

    }

    public static void delayExerciseAlarm(Context context, final int delay, final int duration) {

        LOG("Delay Exercise Alarm | " + Integer.toString(delay));

        //delay by taking the current time adding delay minutes to it
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE,delay);

        final int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        setExerciseAlarm(context,minutes,duration);

    }

    public static void clearExerciseAlarm(Context context) {

        LOG("Clear Exercise Alarm");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmManagerService.class);
        intent.setAction(ACTION_CONFIRMATION_EXERCISE);
        intent.putExtra(EXTRA_DURATION,999);
        PendingIntent alarmIntent = PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(alarmIntent);
    }

    @Override
    protected void onHandleWork(Intent intent) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        final int time = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_INTERVAL,"5"));

        Calendar calendar = Calendar.getInstance().getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE,time);

        int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        final int duration = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_DURATION,"5"));
        setExerciseAlarm(getBaseContext(),minutes,duration);

        LOG("Starting Thread | " + Integer.toString(minutes) + " | " + Integer.toString(duration));

        //TODO new method
        //pull data from google on total steps for the day and activites
        //check delta in activity for last hour
        //if delta was too small then tell the user to exercise
            //present exercise button
            //if user clicks exercise button
                //set snooze alarm for snooze minutes
            //else
                //continuously send vibs until user clicks continue
        //else
            //do nothing

        //TODO, move setExerciseAlarm to thread callback for when the exercise is done
        if (!ExerciseService.getExerciseStatus())
            (new InteractableExerciseRunner(getApplicationContext(),duration)).start();
        else
            LOGE("Exercise is already running, a new exercise will be attempted again after another break periods duration of time");

    }

}
