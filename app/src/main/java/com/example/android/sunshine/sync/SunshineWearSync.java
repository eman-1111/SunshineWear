package com.example.android.sunshine.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.MainActivity;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Eman on 3/20/2017.
 */

public class SunshineWearSync implements GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private final int INDEX_WEATHER_MAX_TEMP = 0;
    private final int INDEX_WEATHER_MIN_TEMP = 1;
    private final int INDEX_WEATHER_CONDITION_ID = 2;


    private final int TODAY_WEATHER = 0;
    private static final String IMAGE_KEY = "photo";
    private static final String HIGH_KEY = "high_temp";
    private static final String LOW_KEY = "low_temp";
    private static final String START_ACTIVITY_PATH = "/new-weather";

    private boolean mResolvingError = false;

    //Request code for launching the Intent to resolve Google Play services errors.
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private final String TAG = SunshineWearSync.class.getSimpleName();

    public SunshineWearSync(Context context) {
        this.mContext = context;
    }

    public void initializeGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mResolvingError = false;
        getWeatherData();
    }

    private void getWeatherData() {
        ContentResolver resolver = mContext.getContentResolver();
        String[] columns = {WeatherContract.WeatherEntry.COLUMN_MAX_TEMP
                , WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                , WeatherContract.WeatherEntry.COLUMN_WEATHER_ID};
        Cursor cursor = resolver.query(WeatherContract.WeatherEntry.CONTENT_URI
                , columns
                , null
                , null
                , null);
        getWearData(cursor);
    }

    private void getWearData(Cursor cursor2) {
        if (cursor2 != null) {
            if (cursor2.moveToPosition(TODAY_WEATHER)) {
                int weatherId = cursor2.getInt(MainActivity.INDEX_WEATHER_CONDITION_ID);
                int weatherImageId = SunshineWeatherUtils
                        .getSmallArtResourceIdForWeatherCondition(weatherId);

                double highInCelsius = cursor2.getDouble(MainActivity.INDEX_WEATHER_MAX_TEMP);
                String highString = SunshineWeatherUtils.formatTemperature(mContext, highInCelsius);

                double lowInCelsius = cursor2.getDouble(MainActivity.INDEX_WEATHER_MIN_TEMP);
                String lowString = SunshineWeatherUtils.formatTemperature(mContext, lowInCelsius);

                sendData(weatherImageId, highString, lowString);
            }
            cursor2.close();
        }
    }


    private void sendData(int imageId, String highT, String lowT) {
        PutDataMapRequest dataMapImage = PutDataMapRequest.create(START_ACTIVITY_PATH);

        dataMapImage.getDataMap().putInt(IMAGE_KEY, imageId);
        dataMapImage.getDataMap().putString(HIGH_KEY, highT);
        dataMapImage.getDataMap().putString(LOW_KEY, lowT);
//        dataMapImage.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest requestData = dataMapImage.asPutDataRequest();
        requestData.setUrgent();

        Wearable.DataApi.putDataItem(mGoogleApiClient, requestData)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        LOGD(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        LOGD(TAG, "Connection to Google API client was suspended");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!mResolvingError) {

//            if (connectionResult.hasResolution()) {
//                try {
//                    mResolvingError = true;
//                    connectionResult.startResolutionForResult(mContext, REQUEST_RESOLVE_ERROR);
//                } catch (IntentSender.SendIntentException e) {
//                    // There was an error with the resolution intent. Try again.
//                    mGoogleApiClient.connect();
//                }
//            } else {
            LOGD(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            // Wearable.DataApi.removeListener(mGoogleApiClient, mContext);
            //   }
        }
    }

    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        LOGD(TAG, "onDataChanged: " + dataEventBuffer);

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                //todo do something when data change
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                //todo
            }
        }

    }


    private Bitmap getBitmap(int drawableRes) {
        Drawable drawable = mContext.getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}