package com.stepmonitor.fragments.NavViewFragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.stepmonitor.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class WeeklyExerciseHistoryFragment extends Fragment {

    private static final String TAG = "WEHistoryFragment";
    private static void LOG(String msg) { Log.d(TAG,msg); }
    private static void LOGE(String msg) { Log.e(TAG,msg); }

    public WeeklyExerciseHistoryFragment() {
        // Required empty public constructor
    }

    public static WeeklyExerciseHistoryFragment newInstance() {
        WeeklyExerciseHistoryFragment fragment = new WeeklyExerciseHistoryFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Role: Creates a DataReadRequest for data in the given time period
     * @param start date to start pulling data from
     * @param end date to end pulling data from
     * @param units units which to break buckets into
     * @return
     */
    public static DataReadRequest queryFitnessData(Date start, Date end, TimeUnit units) {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        cal.setTime(end);
        long endTime = cal.getTimeInMillis();
        cal.setTime(start);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        LOG( "Range Start: " + dateFormat.format(startTime));
        LOG( "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest =
                new DataReadRequest.Builder()
                        // The data request can specify multiple data types to return, effectively
                        // combining multiple data queries into one call.
                        // In this example, it's very unlikely that the request is for several hundred
                        // datapoints each consisting of a few steps and a timestamp.  The more likely
                        // scenario is wanting to see how many steps were walked per day, for 7 days.
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                        .bucketByTime(1, units)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
        // [END build_read_data_request]

        return readRequest;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View parent = inflater.inflate(R.layout.fragment_weekly_exercise_history, container, false);

        final LinearLayout dayFrags = parent.findViewById(R.id.linear_layout_exercise_container);

        //get our sign in
        GoogleSignInAccount gsa = GoogleSignIn.getLastSignedInAccount(getActivity());

        //get step data and add data to day views
        if (gsa != null) {

            //get one weeks forth of data
            Calendar cl = Calendar.getInstance();
            cl.setTime(new Date());
            cl.set(Calendar.HOUR_OF_DAY, 0);
            cl.set(Calendar.MINUTE, 0);
            cl.set(Calendar.SECOND, 0);
            cl.set(Calendar.MILLISECOND, 0);
            cl.add(Calendar.DAY_OF_MONTH,-6);   //go back one week
            Date start = cl.getTime();
            cl.setTime(new Date());
            cl.set(Calendar.HOUR_OF_DAY, 0);
            cl.set(Calendar.MINUTE, 0);
            cl.set(Calendar.SECOND, 0);
            cl.set(Calendar.MILLISECOND, 0);
            cl.add(Calendar.DAY_OF_MONTH,1); //log today
            Date end = cl.getTime();

            //get request for step data for the week
            DataReadRequest readRequest = WeeklyExerciseHistoryFragment.queryFitnessData(start,end,TimeUnit.DAYS);

            //perform request
            Fitness.getHistoryClient(getActivity(), gsa)
                    .readData(readRequest)
                    .addOnSuccessListener(
                            new OnSuccessListener<DataReadResponse>() {
                                @Override
                                public void onSuccess(DataReadResponse dataReadResponse) {
                                    LOG("DATA RECEIVED | " + Integer.toString(dataReadResponse.getBuckets().size()) );

                                    for (int i = 0; i < dataReadResponse.getBuckets().size(); i++) {

                                        Bucket bucket = dataReadResponse.getBuckets().get(i);

                                        // here we accumulate the total string data
                                        int total_steps = 0;

                                        for (DataSet dataSet : bucket.getDataSets()) {

                                            for (DataPoint dp : dataSet.getDataPoints()) {
                                                Field field = dp.getDataType().getFields().get(0);
                                                total_steps += dp.getValue(field).asInt();

                                                LOG(Integer.toString(i) + " " + new Date(dp.getStartTime(TimeUnit.MILLISECONDS)).toString());

                                            }

                                        }

                                        final int idx_day = dataReadResponse.getBuckets().size() - i - 1;

                                        // get views
                                        final View dayView = dayFrags.getChildAt(idx_day);
                                        TextView view_title = dayView.findViewById(R.id.text_view_exercise_select);
                                        TextView view_minutes = dayView.findViewById(R.id.text_view_exercise_select_minutes);
                                        TextView view_steps = dayView.findViewById(R.id.text_view_exercise_select_steps);
                                        ProgressBar progress = dayView.findViewById(R.id.progress_bar_steps);

                                        //get time string
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("E");
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.setTimeInMillis(System.currentTimeMillis());
                                        calendar.add(Calendar.DAY_OF_YEAR,-1 * idx_day);
                                        String df = dateFormat.format(calendar.getTime());

                                        //set view strings
                                        view_title.setText( df );
                                        view_minutes.setText("unknown");
                                        view_steps.setText(Integer.toString(total_steps) + " Steps Taken ");

                                        //set progress bar
                                        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                        final int target_steps = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_TARGET_STEPS,"1").replaceAll("[^\\d.]", ""));
                                        progress.setMax(target_steps);
                                        progress.setProgress(total_steps);
                                        if (total_steps >= target_steps)
                                            progress.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

                                    }
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "There was a problem reading the data.", e);
                                }
                            });
        } else {
            LOGE("Null GSA");
        }

        //TODO add transition here to move to the DailyExerciseHistoryFragment
        for (int i = 0; i < dayFrags.getChildCount(); i++) {
            dayFrags.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    getActivity().getSupportFragmentManager().beginTransaction().replace(
//                        v.getId(),
//                        DailyExerciseHistoryFragment.newInstance()
//                    ).commit();
//

////                                                ViewGroup.LayoutParams params = dayView.getLayoutParams();
////                                                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
////                                                dayView.requestLayout();
//
//                    Scene start;
//                    Scene end;
//
//                    //start = Scene.getSceneForLayout(root,R.layout.fragment_weekly_exercise_history,getActivity());
//                    View end_view = inflater.inflate(R.layout.fragment_weekly_exercise_history,null);
//
//                    LinearLayout ll = end_view.findViewById(R.id.linear_layout_exercise_container);
//                    ll.getChildAt(idx_day).setVisibility(View.INVISIBLE);
//                    end = new Scene(container,end_view);
//
//                    Transition transition = new ChangeTransform();
////                                                        TransitionInflater.from(getActivity()).
////                                                                inflateTransition(R.transition.fade_transition);
//                    TransitionManager.go(end, transition);
                }
            });
        }

        return parent;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    /**
     * Activity target replaces exercise duration
     * exercise interval removed
     * delay unchanged
     * start and end time unchanged
     * height unchanged
     *
     *
     */

    /**
     * -------- Classes
     */

    /**
     * Role: Fragment for showing graphics for daily activity
     */
    //TODO note this is not used right now
    public static class DailyExerciseHistoryFragment extends Fragment {
        private WebView mWebView;
        public static DailyExerciseHistoryFragment newInstance() {
            DailyExerciseHistoryFragment fragment = new DailyExerciseHistoryFragment();

            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                //todo
            }
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View parent = inflater.inflate(R.layout.fragment_daily_exercise_history, container, false);

            mWebView = parent.findViewById(R.id.webview_daily_exercise);

            WebSettings settings = mWebView.getSettings();
            settings.setJavaScriptEnabled(true); //enable javascript
            settings.setAllowFileAccessFromFileURLs(true); //allow different file requests
            settings.setAllowUniversalAccessFromFileURLs(true); //allow access to the whole internet

            mWebView.setVerticalScrollBarEnabled(false);
            mWebView.setHorizontalScrollBarEnabled(false);

            mWebView.loadUrl("file:///android_asset/text.html");

            return parent;
        }
        @Override
        public void onResume() {
            super.onResume();
            mWebView.onResume();
        }
        @Override
        public void onPause() {
            super.onPause();
            mWebView.onPause();
        }
    }

}
