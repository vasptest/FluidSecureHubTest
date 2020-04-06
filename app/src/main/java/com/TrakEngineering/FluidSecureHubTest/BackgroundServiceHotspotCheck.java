package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Display;


import com.TrakEngineering.FluidSecureHubTest.WifiHotspot.WifiApManager;
import com.TrakEngineering.FluidSecureHubTest.enity.StatusForUpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.enity.UserInfoEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatCodePointException;

import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.IsUpgradeInprogress_FS1;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.wifiApManager;
import static com.TrakEngineering.FluidSecureHubTest.server.ServerHandler.TEXT;


/**
 * Created by User on 11/8/2017.
 */

public class BackgroundServiceHotspotCheck extends BackgroundService {

    private String TAG = "BS_HotspotCheck";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        SharedPreferences sharedPrefOAS = BackgroundServiceHotspotCheck.this.getSharedPreferences(AppConstants.sharedPref_OfflineAzureSync, Context.MODE_PRIVATE);
        String datetimeOffline = sharedPrefOAS.getString("datetime", "");
        String dtOfflineFormat = "yyyy-MM-dd HH:mm";

        if (datetimeOffline.trim().isEmpty()) {
            SharedPreferences pref = BackgroundServiceHotspotCheck.this.getSharedPreferences(AppConstants.sharedPref_OfflineAzureSync, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("datetime", AppConstants.currentDateFormat(dtOfflineFormat));
            editor.commit();

        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(dtOfflineFormat);
                Date savedDT = sdf.parse(datetimeOffline);
                Date currentDT = new Date();

                long diff = currentDT.getTime() - savedDT.getTime();
                int numOfDays = (int) (diff / (1000 * 60 * 60 * 24));
                int hours = (int) (diff / (1000 * 60 * 60));
                int minutes = (int) (diff / (1000 * 60));
                int seconds = (int) (diff / (1000));


                if (minutes >= 15) {
                    SharedPreferences pref = BackgroundServiceHotspotCheck.this.getSharedPreferences(AppConstants.sharedPref_OfflineAzureSync, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("datetime", AppConstants.currentDateFormat(dtOfflineFormat));
                    editor.commit();

                    startService(new Intent(this, OffTranzSyncService.class));

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        try {
            super.onStart(intent, startId);
            Bundle extras = intent.getExtras();
            boolean screenOff = intent.getBooleanExtra("screen_state", true);
            if (extras == null) {
                Log.d(TAG, "null");
                this.stopSelf();
            } else {

                //AT&T hotspot issue.
                if (LinkBusyStatus() && AppConstants.RefreshHotspot) {
                    ToggleHotspotATAndT();
                }

                //Reset hotspot_enable_sureMDM Flag
                if (CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this)) {
                    AppConstants.COUNT_HOTSPOT_SMDM = 0;
                    System.out.println("HOT SPOT--ENAbled");
                } else {
                    AppConstants.COUNT_HOTSPOT_SMDM++;
                    System.out.println("HOT SPOT--no enabled");
                }

                System.out.println("HOT SPOT--COUNT: " + AppConstants.COUNT_HOTSPOT_SMDM);

                if (AppConstants.COUNT_HOTSPOT_SMDM > 14) {
                    AppConstants.COUNT_HOTSPOT_SMDM = 0;

                    //sendSureMDMRequestForHotspotpresses(); //not working at svens

                    if (!CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this) && Constants.hotspotstayOn && AppConstants.enableHotspotManuallyWindow){

                        AppConstants.enableHotspotManuallyWindow = false;
                        Log.i(TAG, "EMobileHotspotManually");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "EMobileHotspotManually");
                        CommonUtils.enableMobileHotspotmanuallyStartTimer(this);
                    }
                }


                //Enable hotspot Logic
                if (AppConstants.FlickeringScreenOff) {

                    Log.i(TAG, "Dont do anything Screen off to overcome Flickering issue");
                    AppConstants.FlickeringScreenOff = false; //Do not disable hotspot

                } else if (!screenOff && !CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this) && Constants.hotspotstayOn) {

                    wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
                    Log.i(TAG, "Connecting to hotspot, please wait....");
                    //if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  Hotspot ON--1");

                } else if (screenOff) {

                    if (isScreenOn(this) && !CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this) && Constants.hotspotstayOn) {

                        wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
                        Log.i(TAG, "Connecting to hotspot, please wait....");

                    } else {
                        Log.i(TAG, "Dont do anything");
                    }
                }

            }

        } catch (NullPointerException e) {
            System.out.println(e);
        }
        return Service.START_STICKY;
    }

    public void ToggleHotspotATAndT() {

        if (AppConstants.HOTSPOT_TOGGLE_10MININ >= AppConstants.HotspotRefreshTime) { //

            System.out.println("HOTSPOT_TOGGLE_10MININ Toggle:" + AppConstants.HOTSPOT_TOGGLE_10MININ);
            wifiApManager.setWifiApEnabled(null, false);  //Hotspot Disabled


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                  wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " ToggleHotspotATAndT Hotspot enabled");
                }
            },2000);

            AppConstants.HOTSPOT_TOGGLE_10MININ = 0;
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HOTSPOT_TOGGLE AT&T tablet");

        } else if (AppConstants.HOTSPOT_TOGGLE_10MININ == 0) {

            System.out.println("HOTSPOT_TOGGLE_10MININ Recovery:" + AppConstants.HOTSPOT_TOGGLE_10MININ);
            AppConstants.HOTSPOT_TOGGLE_10MININ++;


        } else {

            System.out.println("HOTSPOT_TOGGLE_10MININ Idle:" + AppConstants.HOTSPOT_TOGGLE_10MININ);
            AppConstants.HOTSPOT_TOGGLE_10MININ++;

        }

    }

    private boolean LinkBusyStatus() {

        boolean status = true;

        if (Constants.FS_1STATUS.equalsIgnoreCase("BUSY")) {

            status = false;

        } else if (Constants.FS_2STATUS.equalsIgnoreCase("BUSY")) {

            status = false;

        } else if (Constants.FS_3STATUS.equalsIgnoreCase("BUSY")) {

            status = false;

        } else if (Constants.FS_4STATUS.equalsIgnoreCase("BUSY")) {

            status = false;

        }

        return status;
    }

    public boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            return pm.isScreenOn();
        }
    }

    public void sendSureMDMRequestForHotspotpresses() {

        UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetailsCC(BackgroundServiceHotspotCheck.this);

        StatusForUpgradeVersionEntity objEntityClass2 = new StatusForUpgradeVersionEntity();
        objEntityClass2.IMEIUDID = AppConstants.getIMEI(BackgroundServiceHotspotCheck.this);
        objEntityClass2.HubName = userInfoEntity.PersonName;
        objEntityClass2.SiteName = userInfoEntity.FluidSecureSiteName;

        Gson gson = new Gson();
        String parm2 = gson.toJson(objEntityClass2);

        String userEmail = CommonUtils.getCustomerDetailsCC(BackgroundServiceHotspotCheck.this).PersonEmail;
        //----------------------------------------------------------------------------------
        String parm1 = AppConstants.getIMEI(BackgroundServiceHotspotCheck.this) + ":" + userEmail + ":" + "sendSureMDMRequestForHotspotpresses";
        String authString = "Basic " + AppConstants.convertStingToBase64(parm1);


        RequestBody body = RequestBody.create(TEXT, parm2);
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(AppConstants.webURL)
                .post(body)
                .addHeader("Authorization", authString)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response");
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " MDMRequest Hotspot error in getting response");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {

                    String result = responseBody.string();
                    Log.e(TAG, "HOTSPOT-" + result);
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " sendSureMDMRequestForHotspotpresses ~Result\n" + result);

                    try {

                        JSONObject jsonObjectSite = null;
                        jsonObjectSite = new JSONObject(result);

                        String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

        });
    }
}