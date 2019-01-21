package com.stepmonitor.preferences;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.EditText;

import com.stepmonitor.R;

import java.text.DecimalFormat;

public class TimerEditTextPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private EditText mEditTextHours;
    private EditText mEditTextMinutes;
    private EditText mEditTextSeconds;

    /**
     * Creates a new Instance of the TimePreferenceDialogFragment and stores the key of the
     * related Preference
     *
     * @param key The key of the related Preference
     * @return A new Instance of the TimePreferenceDialogFragment
     */
    public static TimerEditTextPreferenceDialogFragmentCompat newInstance(String key) {
        final TimerEditTextPreferenceDialogFragmentCompat
                fragment = new TimerEditTextPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditTextHours = (EditText) view.findViewById(R.id.edit_timer_hours);
        mEditTextMinutes = (EditText) view.findViewById(R.id.edit_timer_minutes);
        mEditTextSeconds = (EditText) view.findViewById(R.id.edit_timer_seconds);

        final DecimalFormat df = new DecimalFormat("00");

        View.OnFocusChangeListener listener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v instanceof EditText) {
                    EditText et = (EditText)v;
                    try {
                        int d =  Integer.parseInt(et.getText().toString());
                        et.setText( df.format(d) );
                    } catch (Exception IllegalArgumentException) {
                        et.setText("00");
                    }
                }
            }
        };

        mEditTextHours.setOnFocusChangeListener(listener);
        mEditTextMinutes.setOnFocusChangeListener(listener);
        mEditTextSeconds.setOnFocusChangeListener(listener);

        // Exception: There is no TimePicker with the id 'edit' in the dialog.
        if (mEditTextHours == null || mEditTextMinutes == null || mEditTextSeconds == null) {
            throw new IllegalStateException("Dialog view must contain a TimePicker with id 'edit'");
        }

        // Get the time from the related Preference
        int time = 0;
        DialogPreference preference = getPreference();
        if (preference instanceof TimerEditTextPreference) {
            time = ((TimerEditTextPreference) preference).getTime();
        }

        TimePreferenceDialogFragmentCompat.getTimestamp(time);

        mEditTextHours.setText( df.format(time / 60) );
        mEditTextMinutes.setText( df.format(time % 60) );
        mEditTextSeconds.setText( "00" );
    }

    /**
     * Called when the Dialog is closed.
     *
     * @param positiveResult Whether the Dialog was accepted or canceled.
     */
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {

            int time = Integer.parseInt(mEditTextHours.getText().toString()) * 60 + Integer.parseInt(mEditTextMinutes.getText().toString());

            // Save the value
            DialogPreference preference = getPreference();
            if (preference instanceof TimerEditTextPreference) {
                TimerEditTextPreference timePreference = ((TimerEditTextPreference) preference);
                // This allows the client to ignore the user value.
                if (timePreference.callChangeListener(time)) {
                    // Save the value
                    timePreference.setTime(time);
                }
            }
        }
    }
}
