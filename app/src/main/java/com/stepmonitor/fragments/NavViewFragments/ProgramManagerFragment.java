package com.stepmonitor.fragments.NavViewFragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.stepmonitor.R;
import com.stepmonitor.dataclient.DataClientManager;
import com.stepmonitor.dataclient.SensorClientManager;
import com.stepmonitor.preferences.TimePreferenceDialogFragmentCompat;
import com.stepmonitor.services.ExerciseService;
import com.stepmonitor.threads.InteractableExerciseRunner;


public class ProgramManagerFragment extends Fragment {

    private DataClientManager mDataClient;
    private SensorClientManager mSensorClient;

    private TextView mExerciseTime;
    private TextView mExerciseDuration;

    private Button mButtonConfigure;
    private Button mButtonStart;

    private ExerciseService.ExerciseStateReceiver mReceiver;

    public ProgramManagerFragment() {
        // Required empty public constructor
    }

    public static ProgramManagerFragment newInstance(DataClientManager dataClient, SensorClientManager sensorClient) {
        ProgramManagerFragment fragment = new ProgramManagerFragment();
        fragment.mDataClient = dataClient;
        fragment.mSensorClient = sensorClient;

        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_program_manager, container, false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        mExerciseTime = fragmentView.findViewById(R.id.manager_time_value);
        int time = sharedPref.getInt(SettingsFragment.PREF_KEY_EXERCISE_TRIGGERTIME, 1);
        String timestamp = TimePreferenceDialogFragmentCompat.getTimestamp(time);
        mExerciseTime.setText(timestamp);

        //display the duration for which to perform the exercise
        mExerciseDuration = fragmentView.findViewById(R.id.manager_duration_value);
        int duration = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_DURATION,"0"));
        String durationstamp = Integer.toString(duration) + ":00";
        mExerciseDuration.setText(durationstamp);

        //button to bring up exercise configuration
        mButtonConfigure = fragmentView.findViewById(R.id.button_configure_manager);
        mButtonConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(
                        R.id.content_container,
                        SettingsFragment.newInstance()
                ).commit();
            }
        });

        //button to forcefully start the exercise
        mButtonStart = fragmentView.findViewById(R.id.button_start_from_manager);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                int duration = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_DURATION,"0"));

                if (!ExerciseService.getExerciseStatus()) {
                    //ExerciseService.startActionExercise(getContext(),1,true);
                    (new InteractableExerciseRunner(ProgramManagerFragment.this.getContext(),duration)).start();

                    mButtonStart.setText("Stop");
                } else {
                    ExerciseService.stopActionExercise(getContext());

                    mButtonStart.setText("Start");
                }
            }
        });

        //set text based on current status
        if (!ExerciseService.getExerciseStatus()) {
            mButtonStart.setText("Start");
        } else {
            mButtonStart.setText("Stop");
        }

        //unregistered on activity destruction
        mReceiver = ExerciseService.registerBroadcastReceiver(getContext(),new ExerciseService.ExerciseStateListener() {
            @Override
            public void onStart(final Context context ) {
                Log.e("OnStart","start");
                mButtonStart.setText("Stop");
            }
            @Override
            public void onDelay(final Context context ) {
                
            }
            @Override
            public void onEnd(final Context context ) {
                Log.e("OnEnd","end");
                mButtonStart.setText("Start");
            }
        });

        // Inflate the layout for this fragment
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        //make sure text is set correctly since exercise could be triggered by alarm
        //TODO: maybe use AlarmManager callback instead
        if (ExerciseService.getExerciseStatus()) {
            mButtonStart.setText("Stop");
        } else {
            mButtonStart.setText("Start");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //must unregisterReceiver global reciever
        ExerciseService.unregisterBroadcastReceiver(getContext(),mReceiver);
    }

}
