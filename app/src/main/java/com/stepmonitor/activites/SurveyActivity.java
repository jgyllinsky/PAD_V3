package com.stepmonitor.activites;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.stepmonitor.R;
import com.stepmonitor.system.DataManager;

import org.json.JSONObject;

import java.util.Date;

/**
 * This activity provides a way to get user input and output a formatted json file containing
 * the submissions to the survey
 */
public class SurveyActivity extends Activity implements View.OnClickListener {

    LinearLayout mLinearLayoutSurvey;
    Button mButtonSubmitSurvey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        mButtonSubmitSurvey = findViewById(R.id.button_submit_survey);
        mLinearLayoutSurvey = findViewById(R.id.linear_layout_survey);

        //on click submit the survey
        mButtonSubmitSurvey.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        int count = mLinearLayoutSurvey.getChildCount();
        String key = "";
        JSONObject surveySubmit = new JSONObject();

        //collect results into json object
        for (int i = 0; i < count; i++) {
            View view = mLinearLayoutSurvey.getChildAt(i);

                try {
                    //go through each type of survey element type to find propper formatting
                    if ( view instanceof EditText) {
                        surveySubmit.put(key,( (EditText) view ).getText().toString());
                    }
                    else if ( view instanceof Switch) {
                        surveySubmit.put(key,Boolean.toString(( (Switch)view ).isChecked()) );
                    }
                    else if ( view instanceof RadioGroup) {
                        for (int j = 0; j < ((RadioGroup)view).getChildCount(); j++) {
                            if ( ((RadioButton)(((RadioGroup)view).getChildAt(j))).isChecked() ) {
                                surveySubmit.put(key,Integer.toString(j));
                            }
                        }
                    }
                    else if (view instanceof TextView) {
                        //we are at the question title so we save this to use as the key
                        key = ((TextView) view).getText().toString();
                    }
                } catch (Exception e) {
                    Log.e("Error","Failed Json");
                    e.printStackTrace();
                }

        }

        //write result to local file
        DataManager.writeToSystem("/survey/walking",(new Date()).toString()+".json",surveySubmit.toString(),false);

        //close the activity
        finish();

    }

}
