package com.TrakEngineering.FluidSecureHubTest;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class JobSchedulerService extends JobService {
    private static final String TAG = "JobSchedulerService";

    @Override
    public boolean onStartJob(JobParameters params) {
        System.out.println("JobService onStartJob:");


        try {

            boolean foregroud = new ForegroundCheckTask().execute(getApplicationContext()).get();
            System.out.println("JobService foregroud"+foregroud);

            if (!foregroud){

                Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
                if (intent != null) {
                    // We found the activity now start the activity
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        /*try {
            WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            if (!wifi.isWifiEnabled()) {

                if (AppConstants.isMobileDataAvailable(JobSchedulerService.this)) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        //start foreground service is at welcome activity oncreate
                        this.startForegroundService(new Intent(this, BackgroundService.class));
                    } else {
                        startService(new Intent(this, BackgroundService.class));
                    }

                }

            }
        } catch (Exception e) {
            System.out.println("JobSchedulerService Exception" + e);
        }*/


        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        System.out.print("JobService onStopJob:");
        Log.i(TAG, "JobService onStopJob:");
        return false;
    }

    class ForegroundCheckTask extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            final Context context = params[0].getApplicationContext();
            return isAppOnForeground(context);
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

}