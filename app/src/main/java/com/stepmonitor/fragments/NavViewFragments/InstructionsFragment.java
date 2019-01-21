package com.stepmonitor.fragments.NavViewFragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stepmonitor.R;

public class InstructionsFragment extends Fragment {

    public InstructionsFragment() {
        // Required empty public constructor
    }

    public static InstructionsFragment newInstance() {
        InstructionsFragment fragment = new InstructionsFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_instructions, container, false);
    }

}
