package com.TrakEngineering.FluidSecureHubTest.WifiHotspot;


/**
 * Created by VASP on 9/1/2017.
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.R;
import com.TrakEngineering.FluidSecureHubTest.WelcomeActivity;

import java.lang.reflect.Method;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;


public class WifiApManager {
    private final WifiManager mWifiManager;
    private Context context;
    private static String TAG = "WifiApManager ";

    public static String TabsDefaultHubspotName;
    public static String TabsDefaultHubspotPassword;

    public static WifiManager.LocalOnlyHotspotReservation mReservation;

    public WifiApManager(Context context) {
        this.context = context;
        mWifiManager = (WifiManager) this.context.getSystemService(WIFI_SERVICE);
    }


    public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        try {


            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                if (enabled) {
                    //on
                    //turnOnHotspot();
                    Intent intent = new Intent("com.TrakEngineering.FluidSecureHubTest.fitc.wifihotspot.TURN_ON");
                    sendImplicitBroadcast(context, intent);

                } else {
                    //off
                    //turnOffHotspot();
                    Intent intent = new Intent("com.TrakEngineering.FluidSecureHubTest.fitc.wifihotspot.TURN_OFF");
                    sendImplicitBroadcast(context, intent);
                }


                return false;
            } else {
                if (enabled) { // disable WiFi in any case
                    mWifiManager.setWifiEnabled(false);
                }
                Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                return (Boolean) method.invoke(mWifiManager, wifiConfig, enabled);
            }

        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return false;
        }
    }



    public static void turnOnHotspot(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    Log.d(TAG, "HOTSPOT AT&T Hotspot is on now");
                    if(AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HOTSPOT AT&T Hotspot is on now");
                    mReservation = reservation;
                    String SSID = reservation.getWifiConfiguration().SSID;
                    String Key = reservation.getWifiConfiguration().preSharedKey;

                    System.out.println("OOOO -- SSID:" + SSID);
                    System.out.println("OOOO -- Key:" + Key);

                    TabsDefaultHubspotName = SSID;
                    TabsDefaultHubspotPassword = Key;


                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.d(TAG, "HOTSPOT AT&T onStopped");
                    if(AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HOTSPOT AT&T onStopped");

                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    Log.d(TAG, "HOTSPOT AT&T onFailed");
                    if(AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HOTSPOT AT&T onFailed");
                }
            }, new Handler());
        }


    }


    public static void turnOffHotspot() {

        if (mReservation != null) {
            mReservation.close();
            Log.d(TAG, "HOTSPOT AT&T turnOffHotspot success");
            if(AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HOTSPOT AT&T turnOffHotspot success");
        }else{
            Log.d(TAG, "HOTSPOT AT&T turnOffHotspot mReservation = null");
            if(AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HOTSPOT AT&T turnOffHotspot mReservation = null");
        }

    }

    private static void sendImplicitBroadcast(Context ctxt, Intent i) {
        PackageManager pm = ctxt.getPackageManager();
        List<ResolveInfo> matches = pm.queryBroadcastReceivers(i, 0);

        for (ResolveInfo resolveInfo : matches) {
            Intent explicit = new Intent(i);
            ComponentName cn =
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            explicit.setComponent(cn);
            ctxt.sendBroadcast(explicit);
        }
    }
}
