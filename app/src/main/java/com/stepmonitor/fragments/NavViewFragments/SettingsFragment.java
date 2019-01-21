package com.stepmonitor.fragments.NavViewFragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stepmonitor.R;
import com.stepmonitor.preferences.DistanceEditTextPreference;
import com.stepmonitor.preferences.DistanceEditTextPreferenceDialogFragmentCompat;
import com.stepmonitor.preferences.TimePreference;
import com.stepmonitor.preferences.TimePreferenceDialogFragmentCompat;
import com.stepmonitor.preferences.TimerEditTextPreference;
import com.stepmonitor.preferences.TimerEditTextPreferenceDialogFragmentCompat;

/**
 * Settings Menu for user parameters
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    //identifiers for global access
    public static String PREF_KEY_USER_HEIGHT = "pref_key_user_height";
    public static String PREF_KEY_EXERCISE_START = "pref_key_exercise_start";
    public static String PREF_KEY_EXERCISE_END = "pref_key_exercise_end";
    public static String PREF_KEY_EXERCISE_DURATION = "pref_key_exercise_duration";
    public static String PREF_KEY_EXERCISE_DELAY = "pref_key_exercise_delay";
    public static String PREF_KEY_EXERCISE_INTERVAL = "pref_key_exercise_interval";
    public static String PREF_KEY_TARGET_STEPS = "pref_key_target_steps";

    //the one is for accessing the time of the exercise, its kind of a hack but it works
    public static String PREF_KEY_EXERCISE_TRIGGERTIME = "pref_key_exercise_time";

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = super.onCreateView(inflater,container,savedInstanceState);

        //Because of the way android works with preferences the margins must be set manually
        //after fragment creation

        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.actionBarSize,typedValue,true);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float marginTop = typedValue.getDimension(metrics);

        fragmentView.setPadding(0,(int)marginTop,0,0);

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        // Try if the preference is one of our custom Preferences
        DialogFragment dialogFragment = null;

        if (preference instanceof TimePreference) {
            dialogFragment = TimePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        if (preference instanceof DistanceEditTextPreference) {
            dialogFragment = DistanceEditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        if (preference instanceof TimerEditTextPreference) {
            dialogFragment = TimerEditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }

        if (dialogFragment != null) {
            // The dialog was created (it was one of our custom Preferences), show the dialog for it
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference" +
                    ".PreferenceFragment.DIALOG");
        } else {
            // Dialog creation could not be handled here. Try with the super method.
            super.onDisplayPreferenceDialog(preference);
        }

    }

}
