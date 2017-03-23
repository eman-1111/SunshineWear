package com.example.android.sunshine;

import android.content.Intent;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Eman on 3/20/2017.
 */

public class SunshineWearListenerService extends WearableListenerService {

    private static final String IMAGE_KEY = "photo";
    private static final String HIGH_KEY = "high_temp";
    private static final String LOW_KEY = "low_temp";
    private static final String START_ACTIVITY_PATH = "/new-weather";
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent dataEvent : dataEventBuffer){
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED){
                String path = dataEvent.getDataItem().getUri().getPath();
                if(path.equals(START_ACTIVITY_PATH)){
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                    DataMap dataMap = dataMapItem.getDataMap();
                    String highTemp = dataMap.getString(HIGH_KEY);
                    String lowTemp = dataMap.getString(LOW_KEY);
                    int imageID    = dataMap.getInt(IMAGE_KEY);
                    sendWeatherData(highTemp,lowTemp,imageID);
                }
            }
        }

    }

    private void sendWeatherData(String highTemp, String lowTemp, int imageID){
        Intent weatherIntent = new Intent("ACTION_WEATHER_CHANGED");
        weatherIntent.putExtra(HIGH_KEY, highTemp)
                .putExtra(LOW_KEY, lowTemp)
                .putExtra(IMAGE_KEY, imageID);
        sendBroadcast(weatherIntent);
    }

}