package com.TrakEngineering.FluidSecureHubTest;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.TrakEngineering.FluidSecureHubTest.offline.EntityHub;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AcceptHoursAcitvity extends AppCompatActivity {

    OffDBController controller = new OffDBController(AcceptHoursAcitvity.this);

    private NetworkReceiver receiver = new NetworkReceiver();

    private static final String TAG = "AcceptHoursAcitvity :";
    private EditText etHours;
    private TextView tv_swipekeybord, tv_hours;
    private String vehicleNumber;
    private String odometerTenths, ScreenNameForHours = "Hour";
    private ProgressBar progressBar;
    private ConnectionDetector cd = new ConnectionDetector(AcceptHoursAcitvity.this);

    String OdometerReasonabilityConditions = "", CheckOdometerReasonable = "", PreviousHours = "", HoursLimit = "", IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "", IsHoursRequire = "";
    String TimeOutinMinute;
    boolean Istimeout_Sec = true;
    public int cnt123 = 0;
    public int off_cnt123 = 0;
    Timer t, ScreenOutTime;
    List<Timer> HrScreenTimerlist = new ArrayList<Timer>();

    @Override
    protected void onResume() {
        super.onResume();

        invalidateOptionsMenu();
        //Set/Reset EnterPin text
        etHours.setText("");
        /*if (Constants.CurrentSelectedHose.equals("FS1")) {
            etHours.setText(ZR(String.valueOf(Constants.AccHours_FS1)));
        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            etHours.setText(ZR(String.valueOf(Constants.AccHours)));
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            etHours.setText(ZR(String.valueOf(Constants.AccHours_FS3)));
        } else if (Constants.CurrentSelectedHose.equals("FS4")) {
            etHours.setText(ZR(String.valueOf(Constants.AccHours_FS4)));
        }*/

        Istimeout_Sec = true;
        TimeoutHoursScreen();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.mconfigure_fsnp).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

            menu.findItem(R.id.monline).setVisible(true);
            menu.findItem(R.id.mofline).setVisible(false);

        } else {
            menu.findItem(R.id.monline).setVisible(false);
            menu.findItem(R.id.mofline).setVisible(true);
        }

        return true;
    }

    private void TimeoutHoursScreen() {

        SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");


        OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditions", "");
        CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonable", "");
        PreviousHours = sharedPrefODO.getString("PreviousHours", "");
        HoursLimit = sharedPrefODO.getString("HoursLimit", "");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;

        ScreenOutTime = new Timer();
        HrScreenTimerlist.add(ScreenOutTime);
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (Istimeout_Sec) {

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Istimeout_Sec = false;
                                AppConstants.ClearEdittextFielsOnBack(AcceptHoursAcitvity.this);

                                Intent i = new Intent(AcceptHoursAcitvity.this, WelcomeActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });

                        CancelTimerScreenOut();
                    } catch (Exception e) {

                        System.out.println(e);
                    }

                }

            }

            ;
        };
        ScreenOutTime.schedule(ttt, screenTimeOut, 500);


    }

    public void ResetTimeoutHoursScreen() {

        CancelTimerScreenOut();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TimeoutHoursScreen();
    }

    public String ZR(String zeroString) {
        if (zeroString.trim().equalsIgnoreCase("0"))
            return "";
        else
            return zeroString;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  ActivityHandler.addActivities(5, AcceptHoursAcitvity.this);

        setContentView(R.layout.activity_accept_hours_acitvity);
        getSupportActionBar().setTitle(AppConstants.BrandName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        InItGUI();

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.sharedPref_KeyboardType, 0);
        ScreenNameForHours = myPrefkb.getString("ScreenNameForHours", "Hour");

        if (ScreenNameForHours.trim().isEmpty())
            ScreenNameForHours = "Hour";

        tv_hours.setText("Enter the " + ScreenNameForHours);
        etHours.setHint("Enter the " + ScreenNameForHours);

        /*SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");


        OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditions", "");
        CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonable", "");
        PreviousHours = sharedPrefODO.getString("PreviousHours", "");
        HoursLimit = sharedPrefODO.getString("HoursLimit", "");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");

        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Istimeout_Sec) {
                    Istimeout_Sec = false;
                    AppConstants.ClearEdittextFielsOnBack(AcceptHoursAcitvity.this);

                    // ActivityHandler.GetBacktoWelcomeActivity();

                    Intent i = new Intent(AcceptHoursAcitvity.this, WelcomeActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }

            }
        }, screenTimeOut);*/

        vehicleNumber = getIntent().getStringExtra(Constants.VEHICLE_NUMBER);

        String KeyboardType = "2";
        try {
            etHours.setInputType(Integer.parseInt(KeyboardType));
        } catch (Exception e) {
            System.out.println("keyboard exception");
            etHours.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        tv_swipekeybord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int InputTyp = etHours.getInputType();
                if (InputTyp == 2) {
                    etHours.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for 123");
                } else {

                    etHours.setInputType(InputType.TYPE_CLASS_NUMBER);//| InputType.TYPE_CLASS_TEXT
                    tv_swipekeybord.setText("Press for ABC");
                }

            }
        });


        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);


    }

    private void InItGUI() {
        try {
            tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
            tv_hours = (TextView) findViewById(R.id.tv_hours);
            etHours = (EditText) findViewById(R.id.etHours);
            progressBar = (ProgressBar) findViewById(R.id.progressBar);

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void cancelAction(View v) {

        onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveButtonAction(View view) {
        try {

            Istimeout_Sec = false;

            if (!etHours.getText().toString().trim().isEmpty()) {

                int C_AccHours;
                if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                    Constants.AccHours_FS1 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.AccHours_FS1;
                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                    Constants.AccHours = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.AccHours;
                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                    Constants.AccHours_FS3 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.AccHours_FS3;
                } else { //(Constants.CurrentSelectedHose.equalsIgnoreCase("FS4"))
                    Constants.AccHours_FS4 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.AccHours_FS4;
                }

                OfflineConstants.storeCurrentTransaction(AcceptHoursAcitvity.this, "", "", "", "", etHours.getText().toString().trim(), "", "", "");


                if (OfflineConstants.isTotalOfflineEnabled(AcceptHoursAcitvity.this)) {
                    //skip all validation in permanent offline mode
                    allValid();

                } else {

                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                        int PO = Integer.parseInt(PreviousHours.trim());
                        int OL = Integer.parseInt(HoursLimit.trim());

                        if (CheckOdometerReasonable.trim().toLowerCase().equalsIgnoreCase("true")) {

                            if (OdometerReasonabilityConditions.trim().equalsIgnoreCase("1")) {

                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Hours: Entered" + C_AccHours);
                                if (C_AccHours >= PO && C_AccHours <= OL) {
                                    //gooooo
                                    allValid();
                                } else {
                                    cnt123 += 1;

                                    if (cnt123 > 3) {
                                        //gooooo
                                        allValid();
                                    } else {

                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " Hours: Entered" + C_AccHours + " is not within the reasonability");
                                        etHours.setText("");
                                        //AppConstants.colorToastBigFont(getApplicationContext(), "The Hours entered is not within the reasonability your administrator has assigned, please contact your administrator.", Color.RED);//Bad odometer! Please try again.
                                        CommonUtils.AlertDialogAutoClose(AcceptHoursAcitvity.this, "Message", "The " + ScreenNameForHours + " entered is not within the reasonability your administrator has assigned, please contact your administrator.");
                                        Istimeout_Sec = true;
                                        ResetTimeoutHoursScreen();
                                    }
                                }

                            } else {


                                if (C_AccHours >= PO && C_AccHours <= OL) {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Hours: Entered" + C_AccHours);
                                    ///gooooo
                                    allValid();
                                } else {
                                    etHours.setText("");
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Hours: Entered" + C_AccHours + " is not within the reasonability");
                                    CommonUtils.AlertDialogAutoClose(AcceptHoursAcitvity.this, "Message", "The " + ScreenNameForHours + " entered is not within the reasonability your administrator has assigned, please contact your administrator.");
                                    //AppConstants.colorToastBigFont(getApplicationContext(), "The Hours entered is not within the reasonability your administrator has assigned, please contact your administrator.", Color.RED);
                                    Istimeout_Sec = true;
                                    ResetTimeoutHoursScreen();
                                }
                            }
                        } else {

                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Hours: Entered" + C_AccHours);
                            //comment By JB -it  must take ANY number they enter on the 4th try
                            allValid();


                        }
                    } else {

                        //offline----------------------
                        AppConstants.WriteinFile("Offline Hours : " + etHours.getText().toString().trim());
                        if (OfflineConstants.isOfflineAccess(AcceptHoursAcitvity.this)) {

                            int previous_hrs = 0, hrs_limit = 0;
                            int entered_hrs = Integer.parseInt(etHours.getText().toString().trim());

                            try {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile("Offline Entered Hours : " + entered_hrs);

                                if (AppConstants.OFF_CURRENT_HOUR != null && !AppConstants.OFF_CURRENT_HOUR.isEmpty()) {

                                    previous_hrs = Integer.parseInt(AppConstants.OFF_CURRENT_HOUR);

                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile("Offline Previous Hours : " + previous_hrs);
                                }

                                if (AppConstants.OFF_HRS_Limit != null && !AppConstants.OFF_HRS_Limit.isEmpty()) {

                                    hrs_limit = Integer.parseInt(AppConstants.OFF_HRS_Limit);
                                    hrs_limit = previous_hrs + (hrs_limit) * 5;

                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile("Offline Hours limit * 5 : " + hrs_limit);

                                }
                            } catch (Exception e) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile("Hours saveButtonAction" + e.getMessage());
                            }


                            if (AppConstants.OFF_ODO_Reasonable != null && AppConstants.OFF_ODO_Reasonable.trim().toLowerCase().equalsIgnoreCase("true")) {

                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile("Offline Hours Reasonability : " + AppConstants.OFF_ODO_Reasonable);

                                if (AppConstants.OFF_ODO_Conditions != null && AppConstants.OFF_ODO_Conditions.trim().equalsIgnoreCase("1")) {

                                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Hours conditions : " + AppConstants.OFF_ODO_Conditions);

                                    if (hrs_limit == 0) {

                                        offlineValidHrs();

                                    } else if (entered_hrs >= previous_hrs && entered_hrs <= hrs_limit) {

                                        offlineValidHrs();

                                    } else {
                                        //3 attempt
                                        off_cnt123 += 1;

                                        if (off_cnt123 > 3) {

                                            offlineValidHrs();

                                        } else {
                                            CommonUtils.AlertDialogAutoClose(AcceptHoursAcitvity.this, "Message", "Please enter Correct " + ScreenNameForHours);
                                            //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                                            Istimeout_Sec = true;
                                            ResetTimeoutHoursScreen();
                                        }
                                    }


                                } else {
                                    if (hrs_limit == 0) {

                                        offlineValidHrs();

                                    } else if (entered_hrs >= previous_hrs && entered_hrs <= hrs_limit) {

                                        offlineValidHrs();

                                    } else {
                                        CommonUtils.AlertDialogAutoClose(AcceptHoursAcitvity.this, "Message", "Please enter Correct " + ScreenNameForHours);
                                        //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                                        Istimeout_Sec = true;
                                        ResetTimeoutHoursScreen();
                                    }

                                }
                            } else {
                                offlineValidHrs();
                            }


                        } else {
                            CommonUtils.AlertDialogAutoClose(AcceptHoursAcitvity.this, "Message", "Please check your Offline Access");
                            //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                            Istimeout_Sec = true;
                            ResetTimeoutHoursScreen();
                        }
                    }
                }
            } else {
                CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Error Message", "Please enter " + ScreenNameForHours);
                Istimeout_Sec = true;
                ResetTimeoutHoursScreen();
            }


        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void allValid() {

        SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        String IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequireForHub, "");
        String IsExtraOther = sharedPrefODO.getString(AppConstants.IsExtraOther, "");

        if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Online Hours temp log: " + etHours.getText().toString().trim());

        if (IsExtraOther.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptVehicleOtherInfo.class);
            startActivity(intent);

        } else if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptPinActivity_new.class);//AcceptPinActivity
            startActivity(intent);

        } else if (IsDepartmentRequire.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptDeptActivity.class);
            startActivity(intent);

        } else if (IsOtherRequire.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptOtherActivity.class);
            startActivity(intent);

        } else {

            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = AcceptHoursAcitvity.this;
            asc.checkAllFields();
        }


    }

    public void offlineValidHrs() {

        try {
            controller.updateHoursByVehicleId(AppConstants.OFF_VEHICLE_ID, etHours.getText().toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Hours temp log: " + etHours.getText().toString().trim());

        EntityHub obj = controller.getOfflineHubDetails(AcceptHoursAcitvity.this);
        if (obj.PersonnelPINNumberRequired.equalsIgnoreCase("Y")) {
            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptPinActivity_new.class);//AcceptPinActivity
            startActivity(intent);
        } else {
            Intent intent = new Intent(AcceptHoursAcitvity.this, DisplayMeterActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        // ActivityHandler.removeActivity(5);
        Istimeout_Sec = false;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CancelTimerScreenOut();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CancelTimerScreenOut();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CancelTimerScreenOut();
    }

    private void CancelTimerScreenOut() {

        for (int i = 0; i < HrScreenTimerlist.size(); i++) {
            HrScreenTimerlist.get(i).cancel();
        }

    }
}
