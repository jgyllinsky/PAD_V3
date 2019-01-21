package com.stepmonitor.preferences;

import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.EditTextPreferenceDialogFragmentCompat;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TimePicker;

import com.stepmonitor.R;

public class DistanceEditTextPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private EditText mEditText;

    public static DistanceEditTextPreferenceDialogFragmentCompat newInstance(String key) {
        DistanceEditTextPreferenceDialogFragmentCompat f
                = new DistanceEditTextPreferenceDialogFragmentCompat();

        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        f.setArguments(args);

        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditText = (EditText) view.findViewById(R.id.edit_distance);

        // Exception: There is no TimePicker with the id 'edit' in the dialog.
        if (mEditText == null) {
            throw new IllegalStateException("Dialog view must contain a TimePicker with id 'edit'");
        }

        // Get the time from the related Preference
        double distance = 0.d;
        DialogPreference preference = getPreference();
        if (preference instanceof DistanceEditTextPreference) {
            distance = ((DistanceEditTextPreference) preference).getDistance();
            Log.d("DistancePrefFrag",Double.toString(distance));
        }

        mEditText.setText( Double.toString(distance) );
    }

    /**
     * Called when the Dialog is closed.
     *
     * @param positiveResult Whether the Dialog was accepted or canceled.
     */
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {

            float distance = Float.parseFloat(mEditText.getText().toString());

            // Save the value
            DialogPreference preference = getPreference();
            if (preference instanceof DistanceEditTextPreference) {
                DistanceEditTextPreference timePreference = ((DistanceEditTextPreference) preference);
                // This allows the client to ignore the user value.
                if (timePreference.callChangeListener(distance)) {
                    // Save the value
                    timePreference.setDistance(distance);
                }
            }
        }
    }

}
