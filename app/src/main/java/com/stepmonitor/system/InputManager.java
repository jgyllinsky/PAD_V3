package com.stepmonitor.system;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class InputManager {

    /**
     * Hide the keyboard within a fragment
     * https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
     * @param context
     * @param view
     */
    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
