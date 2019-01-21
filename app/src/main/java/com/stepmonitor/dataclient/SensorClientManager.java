package com.stepmonitor.dataclient;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.stepmonitor.dataclient.AbstractMultiSourceSensor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A Phone Based SensorManager and DataClient wrapper to provide abstraction
 * among phone and wearable based sensors
 */
public class SensorClientManager extends AbstractMultiSourceSensor {

    /**
     * to listener to which to pass local and wearable sensor events
     */
    private OnSensorChanged mListener;

    /**
     * Mode to decide sensors to use
     */
    public enum MODE {
        BEST(0x00),
        LOCAL(0x01),
        WEARABLE(0x02),
        ALL(0x03);

        private int mode;

        MODE(int mode) {
            this.mode = mode;
        }

    }

    private MODE mMode;

    /**
     * a class for callback on historical data requests
     */
    public interface OnHistoricalDataReceived {
        void onHistoricalDataReceived(DataMap data);
    }

    /**
     * Sensor Comparison Variables
     */

    //the result of a device comparison, for each sensor int true indicates
    //use local and false indicates use wearable
    private LinkedHashMap<Integer,Boolean> mDeviceComparison;
    //private var is true when a device comparison is occuring
    //allows sensor changes to be logged to mDeviceComparisonData
    private boolean mIsDeviceComparisonTest;
    //map for use during sensor comparison, each sensor int has array size 2, each index counting
    //wearable and phone occurences
    private LinkedHashMap<Integer,int[]> mDeviceComparisonData;

    /**
     *
     * @param context the program context
     * @param listener a listener for which to pass sensor events to
     */
    //TODO make listener optional
    public SensorClientManager(Context context, OnSensorChanged listener) {
        super(context);

        mListener = listener;
        mMode = MODE.ALL;
        mDataClientManager.addChannel(CHANNEL_SENSOR_DATA);

        //device comparison stuff
        mDeviceComparison = new LinkedHashMap<>();
        mIsDeviceComparisonTest = false;
        mDeviceComparisonData = new LinkedHashMap<>();
    }

    /**
     *
     * @param listener the new listener to pass sensor events to
     */
    public void setOnSensorChangedListener(OnSensorChanged listener) {
        Log.e("Changed Listener","Changed");
        mListener = listener;
    }

    /**
     * Calls appropriate parent method to activate local sensor and sends message to wearable to activate remotely
     * @param type a valid sensor int from Sensor
     */
    public void bindSensor(int type) {
        if (type == Sensor.TYPE_ALL) {
            super.bindAllSensors();
        } else {
            super.bindSensor(type);
        }

        DataMap map = new DataMap();

        map.putInt(TAG_TYPE,type);
        map.putBoolean(TAG_STATE,true);
        mDataClientManager.sendMessage(map,CHANNEL_SENSOR_STATUS);
    }

    public void unbindSensor(int type) {
        if (type == Sensor.TYPE_ALL) {
            super.unbindAllSensors();
        } else {
            super.unbindSensor(type);
        }

        DataMap map = new DataMap();

        map.putInt(TAG_TYPE,type);
        map.putBoolean(TAG_STATE,false);
        mDataClientManager.sendMessage(map,CHANNEL_SENSOR_STATUS);
    }

    public void bindAllSensors() {
        this.bindSensor(Sensor.TYPE_ALL);
    }

    public void unbindAllSensors() {
        this.unbindSensor(Sensor.TYPE_ALL);
    }

    /**
     * Use to use only the best sensors, must have done a comparison test first
     * @param use_best true if best sensor should be used, false otherwise
     */
    public void useBestDevice(boolean use_best) {
        if (use_best) {
            mMode = MODE.BEST;
        } else {
            mMode = MODE.ALL;
        }
    }

    /**
     * Use only phone sensors
     */
    public void usePhone() {
        mMode = MODE.LOCAL;
    }

    /**
     * Use only wearable sensors
     */
    public void useWearable() {
        mMode = MODE.WEARABLE;
    }

    /**
     * Determine mode being used
     */
    public MODE getMode() {
        return mMode;
    }

/*    *//**
     * Sends a request for historical data which will be returned on the callback onHistoricalDataReceived
     * @param sensor - the sensor to request from
     * @param callback - a callback to which to call the resulting data to when finished
     *//*
    public void sendHistoricalDataRequest(int sensor, final OnHistoricalDataReceived callback) {

        //push custom callback onto context which will callback and self destruct on finish
        //TODO: abstract this is DataClientManager, this could be considered leaky, even if it is android api level
        Wearable.getDataClient(mContext).addListener(new DataClient.OnDataChangedListener() {
            @Override
            public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        //process data to confirm that it is the one being requested
                        if (item.getUri().getPath().compareTo(CHANNEL_SENSOR_HISTORICAL) == 0) {

                            //obtain map and return result;
                            DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                            callback.onHistoricalDataReceived(map);

                            //we are done getting the data, remove the listener so contain resources
                            Wearable.getDataClient(mContext).removeListener(this);
                        }
                    }
                }
            }
        });

        //package the data and send on the local data client manager
        DataMap request = new DataMap();
        request.putInt(TAG_RECORD_TYPE,sensor);
        mDataClientManager.sendMessage(request,CHANNEL_SENSOR_UPDATE);

    }*/

