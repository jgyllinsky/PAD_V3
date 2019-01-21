package com.stepmonitor;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.stepmonitor.dataclient.DataClientManager;
import com.stepmonitor.dataclient.AbstractMultiSourceSensor;
import com.stepmonitor.dataclient.SensorClientManager;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.renderscript.Element;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v14.preference.EditTextPreferenceDialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.stepmonitor.fragments.NavViewFragments.InstructionsFragment;
import com.stepmonitor.fragments.NavViewFragments.ProgramManagerFragment;
import com.stepmonitor.fragments.NavViewFragments.SettingsFragment;
import com.stepmonitor.fragments.NavViewFragments.WeeklyExerciseHistoryFragment;
import com.stepmonitor.services.AlarmManagerService;
import com.stepmonitor.services.ExerciseService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.stepmonitor.wearable_activities.AbstractWearableLauncher;
import com.stepmonitor.wearable_activities.WearableConfirmationLauncher;
import com.stepmonitor.wearable_activities.WearableContinueLauncher;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

/**
 * Main Activity using toolbar and draw layout
 * Provides Navigation to:
 * ExerciseSelectionFragment
 * SettingsFragment
 */
public class MainActivity extends AppCompatActivity implements
        DataClientManager.OnDataChangedListener,
        SensorClientManager.OnSensorChanged,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainActivity";
    private static void LOG(String msg) { Log.d(TAG,msg); }
    private static void LOGE(String msg) { Log.e(TAG,msg); }

    /**
     * Permission Identifiers
     */
    private static final int PERMISSION_STORAGE = 1;

    /**
     * Views
     */
    private DrawerLayout mDrawerLayout;
    private TextView mNavTitle;
    private NavigationView mNavigationView;
    private Toolbar mToolbar;

    /**
     * Sensor and Data client
     */
    private DataClientManager mDataClientManager;
    private SensorClientManager mSensorClient;

    // Identifier to identify the sign in activity.
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //permissions for storage
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);

        }

        //permissions for google fitness api
        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            if (GoogleSignIn.getLastSignedInAccount(this) == null) {

                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();

                GoogleSignInClient cl = GoogleSignIn.getClient(this, gso);
                Intent signInIntent = cl.getSignInIntent();
                startActivityForResult(signInIntent, 1);

            } else {
                GoogleSignIn.requestPermissions(
                        this,
                        REQUEST_OAUTH_REQUEST_CODE,
                        GoogleSignIn.getLastSignedInAccount(this),
                        fitnessOptions);
            }

        }

        //data clients
        mDataClientManager = new DataClientManager(this,this);
        mSensorClient = new SensorClientManager(this,this);

        //tool bar setup
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        //drawer setup
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavigationView = findViewById(R.id.nav_view);
        mNavTitle = mNavigationView.getHeaderView(0).findViewById(R.id.nav_title);
        mNavigationView.setNavigationItemSelectedListener( new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                // set item as selected to persist highlight
                menuItem.setChecked(true);
                // close drawer when item is tapped
                mDrawerLayout.closeDrawers();

                //determine menu selection
                switch (menuItem.getItemId()) {
                    //move to the exercise selection menu
                    case R.id.nav_history:
                        getSupportFragmentManager().beginTransaction().replace(
                                R.id.content_container,
                                WeeklyExerciseHistoryFragment.newInstance()
                        ).commit();
                        break;
                    case R.id.nav_exercises:
                        getSupportFragmentManager().beginTransaction().replace(
                            R.id.content_container,
                            ProgramManagerFragment.newInstance(mDataClientManager,mSensorClient)
                        ).commit();
                        break;
                    case R.id.nav_instructions:
                        getSupportFragmentManager().beginTransaction().replace(
                                R.id.content_container,
                                InstructionsFragment.newInstance()
                        ).commit();
                        break;
                    case R.id.nav_settings:
                        getSupportFragmentManager().beginTransaction().replace(
                                R.id.content_container,
                                SettingsFragment.newInstance()
                        ).commit();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        //create default view
        getSupportFragmentManager().beginTransaction().replace(
                R.id.content_container,
                WeeklyExerciseHistoryFragment.newInstance()
        ).commit();

        /**
         * Setup Alarms
         */

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final int duration = Integer.parseInt(sharedPref.getString(SettingsFragment.PREF_KEY_EXERCISE_DURATION,"0"));

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE,2);

        int time = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        AlarmManagerService.setExerciseAlarm(getApplicationContext(),time,duration);

        //registerReceiver listener to preferences to update alarm when the time to trigger changes
        sharedPref.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * https://developers.google.com/chart/interactive/docs/gallery -- charts
         * https://developers.google.com/identity/sign-in/android/sign-in -- stuff for login
         * https://github.com/googlesamples/android-fit/blob/master/BasicHistoryApi/app/src/main/java/com/google/android/gms/fit/samples/basichistoryapi/MainActivity.java#L261 -- exapmle history
         */

        Calendar cl = Calendar.getInstance();
        cl.setTime(new Date());
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.add(Calendar.DAY_OF_MONTH,-6);
        Date start = cl.getTime();
        cl.setTime(new Date());
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.add(Calendar.DAY_OF_MONTH,1);
        Date end = cl.getTime();

        long endTime = end.getTime();

        long startTime = start.getTime();

        java.text.DateFormat dateFormat = getDateInstance();
        LOG( "Range Start: " + dateFormat.format(startTime));
        LOG( "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest =
                new DataReadRequest.Builder()
                        //.read(DataType.TYPE_ACTIVITY_SAMPLES)
                        .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();

        //DataReadRequest readRequest = WeeklyExerciseHistoryFragment.queryFitnessData(start,end,TimeUnit.DAYS);

        GoogleSignInAccount gsa = GoogleSignIn.getLastSignedInAccount(this);

        if (gsa != null) {
            Fitness.getHistoryClient(MainActivity.this, gsa)
                    .readData(readRequest)
                    .addOnSuccessListener(
                            new OnSuccessListener<DataReadResponse>() {
                                @Override
                                public void onSuccess(DataReadResponse dataReadResponse) {
                                    LOG("DATA RECEIVED | " + Integer.toString(dataReadResponse.getBuckets().size()) );
                                    for (Bucket bucket : dataReadResponse.getBuckets()) {
                                        for (DataSet dataSet : bucket.getDataSets()) {
                                            LOG("DATA SIZE : " + Integer.toString(dataSet.getDataPoints().size()));
                                            DateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
                                            for (DataPoint dp : dataSet.getDataPoints()) {
                                                Log.i(TAG, "Data point:");
                                                Log.i(TAG, "\tType: " + dp.getDataType().getName());
                                                Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                                                Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                                                for (Field field : dp.getDataType().getFields())
                                                    Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                                            }
                                        }
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

        /**
         * //TODO REMOVE ABOVE
         */

        //bind clients, they should always be bound during the application use
        mDataClientManager.bindClient();
        mSensorClient.bindClient();

        //determine if wearable is connected and write status to drawer banner
        Wearable.getNodeClient(this).getConnectedNodes().addOnCompleteListener(new OnCompleteListener<List<Node>>() {
            @Override
            public void onComplete(@NonNull Task<List<Node>> task) {
                if (task.getResult().size() > 0) {
                    String output = "Wearable Connected\n";
                    for (Node node : task.getResult()) {
                        output += node.getDisplayName() + "\n";
                    }
                    mNavTitle.setText(output);
                } else {
                    mNavTitle.setText("Wearable Not Connected");
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unbind clients when paused
        mSensorClient.unbindClient();
        mDataClientManager.unbindClient();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //open draw when home button is clicked in top left
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataChanged(DataMap map, String channel) {
        //nothing, needed for DataClientManager creation
    }

    @Override
    public void onSensorChanged(AbstractMultiSourceSensor.PackagedSensorEvent event) {
        //nothing, needed for sensor client creation
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        //for changes in time update the alarm manager
        if (s.equals(SettingsFragment.PREF_KEY_EXERCISE_START) ||
                s.equals(SettingsFragment.PREF_KEY_EXERCISE_END) ||
                s.equals(SettingsFragment.PREF_KEY_EXERCISE_DURATION) ||
                s.equals(SettingsFragment.PREF_KEY_EXERCISE_INTERVAL)) {

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            final int start = Integer.parseInt(sharedPreferences.getString(SettingsFragment.PREF_KEY_EXERCISE_START,"0"));
            final int end = Integer.parseInt(sharedPreferences.getString(SettingsFragment.PREF_KEY_EXERCISE_END,"0"));

            LOG("onSharedPref ... " + Integer.toString(start) + " , " + Integer.toString(end));

            //update exercise alarm based on new time
            final int duration = Integer.parseInt(sharedPreferences.getString(SettingsFragment.PREF_KEY_EXERCISE_DURATION,"0"));
            final int interval = Integer.parseInt(sharedPreferences.getString(SettingsFragment.PREF_KEY_EXERCISE_INTERVAL,"0"));

            calendar.add(Calendar.MINUTE,interval);

            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

            AlarmManagerService.setExerciseAlarm(this,minutes,duration);

        }
    }

    /*
     * Permissions Callback
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.i("Permissions","Body Sensor Permissions Granted!");

                } else {

                    Log.i("Permissions","Failed to obtain permissions for body sensors.");
                }
                return;
            }
        }
    }

}