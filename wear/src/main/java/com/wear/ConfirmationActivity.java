package com.wear;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.wear.R;

import java.util.function.Function;

/**
 * This phone launched activity provides the user a way to agree to disagree to the message given
 * the response is sent back to the phone via CHANNEL_REQUEST_RESPONSE
 */
public class ConfirmationActivity extends BackgroundService.ServiceLaunchedActivity implements
        DataClientManager.OnDataChangedListener {

    /**
     * channel used to send data back to phone
     */
    public static String CHANNEL_REQUEST_RESPONSE = "/exercise_request_response";

    /**
     * Dataclient tags for sending data back to phone
     */
    public static String TAG_RESPONSE = "response";     //boolean

    /**
     * Dataclient tags for activity creation
     */
    public static String TAG_INTENT_TITLE = "title";       //string
    public static String TAG_INTENT_REQYES = "yes";        //string
    public static String TAG_INTENT_REQNO = "no";          //string

    /**
     * Views
     */
    private TextView mTextViewTitle;
    private Button mButtonYes;
    private Button mButtonNo;

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conformation);

        mTextViewTitle = findViewById(R.id.confirmation_text);
        mButtonYes = findViewById(R.id.confirmation_yes);
        mButtonNo = findViewById(R.id.confirmation_no);

    }

    @Override
    protected void onResume() {

        Bundle map = getIntent().getBundleExtra(BackgroundService.EXTRA_DATA);

        mTextViewTitle.setText(map.getString(TAG_INTENT_TITLE));
        mButtonYes.setText(map.getString(TAG_INTENT_REQYES));
        mButtonNo.setText(map.getString(TAG_INTENT_REQNO));

        mButtonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataMap result = new DataMap();
                result.putBoolean(TAG_RESPONSE,true);
                mDataClientManager.sendMessage(result,CHANNEL_REQUEST_RESPONSE);
            }
        });

        mButtonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataMap result = new DataMap();
                result.putBoolean(TAG_RESPONSE,false);
                mDataClientManager.sendMessage(result,CHANNEL_REQUEST_RESPONSE);
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
