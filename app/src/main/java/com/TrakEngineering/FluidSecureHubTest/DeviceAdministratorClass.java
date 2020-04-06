package com.TrakEngineering.FluidSecureHubTest;

import android.app.admin.DeviceAdminReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.widget.Toast;

public class DeviceAdministratorClass extends DeviceAdminReceiver {

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "Device Administrator: Activated");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "This is an optional message to warn the user about disabling.";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "Device Administrator: Deactivated");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        showToast(context, "Sample Device DeviceAdministratorClass: pw failed");
        Vibrator v = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        // Vibrate for 2 seconds
        v.vibrate(2000);
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        showToast(context, "Welcome Device Owner");
    }
}

