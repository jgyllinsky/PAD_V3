package com.wear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * An abstract class containing data client channels and tags as well as data parsing functions and data client managment
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
    protected static final String CHANNEL_SENSOR_HISTORICAL = "/sensor_historical";
    protected static final String CHANNEL_SENSOR_STATUS = "/sensor_status";
    protected static final String CHANNEL_SENSOR_UPDATE = "/sensor_update";

    /**
     * Tags for sensor event data
     * these are specifically for datamaps moving from wearable -> device for sensor_data
     */
    protected static final String TAG_DATA_TYPE = "data_type";
    protected static final String TYPE_SINGLE = "single";
    protected static final String TYPE_SERIAL = "serial";

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
     * List of constrains for each sensor for its buffer size
     */
    public static HashMap<Integer,Integer> mBufferSizes;

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
     * A class representing a circular data buffer to store rapidly generated wearable sensor data
     */
    public static class SensorBuffer {

        private SensorEvent[] mEvents;
        private int mSize;
        private int mCapacity;

        SensorBuffer(int size) {
            mEvents = new SensorEvent[size];
            mSize = 0;
            mCapacity = size;
        }

        public void insert(SensorEvent event) {
            if (mSize % 10 == 0)
                Log.e("Insert",Integer.toString(mSize));

            mEvents[mSize % mCapacity] = event;
            mSize++;
        }

        public DataMap packageSensorEvents() {
            Asset ser = searlizeSensorEvent(Arrays.asList(mEvents));

            DataMap map = new DataMap();
            map.putString(TAG_DATA_TYPE,TYPE_SERIAL);
            map.putAsset(TAG_DATA,ser);

            return map;
        }

        public void clear() {
            mSize = 0;
        }

        public int size() {
            return mSize;
        }

        /**
         * convert fetch results to csv type string array lists
         * @param map - the map formatted by fetch or fetchAll
         */
/*        public static List<String[]> toList(DataMap map) {

            List<String[]> result = new ArrayList<>();

            ArrayList<String> timestamps = map.getStringArrayList(TAG_TIMES);

            for (String timestamp : timestamps) {
                float[] values = map.getFloatArray(timestamp);

                ArrayList<String> row = new ArrayList<>();
                row.add(timestamp);

                for (float value : values) {
                    row.add(Float.toString(value));
                }

                result.add(row.toArray(new String[row.size()]));
            }

            return result;
        }*/

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

        /**
         * Setup sensor buffer configuration
         */
        mBufferSizes = new HashMap<>();

        mBufferSizes.put(Sensor.TYPE_ACCELEROMETER,1024);
        mBufferSizes.put(Sensor.TYPE_STEP_DETECTOR,1);
        mBufferSizes.put(Sensor.TYPE_STEP_COUNTER,1);

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
     * Convert a SensorEvent from onSensorChanged to a DataMap for use in a DataClient
     * @param event the event to convert into a DataMap
     * @return the DataMap containing the contents of event for which are present in PackagedDataEvent
     */
    public static DataMap packageSensorEvent(SensorEvent event) {

        DataMap map = new DataMap();

        Sensor sensor = event.sensor;

        map.putInt(TAG_ACC, event.accuracy);
        map.putLong(TAG_TIME, event.timestamp);
        map.putFloatArray(TAG_VALUES, event.values);

        map.putInt(TAG_TYPE, sensor.getType());
        map.putString(TAG_NAME, sensor.getName());
        map.putFloat(TAG_RES, sensor.getResolution());
        map.putInt(TAG_MAX_DELAY, sensor.getMaxDelay());
        map.putInt(TAG_MIN_DELAY, sensor.getMinDelay());

        map.putString(TAG_DATA_TYPE,TYPE_SINGLE);

        return map;
    }

    /**
     * Take a list of sensor events and searialize them
     * @param events to searlize
     */
    public static Asset searlizeSensorEvent(List<SensorEvent> events) {

        String output = "";

        for (SensorEvent event : events) {

            String row = "";

            row += Integer.toString(event.accuracy) + ",";
            row += Long.toString(event.timestamp) + ",";
            row += Integer.toString(event.sensor.getType()) + ",";
            row += event.sensor.getName() + ",";
            row += Float.toString(event.sensor.getResolution()) + ",";
            row += Integer.toString(event.sensor.getMaxDelay()) + ",";
            row += Integer.toString(event.sensor.getMinDelay()) + ",";

            row += Integer.toString(event.values.length) + ",";
            for( float value : event.values ) {
                row += Float.toString(value) + ",";
            }

            output += row.substring(0,row.length() - 1) + "\n";

        }

        return Asset.createFromBytes(output.getBytes());
    }

    /**
     * desearlize a listed of sensor events
     * @param
     * @param
     */
    public static List<PackagedSensorEvent> deserializeSensorEvent(Asset data) {
        ArrayList<PackagedSensorEvent> result = new ArrayList<>();

        String ser = new String(data.getData());
        String[] records = ser.split("\\n");
        for (String record : records) {
            String[] items = record.split(",");

            PackagedSensorEvent event = new PackagedSensorEvent();

            event.accuracy = Integer.parseInt(items[0]);
            event.timestamp = Long.parseLong(items[1]);
            event.type = Integer.parseInt(items[3]);
            event.name = items[4];
            event.resolution = Float.parseFloat(items[5]);
            event.max_delay = Integer.parseInt(items[6]);
            event.min_delay = Integer.parseInt(items[7]);

            int length = Integer.parseInt(items[8]);
            event.values = new float[length];
            for (int i = 0; i < items.length; i++) {
                event.values[i] = Float.parseFloat(items[i + 9]);
            }

        }

        return result;
    }

    @Override
    abstract public void onDataChanged(DataMap map, String channel);

    @Override
    abstract public void onSensorChanged(SensorEvent event);

    @Override
    abstract public void onAccuracyChanged(Sensor sensor, int accuracy);

}