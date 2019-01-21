package com.wear;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

/**
 * DataClient Wrapper to filter channels and easily send data
 */
public class DataClientManager implements
        DataClient.OnDataChangedListener {

    /**
     * A Context to get the DataClient from
     */
    private Context mContext;

    /**
     * A listener to pass DataClient information to
     */
    private OnDataChangedListener mListener;

    /**
     * A list of filters for which data is passed to from the DataClient if its channel is present
     */
    private ArrayList<String> mChannels;

    /**
     *
     * @param context A context to create the DataClient From
     * @param listener A listener to pass DataClient information to
     */
    DataClientManager(Context context,OnDataChangedListener listener) {
        mContext = context;
        mListener = listener;
        mChannels = new ArrayList<>();
    }

    /**
     * Add this to DataClient Listener
     */
    public void bindClient() {
        Wearable.getDataClient(mContext).addListener(this);
    }

    /**
     * Remove this from DataClient Listener
     */
    public void unbindClient() {
        Wearable.getDataClient(mContext).removeListener(this);
    }

    /**
     * adds a channel for which onDataChanged will be called if it is found in the DataClient onDataChanged
     * @param channel the channels to listen to
     * @return true if added, false if already in the list
     */
    public boolean addChannel(String channel) {
        if (mChannels.contains(channel)) {
            return false;
        } else {
            mChannels.add(channel);
            return true;
        }
    }

    /**
     * Remove a channel
     * @param channel the channel to remove
     * @return true if remove, false if not found
     */
    public boolean removeChannel(String channel) {
        if (mChannels.contains(channel)) {
            mChannels.remove(channel);
            return true;
        } else {
            return false;
        }
    }

    /**
     * the actual DataClient callback for which events are passed to
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();

                //go through channels and compare event, if found then update data to listener
                for (String channel : mChannels) {
                    if (item.getUri().getPath().compareTo(channel) == 0) {
                        Log.i("DataClient","Received data on channel" + channel);
                        DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                        mListener.onDataChanged(map,channel);
                    }
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }

        }
    }

    /**
     * Send a datamap to the given channel
     * @param message the DataMap to send
     * @param channel the channel to which to send it to
     */
    public void sendMessage(DataMap message, final String channel) {
        sendMessage( message,channel,
                new OnCompleteListener<DataItem>() {
                    @Override
                    public void onComplete(@NonNull Task<DataItem> task) {
                        Log.i("Send Success","Successfully sent a message through DataClientManager : " + channel );
                    }
                },
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Send Failure","Failed to send a message through DataClientManager : " + e.getMessage());
                    }
                });
    }

    /**
     * Send a datamap to a given channel with the passed callbacks
     * @param message
     * @param channel
     * @param onSuccessListener
     * @param onFailureListener
     */
    public int sendMessage(DataMap message,
                           final String channel,
                           OnCompleteListener<DataItem> onSuccessListener,
                           OnFailureListener onFailureListener) {

        int ID = (int)(Integer.MAX_VALUE * Math.random());
        message.putInt("ID",ID);

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(channel);
        putDataMapReq.getDataMap().putAll(message);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Task<DataItem> putDataTask = Wearable.getDataClient(mContext).putDataItem(putDataReq);

        putDataTask.addOnCompleteListener(onSuccessListener);
        putDataTask.addOnFailureListener(onFailureListener);

        return ID;
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        mListener = listener;
    }

    public interface OnDataChangedListener {
        void onDataChanged(DataMap map, String channel);
    }

}