    /**
     * Wearable Sensor Change
     *
     * callback from DataClientManager, packages and exports the data and logs it for
     * the comparison test if neccisary
     * @param map
     * @param channel
     */
    @Override
    public void onDataChanged(DataMap map, String channel) {
        //in the case that it is a real-time data unit, package and send to listener
        if (channel.equals(CHANNEL_SENSOR_DATA)) {

            if (map.getString(TAG_DATA_TYPE).equals(TYPE_SINGLE)) {
                PackagedSensorEvent event = openSensorEvent(map);
                event.is_local = false;
                exportSensorChange(event);
                if (mIsDeviceComparisonTest) {
                    if (!mDeviceComparisonData.containsKey(event.type)) {
                        mDeviceComparisonData.put(event.type, new int[2]);
                    }
                    mDeviceComparisonData.get(event.type)[0]++;
                }
            } else {
                Asset ser = map.getAsset(TAG_DATA);
                new deserializeSensorEvent().execute(ser);
            }

        }
    }

    /**
     * Local Sensor Change
     *
     * a local sensor event which is exported to the listener
     * the event is also logged for the comparison test as needed
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        PackagedSensorEvent packaged_event = convertSensorEvent(event);
        packaged_event.is_local = true;
        exportSensorChange(packaged_event);
        //if we are testing then increment device
        if (mIsDeviceComparisonTest) {
            if (!mDeviceComparisonData.containsKey(event.sensor.getType())) {
                mDeviceComparisonData.put(event.sensor.getType(),new int[2]);
            }
            mDeviceComparisonData.get(event.sensor.getType())[1]++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //TODO: maybe do something here later
    }

    /**
     * send event to callback, dependent on sensor performance for wearable against phone
     * @param event
     */
    protected void exportSensorChange(PackagedSensorEvent event) {

        //if we are using the best device make sure we have one determined
        if ((mDeviceComparison == null || !mDeviceComparison.containsKey(event.type)) && mMode == MODE.BEST) {
            throw new NullPointerException();
        //if both are same type or not using best device
        } else if (
                mMode == MODE.BEST && mDeviceComparison.get(event.type) == event.is_local ||
                mMode == MODE.LOCAL && event.is_local ||
                mMode == MODE.WEARABLE && !event.is_local ||
                mMode == MODE.ALL ) {
            if (mListener != null)
                mListener.onSensorChanged(event);
            else
                Log.e("SensorClient","Listener is null");
        }
    }

    /**
     * desearlize a listed of sensor events
     * @param
     * @param
     */
    public class deserializeSensorEvent extends AsyncTask<Asset, Integer, List<PackagedSensorEvent>> {

        @Override
        protected List<PackagedSensorEvent> doInBackground(Asset... assets) {

            ArrayList<PackagedSensorEvent> result = new ArrayList<>();

            try {

                //for each queued asset
                for (Asset asset : assets) {
                    InputStream assetInputStream =
                            Tasks.await(Wearable.getDataClient(mContext).getFdForAsset(asset))
                                    .getInputStream();


                    String ser = "";

                    //pull while data is available
                    while (assetInputStream.available() > 0) {
                        byte[] data = new byte[1];
                        assetInputStream.read(data,0,1);
                        ser += new String(data);
                    }

                    String[] records = ser.split("\\n");
                    for (String record : records) {
                        String[] items = record.split(",");

                        PackagedSensorEvent event = new PackagedSensorEvent();

                        event.accuracy = Integer.parseInt(items[0]);
                        event.timestamp = Long.parseLong(items[1]);
                        event.type = Integer.parseInt(items[2]);
                        event.name = items[3];
                        event.resolution = Float.parseFloat(items[4]);
                        event.max_delay = Integer.parseInt(items[5]);
                        event.min_delay = Integer.parseInt(items[6]);

                        int length = Integer.parseInt(items[7]);
                        event.values = new float[length];
                        for (int i = 0; i < length; i++) {
                            event.values[i] = Float.parseFloat(items[i + 8]);
                        }

                        result.add(event);

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<PackagedSensorEvent> packagedSensorEvents) {
            super.onPostExecute(packagedSensorEvents);

            for (PackagedSensorEvent event : packagedSensorEvents) {
                event.is_local = false;
                exportSensorChange(event);
                if (mIsDeviceComparisonTest) {
                    if (!mDeviceComparisonData.containsKey(event.type)) {
                        mDeviceComparisonData.put(event.type, new int[2]);
                    }
                    mDeviceComparisonData.get(event.type)[0]++;
                }
            }

        }
    }

    /**
     * Start a sensor comparison
     */
    public void beginSensorComparison() {
        mDeviceComparisonData.clear();
        mIsDeviceComparisonTest = true;
    }

    /**
     * End the sensor comparison and generate best sensor map
     */
    public void endSensorComparison() {
        mIsDeviceComparisonTest = false;
        mDeviceComparison.clear();
        for (LinkedHashMap.Entry<Integer, int[]> entry : mDeviceComparisonData.entrySet()) {
            mDeviceComparison.put(entry.getKey(),entry.getValue()[0] < entry.getValue()[1]);
        }
    }

    /**
     * manually select the device to use
     * @param sensor the int type of the sensor
     * @param is_local true if using phone sensor, false if otherwise
     */
    public void selectBestDevice(int sensor, boolean is_local) {
        mDeviceComparison.put(sensor,is_local);
    }

    /**
     * Determine if the best sensor between wearable and phone has been selected
     * @param type the int senor type
     * @return true if the best sensor has been determined
     */
    public boolean isBestDetermined(int type) {
        return (mDeviceComparison != null && mDeviceComparison.containsKey(type));
    }

    /**
     * The callback to pass events to
     */
    public interface OnSensorChanged {
        void onSensorChanged(PackagedSensorEvent event);
    }

}
