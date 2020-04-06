package com.TrakEngineering.FluidSecureHubTest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

public class WifiAPReceiver extends BroadcastReceiver {

    public static final String TAG = WifiAPReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
            int apState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            if (apState == 13) {
                Log.i(TAG,"Hotspot AP is enabled");
                AppConstants.enableHotspotManuallyWindow = true;
            } else {
                Log.i(TAG,"Hotspot AP is disabled/not ready");

                //TODO Also check App not in Link Configuration process
                if (!CommonUtils.isHotspotEnabled(context) && Build.VERSION.SDK_INT >= Constants.VERSION_CODES_NINE && AppConstants.enableHotspotManuallyWindow && Constants.hotspotstayOn && !AppConstants.busyWithHotspotToggle){
                    AppConstants.enableHotspotManuallyWindow = false;
                    Log.i(TAG, "EMobileHotspotManually");
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "EMobileHotspotManually");
                    CommonUtils.enableMobileHotspotmanuallyStartTimer(context);

                }
            }
        }
    }
}
