package com.stepmonitor.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.stepmonitor.R;

public class DistanceEditTextPreference extends DialogPreference {

    private float mDistance;

    /**
     * Resource of the dialog layout
     */
    private int mDialogLayoutResId = R.layout.pref_dialog_distance;

    public DistanceEditTextPreference(Context context) {
        // Delegate to other constructor
        super(context);

    }

    public DistanceEditTextPreference(Context context, AttributeSet attrs) {
        // Delegate to other constructor
        // Use the preferenceStyle as the default style
        super(context, attrs);

    }

    public DistanceEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        // Delegate to other constructor
        super(context, attrs, defStyleAttr);

    }

    public DistanceEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    public double getDistance() {
        return mDistance;
    }

    public void setDistance(float distance) {
        mDistance = distance;

        persistString(Double.toString(mDistance));
    }

    /**
     * Called when a Preference is being inflated and the default value attribute needs to be read
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // The type of this preference is Int, so we read the default value from the attributes
        // as Int. Fallback value is set to 0.
        return a.getString(index);
    }

    /**
     * Returns the layout resource that is used as the content View for the dialog
     */
    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    /**
     * Implement this to set the initial value of the Preference.
     */
    @Override
    protected void onSetInitialValue(Object defaultValue) {

        //TODO fix default value passed
        setDistance( Float.parseFloat( this.getPersistedString( "0" ) ) );
    }


}
