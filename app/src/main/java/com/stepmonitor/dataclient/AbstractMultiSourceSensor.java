package com.stepmonitor.dataclient;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * An abstract class containing data client channels and tags as well as data parsing functions and data client managment
 * TODO merge this with SensorClientManager
 */
abstract public class AbstractMultiSourceSensor implements
        DataClientManager.OnDataChangedListener,
        SensorEventListener {

    /**
     * Channels for sensors
     * sensor_data : channel for wearable -> phone to send data
     * sensor_historical : channel for wearable -> phone to send a large data packet of a specific sensor
     * sensor_status : channel for phone -> wearable to turn on specific sensors or all sensor
     * sensor_update : channel for phone -> wearable to force clear wearable history and update on phone
     */
    protected static final String CHANNEL_SENSOR_DATA = "/sensor_data";
    protected static final String CHANNEL_SENSOR_STATUS = "/sensor_status";

    /**
     * Tags for sensor event data
     * these are specifically for datamaps moving from wearable -> device for sensor_data
     */
    protected static final String TAG_DATA_TYPE = "data_type";
    protected static final String TYPE_SINGLE = "single";

    protected static final String TAG_ACC = "accuracy";
    protected static final String TAG_TIME = "timestamp";
    protected static final String TAG_VALUES = "values";
    protected static final String TAG_TYPE = "type";
    protected static final String TAG_NAME = "name";
    protected static final String TAG_RES = "resolution";
    protected static final String TAG_MAX_DELAY = "max_delay";
    protected static final String TAG_MIN_DELAY = "max_delay";

    //serialized data tags
    protected static final String TAG_DATA = "serial";

    /**
     * tag for datamaps moving from device -> wearable for sensor_status
     */
    protected static final String TAG_STATE = "state";

    /**
     * Context passed for DatsClient and SensorManager binding
     */
    protected Context mContext;

    /**
     * Local Sensor Manager
     */
    protected SensorManager mSensorManager;

    /**
     * Data Client Manager for sending and recieving data
     */
    protected DataClientManager mDataClientManager;

    /**
     * List of sensors found on the local SensorManager
     */
    protected List<Sensor> mSensors;

    /**
     * A modfiied version of SensorEvent to easily transfer across DataClient
     */
    public static class PackagedSensorEvent {

        public int accuracy;
        public long timestamp;
        public float[] values;

        public int type;
        public String name;
        public float resolution;
        public int max_delay;
        public int min_delay;

        public boolean is_local;

    }

    /**
     * Constructor
     * @param context - a context which can be used to get system services for sensors
     */
    AbstractMultiSourceSensor(Context context) {

        mContext = context;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mDataClientManager = new DataClientManager(mContext, this);
        mSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

    }

    /**
     * Bind he internal DataClient
     */
    public void bindClient() {
        mDataClientManager.bindClient();
    }

    /**
     * Unbind the internal DataClient (remove listener)
     */
    public void unbindClient() {
        mDataClientManager.unbindClient();
    }

    /**
     * Manage Sensors across local device and wearable
     */

    /**
     * Bind a sensor with the given integer type, if Sensor.TYPE_ALL then all possible sensors are enabled
     * @param type a valid sensor int from Sensor
     */
    public void bindSensor(int type) {
        if (type == Sensor.TYPE_ALL) {
            bindAllSensors();
        } else if (mSensors.contains(mSensorManager.getDefaultSensor(type))) {
            Sensor sensor = mSensorManager.getDefaultSensor(type);
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Unbind a sensor with the given integer type, if Sensor.TYPE_ALL then all possible sensors are disabled
     * @param type a valid sensor int from Sensor
     */
    public void unbindSensor(int type) {
        if (type == Sensor.TYPE_ALL) {
            unbindAllSensors();
        } else if (mSensors.contains(mSensorManager.getDefaultSensor(type))) {
            Sensor sensor = mSensorManager.getDefaultSensor(type);
            mSensorManager.unregisterListener(this, sensor);
        }
    }

    /**
     * Bind all sensors on device ( those which were found from getSensorList
     */
    public void bindAllSensors() {
        for (Sensor sensor : mSensors) {
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    /**
     * Unbinds all sensors, this is valid event if all sensors are not bound
     */
    public void unbindAllSensors() {
        for (Sensor sensor : mSensors) {
            mSensorManager.unregisterListener(this, sensor);
        }
    }

    /**
     * open a DataMap formatted by packageSensorEvent
     * @param map the map to open
     * @return the PackagedSensorEvent containing maps data
     */
    public PackagedSensorEvent openSensorEvent(DataMap map) {
        PackagedSensorEvent event = new PackagedSensorEvent();

        event.accuracy = map.getInt(TAG_ACC);
        event.timestamp = map.getLong(TAG_TIME);
        event.values = map.getFloatArray(TAG_VALUES);

        event.type = map.getInt(TAG_TYPE);
        event.name = map.getString(TAG_NAME);
        event.resolution = map.getFloat(TAG_RES);
        event.max_delay = map.getInt(TAG_MAX_DELAY);
        event.min_delay = map.getInt(TAG_MIN_DELAY);

        return event;
    }

    /**
     * Convert a event to PackagedSensorEvent
     * @param event - event from a onSensorChanged callback
     * @return PackagedSensorEvent data pulled from event
     */
    public PackagedSensorEvent convertSensorEvent(SensorEvent event) {
        PackagedSensorEvent new_event = new PackagedSensorEvent();

        new_event.accuracy = event.accuracy;
        new_event.timestamp = event.timestamp;
        new_event.values = event.values;

        new_event.type = event.sensor.getType();
        new_event.name = event.sensor.getName();
        new_event.resolution = event.sensor.getResolution();
        new_event.max_delay = event.sensor.getMaxDelay();
        new_event.min_delay = event.sensor.getMinDelay();

        return new_event;
    }

    @Override
    abstract public void onDataChanged(DataMap map, String channel);

    @Override
    abstract public void onSensorChanged(SensorEvent event);

    @Override
    abstract public void onAccuracyChanged(Sensor sensor, int accuracy);

}