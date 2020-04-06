package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.TrakEngineering.FluidSecureHubTest.enity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.enity.StatusForUpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.enity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.EntityOffTranz;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.text.TextUtils.isEmpty;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.wifiApManager;
import static java.lang.String.format;


public class DisplayMeterActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    //WifiManager wifiManager;
    private static final String TAG = "DisplayMAct";
    private String vehicleNumber, odometerTenths = "0", dNumber = "", pNumber = "", oText = "", hNumber = "";
    private TextView textDateTime, tvCounts, tvGallons;
    private TextView textOdometer, txtVehicleNumber, tvConsole,tvOdometer,tvVehicle;
    private Socket socket;
    private Button btnCancel, btnFuelAnotherYes, btnFuelAnotherNo;
    ProgressBar progressBar2;
    LinearLayout linearTimer;
    TextView tvCountDownTimer, tv_hoseConnected;
    LinearLayout linearFuelAnother;
    Integer Pulses = 0;
    String iot_version = "";
    public int count_InfoCmd = 0;
    public int count_relayCmd = 0;
    private ProgressDialog progressDialog;
    private static final int SERVER_PORT = 2901;
    private static final String SERVER_IP = "192.168.4.1";
    ArrayList<HashMap<String, String>> ListOfConnectedDevices = new ArrayList<>();


    Socket socketFS = new Socket();
    String consoleString = "", outputQuantity = "0";
    boolean stopTimer = true;

    DBController controller = new DBController(DisplayMeterActivity.this);
    OffDBController offcontroller = new OffDBController(DisplayMeterActivity.this);

    private NetworkReceiver receiver = new NetworkReceiver();

    String VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel;
    double minFuelLimit = 0, numPulseRatio = 0;
    long stopAutoFuelSeconds = 0;
    double fillqty = 0;
    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();
    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    boolean isTransactionComp = false;
    ServerHandler serverHandler = new ServerHandler();

    String EMPTY_Val = "", IsFuelingStop = "0", IsLastTransaction = "1";

    public String HTTP_URL = "";
    public String URL_GET_TXNID = "";
    public String URL_SET_TXNID = "";
    //String HTTP_URL = "http://192.168.4.1:80/";

    String URL_GET_PULSAR = "";//HTTP_URL + "client?command=pulsar ";
    String URL_RECORD10_PULSAR = "";//HTTP_URL + "client?command=record10";
    String URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

    String URL_INFO = HTTP_URL + "client?command=info";
    String URL_STATUS = HTTP_URL + "client?command=status";
    String URL_RECORD = HTTP_URL + "client?command=record10";


    String URL_WIFI = HTTP_URL + "config?command=wifi";
    String URL_RELAY = HTTP_URL + "config?command=relay";

    String URL_GET_USER = HTTP_URL + "upgrade?command=getuser";
    String URL_RESET = HTTP_URL + "upgrade?command=reset";
    String URL_FILE_UPLOAD = HTTP_URL + "upgrade?command=start";

    String jsonRename;
    String jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
    String jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
    String jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

    String jsonPulsar = "{\"pulsar_request\":{\"counter_set\":1}}";
    String jsonPulsarOff = "{\"pulsar_request\":{\"counter_set\":0}}";


    public Network networkTransportWifi;

    boolean pulsarConnected = false;

    ConnectivityManager connection_manager;


    public static boolean BRisWiFiConnected;

    public static TextView tvStatus;
    public static Button btnStart;
    TimerTask mTimerTask;
    final Handler handler = new Handler();
    Timer t = new Timer();
    int attempt = 1;
    boolean Istimeout_Sec = true;
    boolean isTCancelled = false;
    String IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "";
    String TimeOutinMinute;
    List<Timer> DisplayMScreeTimerlist = new ArrayList<Timer>();
    GoogleApiClient mGoogleApiClient;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;


    double CurrentLat = 0, CurrentLng = 0;


    TextView tvWifiList;


    int timeThanks = 6;
    Timer tThanks;
    TimerTask taskThanks;


    // int timeFirst = 60;
    Timer tFirst;
    TimerTask taskFirst;

    String URL_GET_TXN_LAST10 = "";
    ArrayList<HashMap<String, String>> arrayList;

    ConnectionDetector cd = new ConnectionDetector(DisplayMeterActivity.this);


    long sqlite_id = 0;

    Timer  ScreenOutTime;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!CommonUtils.isHotspotEnabled(DisplayMeterActivity.this)) {

            btnStart.setText("Please wait..");
            btnStart.setEnabled(false);
            wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
            AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Connecting to hotspot, please wait", Color.RED);


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnStart.setText("START");
                    btnStart.setEnabled(true);
                }
            }, 10000);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.mconfigure_fsnp).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);
        menu.findItem(R.id.mreconnect_ble_readers).setVisible(false);

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH){

            menu.findItem(R.id.monline).setVisible(true);
            menu.findItem(R.id.mofline).setVisible(false);

        }else{
            menu.findItem(R.id.monline).setVisible(false);
            menu.findItem(R.id.mofline).setVisible(true);
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        invalidateOptionsMenu();
        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH){
            AppConstants.CURRENT_STATE_MOBILEDATA = true;
        }else{
            AppConstants.CURRENT_STATE_MOBILEDATA = false;
        }

        //Hide keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (AppConstants.DetailsListOfConnectedDevices.size() == 0) {
            new GetConnectedDevicesIP().execute();
        }
        //new TasksbeforeStartBtnAsyncTask().execute();//test
        CompleteTasksbeforeStartbuttonClick();

        Istimeout_Sec = true;
        TimeOutDisplayMeterScreen();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_meter);

        // ActivityHandler.addActivities(7, DisplayMeterActivity.this);

        setHttpTransportToDefaultNetwork(DisplayMeterActivity.this);

        getSupportActionBar().setTitle(R.string.fs_name);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        InItGUI();
        tvWifiList = (TextView) findViewById(R.id.tvWifiList);

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.sharedPref_KeyboardType, 0);

        String ScreenNameForOdometer = myPrefkb.getString("ScreenNameForOdometer", "odometer");
        if(ScreenNameForOdometer.trim().isEmpty())
            ScreenNameForOdometer="odometer";


        String ScreenNameForVehicle = myPrefkb.getString("ScreenNameForVehicle", "Vehicle");
        if (ScreenNameForVehicle.trim().isEmpty())
            ScreenNameForVehicle = "Vehicle";

        tvVehicle.setText(ScreenNameForVehicle+" Number:");
        tvOdometer.setText(ScreenNameForOdometer+" Entry:");


        //offline-----------start
        EntityOffTranz authEntityClass = OfflineConstants.getCurrentTransaction(DisplayMeterActivity.this);
        authEntityClass.PersonPin = AppConstants.OFF_PERSON_PIN;
        authEntityClass.OnlineTransactionId = "0";

        sqlite_id = offcontroller.insertOfflineTransactions(authEntityClass);

        AppConstants.clearSharedPrefByName(DisplayMeterActivity.this, "storeCurrentTransaction");

        //offline-----------end


        /*SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");


        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Istimeout_Sec) {
                    Istimeout_Sec = false;
                    AppConstants.ClearEdittextFielsOnBack(DisplayMeterActivity.this);
                    Intent intent = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

            }
        }, screenTimeOut);*/

        getListOfConnectedDevice();

        tv_hoseConnected.setText("Connected to " + AppConstants.CURRENT_SELECTED_SSID);
        if (Constants.CurrentSelectedHose.equals("FS1")) {
            txtVehicleNumber.setText(Constants.AccVehicleNumber_FS1);
            textOdometer.setText(Integer.toString(Constants.AccOdoMeter_FS1));

        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            textOdometer.setText(Integer.toString(Constants.AccOdoMeter));
            txtVehicleNumber.setText(Constants.AccVehicleNumber);
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            textOdometer.setText(Integer.toString(Constants.AccOdoMeter_FS3));
            txtVehicleNumber.setText(Constants.AccVehicleNumber_FS3);
        } else {
            textOdometer.setText(Integer.toString(Constants.AccOdoMeter_FS4));
            txtVehicleNumber.setText(Constants.AccVehicleNumber_FS4);
        }

        /*
        LocationManager locationManager = (LocationManager) DisplayMeterActivity.this.getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


        if (!statusOfGPS) {

            turnGPSOn();

        }*/


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();


       /* if (ActivityHandler.screenStack != null)
            ActivityHandler.screenStack.clear();*/

        vehicleNumber = Constants.AccVehicleNumber;
        odometerTenths = Constants.AccOdoMeter + "";
        dNumber = Constants.AccDepartmentNumber;
        pNumber = Constants.AccPersonnelPIN;
        oText = Constants.AccOther;
        hNumber = Constants.AccHours + "";


        if (dNumber != null) {
        } else {
            dNumber = "";
        }

        if (pNumber != null) {
        } else {
            pNumber = "";
        }

        if (oText != null) {
        } else {
            oText = "";
        }


        //--------------------------------------------------------------------------
        // Display current date time
        Thread myThread = null;
        Runnable myRunnableThread = new CountDownRunner(this, textDateTime);
        myThread = new Thread(myRunnableThread);
        myThread.start();
        //end current date time-----------------------------------------------------

        //textOdometer.setText(isEmpty(odometerTenths) ? "" : odometerTenths);
        // txtVehicleNumber.setText(isEmpty(vehicleNumber) ? "" : vehicleNumber);

        //--------------------------------------------------------

        String networkSSID;
        if (AppConstants.NeedToRename) {
            jsonRename = "{\"Request\":{\"SoftAP\":{\"Connect_SoftAP\":{\"authmode\":\"OPEN\",\"channel\":6,\"ssid\":\"" + AppConstants.REPLACEBLE_WIFI_NAME + "\",\"password\":\"\"}}}}";
        }
        networkSSID = AppConstants.LAST_CONNECTED_SSID;


        System.out.println("NeedToRename--" + AppConstants.NeedToRename);

        String networkPass = AppConstants.WIFI_PASSWORD;

        BRisWiFiConnected = false;


        //--------------------------------------------------

        if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS1", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS1", "");
            PersonId = sharedPref.getString("PersonId_FS1", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS1", "1");
            MinLimit = sharedPref.getString("MinLimit_FS1", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS1", "");
            ServerDate = sharedPref.getString("ServerDate_FS1", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS1", "0");

        } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId", "");
            PhoneNumber = sharedPref.getString("PhoneNumber", "");
            PersonId = sharedPref.getString("PersonId", "");
            PulseRatio = sharedPref.getString("PulseRatio", "1");
            MinLimit = sharedPref.getString("MinLimit", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId", "");
            ServerDate = sharedPref.getString("ServerDate", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel", "0");


        } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS3", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS3", "");
            PersonId = sharedPref.getString("PersonId_FS3", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS3", "1");
            MinLimit = sharedPref.getString("MinLimit_FS3", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS3", "");
            ServerDate = sharedPref.getString("ServerDate_FS3", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS3", "0");


        } else {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS4", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS4", "");
            PersonId = sharedPref.getString("PersonId_FS4", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS4", "1");
            MinLimit = sharedPref.getString("MinLimit_FS4", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS4", "");
            ServerDate = sharedPref.getString("ServerDate_FS4", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS4", "0");

        }

        minFuelLimit = Double.parseDouble(MinLimit);

        numPulseRatio = Double.parseDouble(PulseRatio);

        stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);


        System.out.println("iiiiii" + IntervalToStopFuel);
        System.out.println("minFuelLimit" + minFuelLimit);
        System.out.println("getDeviceName" + minFuelLimit);

        String mobDevName = AppConstants.getDeviceName().toLowerCase();
        System.out.println("oooooooooo" + mobDevName);

        //Connect to bluetoothPrinter
//        new SetBTConnectionPrinter().execute();

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);


    }

    private void TimeOutDisplayMeterScreen() {
        SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;

        ScreenOutTime = new Timer();
        DisplayMScreeTimerlist.add(ScreenOutTime);
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                //do something

                if (Istimeout_Sec) {

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (btnStart.isClickable()){
                                    //did not press start (start appeared, was never pressed):  User did not Press Start = 7
                                    UpdateDiffStatusMessages("7");
                                }

                                Istimeout_Sec = false;
                                AppConstants.ClearEdittextFielsOnBack(DisplayMeterActivity.this);

                                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
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

    public void ResetTimeoutDisplayMeterScreen(){


        CancelTimerScreenOut();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TimeOutDisplayMeterScreen();
    }

    public void getListOfConnectedDevice() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedReader br = null;
                boolean isFirstLine = true;

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue;
                        }

                        String[] splitted = line.split(" +");

                        if (splitted != null && splitted.length >= 4) {

                            String ipAddress = splitted[0];
                            String macAddress = splitted[3];
                            System.out.println("IPAddress" + ipAddress);
                            boolean isReachable = InetAddress.getByName(
                                    splitted[0]).isReachable(500);  // this is network call so we cant do that on UI thread, so i take background thread.
                            if (isReachable) {
                                Log.d("Device Information", ipAddress + " : "
                                        + macAddress);
                            }


                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void turnGPSOn() {


        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationRequest mLocationRequest1 = new LocationRequest();
        mLocationRequest1.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .addLocationRequest(mLocationRequest1);


        LocationSettingsRequest mLocationSettingsRequest = builder.build();


        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("Splash", "All location settings are satisfied.");

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("Splash", "Location settings are not satisfied. Show the user a dialog to" +
                                "upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(DisplayMeterActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("Splash", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("Splash", "Location settings are inadequate, and cannot be fixed here. Dialog " +
                                "not created.");
                        break;
                }
            }
        });


        //Intent in = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //startActivity(in);

    }


    @Override
    public void onConnected(Bundle bundle) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {

            CurrentLat = mLastLocation.getLatitude();
            CurrentLng = mLastLocation.getLongitude();

            System.out.println("CCCrrr" + CurrentLat);
            System.out.println("CCCrrr" + CurrentLng);

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void doTimerTask() {

        mTimerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {


                        System.out.println(Calendar.getInstance().getTime());


                        if (BRisWiFiConnected && AppConstants.getConnectedWifiName(DisplayMeterActivity.this).equalsIgnoreCase("\"" + AppConstants.LAST_CONNECTED_SSID + "\"")) {

                            tvStatus.setText("");
                            stopTask();


                        } else {
                            if (attempt >= 3) {
                                tvStatus.setText("");
                                stopTask();

                                if (!isTCancelled)
                                    AlertSettings(DisplayMeterActivity.this, "Unable to connect " + AppConstants.LAST_CONNECTED_SSID + "!\n\nPlease connect to " + AppConstants.LAST_CONNECTED_SSID + " manually using the 'WIFI settings' screen.\nThen hit back and click on the 'START' button to continue.");
                            } else {

                                /*
                                if (linearFuelAnother.getVisibility() != View.VISIBLE) {

                                    AppConstants.dontConnectWiFi(DisplayMeterActivity.this);

                                    connectToWiFiNew();

                                    attempt++;

                                    tvStatus.setText("Please wait...\nConnecting to '" + AppConstants.LAST_CONNECTED_SSID + "'" + "\nAttempt " + attempt + "/3");
                                } else {
                                    tvStatus.setText("");
                                    stopTask();
                                }
                                */
                            }

                        }


                    }
                });
            }
        };


        t.schedule(mTimerTask, 0, 15000);  //10seconds

    }

    public void stopTask() {

        attempt = 100;

        if (mTimerTask != null) {


            Log.d("TIMER", "timer canceled");
            mTimerTask.cancel();
        }

    }


    private void InItGUI() {
        try {

            //---TextView-------------
            textDateTime = (TextView) findViewById(R.id.textDateTime);
            tv_hoseConnected = (TextView) findViewById(R.id.tv_hoseConnected);
            textOdometer = (TextView) findViewById(R.id.textOdometer);
            txtVehicleNumber = (TextView) findViewById(R.id.txtVehicleNumber);
            tvCounts = (TextView) findViewById(R.id.tvCounts);
            tvGallons = (TextView) findViewById(R.id.tvGallons);
            tvConsole = (TextView) findViewById(R.id.tvConsole);
            tvCountDownTimer = (TextView) findViewById(R.id.tvCountDownTimer);

            linearTimer = (LinearLayout) findViewById(R.id.linearTimer);


            //-----------Buttons----
            btnStart = (Button) findViewById(R.id.btnStart);
            btnCancel = (Button) findViewById(R.id.btnCancel);
            linearFuelAnother = (LinearLayout) findViewById(R.id.linearFuelAnother);
            //btnFuelHistory = (Button) findViewById(R.id.btnFuelHistory);
            btnFuelAnotherYes = (Button) findViewById(R.id.btnFuelAnotherYes);
            btnFuelAnotherNo = (Button) findViewById(R.id.btnFuelAnotherNo);

            tvOdometer = (TextView) findViewById(R.id.tvOdometer);
            tvVehicle = (TextView) findViewById(R.id.tvVehicle);


            btnStart.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
            // btnFuelHistory.setOnClickListener(this);
            btnFuelAnotherYes.setOnClickListener(this);
            btnFuelAnotherNo.setOnClickListener(this);


        } catch (Exception ex) {

            CommonUtils.LogMessage(TAG, " InItGUI ", ex);
        }
    }

    @Override
    public void onBackPressed() {

        // ActivityHandler.removeActivity(7);
        Istimeout_Sec = false;
        finish();

    }


    @TargetApi(21)
    public void setGlobalMobileDatConnection() {

        NetworkRequest.Builder requestbuilder = new NetworkRequest.Builder();
        requestbuilder
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        connection_manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);


        connection_manager.requestNetwork(requestbuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {


                System.out.println(" network......." + network);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connection_manager.bindProcessToNetwork(network);

                }
            }
        });
    }

    @TargetApi(21)
    private void setGlobalWifiConnection() {

        NetworkRequest.Builder requestbuilder = new NetworkRequest.Builder();
        requestbuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        connection_manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);


        connection_manager.requestNetwork(requestbuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {


                System.out.println(" network......." + network);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connection_manager.bindProcessToNetwork(network);

                }
            }
        });
    }

    public void startWelcomeActityDiscWifi() {


        WelcomeActivity.SelectedItemPos = -1;

        finish();
    }


    @SuppressLint({"ShowToast", "ResourceAsColor"})
    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.btnStart:

                Istimeout_Sec = false;
                btnStart.setClickable(false);
                btnStart.setBackgroundColor(Color.parseColor("#F88800"));
                boolean test = AppConstants.CURRENT_STATE_MOBILEDATA;

                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
                    StartbuttonFunctionality();
                } else {
                    if (OfflineConstants.isOfflineAccess(DisplayMeterActivity.this)) {
                        StartbuttonFunctionality();
                    } else {
                        AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                    }
                }


                break;

            case R.id.btnCancel:

                if (btnStart.isClickable() && cd.isConnectingToInternet()){
                    //did not press start (start appeared, was never pressed):  User did not Press Start = 7
                    UpdateDiffStatusMessages("7");
                }

                Istimeout_Sec = false;
                onBackPressed();
                break;

            case R.id.btnFuelAnotherYes:


                startWelcomeActityDiscWifi();


                break;

            case R.id.btnFuelAnotherNo:


                /*
                if (AppConstants.IS_WIFI_ON) {
                    wifiManagerMM = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
                    if (!wifiManagerMM.isWifiEnabled()) {
                        wifiManagerMM.setWifiEnabled(true);
                        wifiManagerMM.disconnect();
                    }
                }*/


                finish();

                break;

        }
    }

    @SuppressLint("ResourceAsColor")
    public void CompleteTasksbeforeStartbuttonClick() {

        SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        String HubId = sharedPrefODO.getString(AppConstants.HubId, "");

        BtnStartStateChange(false);
        //btnCancel.setClickable(false);

        String macaddress = AppConstants.SELECTED_MACADDRESS;


        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
            String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
            if (macaddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                String IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                HTTP_URL = "http://" + IpAddress + ":80/";

            }
        }

        URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
        URL_SET_TXNID = HTTP_URL + "config?command=txtnid";
        URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
        URL_RECORD10_PULSAR = HTTP_URL + "client?command=record10";
        URL_INFO = HTTP_URL + "client?command=info";
        URL_RELAY = HTTP_URL + "config?command=relay";

        String PulserTimingAd = HTTP_URL + "config?command=pulsar";
        URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

        URL_GET_TXN_LAST10 = HTTP_URL + "client?command=cmtxtnid10";

        //Check if Hose connected to hotspot or not
        try {
            //Info command commented
            new CommandsGET_Info().execute(URL_INFO);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void CheckForUpdateFirmware(final String hoseid, String iot_version, final String FS_selected) {

        SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String HubId = sharedPrefODO.getString(AppConstants.HubId, "");// HubId equals to personId


        //First call which will Update Fs firmware to Server--
        final UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(DisplayMeterActivity.this);
        objEntityClass.Email = CommonUtils.getCustomerDetails(this).PersonEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = iot_version;

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
            if (hoseid != null && !hoseid.trim().isEmpty()) {
                new DisplayMeterActivity.UpgradeCurrentVersionWithUgradableVersion(objEntityClass).execute();

                /*try {
                    JSONObject jsonObject = new JSONObject(objUP.response);
                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    String ResponceText = jsonObject.getString("ResponceText");

                    if (ResponceMessage.equalsIgnoreCase("success")) {
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
        } else {
            AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
            Istimeout_Sec = true;
            ResetTimeoutDisplayMeterScreen();
        }

        //Second call will get Status for firwareupdate
        StatusForUpgradeVersionEntity objEntityClass1 = new StatusForUpgradeVersionEntity();
        objEntityClass1.IMEIUDID = AppConstants.getIMEI(DisplayMeterActivity.this);
        objEntityClass1.Email = CommonUtils.getCustomerDetails(this).PersonEmail;
        objEntityClass1.HoseId = hoseid;
        objEntityClass1.PersonId = HubId;

        Gson gson = new Gson();
        String jsonData = gson.toJson(objEntityClass1);

        String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "IsUpgradeCurrentVersionWithUgradableVersion");


        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
            new GetUpgrateFirmwareStatus().execute(FS_selected, jsonData, authString);
        else {
            AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
            Istimeout_Sec = true;
            ResetTimeoutDisplayMeterScreen();
        }

    }

    public class GetUpgrateFirmwareStatus extends AsyncTask<String, Void, String> {

        String FS_selected;
        String jsonData;
        String authString;

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
        }

        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                FS_selected = params[0];
                jsonData = params[1];
                authString = params[2];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(DisplayMeterActivity.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                System.out.println(e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            pd.dismiss();
            BtnStartStateChange(true);
            System.out.println("resp..." + resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    //--------------------------------------------
                    if (FS_selected.equalsIgnoreCase("0")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs1 = true;
                        else
                            AppConstants.UP_Upgrade_fs1 = false;

                    } else if (FS_selected.equalsIgnoreCase("1")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs2 = true;
                        else
                            AppConstants.UP_Upgrade_fs2 = false;

                    } else if (FS_selected.equalsIgnoreCase("2")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs3 = true;
                        else
                            AppConstants.UP_Upgrade_fs3 = false;

                    } else {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs4 = true;
                        else
                            AppConstants.UP_Upgrade_fs4 = false;

                    }
                    //--------------------------------------------

                } else {

                    System.out.println("Something went wrong");
                }


            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public class UpgradeCurrentVersionWithUgradableVersion extends AsyncTask<Void, Void, Void> {


        UpgradeVersionEntity objupgrade;
        public String response = null;

        public UpgradeCurrentVersionWithUgradableVersion(UpgradeVersionEntity objupgrade) {

            this.objupgrade = objupgrade;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(objupgrade);


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objupgrade.IMEIUDID + ":" + objupgrade.Email + ":" + "UpgradeCurrentVersionWithUgradableVersion");
                response = serverHandler.PostTextData(DisplayMeterActivity.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }

    }


    public void GetLastTransaction() {
        try {
            new CommandsGET_TxnId().execute(URL_GET_TXNID);

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  LastTXNid Ex:" + e.getMessage() + " ");
        }


    }


    public void stopButtonFunctionality() {


        quantityRecords.clear();

        btnStart.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        //btnFuelHistory.setVisibility(View.VISIBLE);
        consoleString = "";
        tvConsole.setText("");

        //it stops pulsar logic------
        stopTimer = false;


        new CommandsPOST().execute(URL_RELAY, jsonRelayOff);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    new GETFINALPulsar().execute(URL_GET_PULSAR);

                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 2000);


    }


    public void finalLastStep() {


        new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (AppConstants.NeedToRename) {

                    consoleString += "RENAME:\n" + jsonRename;

                    new CommandsPOST().execute(URL_WIFI, jsonRename);

                }
            }

        }, 2500);

        long secondsTime = 3000;

        if (AppConstants.NeedToRename) {
            secondsTime = 5000;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //AppConstants.disconnectWiFi(DisplayMeterActivity.this);

                TransactionCompleteFunction();

            }

        }, secondsTime);
    }

    public void startQuantityInterval() {


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                try {

                    if (stopTimer) {

                        /*
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                            setHttpTransportWifi(URL_GET_PULSAR, EMPTY_Val);

                        } else {

                        }
                        */

                        new GETPulsarQuantity().execute(URL_GET_PULSAR);

                    }

                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }, 0, 2000);


    }


    public void secondsTimeLogic(String currentDT) {

        try {


            if (quantityRecords.size() > 0) {

                Date nowDT = sdformat.parse(currentDT);
                Date d2 = sdformat.parse(quantityRecords.get(0).get("b"));

                long seconds = (nowDT.getTime() - d2.getTime()) / 1000;


                if (stopAutoFuelSeconds > 0) {

                    if (seconds >= stopAutoFuelSeconds) {

                        if (qtyFrequencyCount()) {

                            //qty is same for some time
                            AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nQuantity is same for last " + stopAutoFuelSeconds + " seconds.", Color.BLUE);
                            stopButtonFunctionality();
                            stopTimer = false;

                        } else {
                            quantityRecords.remove(0);
                            System.out.println("0 th pos deleted");
                            System.out.println("seconds--" + seconds);
                        }
                    }
                }

            }
        } catch (Exception e) {

        }
    }


    public void AlertSettings(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                }

        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void TransactionCompleteFunction() {


        try {


            TrazComp authEntityClass = new TrazComp();
            authEntityClass.PersonId = PersonId;
            authEntityClass.SiteId = AppConstants.SITE_ID;
            authEntityClass.VehicleId = VehicleId;
            authEntityClass.CurrentOdometer = odometerTenths;
            authEntityClass.FuelQuantity = fillqty;
            authEntityClass.FuelTypeId = FuelTypeId;
            authEntityClass.PhoneNumber = PhoneNumber;
            authEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
            authEntityClass.TransactionDate = ServerDate;
            authEntityClass.TransactionFrom = "A";
            authEntityClass.CurrentLat = "" + CurrentLat;
            authEntityClass.CurrentLng = "" + CurrentLng;
            authEntityClass.VehicleNumber = vehicleNumber;

            authEntityClass.DepartmentNumber = dNumber;
            authEntityClass.PersonnelPIN = pNumber;
            authEntityClass.Other = oText;
            authEntityClass.Hours = hNumber;

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            System.out.println("TrazComp......" + jsonData);

            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).Email;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "TransactionComplete");

            HashMap<String, String> imap = new HashMap<>();
            imap.put("jsonData", jsonData);
            imap.put("authString", authString);

            boolean isInsert = true;
            ArrayList<HashMap<String, String>> alltranz = controller.getAllTransaction();
            if (alltranz != null && alltranz.size() > 0) {

                for (int i = 0; i < alltranz.size(); i++) {

                    if (jsonData.equalsIgnoreCase(alltranz.get(i).get("jsonData")) && authString.equalsIgnoreCase(alltranz.get(i).get("authString"))) {
                        isInsert = false;
                        break;
                    }
                }
            }


            if (isInsert && fillqty > 0) {
                controller.insertTransactions(imap);
            }

           /* Constants.AccVehicleNumber = "";
            Constants.AccOdoMeter = 0;
            Constants.AccDepartmentNumber = "";
            Constants.AccPersonnelPIN = "";
            Constants.AccOther = "";*/


        } catch (Exception ex) {

            CommonUtils.LogMessage(TAG, "AuthTestAsyncTask ", ex);
        }


        isTransactionComp = true;

        AppConstants.BUSY_STATUS = true;


        btnCancel.setVisibility(View.GONE);
        consoleString = "";
        tvConsole.setText("");


        if (AppConstants.NeedToRename) {
            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).Email;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.R_SITE_ID;
            rhose.HoseId = AppConstants.R_HOSE_ID;
            rhose.IsHoseNameReplaced = "Y";

            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);

            storeIsRenameFlag(DisplayMeterActivity.this, AppConstants.NeedToRename, jsonData, authString);

        }


        //startService(new Intent(DisplayMeterActivity.this, BackgroundService.class));

        //linearFuelAnother.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressBar2.setVisibility(View.GONE);
                startTimer();
                alertThankYou(tvGallons.getText().toString() + "\n" + tvCounts.getText().toString() + "\n\nThank You!!!");
            }
        }, 1500);


    }

    public boolean qtyFrequencyCount() {


        if (quantityRecords.size() > 0) {

            ArrayList<String> data = new ArrayList<>();

            for (HashMap<String, String> hm : quantityRecords) {
                data.add(hm.get("a"));
            }

            System.out.println("\n Count all with frequency");
            Set<String> uniqueSet = new HashSet<String>(data);

            System.out.println("size--" + uniqueSet.size());

            /*for (String temp : uniqueSet) {
                System.out.println(temp + ": " + Collections.frequency(data, temp));
            }*/

            if (uniqueSet.size() == 1) {
                return true;
            }
        }

        return false;
    }


    public class GETPulsarQuantity extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                tvConsole.setText(consoleString);

                System.out.println(result);

                if (stopTimer)
                    pulsarQtyLogic(result);


            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class GETFINALPulsar extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                System.out.println(result);


                if (result.contains("pulsar_status")) {

                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                    String counts = joPulsarStat.getString("counts");

                    convertCountToQuantity(counts);


                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finalLastStep();
                        }
                    }, 1000);


                }

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public void pulsarQtyLogic(String result) {

        int secure_status = 0;

        try {
            if (result.contains("pulsar_status")) {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                String counts = joPulsarStat.getString("counts");
                String pulsar_status = joPulsarStat.getString("pulsar_status");
                String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");


                if (pulsar_status.trim().equalsIgnoreCase("1")) {
                    pulsarConnected = true;
                } else if (pulsar_status.trim().equalsIgnoreCase("0")) {

                    pulsarConnected = false;
                    if (!pulsarConnected) {
                        AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nPulsar disconnected", Color.BLUE);
                        stopButtonFunctionality();
                    }
                }


                convertCountToQuantity(counts);


                if (!pulsar_secure_status.trim().isEmpty()) {
                    secure_status = Integer.parseInt(pulsar_secure_status);

                    if (secure_status == 0) {
                        linearTimer.setVisibility(View.GONE);
                        tvCountDownTimer.setText("-");

                    } else if (secure_status == 1) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("5");

                    } else if (secure_status == 2) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("4");

                    } else if (secure_status == 3) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("3");

                    } else if (secure_status == 4) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("2");

                    } else if (secure_status >= 5) {
                        linearTimer.setVisibility(View.GONE);
                        tvCountDownTimer.setText("1");

                        AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nCount down timer completed.", Color.BLUE);
                        stopButtonFunctionality();
                    }

                }

            }
            Date currDT = new Date();
            String strCurDT = sdformat.format(currDT);

            HashMap<String, String> hmap = new HashMap<>();
            hmap.put("a", outputQuantity);
            hmap.put("b", strCurDT);
            quantityRecords.add(hmap);

            //if quantity same for some interval
            secondsTimeLogic(strCurDT);


            //if quantity reach max limit
            if (!outputQuantity.trim().isEmpty()) {
                try {


                    if (minFuelLimit > 0) {
                        if (fillqty >= minFuelLimit) {

                            AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nYou reached MAX fuel limit.", Color.BLUE);
                            Istimeout_Sec = true;
                            ResetTimeoutDisplayMeterScreen();
                            stopButtonFunctionality();
                        }
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void convertCountToQuantity(String counts) {
        outputQuantity = counts;

        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);


        tvCounts.setText("Pulse: " + outputQuantity);
        tvGallons.setText("Quantity: " + (fillqty));

    }


    public class CommandsGET extends AsyncTask<String, Void, String> {

        public String resp = "";


        @Override
        protected void onPreExecute() {

        }

        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
        }

        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                consoleString += "OUTPUT- " + result + "\n";

                tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    /*public void startTimerForQuantityCheck(long millisInFuture) {

        //30000 -- 30 seconds
        long countDownInterval = 1000; //1 second

        CountDownTimer timer = new CountDownTimer(millisInFuture, countDownInterval) {
            public void onTick(long millisUntilFinished) {
                //do something in every tick
            }

            public void onFinish() {
                System.out.println("CountDownTimer...onFinish");


                quantityRecords.clear();
            }
        }.start();
    }
*/


    public void connectToWiFiOld() {

        tvWifiList.setText("");

        Log.i(TAG, "* connectToAP");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        List<ScanResult> scanResultList = wifiManager.getScanResults();

        /*
        String wifiavailList = "";
        for (ScanResult result : scanResultList) {
            wifiavailList += result.SSID + "\n";
        }

        tvWifiList.setText(wifiavailList);
        */

        String networkSSID = AppConstants.LAST_CONNECTED_SSID;
        String networkPass = AppConstants.WIFI_PASSWORD;

        Log.d(TAG, "# password " + networkPass);

        for (ScanResult result : scanResultList) {
            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);
                Log.d(TAG, "# securityMode " + securityMode);

                if (securityMode.equalsIgnoreCase("OPEN")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int resW = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "# add Network returned " + resW);

                    if (resW == -1) {
                        resW = getExistingNetworkId(networkSSID);
                    }

                    if (resW != -1) {
                        wifiManager.enableNetwork(resW, true);
                    }

                    wifiManager.setWifiEnabled(true);

                    break;

                } else if (securityMode.equalsIgnoreCase("WEP")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "### 1 ### add Network returned " + res);

                    boolean b = wifiManager.enableNetwork(res, true);
                    Log.d(TAG, "# enableNetwork returned " + b);

                    wifiManager.setWifiEnabled(true);
                } else {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                    wifiConfiguration.hiddenSSID = true;
                    wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);


                    int res = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "### 2 ### add Network returned " + res);

                    // wifiManager.enableNetwork(res, true);

                    boolean changeHappen = wifiManager.saveConfiguration();

                    if (res != -1 && changeHappen) {
                        Log.d(TAG, "### Change happen");


                    } else {
                        Log.d(TAG, "*** Change NOT happen");
                    }

                    wifiManager.setWifiEnabled(true);
                }
            }

        }


    }

    public String getScanResultSecurity(ScanResult scanResult) {
        Log.i(TAG, "* getScanResultSecurity");

        final String cap = scanResult.capabilities;
        final String[] securityModes = {"WEP", "PSK", "EAP"};

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void connectToWiFiNew() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);

            Thread.sleep(2000);


            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", AppConstants.LAST_CONNECTED_SSID);
            wifiConfig.preSharedKey = String.format("\"%s\"", AppConstants.WIFI_PASSWORD);


            //remember id
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            Thread.sleep(2000);
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private int getExistingNetworkId(String SSID) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID.equals(SSID)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void alertThankYou(String msg) {
        final Dialog dialog = new Dialog(DisplayMeterActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_thankyou);
        dialog.setCancelable(false);


        TextView tvText = (TextView) dialog.findViewById(R.id.tvText);

        tvText.setText(msg);


        final Button btnDailogOk = (Button) dialog.findViewById(R.id.btnOk);

        btnDailogOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                stopThankYouTimer();

                dialog.dismiss();
            }
        });
        dialog.show();
    }


    public void stopThankYouTimer() {
        tThanks.cancel();
        tThanks.purge();

        WelcomeActivity.SelectedItemPos = -1;

        Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    public class AboveVessionHttp extends AsyncTask<Network, Void, String> {

        String cmdURL;
        String cmdJSON;

        public AboveVessionHttp(String cmdURL, String cmdJSON) {
            this.cmdURL = cmdURL;
            this.cmdJSON = cmdJSON;

        }

        protected String doInBackground(Network... ntwrk) {
            String resp = "";


            try {

                resp = sendCommandViaWiFi(ntwrk[0], cmdURL, cmdJSON);


            } catch (Exception e) {
                System.out.println(e);
            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            System.out.println("MM-" + result);

            consoleString += "OUTPUT- " + result + "\n";

            tvConsole.setText(consoleString);


            if (result.contains("pulsar_status")) {

                if (stopTimer)
                    pulsarQtyLogic(result);
            }
        }
    }


    @TargetApi(21)
    private void setHttpTransportWifi(final String cmdURL, final String cmdJSON) {


        NetworkRequest.Builder requestbuilder = new NetworkRequest.Builder();
        requestbuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        cm.requestNetwork(requestbuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                System.out.println("wifi network found");

                new AboveVessionHttp(cmdURL, cmdJSON).execute(network);

            }
        });

    }


    @TargetApi(21)
    private String sendCommandViaWiFi(Network network, String URLName, String jsonN) {
        String ress = "";
        try {


            // client one, should go via wifi
            okhttp3.OkHttpClient.Builder builder1 = new okhttp3.OkHttpClient.Builder();
            builder1.socketFactory(network.getSocketFactory());

            okhttp3.OkHttpClient client1 = builder1.build();

            okhttp3.Request request1;

            if (jsonN.equalsIgnoreCase(EMPTY_Val)) {
                request1 = new okhttp3.Request.Builder()
                        .url(URLName)
                        .build();
            } else {

                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json");


                okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, jsonN);


                request1 = new okhttp3.Request.Builder()
                        .url(URLName)
                        .post(body)
                        .build();
            }


            System.out.println("sending via wifi network");

            okhttp3.Response response = client1.newCall(request1).execute();

            ress = response.body().string();


        } catch (Exception e) {
            System.out.println(e);
        }
        return ress;
    }


    /////////////////////////////////////////////////////////

    /*public boolean connectToSSID(String SSID) {

        WifiConfiguration configuration = createOpenWifiConfiguration(SSID);
        int networkId = wifiManagerMM.addNetwork(configuration);
        Log.d("", "networkId assigned while adding network is " + networkId);
        return enableNetwork(SSID, networkId);
    }

    private WifiConfiguration createOpenWifiConfiguration(String SSID) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = formatSSID(SSID);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        assignHighestPriority(configuration);
        return configuration;
    }



    private boolean enableNetwork(String SSID, int networkId) {
        if (networkId == -1) {
            networkId = getExistingNetworkId(SSID);

            if (networkId == -1) {
                Log.e("ssss", "Couldn't add network with SSID: " + SSID);
                return false;
            }
        }
        return wifiManagerMM.enableNetwork(networkId, true);
    }


    //To tell OS to give preference to this network
    private void assignHighestPriority(WifiConfiguration config) {
        List<WifiConfiguration> configuredNetworks = wifiManagerMM.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (config.priority <= existingConfig.priority) {
                    config.priority = existingConfig.priority + 1;
                }
            }
        }
    }
    */

    private static String formatSSID(String wifiSSID) {
        return format("\"%s\"", wifiSSID);
    }

    private static String trimQuotes(String str) {
        if (!isEmpty(str)) {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }

        return str;
    }


    public void storeIsRenameFlag(Context context, boolean flag, String jsonData, String authString) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeIsRenameFlag", 0);
        editor = pref.edit();


        // Storing
        editor.putBoolean("flag", flag);
        editor.putString("jsonData", jsonData);
        editor.putString("authString", authString);

        // commit changes
        editor.commit();


    }


    private class WiFiConnectTask extends AsyncTask<String, Void, String> {
        // Do the long-running work in here
        protected String doInBackground(String... asd) {


            connectToWiFiOld();


            return "";
        }


        @Override
        protected void onPostExecute(String s) {


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {


                    /*
                    if (!BRisWiFiConnected || !AppConstants.getConnectedWifiName(DisplayMeterActivity.this).equalsIgnoreCase("\"" + AppConstants.LAST_CONNECTED_SSID + "\"")) {

                        if (linearFuelAnother.getVisibility() != View.VISIBLE) {

                            //attempt = 4;
                            //doTimerTask();
                        }
                    }
                    */

                    if (BRisWiFiConnected && AppConstants.getConnectedWifiName(DisplayMeterActivity.this).equalsIgnoreCase("\"" + AppConstants.LAST_CONNECTED_SSID + "\"")) {

                        tvStatus.setText("");

                    } else {

                        tvStatus.setText("");

                        if (!isTCancelled)
                            AlertSettings(DisplayMeterActivity.this, "Unable to connect " + AppConstants.LAST_CONNECTED_SSID + "!\n\nPlease connect to " + AppConstants.LAST_CONNECTED_SSID + " manually using the 'WIFI settings' screen.\nThen hit back and click on the 'START' button to continue.");

                    }


                }
            }, 6000);

        }
    }


    public void startTimer() {
        tThanks = new Timer();
        taskThanks = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (timeThanks > 0)
                            timeThanks -= 1;
                        else {
                            stopThankYouTimer();
                        }
                    }
                });
            }
        };
        tThanks.scheduleAtFixedRate(taskThanks, 0, 1000);
    }


    public void connectToWifiMarsh() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            WifiConfiguration wc = new WifiConfiguration();

            wc.SSID = "\"" + AppConstants.LAST_CONNECTED_SSID + "\"";
            wc.preSharedKey = "\"\"";
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiManager.setWifiEnabled(true);
            int netId = wifiManager.addNetwork(wc);
            if (netId == -1) {
                netId = getExistingNetworkId(AppConstants.LAST_CONNECTED_SSID);
            }
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


  /*  public void startTimerFirst() {
        tFirst = new Timer();
        taskFirst = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (timeFirst > 0)
                            timeFirst -= 1;
                        else {
                            stopFirstTimer(false);
                        }
                    }
                });
            }
        };
        tFirst.scheduleAtFixedRate(taskFirst, 0, 1000);
    }*/

    public void stopFirstTimer(boolean flag) {
        if (flag) {
            tFirst.cancel();
            tFirst.purge();
        } else {
            tFirst.cancel();
            tFirst.purge();

            WelcomeActivity.SelectedItemPos = -1;
            AppConstants.BUSY_STATUS = true;

            Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }

    }


    /* public class SetBTConnectionPrinter extends AsyncTask<String, Void, String> {


         @Override
         protected String doInBackground(String... strings) {


             try {
                 BluetoothPrinter.findBT();
                 BluetoothPrinter.openBT();

                 System.out.println("printer. FindBT and OpenBT");
             } catch (IOException e) {
                 e.printStackTrace();
             }


             return null;
         }
     }*/
    public class GetConnectedDevicesIP extends AsyncTask<String, Void, String> {
        ProgressDialog dialog;


        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(DisplayMeterActivity.this);
            dialog.setMessage("Fetching connected device info..");
            dialog.setCancelable(false);
            dialog.show();

        }

        protected String doInBackground(String... arg0) {


            ListOfConnectedDevices.clear();

            String resp = "";

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    BufferedReader br = null;
                    boolean isFirstLine = true;

                    try {
                        br = new BufferedReader(new FileReader("/proc/net/arp"));
                        String line;

                        while ((line = br.readLine()) != null) {
                            if (isFirstLine) {
                                isFirstLine = false;
                                continue;
                            }

                            String[] splitted = line.split(" +");

                            if (splitted != null && splitted.length >= 4) {

                                String ipAddress = splitted[0];
                                String macAddress = splitted[3];
                                System.out.println("IPAddress" + ipAddress);
                                boolean isReachable = InetAddress.getByName(
                                        splitted[0]).isReachable(500);  // this is network call so we cant do that on UI thread, so i take background thread.
                                if (isReachable) {
                                    Log.d("Device Information", ipAddress + " : "
                                            + macAddress);
                                }

                                if (ipAddress != null || macAddress != null) {

                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("ipAddress", ipAddress);
                                    map.put("macAddress", macAddress);

                                    ListOfConnectedDevices.add(map);

                                }
                                AppConstants.DetailsListOfConnectedDevices = ListOfConnectedDevices;
                                System.out.println("DeviceConnected" + ListOfConnectedDevices);

                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  GetConnectedDevicesIP 1 --Exception " + e);
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  GetConnectedDevicesIP 2 --Exception " + e);
                        }
                    }
                }
            });
            thread.start();


            return resp;


        }


        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);
            String strJson = result;


            dialog.dismiss();

        }

    }

    @SuppressLint("ResourceAsColor")
    public void BtnStartStateChange(boolean btnState) {


        if (btnState) {
            btnStart.setClickable(true);
            btnStart.setBackgroundColor(Color.parseColor("#56AF47"));
            btnStart.setTextColor(Color.parseColor("#FFFFFF"));
        } else {

            btnStart.setClickable(false);
            btnStart.setBackgroundColor(Color.parseColor("#f5f1f0"));
            btnStart.setTextColor(Color.parseColor("#000000"));

        }

    }

    public void StartbuttonFunctionality() {


        try {

            if (AppConstants.FS_selected.equalsIgnoreCase("0")) {


                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_AP_PIPE.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);


            } else if (AppConstants.FS_selected.equalsIgnoreCase("1")) {


                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_AP.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

            } else if (AppConstants.FS_selected.equalsIgnoreCase("2")) {


                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_FS_UNIT_3.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

            } else if (AppConstants.FS_selected.equalsIgnoreCase("3")) {


                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_FS_UNIT_4.class);//change background service to fsunite3
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }


        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " StartbuttonFunctionality " + e);
        }

    }

    public void getCMDLast10Txn() {
        try {
           new CommandsGET_CmdTxt10().execute(URL_GET_TXN_LAST10);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getCMDLastSingleTXn(){

        String txn1 = "";

        try {
            String resp = new CommandsGET_CmdTxt10_Single().execute(URL_GET_TXN_LAST10).get();
            if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + "  cmtxtnid10~ :" + resp);

            if (resp.contains("cmtxtnid_10_record")) {
                JSONObject jobj = new JSONObject(resp);
                JSONObject cm = jobj.getJSONObject("cmtxtnid_10_record");

                txn1 = cm.getString("1:TXTNINFO:");
            }

            } catch(JSONException e){
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
        }
        return txn1;
    }

    public HashMap<String, String> splitTrIdQty(String val) {
        HashMap<String, String> map = new HashMap<>();
        String parts[];
        if (val != null && val.contains("-")) {
            parts = val.split("-");

            map.put("TransactionId", parts[0]);


            double cmqty = Double.parseDouble(parts[1]);

            if (cmqty > 0) {
                cmqty = cmqty;
                map.put("Pulses", (int) cmqty + "");
                cmqty = cmqty / numPulseRatio;//convert to gallons
            } else {
                cmqty = 0;
                map.put("Pulses", cmqty + "");
            }

            cmqty = AppConstants.roundNumber(cmqty, 2);

            map.put("FuelQuantity", cmqty + "");

            arrayList.add(map);
        }

        return map;
    }


    public class EntityCmd10Txn {
        ArrayList cmtxtnid_10_record;
    }

    public class TasksbeforeStartBtnAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] objects) {

            CompleteTasksbeforeStartbuttonClick();

            return null;
        }
    }


    public class CommandsGET_Info extends AsyncTask<String, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(String... param) {

            String resp = "";
            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String FSStatus) {


            try {
                pd.dismiss();

                System.out.println(FSStatus);

                if (FSStatus.startsWith("{") && FSStatus.contains("Version")) {

                    try {

                        JSONObject jsonObj = new JSONObject(FSStatus);
                        String userData = jsonObj.getString("Version");
                        JSONObject jsonObject = new JSONObject(userData);

                        String sdk_version = jsonObject.getString("sdk_version");
                        String mac_address = jsonObject.getString("mac_address");
                        iot_version = jsonObject.getString("iot_version");


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //Skip in offline mode
                    if (cd.isConnectingToInternet() && AppConstants.AUTH_CALL_SUCCESS && AppConstants.NETWORK_STRENGTH){
                        //Current transaction is online
                        //getCMDLast10Txn(); //temp comment
                        String resp_value = getCMDLastSingleTXn();
                        if (resp_value == null || resp_value.isEmpty()){
                            GetLastTransaction();
                        }else{
                            System.out.println("resp_value"+resp_value);
                            String[] raw_string = resp_value.trim().split("-");
                            String txnid = raw_string[0];
                            String count = raw_string[1];

                            if (!txnid.equals("-1") && !txnid.equals("99999999")){
                                SaveLastTransactionToServer(txnid,count);
                            }
                        }

                    }else{
                        //Current transaction is offline dont save
                    }

                    new CommandsGET_RelayResp().execute(URL_RELAY);

                    //Info command else commented
                } else {

                    count_InfoCmd = count_InfoCmd + 1;

                    if (count_InfoCmd > 1) {

                        //unable to start (start never appeared): Potential Wifi Connection Issue = 6
                        UpdateDiffStatusMessages("6");

                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Link is unavailable infoCmd");
                        AppConstants.colorToastBigFont(DisplayMeterActivity.this, " Link is unavailable", Color.RED);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                        AppConstants.ClearEdittextFielsOnBack(DisplayMeterActivity.this); //Clear EditText on move to welcome activity.
                        Intent intent = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } else {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Link Unavailable infoCmd Retry attempt: " + count_InfoCmd);
                        AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Link Unavailable Retry attempt" + count_InfoCmd, Color.RED);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                        getListOfConnectedDevice();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                CompleteTasksbeforeStartbuttonClick(); //retry
                            }
                        }, 2000);

                    }

                }


            } catch (Exception e) {
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " CommandsGET_Info post " +e);
                System.out.println(e);
            }

        }
    }

    public class CommandsGET_TxnId extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();

        }


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " CommandsGET_TxnId doInBackground " +e);
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String LastTXNid) {


            try {

                pd.dismiss();

                System.out.println("LastTXNid;;;" + LastTXNid);
                System.out.println("OfflineLastTransactionID_DisplayMeterAct" + LastTXNid);

                if (LastTXNid.equalsIgnoreCase("99999999")){
                    System.out.println("Offline last transaction not saved in sqlite OffLastTXNid:"+LastTXNid);
                }else{
                    new CommandsGET_Record10().execute(URL_RECORD10_PULSAR, LastTXNid);
                }


            } catch (Exception e) {
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " CommandsGET_TxnId post " +e);
                System.out.println(e);
            }

        }
    }

    public class CommandsGET_Record10 extends AsyncTask<String, Void, String> {

        public String LastTXNid = "";


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();

        }


        protected String doInBackground(String... param) {

            String resp = "";

            try {
                LastTXNid = param[1];

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String respp) {


            try {
                pd.dismiss();

                System.out.println(respp);

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  LAST TRANS RawData " + " LastTXNid" + LastTXNid + "Resp " + respp);

                if (LastTXNid.equals("-1")) {
                    System.out.println(LastTXNid);
                } else {

                    if (respp.contains("quantity_10_record")) {
                        JSONObject jsonObject = new JSONObject(respp);
                        JSONObject joPulsarStat = jsonObject.getJSONObject("quantity_10_record");
                        int Initialcount = Integer.parseInt(joPulsarStat.getString("1:"));
                        String counts = "";
                        if (Initialcount > 0) {
                            counts = String.valueOf(Initialcount);
                        } else {
                            counts = String.valueOf(Initialcount);
                        }

                        Pulses = Integer.parseInt(counts);
                        double lastCnt = Double.parseDouble(counts);
                        double Lastqty = lastCnt / numPulseRatio; //convert to gallons
                        Lastqty = AppConstants.roundNumber(Lastqty, 2);

                        //-----------------------------------------------
                        try {

                            TrazComp authEntityClass = new TrazComp();
                            authEntityClass.TransactionId = LastTXNid;
                            authEntityClass.FuelQuantity = Lastqty;
                            authEntityClass.Pulses = Pulses;
                            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Last Transaction--";
                            authEntityClass.TransactionFrom = "A";
                            authEntityClass.IsFuelingStop = IsFuelingStop;
                            authEntityClass.IsLastTransaction = IsLastTransaction;

                            Gson gson = new Gson();
                            String jsonData = gson.toJson(authEntityClass);

                            System.out.println("TrazComp......" + jsonData);
                            String AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE;
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " LastTXNid:" + LastTXNid + " Qty:" + Lastqty + " Pulses" + Pulses + " AppInfo" + AppInfo);
                            //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  LAST TRANS jsonData " + jsonData);

                            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

                            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "TransactionComplete");

                            HashMap<String, String> imap = new HashMap<>();
                            imap.put("jsonData", jsonData);
                            imap.put("authString", authString);

                            boolean isInsert = true;
                            ArrayList<HashMap<String, String>> alltranz = controller.getAllTransaction();
                            if (alltranz != null && alltranz.size() > 0) {

                                for (int i = 0; i < alltranz.size(); i++) {

                                    if (jsonData.equalsIgnoreCase(alltranz.get(i).get("jsonData")) && authString.equalsIgnoreCase(alltranz.get(i).get("authString"))) {
                                        isInsert = false;
                                        break;
                                    }
                                }
                            }


                            if (isInsert && Lastqty > 0) {
                                controller.insertTransactions(imap);

                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "  LAST TRANS SAVED in sqlite");
                            }


                        } catch (Exception ex) {

                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  LAST TRANS Exception " + ex.getMessage());
                        }


                    }
                }


            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class CommandsGET_RelayResp extends AsyncTask<String, Void, String> {

        public String resp = "";


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();

        }


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String Relay_result) {


            try {

                pd.dismiss();

                System.out.println(Relay_result);


                if (Relay_result.trim().startsWith("{") && Relay_result.trim().contains("relay_response")) {

                    try {

                        JSONObject jsonObj = new JSONObject(Relay_result);
                        String userData = jsonObj.getString("relay_response");
                        JSONObject jsonObject = new JSONObject(userData);
                        String status = jsonObject.getString("status");

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Relay_Status: " + status);

                        //IF relay status zero go back to dashboard
                        if (status.equalsIgnoreCase("1")) {

                            //unable to start (start never appeared): Potential Wifi Connection Issue = 6
                            UpdateDiffStatusMessages("6");
                            AppConstants.colorToastBigFont(DisplayMeterActivity.this, "The link is busy, please try after some time.", Color.RED);
                            Istimeout_Sec = true;
                            ResetTimeoutDisplayMeterScreen();
                            AppConstants.ClearEdittextFielsOnBack(DisplayMeterActivity.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);


                        } else {

                            //We use pumppff time insted pulseroff time
                            long pulsar_off_time = (stopAutoFuelSeconds * 1000) + 3000;
                            new CommandsPOST().execute(URL_SET_PULSAR, "{\"pulsar_status\":{\"pulsar_off_time\":" + pulsar_off_time + "}}");

                            if (AppConstants.FS_selected.equalsIgnoreCase("0")) {

                                //Store Hose ID and Firmware version in sharedpreferance
                                SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("hoseid_fs1", AppConstants.UP_HoseId_fs1);
                                editor.putString("fsversion_fs1", iot_version);
                                editor.commit();


                                //IF upgrade firmware true check below
                                if (AppConstants.UP_Upgrade) {
                                    CheckForUpdateFirmware(AppConstants.UP_HoseId_fs1, iot_version, AppConstants.FS_selected);
                                } else {
                                    BtnStartStateChange(true);
                                }


                            } else if (AppConstants.FS_selected.equalsIgnoreCase("1")) {

                                //Store Hose ID and Firmware version in sharedpreferance
                                SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("hoseid_fs2", AppConstants.UP_HoseId_fs2);
                                editor.putString("fsversion_fs2", iot_version);
                                editor.commit();

                                //IF upgrade firmware true check below
                                if (AppConstants.UP_Upgrade) {
                                    CheckForUpdateFirmware(AppConstants.UP_HoseId_fs2, iot_version, AppConstants.FS_selected);
                                } else {
                                    BtnStartStateChange(true);
                                }

                            } else if (AppConstants.FS_selected.equalsIgnoreCase("2")) {

                                //Store Hose ID and Firmware version in sharedpreferance
                                SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("hoseid_fs3", AppConstants.UP_HoseId_fs3);
                                editor.putString("fsversion_fs3", iot_version);
                                editor.commit();

                                //IF upgrade firmware true check below
                                if (AppConstants.UP_Upgrade) {
                                    CheckForUpdateFirmware(AppConstants.UP_HoseId_fs3, iot_version, AppConstants.FS_selected);
                                } else {
                                    BtnStartStateChange(true);
                                }
                            } else if (AppConstants.FS_selected.equalsIgnoreCase("3")) {

                                //Store Hose ID and Firmware version in sharedpreferance
                                SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("hoseid_fs4", AppConstants.UP_HoseId_fs4);
                                editor.putString("fsversion_fs4", iot_version);
                                editor.commit();

                                //IF upgrade firmware true check below
                                if (AppConstants.UP_Upgrade) {
                                    CheckForUpdateFirmware(AppConstants.UP_HoseId_fs4, iot_version, AppConstants.FS_selected);
                                } else {
                                    BtnStartStateChange(true);
                                }

                            }

                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " CommandsGET_RelayResp OnPost " +e);
                    }

                } else {

                    count_relayCmd = count_relayCmd + 1;

                    if (count_relayCmd > 1) {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Link Unavailable relay");
                        AppConstants.colorToastBigFont(DisplayMeterActivity.this, " Link is unavailable", Color.RED);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                        AppConstants.ClearEdittextFielsOnBack(DisplayMeterActivity.this); //Clear EditText on move to welcome activity.
                        BackgroundServiceKeepDataTransferAlive.IstoggleRequired_DA = true;
                        Intent intent = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } else {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Link Unavailable relay Retry attempt: " + count_InfoCmd);
                        AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Link is unavailable Retry attempt" + count_relayCmd, Color.RED);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                        new Handler().postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                CompleteTasksbeforeStartbuttonClick(); //retry
                            }
                        }, 2000);

                    }

                }


            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class CommandsGET_CmdTxt10 extends AsyncTask<String, Void, String> {

        public String resp = "";


        @Override
        protected void onPreExecute() {

        }

        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                System.out.println("cmtxtnid10:"+result);


                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  cmtxtnid10~ :" + resp);

                arrayList = new ArrayList<>();

                if (resp.contains("cmtxtnid_10_record")) {
                    JSONObject jobj = new JSONObject(resp);
                    JSONObject cm = jobj.getJSONObject("cmtxtnid_10_record");
                    String txn1 = cm.getString("1:TXTNINFO:");
                    String txn2 = cm.getString("2:TXTNINFO:");
                    String txn3 = cm.getString("3:TXTNINFO:");
                    String txn4 = cm.getString("4:TXTNINFO:");
                    String txn5 = cm.getString("5:TXTNINFO:");
                    String txn6 = cm.getString("6:TXTNINFO:");
                    String txn7 = cm.getString("7:TXTNINFO:");
                    String txn8 = cm.getString("8:TXTNINFO:");
                    String txn9 = cm.getString("9:TXTNINFO:");
                    String txn10 = cm.getString("10:TXTNINFO:");


                    splitTrIdQty(txn1);
                    splitTrIdQty(txn2);
                    splitTrIdQty(txn3);
                    splitTrIdQty(txn4);
                    splitTrIdQty(txn5);
                    splitTrIdQty(txn6);
                    splitTrIdQty(txn7);
                    splitTrIdQty(txn8);
                    splitTrIdQty(txn9);
                    splitTrIdQty(txn10);

                    Gson gs = new Gson();
                    EntityCmd10Txn ety = new EntityCmd10Txn();
                    ety.cmtxtnid_10_record = arrayList;

                    String json10txn = gs.toJson(ety);
                    System.out.println("cmtxtnid_10_record----" + json10txn);
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  txn json " + json10txn);


                    System.out.println("storeCmtxtnid_10_record" + WelcomeActivity.SelectedItemPosFor10Txn);


                    SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences("storeCmtxtnid_10_record" + WelcomeActivity.SelectedItemPosFor10Txn, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("json", json10txn);
                    editor.apply();

                    WelcomeActivity.SelectedItemPosFor10Txn = -1;
                }

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class CommandsGET_CmdTxt10_Single extends AsyncTask<String, Void, String> {

        public String resp = "";


        @Override
        protected void onPreExecute() {

        }

        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @TargetApi(21)
    public static void setHttpTransportToDefaultNetwork(Context ctx) {

        ConnectivityManager connection_manager = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);

        connection_manager.bindProcessToNetwork(null);

    }

    private void SaveLastTransactionToServer(String txnid, String counts){

        try {

                Pulses = Integer.parseInt(counts);
                double lastCnt = Double.parseDouble(counts);
                double Lastqty = lastCnt / numPulseRatio; //convert to gallons
                Lastqty = AppConstants.roundNumber(Lastqty, 2);

                //-----------------------------------------------
                    TrazComp authEntityClass = new TrazComp();
                    authEntityClass.TransactionId = txnid;
                    authEntityClass.FuelQuantity = Lastqty;
                    authEntityClass.Pulses = Pulses;
                    authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Last Transaction--";
                    authEntityClass.TransactionFrom = "A";
                    authEntityClass.IsFuelingStop = IsFuelingStop;
                    authEntityClass.IsLastTransaction = IsLastTransaction;

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(authEntityClass);

                    System.out.println("TrazComp......" + jsonData);
                    String AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "SaveLastTransactionToServer LastTXNid:" + txnid + " Qty:" + Lastqty + " Pulses" + Pulses + " AppInfo" + AppInfo);
                    //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  LAST TRANS jsonData " + jsonData);

                    String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "TransactionComplete");

                    HashMap<String, String> imap = new HashMap<>();
                    imap.put("jsonData", jsonData);
                    imap.put("authString", authString);

                    boolean isInsert = true;
                    ArrayList<HashMap<String, String>> alltranz = controller.getAllTransaction();
                    if (alltranz != null && alltranz.size() > 0) {

                        for (int i = 0; i < alltranz.size(); i++) {

                            if (jsonData.equalsIgnoreCase(alltranz.get(i).get("jsonData")) && authString.equalsIgnoreCase(alltranz.get(i).get("authString"))) {
                                isInsert = false;
                                break;
                            }
                        }
                    }


                    if (isInsert && Lastqty > 0) {
                        controller.insertTransactions(imap);

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  LAST TRANS SAVED in sqlite");
                    }

                } catch (Exception ex) {

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "SaveLastTransactionToServer  LAST TRANS Exception " + ex.getMessage());
                }


            }

    private void CancelTimerScreenOut(){

        for (int i = 0; i < DisplayMScreeTimerlist.size(); i++) {
            DisplayMScreeTimerlist.get(i).cancel();
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

    private void UpdateDiffStatusMessages(String s){

        try {
            String TransactionId_US = null;
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            if (AppConstants.FS_selected.equalsIgnoreCase("0")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS1", "");
            } else if (AppConstants.FS_selected.equalsIgnoreCase("1")) {
                TransactionId_US = sharedPref.getString("TransactionId", "");
            } else if (AppConstants.FS_selected.equalsIgnoreCase("2")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS3", "");
            } else if (AppConstants.FS_selected.equalsIgnoreCase("3")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS4", "");
            } else {
                //Something went wrong in link selection
                Log.i(TAG,"Something went wrong in link selection");
            }

            if (TransactionId_US != null && !TransactionId_US.isEmpty() && cd.isConnectingToInternet()) {
                Log.i(TAG,"UpdateDiffStatusMessages sent: "+s+" TransactionId:"+TransactionId_US);
                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                CommonUtils.UpgradeTransactionStatusRetroFit(TransactionId_US, s, this);
            }

        }catch (Exception e){
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "UpdateDiffStatusMessages Ex:"+e.toString());
        }

    }
}
