package com.stepmonitor.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;

import com.stepmonitor.R;
import com.stepmonitor.fragments.NavViewFragments.SettingsFragment;

public class TimePreferenceStart extends TimePreference {

    /**
     * a bunch of constructor overrides, they are required
     * @param context
     */

    public TimePreferenceStart(Context context) {
        // Delegate to other constructor
        this(context, null);
    }

    public TimePreferenceStart(Context context, AttributeSet attrs) {
        // Delegate to other constructor
        // Use the preferenceStyle as the default style
        this(context, attrs, R.attr.preferenceStyle);
    }

    public TimePreferenceStart(Context context, AttributeSet attrs, int defStyleAttr) {
        // Delegate to other constructor
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public TimePreferenceStart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // Du custom stuff here
        // ...
        // read attributes etc.
    }

    /**
     * Role: Overrides setTime so that it does not allow times after the end period
     * @param time The time to save
     */
    @Override
    public void setTime(int time) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        //final int start = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_START,"0"));
        final int end = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_END,"0"));

        //if are start time was less then the end time then we can set it
        if (time < end) {
            mTime = time;

            // Save to SharedPreference
            persistString(Integer.toString(time));
        } else {
            //else the time is invalid and we must let the user know, we must also run on ui thread
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),"You cannot start your daily exercises after you stopped doing them!",Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
