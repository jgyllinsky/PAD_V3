package com.wear;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

/**
 * This phone launched activity allows the user to read a short information string and click the continue button when done
 */
public class ContinueActivity extends BackgroundService.ServiceLaunchedActivity implements
        DataClientManager.OnDataChangedListener {

    /**
     * Channel used to communicate with the phone based activity
     */
    public static String CHANNEL_REQUEST_CONTINUE = "/exercise_request_continue";

    /**
     * Dataclient tags for activity creation
     */
    public static String TAG_INTENT_TITLE = "title";             //string
    public static String TAG_INTENT_REQCONT = "continue";        //string

    /**
     * Views
     */
    private TextView mTextViewTitle;
    private Button mButtonContinue;

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continue);

        mTextViewTitle = findViewById(R.id.continue_text);
        mButtonContinue = findViewById(R.id.continue_continue);

    }


    @Override
    protected void onResume() {

        Bundle map = getIntent().getBundleExtra(BackgroundService.EXTRA_DATA);

        mTextViewTitle.setText(map.getString(TAG_INTENT_TITLE));
        mButtonContinue.setText(map.getString(TAG_INTENT_REQCONT));

        mButtonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //we only need to send an empty data map
                DataMap result = new DataMap();
                mDataClientManager.sendMessage(result, CHANNEL_REQUEST_CONTINUE);
            }
        });

        super.onResume();

    }

    @Override
    public void onDataChanged(DataMap map, String channel) {

        //do nothing since no data needed after creation
        super.onDataChanged(map,channel);
    }

}
