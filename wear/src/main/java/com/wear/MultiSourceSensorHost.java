package com.wear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;

import java.util.HashMap;

/**
 * Class for hosting a client for data from a wearable device
 * This acts like a slave to its client
 */
public class MultiSourceSensorHost extends AbstractMultiSourceSensor {

    /**
     * a listener for the purpose of sending sensor data to for the local sensor only
     */
    private OnSensorChanged mListener;

    /**
     *
     */
    private HashMap<Integer,SensorBuffer> mSensorBuffers;

    /**
     *
     * @param context the program context
     * @param listener the listener to send sensor changes to
     */
    MultiSourceSensorHost(Context context, OnSensorChanged listener) {
        super(context);

        mListener = listener;
        mDataClientManager.addChannel(CHANNEL_SENSOR_STATUS);
        mDataClientManager.addChannel(CHANNEL_SENSOR_UPDATE);
        mSensorBuffers = new HashMap<>();

        for (HashMap.Entry<Integer,Integer> entry : mBufferSizes.entrySet()) {
            if (entry.getValue() > 1)
                mSensorBuffers.put(entry.getKey(),new SensorBuffer(entry.getValue()));
        }

    }

    /**
     * set the sensor change listener
     * @param listener the listener
     */
    public void setOnSensorChangedListener(OnSensorChanged listener) {
        mListener = listener;
    }

    /**
     * DataClientManager callback, listens to sensor status and binds or unbinds the sensor passed
     * @param map
     * @param channel
     */
    @Override
    public void onDataChanged(DataMap map, String channel) {

        //toggle sensor on or off based on message
        if (channel.equals(CHANNEL_SENSOR_STATUS)) {

            int sensor = map.getInt(TAG_TYPE);
            boolean activate = map.getBoolean(TAG_STATE);

            if (activate) {
                bindSensor(sensor);
            } else {
                unbindSensor(sensor);
            }
        }
    }

    /**
     * On sensor change send data to the phone
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        //throw exception if the sensor type is not configured for
        if (!mBufferSizes.containsKey(event.sensor.getType()))
            throw new RuntimeException("Sensor is not configured");

        if (mBufferSizes.get(event.sensor.getType()) == 1) {
            //sensor is singular and does not buffer
            DataMap map = packageSensorEvent(event);
            mDataClientManager.sendMessage(map,CHANNEL_SENSOR_DATA);
        } else {
            //sensor is buffer, insert and if full send package
            SensorBuffer buffer = mSensorBuffers.get(event.sensor.getType());
            buffer.insert(event);

            //if buffer is full then send
            if (buffer.size() > mBufferSizes.get(event.sensor.getType())) {
                DataMap map = buffer.packageSensorEvents();
                buffer.clear();
                mDataClientManager.sendMessage(map,CHANNEL_SENSOR_DATA);
            }
        }

        //pass event to listener
        mListener.onSensorChanged(event);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //TODO: maybe implement accuracy change updates
    }

    public interface OnSensorChanged {
        void onSensorChanged(SensorEvent event);
    }

}
