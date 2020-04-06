package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.enity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.enity.TankMonitorEntity;
import com.TrakEngineering.FluidSecureHubTest.enity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.enity.UpdateTransactionStatusClass;
import com.TrakEngineering.FluidSecureHubTest.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.EntityOffTranz;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.internal.zzid.runOnUiThread;


/**
 * Created by VASP on 7/24/2017.
 */

public class BackgroundService_AP_PIPE extends Service {


    private static final String TAG = "BS_AP_PI";
    String EMPTY_Val = "";
    ConnectionDetector cd = new ConnectionDetector(BackgroundService_AP_PIPE.this);
    private int AttemptCount = 0;
    private int RelayAttemptCount = 0;
    private String CurrTxnMode = "online",OffLastTXNid = "0";

    //String HTTP_URL = "http://192.168.43.140:80/";//for pipe
    //String HTTP_URL = "http://192.168.43.5:80/";//Other FS
    String HTTP_URL = "";

    String URL_INFO = HTTP_URL + "client?command=info";
    String URL_STATUS = HTTP_URL + "client?command=status";
    String URL_RECORD = HTTP_URL + "client?command=record10";

    String URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
    String URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

    String URL_WIFI = HTTP_URL + "config?command=wifi";
    String URL_RELAY = HTTP_URL + "config?command=relay";

    String URL_GET_USER = HTTP_URL + "upgrade?command=getuser";
    String URL_RESET = HTTP_URL + "upgrade?command=reset";
    String URL_FILE_UPLOAD = HTTP_URL + "upgrade?command=start";

    String URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
    String URL_SET_TXNID = HTTP_URL + "config?command=txtnid";

    String jsonRename;
    //{\"Request\":{\"Station\":{\"Connect_Station\":{\"ssid\":\"BQ_House\",\"password\":\"31415926\ ,\"station_connect_status\":1}}}}" http://192.168.4.1/config?command=wif
    String jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
    String jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
    String jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

    String jsonPulsar = "{\"pulsar_request\":{\"counter_set\":1}}";
    String jsonPulsarOff = "{\"pulsar_request\":{\"counter_set\":0}}";

    String URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";

    String URL_TDL_info = HTTP_URL + "tld?level=info";

    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();
    ArrayList<Integer> respCounter = new ArrayList<>();
    public static ArrayList<String> listOfConnectedIP_AP_PIPE = new ArrayList<String>();

    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private String vehicleNumber, odometerTenths = "0", dNumber = "", pNumber = "", oText = "", hNumber = "";
    String LinkName = "", OtherName, IsOtherRequire, OtherLabel, VehicleNumber, PrintDate, CompanyName, Location, PersonName, PrinterMacAddress, PrinterName, TransactionId, VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel, IsTLDCall, EnablePrinter,_OdoMeter,_Hours,PumpOnTime;

    int timeFirst = 60;
    Timer tFirst;
    TimerTask taskFirst;
    boolean stopTimer;
    boolean pulsarConnected = false;
    double minFuelLimit = 0, numPulseRatio = 0;
    String consoleString = "", outputQuantity = "0";
    double CurrentLat = 0, CurrentLng = 0;
    GoogleApiClient mGoogleApiClient;
    long stopAutoFuelSeconds = 0;
    boolean isTransactionComp = false;
    boolean ongoingStatusSend = true;
    double fillqty = 0;
    int CNT_LAST = 0;
    Integer Pulses = 0;
    long sqliteID = 0;

    double Lastfillqty = 0;
    ConnectivityManager connection_manager;
    String printReceipt = "", IsFuelingStop = "0", IsLastTransaction = "0";
    DBController controller = new DBController(BackgroundService_AP_PIPE.this);
    OffDBController offcontroller = new OffDBController(BackgroundService_AP_PIPE.this);

    public long sqlite_id = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            super.onStart(intent, startId);

            //LinkName = AppConstants.CURRENT_SELECTED_SSID;

            if (LinkName == null || LinkName.isEmpty()) {
                try {
                    LinkName = AppConstants.DetailsServerSSIDList.get(0).get("WifiSSId");
                } catch (Exception e) {
                    if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG+ "Something went wrong please check Link name Ex:"+e.toString());
                    e.printStackTrace();
                }
            }else {
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG+ "Please check Link name:"+LinkName);
            }

            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.d("Service", "null");
                this.stopSelf();
                Constants.FS_1STATUS = "FREE";
                clearEditTextFields();
                if (!Constants.BusyVehicleNumberList.equals(null)) {
                    Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
                }


            } else {

                stopTimer = true;
                AttemptCount = 0;
                IsFuelingStop = "0";
                IsLastTransaction = "0";


                sqlite_id = (long) extras.get("sqlite_id");

                HTTP_URL = (String) extras.get("HTTP_URL");

                URL_INFO = HTTP_URL + "client?command=info";
                URL_STATUS = HTTP_URL + "client?command=status";
                URL_RECORD = HTTP_URL + "client?command=record10";

                URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
                URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

                URL_WIFI = HTTP_URL + "config?command=wifi";
                URL_RELAY = HTTP_URL + "config?command=relay";

                URL_GET_USER = HTTP_URL + "upgrade?command=getuser";
                URL_RESET = HTTP_URL + "upgrade?command=reset";
                URL_FILE_UPLOAD = HTTP_URL + "upgrade?command=start";

                URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
                URL_SET_TXNID = HTTP_URL + "config?command=txtnid";

                URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";

                URL_TDL_info = HTTP_URL + "tld?level=info";


                jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
                jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
                jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

                jsonPulsar = "{\"pulsar_request\":{\"counter_set\":1}}";
                jsonPulsarOff = "{\"pulsar_request\":{\"counter_set\":0}}";

                jsonRename = "{\"Request\":{\"SoftAP\":{\"Connect_SoftAP\":{\"authmode\":\"WPAPSK/WPA2PSK\",\"channel\":6,\"ssid\":\"" + AppConstants.REPLACEBLE_WIFI_NAME_FS1 + "\",\"password\":\"123456789\"}}}}";

                System.out.println("BackgroundService is on. AP_FS_PIPE" + HTTP_URL);
                Constants.FS_1STATUS = "BUSY";
                Constants.BusyVehicleNumberList.add(Constants.AccVehicleNumber_FS1);

                if (cd.isConnectingToInternet() && AppConstants.AUTH_CALL_SUCCESS ){
                    CurrTxnMode = "online";
                }else{

                    if (AppConstants.AUTH_CALL_SUCCESS){
                        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
                        TransactionId = sharedPref.getString("TransactionId_FS1", "");
                        OffLastTXNid = TransactionId;//Set transaction id to offline
                    }
                    CurrTxnMode = "offline";
                }



                SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
                TransactionId = sharedPref.getString("TransactionId_FS1", "");
                VehicleId = sharedPref.getString("VehicleId_FS1", "");
                PhoneNumber = sharedPref.getString("PhoneNumber_FS1", "");
                PersonId = sharedPref.getString("PersonId_FS1", "");
                PulseRatio = sharedPref.getString("PulseRatio_FS1", "1");
                MinLimit = sharedPref.getString("MinLimit_FS1", "0");
                FuelTypeId = sharedPref.getString("FuelTypeId_FS1", "");
                ServerDate = sharedPref.getString("ServerDate_FS1", "");
                IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS1", "0");
                IsTLDCall = sharedPref.getString("IsTLDCall_FS1", "False");
                EnablePrinter = sharedPref.getString("EnablePrinter_FS1", "False");
                PumpOnTime = sharedPref.getString("PumpOnTime_FS1", "0");


                if (cd.isConnectingToInternet() && CurrTxnMode.equalsIgnoreCase("online")) {

                    listOfConnectedIP_AP_PIPE.clear();
                    ListConnectedHotspotIP_AP_PIPEAsyncCall();


                    //settransactionID to FSUNIT
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            new CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + TransactionId + "}");

                        }
                    }, 1500);


                    //Create and Empty transactiin into SQLite DB
                    HashMap<String, String> mapsts = new HashMap<>();
                    mapsts.put("transId", TransactionId);
                    mapsts.put("transStatus", "1");

                    controller.insertTransStatus(mapsts);
                    ////////////////////////////////////////////
                    String userEmail = CommonUtils.getCustomerDetails_backgroundService_PIPE(BackgroundService_AP_PIPE.this).PersonEmail;
                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_AP_PIPE.this) + ":" + userEmail + ":" + "TransactionComplete");

                    HashMap<String, String> imap = new HashMap<>();
                    imap.put("jsonData", "");
                    imap.put("authString", authString);

                    sqliteID = controller.insertTransactions(imap);
                    CommonUtils.AddRemovecurrentTransactionList(true, TransactionId);//Add transaction Id to list
                    //////////////////////////////////////////////////////////////

                    //=====================UpgradeTransaction Status ==1================
                    cd = new ConnectionDetector(BackgroundService_AP_PIPE.this);
                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
                        try {
                            /*UpdateTransactionStatusClass authEntity = new UpdateTransactionStatusClass();
                            authEntity.TransactionId = TransactionId;
                            authEntity.Status = "1";
                            authEntity.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP_PIPE.this);*/

                            //---------------------------------------------------
                            if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                            CommonUtils.UpgradeTransactionStatusRetroFit(TransactionId,"1",BackgroundService_AP_PIPE.this);
                            //---------------------------------------------------

                            /*BackgroundService_AP_PIPE.UpdateAsynTask authTestAsynTask = new BackgroundService_AP_PIPE.UpdateAsynTask(authEntity);
                            authTestAsynTask.execute();
                            authTestAsynTask.get();

                            String serverRes = authTestAsynTask.response;

                            if (serverRes != null) {
                            }
*/

                        } catch (Exception e) {
                            e.printStackTrace();
                            if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG+ " UpgradeTransactionStatusRetroFit Ex:"+e.toString());
                        }

                    } else {

                        //AppConstants.colorToast(BackgroundService_AP_PIPE.this, "Please check Internet Connection.", Color.RED);
                        UpdateTransactionStatusClass authEntity = new UpdateTransactionStatusClass();
                        authEntity.TransactionId = TransactionId;
                        authEntity.Status = "1";
                        authEntity.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP_PIPE.this);


                        Gson gson1 = new Gson();
                        String jsonData1 = gson1.toJson(authEntity);

                        System.out.println("AP_FS_PIPE UpdatetransactionData......" + jsonData1);

                        String userEmail1 = CommonUtils.getCustomerDetails_backgroundService_PIPE(this).PersonEmail;
                        String authString1 = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail1 + ":" + "UpgradeTransactionStatus");

                        HashMap<String, String> imapStatus = new HashMap<>();
                        imapStatus.put("jsonData", jsonData1);
                        imapStatus.put("authString", authString1);

                        controller.insertIntoUpdateTranStatus(imapStatus);

                    }

                    //=========================UpgradeTransactionStatus Ends===============

                    minFuelLimit = Double.parseDouble(MinLimit);

                    numPulseRatio = Double.parseDouble(PulseRatio);

                    stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

                    if (AppConstants.EnableFA) {
                        stopAutoFuelSeconds=stopAutoFuelSeconds*3;
                    }


                } else {

                    offlineLogic1();
                }


                System.out.println("iiiiii" + IntervalToStopFuel);
                System.out.println("minFuelLimit" + minFuelLimit);
                System.out.println("getDeviceName" + minFuelLimit);

                String mobDevName = AppConstants.getDeviceName().toLowerCase();
                System.out.println("oooooooooo" + mobDevName);


            }
        } catch (NullPointerException e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  onStartCommand Execption " + e);
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }


        //GetLatLng();
        //Commented cmd for gatehub -->jsonPulsarOff,jsonPulsar,URL_RELAY
        /*new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);

        //Relay On cmd
        new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsar);//pulsar on swipe


        new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {


                        new CommandsGET().execute(URL_RELAY);//Not required

                        // new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);

                    }
                }, 1000);*/


        //Pulsar On
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //Call Relay On Function
                String IsRelayOnCheck = RelayOnThreeAttempts();
                if (IsRelayOnCheck != null && !IsRelayOnCheck.equalsIgnoreCase("")) {

                    try {

                        JSONObject jsonObject = new JSONObject(IsRelayOnCheck);
                        String relay_status1 = null;
                        relay_status1 = jsonObject.getString("relay_response");
                        if (relay_status1.equalsIgnoreCase("{\"status\":1}")) {
                            startQuantityInterval();
                        } else {
                            ExitBackgroundService();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    ExitBackgroundService();
                }

            }
        }, 1500);

        // return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }


    private String RelayOnThreeAttempts() {

        String IsRelayOn = "";
        try {
            IsRelayOn = new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get();//Relay ON First Attempt

            JSONObject jsonObject = new JSONObject(IsRelayOn);
            String relay_status1 = jsonObject.getString("relay_response");

            if (relay_status1.equalsIgnoreCase("{\"status\":1}")) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Relay On attempt 1");

            } else {
                Thread.sleep(1000);
                IsRelayOn = new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get();//Relay ON Second attempt

                JSONObject jsonObjectSite = new JSONObject(IsRelayOn);
                String relay_status2 = jsonObjectSite.getString("relay_response");
                if (relay_status2.equalsIgnoreCase("{\"status\":1}")) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Relay On attempt 2");
                } else {
                    Thread.sleep(1000);
                    new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get();//Relay ON Third attempt
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Relay On attempt 3");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  RelayOnThreeAttempts. --Exception " + e);
        }
        return IsRelayOn;
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

    public void GetLatLng() {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {

            CurrentLat = mLastLocation.getLatitude();
            CurrentLng = mLastLocation.getLongitude();

            System.out.println("CCCrrr" + CurrentLat);
            System.out.println("CCCrrr" + CurrentLng);

        }
    }

    public void stopFirstTimer(boolean flag) {
        if (flag) {
            tFirst.cancel();
            tFirst.purge();
        } else {
            tFirst.cancel();
            tFirst.purge();

            WelcomeActivity.SelectedItemPos = -1;
            AppConstants.BUSY_STATUS = true;

            Intent i = new Intent(this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {

            try {

                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .header("Accept-Encoding", "identity")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsPOST doInBackground Execption " + e);
                stopSelf();
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                System.out.println("APFS_PIPE OUTPUT" + result);

            } catch (Exception e) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsPOST doInBackground Execption " + e);
                System.out.println(e);
                stopSelf();
            }

        }
    }

    public class CommandsGET extends AsyncTask<String, Void, String> {

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
                //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  CommandsGET doInBackground Execption " + e);
                stopSelf();
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                System.out.println("APFS_PIPE OUTPUT" + result);

            } catch (Exception e) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsGET onPostExecute Execption " + e);
                System.out.println(e);
                stopSelf();
            }

        }
    }

    public class TCPCommandsGET extends AsyncTask<String, Void, String> {

        String response = "";

        @SuppressLint("LongLogTag")
        @Override
        protected String doInBackground(String... param) {



            // Log.e(TAG, "~~~~~TCP for strt~~~~~");
            //http://192.168.43.67:80/tld?level=info
            String Command = param[1];
            String serverip1 = param[0].replace("http://","");
            String SERVER_IP = serverip1.replace(":80/","").trim();
            String strcmd = "GET /"+Command+" HTTP/1.1\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: \r\nHost: 192.168.4.1\r\nConnection: Keep-Alive\r\nAccept-Encoding: gzip\r\nUser-Agent: okhttp/3.6.0\r\n\r\n";


            try {
                String host = SERVER_IP;//"192.168.43.210";//url.getHost();
                int port = 80;// SERVER_PORT;

                // Open a TCP connection
                Socket socket = new Socket(host, port);
                // Send the request over the socket
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.print(strcmd);
                writer.flush();
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                StringBuilder InfoRespo = new StringBuilder();
                String next_record = null;
                while ((next_record = reader.readLine()) != null) {
                    System.out.println("next_record"+next_record);
                    InfoRespo.append(next_record);

                }
                //Log.i(TAG, " InfoRespo: " + InfoRespo);
                response = InfoRespo.toString();
                socket.close();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return response;
        }



        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String res) {

            //Log.e(TAG, "~~~~~TCP for end~~~~~");
            Log.i(TAG, "Socket response" + res);
            if (!res.contains("Version")){
                //new UDPClientTask().execute(SERVER_IP);
            }

        }

    }

    public class TCPCommandsPOST extends AsyncTask<String, Void, String> {

        String response = "";

        @SuppressLint("LongLogTag")
        @Override
        protected String doInBackground(String... param) {



            // Log.e(TAG, "~~~~~TCP for strt~~~~~");
            //http://192.168.43.67:80/tld?level=info
            String Command = param[1];
            String serverip1 = param[0].replace("http://","");
            String SERVER_IP = serverip1.replace(":80/","").trim();
            String strcmd = "POST /"+Command+" HTTP/1.1\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: \r\nHost: 192.168.4.1\r\nConnection: Keep-Alive\r\nAccept-Encoding: gzip\r\nUser-Agent: okhttp/3.6.0\r\n\r\n";


            try {
                String host = SERVER_IP;//"192.168.43.210";//url.getHost();
                int port = 80;// SERVER_PORT;

                // Open a TCP connection
                Socket socket = new Socket(host, port);
                // Send the request over the socket
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.print(strcmd);
                writer.flush();
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                StringBuilder InfoRespo = new StringBuilder();
                String next_record = null;
                while ((next_record = reader.readLine()) != null) {
                    System.out.println("next_record"+next_record);
                    InfoRespo.append(next_record);

                }
                //Log.i(TAG, " InfoRespo: " + InfoRespo);
                response = InfoRespo.toString();
                socket.close();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex){
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " TCPCommandsPOST DoInbackground"+ex.getMessage());
            }
            return response;
        }



        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String res) {

            //Log.e(TAG, "~~~~~TCP for end~~~~~");
            Log.i(TAG, "Socket response" + res);


        }

    }

    public void startQuantityInterval() {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                try {

                    if (stopTimer) {

                        if (IsFsConnected(HTTP_URL)) {
                            AttemptCount = 0;
                            //FS link is connected
                            //Synchronous okhttp call
                            //new GETPulsarQuantity().execute(URL_GET_PULSAR);

                            //Asynchronous okhttp call
                            GETPulsarQuantityAsyncCall(URL_GET_PULSAR);

                        } else {

                            if (AttemptCount > 2) {
                                //FS Link DisConnected
                                System.out.println("FS Link not connected" + listOfConnectedIP_AP_PIPE);
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "  Link not connected");
                                stopTimer = false;
                                new BackgroundService_AP_PIPE.CommandsPOST().execute(URL_RELAY, jsonRelayOff);
                                Constants.FS_1STATUS = "FREE";
                                clearEditTextFields();
//                              BackgroundService_AP.this.stopSelf();

                            } else {

                                listOfConnectedIP_AP_PIPE.clear();
                                ListConnectedHotspotIP_AP_PIPEAsyncCall();

                                Thread.sleep(2000);
                                System.out.println("FS Link not connected ~~AttemptCount:" + AttemptCount);
                                AttemptCount = AttemptCount + 1;
                            }
                        }
                    }

                } catch (Exception e) {
                    System.out.println(e);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  startQuantityInterval Execption " + e);
                }

            }
        }, 0, 2000);


    }

    public boolean IsFsConnected(String toMatchString) {

        for (String HttpAddress : listOfConnectedIP_AP_PIPE) {
            if (HttpAddress.contains(toMatchString))
                return true;
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
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GETPulsarQuantity doInBackground Execption~ " + e);
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            System.out.println("Result" + result);

            try {

                if (result.equalsIgnoreCase("")) {
                    respCounter.add(0);
                    System.out.println("FR:0");
                } else {
                    respCounter.add(1);
                    System.out.println("FR:1");
                }

                if (getPulsarResponseEmptyFor3times()) {
                    // btnStop.performClick();
                    stopButtonFunctionality();

                } else {

                    System.out.println("OUTPUT" + result);

                    if (stopTimer)
                        pulsarQtyLogic(result);
                }


            } catch (Exception e) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GETPulsarQuantity onPostExecute Execption " + e);
                System.out.println(e);
            }

        }
    }

    public void GETPulsarQuantityAsyncCall(String URL_GET_PULSAR) {
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setConnectTimeout(AppConstants.CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS);
        httpClient.setReadTimeout(AppConstants.READ_TIMEOUT_SEC, TimeUnit.SECONDS);
        httpClient.setWriteTimeout(AppConstants.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS);

        Request request = new Request.Builder()
                .url(URL_GET_PULSAR)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response using async okhttp call");
                //Temp code..
                CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list
                System.out.println("FS Link not connected" + listOfConnectedIP_AP_PIPE);
                stopTimer = false;
                new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
                Constants.FS_1STATUS = "FREE";
                clearEditTextFields();
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {

                    String result = responseBody.string();
                    System.out.println("Result" + result);
                    System.out.println("Get pulsar---------- FS PIPE ~~~onPostExecute~~~" + result);

                    try {

                        if (result.equalsIgnoreCase("")) {
                            respCounter.add(0);
                            System.out.println("FR:0");
                        } else {
                            respCounter.add(1);
                            System.out.println("FR:1");
                        }

                        if (getPulsarResponseEmptyFor3times()) {
                            // btnStop.performClick();
                            stopButtonFunctionality();

                        } else {

                            System.out.println("OUTPUT" + result);

                            if (stopTimer)
                                pulsarQtyLogic(result);
                        }


                    } catch (Exception e) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  ETPulsarQuantity onPostExecute Execption " + e);
                        System.out.println(e);
                    }


                }

            }

        });
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

                        this.stopSelf();

                        if (!Constants.BusyVehicleNumberList.equals(null)) {
                            Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
                        }

                        IsFuelingStop = "1";
                        System.out.println("APFS_PIPE Auto Stop! Pulsar disconnected");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Link:" + LinkName + " Auto Stop! Pulsar disconnected");
                        // AppConstants.colorToastBigFont(this, AppConstants.FS1_CONNECTED_SSID+" Auto Stop!\n\nPulsar disconnected", Color.BLUE);
                        stopButtonFunctionality(); //temp on #574 Server Update
                    }
                }

                //To avoid accepting Lower Quantity
                int CNT_current = Integer.parseInt(counts);

                //in progress (transaction recently started, no new information): Transaction ongoing = 8  --non zero qty
                if (CNT_current > 0 && ongoingStatusSend){
                    ongoingStatusSend = false;
                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                    CommonUtils.UpgradeTransactionStatusRetroFit(TransactionId,"8",BackgroundService_AP_PIPE.this);
                }

                if (CNT_LAST <= CNT_current) {
                    CNT_LAST = CNT_current;

                    convertCountToQuantity(counts);
                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Count recorded from the link: " + counts);
                }


                if (!pulsar_secure_status.trim().isEmpty()) {
                    secure_status = Integer.parseInt(pulsar_secure_status);

                    if (secure_status == 0) {
                        //linearTimer.setVisibility(View.GONE);
                        //tvCountDownTimer.setText("-");

                    } else if (secure_status == 1) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("5");

                    } else if (secure_status == 2) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("4");

                    } else if (secure_status == 3) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("3");

                    } else if (secure_status == 4) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("2");

                    } else if (secure_status >= 5) {
                        //linearTimer.setVisibility(View.GONE);
                        //tvCountDownTimer.setText("1");

                        this.stopSelf();

                        if (!Constants.BusyVehicleNumberList.equals(null)) {
                            Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
                        }

                        IsFuelingStop = "1";
                        System.out.println("APFS_PIPE Auto Stop! Count down timer completed");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Link:" + LinkName + " Auto Stop! Count down timer completed");
                        AppConstants.colorToastBigFont(this, AppConstants.FS1_CONNECTED_SSID + " Auto Stop!\n\nCount down timer completed.", Color.BLUE);
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

                            this.stopSelf();

                            if (!Constants.BusyVehicleNumberList.equals(null)) {
                                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
                            }

                            IsFuelingStop = "1";
                            System.out.println("APFS_PIPE Auto Stop!You reached MAX fuel limit.");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  Link:" + LinkName + " Auto Stop!You reached MAX fuel limit.");
                            //AppConstants.colorToastBigFont(this, "Auto Stop!\n\nYou reached MAX fuel limit.", Color.BLUE);
                            stopButtonFunctionality();
                        }
                    }
                } catch (Exception e) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  quantity reach max limit1 Execption " + e);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  quantity reach max limit2 Execption " + e);
        }
    }

    public void stopButtonFunctionality() {


        quantityRecords.clear();

        // btnStart.setVisibility(View.GONE);
        //btnStop.setVisibility(View.GONE);
        //btnFuelHistory.setVisibility(View.VISIBLE);
        consoleString = "";
        // tvConsole.setText("");

        //it stops pulsar logic------
        stopTimer = false;


        new BackgroundService_AP_PIPE.CommandsPOST().execute(URL_RELAY, jsonRelayOff);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String cntA = "0", cntB = "0", cntC = "0";

                            for (int i = 0; i < 3; i++) {

                                String result = new BackgroundService_AP_PIPE.GETFINALPulsar().execute(URL_GET_PULSAR).get();


                                if (result.contains("pulsar_status")) {

                                    JSONObject jsonObject = new JSONObject(result);
                                    JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                                    String counts = joPulsarStat.getString("counts");
                                    //String pulsar_status = joPulsarStat.getString("pulsar_status");
                                    //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                                    //To avoid accepting Lower Quantity
                                    int CNT_current = Integer.parseInt(counts);

                                    if (CNT_LAST <= CNT_current) {
                                        CNT_LAST = CNT_current;

                                        convertCountToQuantity(counts);
                                    } else {
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + "  Count recorded from the link: " + counts);
                                    }

                            /*
                            if (i == 0)
                                cntA = counts;
                            else if (i == 1)
                                cntB = counts;
                            else
                                cntC = counts;
                            */


                                    if (i == 2) {

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                finalLastStep();
                                            }
                                        }, 1000);


                                    }


                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  stopButtonFunctionality Execption " + e);
                        }
                    }
                }, 1000);

            }
        });


    }

    public void finalLastStep() {


        new BackgroundService_AP_PIPE.CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (AppConstants.NeedToRenameFS1) {

                    consoleString += "RENAME:\n" + jsonRename;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Link Rename true:" + jsonRename);
                    new BackgroundService_AP_PIPE.CommandsPOST().execute(URL_WIFI, jsonRename);

                }
            }

        }, 2500);

        long secondsTime = 3000;

        if (AppConstants.NeedToRenameFS1) {
            secondsTime = 5000;
        }

        if (AppConstants.UP_Upgrade_fs1) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

//                 String command = "upgrade?command=start";
//                 new TCPCommandsPOST().execute(HTTP_URL, command);

                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " URL_UPGRADE_START CMD");
                    new CommandsPOST().execute(URL_UPGRADE_START, "");

                    //Add 1.5 sec temporary delay  #863 Link Update.
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " 2 sec temporary delay  #863 Link Update");
                    }
                    //upgrade bin
                    String LocalPath = AppConstants.FOLDER_PATH + AppConstants.UP_Upgrade_File_name;
                    File f = new File(LocalPath);
                    if (f.exists()) {
                        new OkHttpFileUpload().execute(LocalPath, "application/binary");
                    } else {
                        Toast.makeText(getApplicationContext(), "File Not found " + LocalPath, Toast.LENGTH_LONG).show();
                    }
                }

            }, 3000);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //AppConstants.disconnectWiFi(DisplayMeterActivity.this);
                Constants.FS_1STATUS = "FREE";
                clearEditTextFields();
                GetDetails();
                if (!AppConstants.UP_Upgrade_fs1) {

                    TransactionCompleteFunction();
                }

            }

        }, secondsTime);
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
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GETFINALPulsar doInBackground Execption " + e);

            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                // tvConsole.setText(consoleString);

                System.out.println("APFS_PIPE OUTPUT" + result);


            } catch (Exception e) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GETFINALPulsar onPostExecute Execption " + e);
                System.out.println(e);
            }

        }

    }

    public void secondsTimeLogic(String currentDT) {

        try {

            if (quantityRecords.size() > 0) {

                Date nowDT = sdformat.parse(currentDT);
                Date d2 = sdformat.parse(quantityRecords.get(0).get("b"));

                long seconds = (nowDT.getTime() - d2.getTime()) / 1000;

                if (Pulses <= 0) {

                    try {
                        int pont = Integer.parseInt(PumpOnTime);
                        //temp log below
                        Log.w(TAG, "Temp log PumpOnTime:"+PumpOnTime);
                        if (AppConstants.ServerCallLogs)AppConstants.WriteinFile(TAG + "Temp log PumpOnTime:"+PumpOnTime);

                        if (seconds >= pont) {
                            //Timed out (Start was pressed, and pump on timer hit): Pump Time On limit reached* = 4
                            if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                            CommonUtils.UpgradeTransactionStatusRetroFit(TransactionId,"4",BackgroundService_AP_PIPE.this);

                            commonForAutoStopQtySameForSeconds();
                        }

                    } catch (Exception e) {
                        AppConstants.WriteinFile(TAG + "Exception  :" + LinkName + " PumpOnTime -" + PumpOnTime + "-" + e.getMessage());
                    }


                } else {

                if (stopAutoFuelSeconds > 0) {

                    if (seconds >= stopAutoFuelSeconds) {

                        if (qtyFrequencyCount()) {

                            //qty is same for some time
                            IsFuelingStop = "1";
                            System.out.println("APFS_PIPE Auto Stop!Quantity is same for last");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  Link:" + LinkName + " Auto Stop!Quantity is same for last");
                            //AppConstants.colorToastBigFont(this, "Auto Stop!\n\nQuantity is same for last " + stopAutoFuelSeconds + " seconds.", Color.BLUE);
                            stopButtonFunctionality();
                            stopTimer = false;
                            this.stopSelf();
                            Constants.FS_1STATUS = "FREE";
                            clearEditTextFields();
                            if (!Constants.BusyVehicleNumberList.equals(null)) {
                                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
                            }


                        } else {
                            quantityRecords.remove(0);
                            System.out.println("0 th pos deleted");
                            System.out.println("seconds--" + seconds);

                            commonForAutoStopQtySameForSeconds();
                        }
                    }
                }

            }
            }
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  secondsTimeLogic Execption " + e);
        }
    }

    public void commonForAutoStopQtySameForSeconds() {
        if (qtyFrequencyCount()) {

            IsFuelingStop = "1";
            stopButtonFunctionality();
            stopTimer = false;
            this.stopSelf();
            Constants.FS_1STATUS = "FREE";
            clearEditTextFields();
            if (!Constants.BusyVehicleNumberList.equals(null)) {
                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
            }

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  Link:" + LinkName + " Auto Stop!Quantity is same for last");
        } else {
            quantityRecords.remove(0);

        }
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

            if (uniqueSet.size() == 1) {  //Autostop unique records
                return true;
            }
        }

        return false;
    }

    public void convertCountToQuantity(String counts) {
        outputQuantity = counts;

        Pulses = Integer.parseInt(outputQuantity);
        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

        DecimalFormat precision = new DecimalFormat("0.00");
        Constants.FS_1Gallons = (precision.format(fillqty));
        Constants.FS_1Pulse = outputQuantity;

        System.out.println("APFS_PIPE Pulse" + outputQuantity);
        System.out.println("APFS_PIPE Quantity" + (fillqty));

        if (CurrTxnMode.equalsIgnoreCase("online")) {

            TrazComp authEntityClass = new TrazComp();
            authEntityClass.TransactionId = TransactionId;
            authEntityClass.FuelQuantity = fillqty;
            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP_PIPE.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Main Transaction--";
            authEntityClass.TransactionFrom = "A";
            authEntityClass.Pulses = Integer.parseInt(counts);
            authEntityClass.IsFuelingStop = IsFuelingStop;
            authEntityClass.IsLastTransaction = IsLastTransaction;

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  Link:" + LinkName + " received Pulses:" + Integer.parseInt(counts) + " Qty:" + fillqty + " TxnID:" + TransactionId);

            String userEmail = CommonUtils.getCustomerDetails_backgroundService_PIPE(BackgroundService_AP_PIPE.this).PersonEmail;
            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_AP_PIPE.this) + ":" + userEmail + ":" + "TransactionComplete");


            HashMap<String, String> imap = new HashMap<>();
            imap.put("jsonData", jsonData);
            imap.put("authString", authString);
            imap.put("sqliteId", sqliteID + "");


            if (fillqty > 0) {


                int rowseffected = controller.updateTransactions(imap);
                System.out.println("rowseffected-" + rowseffected);
                if (rowseffected == 0) {

                    controller.insertTransactions(imap);
                }

                controller.deleteTransStatusByTransID(TransactionId);
            }

        } else {



            if (fillqty > 0) {
                offcontroller.updateOfflinePulsesQuantity(sqlite_id + "", counts, fillqty + "",OffLastTXNid);
            }

            if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline  Link:" + LinkName + "  Pulses:" + Integer.parseInt(counts) + " Qty:" + fillqty);

        }
    }

    public void GetDetails() {
        vehicleNumber = Constants.AccVehicleNumber_FS1;
        odometerTenths = Constants.AccOdoMeter_FS1 + "";
        dNumber = Constants.AccDepartmentNumber_FS1;
        pNumber = Constants.AccPersonnelPIN_FS1;
        oText = Constants.AccOther_FS1;
        hNumber = Constants.AccHours_FS1 + "";


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
    }

    public void TransactionCompleteFunction() {

        CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list

        SharedPreferences sharedPrefODO = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        OtherLabel = sharedPrefODO.getString(AppConstants.OtherLabel, "Other");


        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
        TransactionId = sharedPref.getString("TransactionId_FS1", "");
        VehicleId = sharedPref.getString("VehicleId_FS1", "");
        PhoneNumber = sharedPref.getString("PhoneNumber_FS1", "");
        PersonId = sharedPref.getString("PersonId_FS1", "");
        PulseRatio = sharedPref.getString("PulseRatio_FS1", "1");
        MinLimit = sharedPref.getString("MinLimit_FS1", "0");
        FuelTypeId = sharedPref.getString("FuelTypeId_FS1", "");
        ServerDate = sharedPref.getString("ServerDate_FS1", "");
        IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS1", "0");

        PrintDate = sharedPref.getString("PrintDate_FS1", "");
        CompanyName = sharedPref.getString("Company_FS1", "");
        Location = sharedPref.getString("Location_FS1", "");
        PersonName = sharedPref.getString("PersonName_FS1", "");
        PrinterMacAddress = sharedPref.getString("PrinterMacAddress_FS1", "");
        PrinterName = sharedPref.getString("PrinterName_FS1", "");
        VehicleNumber = sharedPref.getString("vehicleNumber_FS1", "");
        OtherName = sharedPref.getString("accOther_FS1", "");
        _OdoMeter = sharedPref.getString("OdoMeter_FS1", "");
        _Hours = sharedPref.getString("Hours_FS1", "");

        EntityOffTranz tzc = offcontroller.getTransactionDetailsBySqliteId(sqlite_id);
        String siteid = tzc.SiteId;
        HashMap<String, String> linkmap = offcontroller.getLinksDetailsBySiteId(siteid);
        String IsTLDCallOffline = linkmap.get("IsTLDCall");

        if (IsTLDCall.equalsIgnoreCase("True") || (IsTLDCallOffline != null && IsTLDCallOffline.equalsIgnoreCase("True"))) {
            TankMonitorReading(); //Get Tank Monitor Reading and save it to server
        }

        if (cd.isConnectingToInternet()) {


            ////////////////////--UpgradeCurrentVersion to server--///////////////////////////////////////////////////////

            SharedPreferences myPrefUP = this.getSharedPreferences(Constants.PREF_FS_UPGRADE, 0);
            String hoseid = myPrefUP.getString("hoseid_fs1", "");
            String fsversion = myPrefUP.getString("fsversion_fs1", "");

            UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
            objEntityClass.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP_PIPE.this);
            objEntityClass.Email = CommonUtils.getCustomerDetails_backgroundService_PIPE(this).PersonEmail;
            objEntityClass.HoseId = hoseid;
            objEntityClass.Version = fsversion;

            if (hoseid != null && !hoseid.trim().isEmpty()) {
                BackgroundService_AP_PIPE.UpgradeCurrentVersionWithUgradableVersion objUP = new BackgroundService_AP_PIPE.UpgradeCurrentVersionWithUgradableVersion(objEntityClass);
                objUP.execute();
                System.out.println(objUP.response);

                try {
                    JSONObject jsonObject = new JSONObject(objUP.response);
                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    String ResponceText = jsonObject.getString("ResponceText");

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        // AppConstants.clearSharedPrefByName(BackgroundService_AP.this, Constants.PREF_FS_UPGRADE);
                    }

                } catch (Exception e) {

                }
            }

            try {

                double VehicleSum_FS1 = Double.parseDouble(sharedPref.getString("VehicleSum_FS1", ""));
                double DeptSum_FS1 = Double.parseDouble(sharedPref.getString("DeptSum_FS1", ""));
                double VehPercentage_FS1 = Double.parseDouble(sharedPref.getString("VehPercentage_FS1", ""));
                double DeptPercentage_FS1 = Double.parseDouble(sharedPref.getString("DeptPercentage_FS1", ""));
                String SurchargeType_FS1 = sharedPref.getString("SurchargeType_FS1", "");
                double ProductPrice_FS1 = Double.parseDouble(sharedPref.getString("ProductPrice_FS1", ""));



       /* String PD = CommonUtils.FormatPrintRecipte(sharedPref.getString("PrintDate_FS1", ""));
        System.out.println("printtttttttt  "+PD);*/

                //Print Transaction Receipt
                DecimalFormat precision = new DecimalFormat("0.00");
                String Qty = (precision.format(fillqty));
                double FuelQuantity = Double.parseDouble(Qty);

                //---------print cost--------
                String InitPrintCost = CalculatePrice(SurchargeType_FS1, FuelQuantity, ProductPrice_FS1, VehicleSum_FS1, DeptSum_FS1, VehPercentage_FS1, DeptPercentage_FS1);
                DecimalFormat precision_cost = new DecimalFormat("0.00");
                String PrintCost = (precision_cost.format(Double.parseDouble(InitPrintCost)));

                printReceipt = CommonUtils.GetPrintReciptNew(IsOtherRequire,CompanyName, PrintDate, LinkName, Location, VehicleNumber, PersonName, Qty, PrintCost,OtherLabel, OtherName,_OdoMeter,_Hours);

                if (EnablePrinter.equalsIgnoreCase("True")) {
                    //Start background Service to print recipt
                    Intent serviceIntent = new Intent(BackgroundService_AP_PIPE.this, BackgroundServiceBluetoothPrinter.class);
                    serviceIntent.putExtra("printReceipt", printReceipt);
                    startService(serviceIntent);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


   /*     //===================== UpgradeTransaction Status = 2 =================

        UpdateTransactionStatusClass authEntity = new UpdateTransactionStatusClass();
        authEntity.TransactionId = TransactionId;
        authEntity.Status = "2";
        authEntity.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP_PIPE.this);


        Gson gson1 = new Gson();
        String jsonData1 = gson1.toJson(authEntity);

        System.out.println("AP_FS_PIPE UpdatetransactionData......" + jsonData1);

        String userEmail1 = CommonUtils.getCustomerDetails_backgroundService_PIPE(this).PersonEmail;
        String authString1 = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail1 + ":" + "UpgradeTransactionStatus");

        HashMap<String, String> imapStatus = new HashMap<>();
        imapStatus.put("jsonData", jsonData1);
        imapStatus.put("authString", authString1);

        controller.insertIntoUpdateTranStatus(imapStatus);


        //=========================UpgradeTransactionStatus Ends===============*/

            try {

                TrazComp authEntityClass = new TrazComp();
                authEntityClass.TransactionId = TransactionId;
                authEntityClass.FuelQuantity = fillqty;
                authEntityClass.Pulses = Pulses;
                authEntityClass.TransactionFrom = "A";
                authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP_PIPE.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                authEntityClass.IsFuelingStop = IsFuelingStop;
                authEntityClass.IsLastTransaction = IsLastTransaction;


                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntityClass);

                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  InTransactionComplete jsonData " + jsonData);
                System.out.println("AP_FS_PIPE TrazComp......" + jsonData);

                String userEmail = CommonUtils.getCustomerDetails_backgroundService_PIPE(this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "TransactionComplete");

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

                    // controller.insertTransactions(imap);

                }

           /* //settransactionID to FSUNIT
            //==========================

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    new CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + TransactionId + "}");

                    //new CommandsPOST().execute(URL_RELAY, jsonRelayOn);
                }
            }, 1500);

            //==========================*/
                clearEditTextFields();


            } catch (Exception ex) {

                CommonUtils.LogMessage("APFS_PIPE", "AuthTestAsyncTask ", ex);
            }


            //btnStop.setVisibility(View.GONE);
            consoleString = "";
            //tvConsole.setText("");


            if (AppConstants.NeedToRename) {
                String userEmail = CommonUtils.getCustomerDetails_backgroundService_PIPE(this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag");

                RenameHose rhose = new RenameHose();
                rhose.SiteId = AppConstants.R_SITE_ID;
                rhose.HoseId = AppConstants.R_HOSE_ID;
                rhose.IsHoseNameReplaced = "Y";

                Gson gson = new Gson();
                String jsonData = gson.toJson(rhose);

                storeIsRenameFlag(this, AppConstants.NeedToRename, jsonData, authString);

            }

            boolean BSRunning =  CommonUtils.checkServiceRunning(BackgroundService_AP_PIPE.this,AppConstants.PACKAGE_BACKGROUND_SERVICE);
            if (!BSRunning) {
                startService(new Intent(this, BackgroundService.class));
            }

        } else {
            //offline---------------------------

            try {

                EntityOffTranz authEntityClass = offcontroller.getTransactionDetailsBySqliteId(sqlite_id);
                authEntityClass.TransactionFrom = "AP";
                authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP_PIPE.this) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " ";

                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntityClass);

                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  InTransactionComplete jsonData " + jsonData);
                System.out.println("link1 TrazComp......" + jsonData);

                String userEmail = CommonUtils.getCustomerDetailsCC(this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "TransactionComplete");


                clearEditTextFields();

                SyncOfflineData();


            } catch (Exception ex) {

                CommonUtils.LogMessage("APFS_PIPE", "AuthTestAsyncTask ", ex);
            }

            //offline end-----------------------

        }
        ///////////////////////////////////

        isTransactionComp = true;

        AppConstants.BUSY_STATUS = true;


    }

    public void TankMonitorReading() {

        String mac_address = "";
        String probe_reading = "";
        String probe_temperature = "";
        String LSB = "";
        String Response_code = "";
        String MSB = "";
        String Tem_data = "";

        try {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            TransactionId = sharedPref.getString("TransactionId_FS1", "");

            ServerDate = sharedPref.getString("ServerDate_FS1", "");
            PrintDate = sharedPref.getString("PrintDate_FS1", "");

            //Get TankMonitoring details from FluidSecure Link
            String response1 = new CommandsGET().execute(URL_TDL_info).get();

            //String response1 = "{  \"tld\":{ \"level\":\"180, 212, 11, 34, 110, 175, 1, 47, 231, 15, 78, 65\"  }  }";
            //if (AppConstants.GenerateLogs)AppConstants.WriteinFile("\n" + TAG + "Backgroundservice_AP_PIPE TankMonitorReading ~~~URL_TDL_info_Resp~~\n" + response1);

            if (response1.equalsIgnoreCase("")) {
                System.out.println("TLD response empty");
            } else {


                try {
                    JSONObject reader = null;
                    reader = new JSONObject(response1);

                    JSONObject tld = reader.getJSONObject("tld");
                    mac_address = tld.getString("Mac_address");
                    String Sensor_ID = tld.getString("Sensor_ID");
                    Response_code = tld.getString("Response_code");
                    LSB = tld.getString("LSB");
                    MSB = tld.getString("MSB");
                    Tem_data = tld.getString("Tem_data");
                    String Checksum = tld.getString("Checksum");

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  TLD:" + " mac:" + mac_address + " SensorID:" + Sensor_ID + " Resp_code:" + Response_code + " LSB:" + LSB + " MSB:" + MSB + " Temp:" + Tem_data + " Chksum:" + Checksum);

                    //Get mac address of probe
                    //String mac_str = GetMacAddressOfProbe(level);
                    //mac_address = ConvertToMacAddressFormat(mac_str);

                    //Calculate probe reading
                    //probe_reading = GetProbeReading(LSB,MSB);

                    //probe_temperature = CalculateTemperature(Tem_data);


                } catch (JSONException e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  TankMonitorReading ~~~JSONException~~" + e);
                }

                //-----------------------------------------------------------
                String CurrentDeviceDate = CommonUtils.getTodaysDateInString();
                TankMonitorEntity obj_entity = new TankMonitorEntity();
                obj_entity.IMEI_UDID = AppConstants.getIMEI(BackgroundService_AP_PIPE.this);
                obj_entity.FromSiteId = Integer.parseInt(AppConstants.SITE_ID);
                // obj_entity.ProbeReading = probe_reading;
                obj_entity.TLD = mac_address;
                obj_entity.LSB = LSB;
                obj_entity.MSB = MSB;
                obj_entity.TLDTemperature = Tem_data;
                obj_entity.ReadingDateTime = CurrentDeviceDate;//PrintDate;
                obj_entity.Response_code = Response_code;//Response_code;
                obj_entity.Level = "";
                obj_entity.FromDirectTLD = "n";


                if(cd.isConnectingToInternet()) {
                    SaveTankMonitorReadingy TestAsynTask = new SaveTankMonitorReadingy(obj_entity);
                    TestAsynTask.execute();
                    TestAsynTask.get();

                }else {
                    offcontroller.insertTLDReadings(mac_address,"",AppConstants.SITE_ID,"",AppConstants.getIMEI(BackgroundService_AP_PIPE.this),LSB,MSB,Tem_data,CurrentDeviceDate,Response_code,"n");
                }



            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  TankMonitorReading ~~~Execption~~" + e);
        }


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

    public class UpdateAsynTask extends AsyncTask<Void, Void, Void> {

        UpdateTransactionStatusClass authEntity = null;


        public String response = null;

        public UpdateAsynTask(UpdateTransactionStatusClass authEntity) {
            this.authEntity = authEntity;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntity);
                String userEmail = CommonUtils.getCustomerDetails_backgroundService_PIPE(BackgroundService_AP_PIPE.this).PersonEmail;


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(authEntity.IMEIUDID + ":" + userEmail + ":" + "UpgradeTransactionStatus");
                response = serverHandler.PostTextData(BackgroundService_AP_PIPE.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("", "UpgradeTransactionStatus ", ex);
            }
            return null;
        }

    }

    public void clearEditTextFields() {

        Constants.AccVehicleNumber_FS1 = "";
        Constants.AccOdoMeter_FS1 = 0;
        Constants.AccDepartmentNumber_FS1 = "";
        Constants.AccPersonnelPIN_FS1 = "";
        Constants.AccOther_FS1 = "";
        Constants.AccVehicleOther_FS1 = "";
        Constants.AccHours_FS1 = 0;

    }

    public class OkHttpFileUpload extends AsyncTask<String, Void, String> {

        public String resp = "";

        //ProgressDialog pd;

        @Override
        protected void onPreExecute() {
                /*
                pd = new ProgressDialog(DisplayMeterActivity.this);
                pd.setMessage("Upgrading FS unit...\nIt takes a minute.");
                pd.setCancelable(false);
                pd.show();
                */


        }

        protected String doInBackground(String... param) {


            try {
                String LocalPath = param[0];
                String Localcontenttype = param[1];

                MediaType contentype = MediaType.parse(Localcontenttype);

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                RequestBody body = RequestBody.create(contentype, readBytesFromFile(LocalPath));
                Request request = new Request.Builder()
                        .url(HTTP_URL)//HTTP_URL  192.168.43.210
                        .header("Accept-Encoding", "identity")
                        .post(body)
                        .build();


                Response response = client.newCall(request).execute();
                System.out.println("tesssss1" + response);
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " OkHttpFileUpload -InBackground"+e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(" resp......." + result);


            // pd.dismiss();
            try {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

//                        String command = "upgrade?command=reset";
//                        new TCPCommandsPOST().execute(HTTP_URL, command);

                          if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " URL_RESET CMD");
                          new CommandsPOST().execute(URL_RESET, "");

                        System.out.println("AFTER SECONDS 5");
                    }
                }, 10000); ////Add 10 sec delay original delay 5 Seconds

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  SAVE TRANS locally");
                        TransactionCompleteFunction();

                    }
                }, 3000);


          /*      new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String resultinfo = new CommandsGET().execute(URL_INFO).get();

                            if (resultinfo.trim().startsWith("{") && resultinfo.trim().contains("Version")) {

                                JSONObject jsonObj = new JSONObject(resultinfo);
                                String userData = jsonObj.getString("Version");
                                JSONObject jsonObject = new JSONObject(userData);
                                String sdk_version = jsonObject.getString("sdk_version");
                                String iot_version = jsonObject.getString("iot_version");
                                String mac_address = jsonObject.getString("mac_address");

                                storeUpgradeFSVersion(BackgroundService_AP_PIPE.this, AppConstants.UP_HoseId_fs1, iot_version);


                            } else {

                            }

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                     if (AppConstants.GenerateLogs)AppConstants.WriteinFile("BackgroundService_Ap_PIPE~~~~~~~~~" + "SAVE TRANS locally");
                                    TransactionCompleteFunction();

                                    System.out.println("AFTER SECONDS 15");
                                }
                            }, 3000);


                        } catch (Exception e) {
                            System.out.println(e);
                        }

                        System.out.println("AFTER SECONDS 12");

                    }
                }, 12000);*/


            } catch (Exception e) {
                System.out.println(e);
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " OkHttpFileUpload OnPost"+e.getMessage());
            }

        }
    }

    public void storeUpgradeFSVersion(BackgroundService_AP_PIPE activity, String hoseid, String fsversion) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("hoseid", hoseid);
        editor.putString("fsversion", fsversion);

        editor.commit();
    }

    private static byte[] readBytesFromFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {

            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bytesArray;

    }

    public boolean getPulsarResponseEmptyFor3times() {

        boolean flag = false;

        if (respCounter.size() > 3) {
            for (int i = 0; i < respCounter.size() - 2; i++) {

                int r1 = respCounter.get(i);
                int r2 = respCounter.get(i + 1);
                int r3 = respCounter.get(i + 2);
                System.out.println(r1);
                System.out.println(r2);
                System.out.println(r3);
                System.out.println("respCounter----------");

                if (r1 == 0 && r1 == r2 && r2 == r3)
                    flag = true;
            }
        }
        return flag;
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
                response = serverHandler.PostTextData(BackgroundService_AP_PIPE.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }

    }

    private String CalculatePrice(String SurchargeType_FS1, double FuelQuantity, double ProductPrice_FS1, double VehicleSum_FS1, double DeptSum_FS1, double VehPercentage_FS1, double DeptPercentage_FS1) {

        double cost = 0.0;
        if (SurchargeType_FS1.equalsIgnoreCase("0")) {
            cost = (FuelQuantity) * (ProductPrice_FS1 + VehicleSum_FS1 + DeptSum_FS1);
        } else {

            cost = (FuelQuantity * ProductPrice_FS1) + (((FuelQuantity * ProductPrice_FS1) * VehPercentage_FS1) / 100) + (((FuelQuantity * ProductPrice_FS1) * DeptPercentage_FS1) / 100);
        }

        DecimalFormat precision = new DecimalFormat("0.000");
        String Qty = (precision.format(cost));
        double cost_prec = Double.parseDouble(Qty);

        return String.valueOf(cost_prec);

    }

    public class SaveTankMonitorReadingy extends AsyncTask<Void, Void, Void> {

        TankMonitorEntity vrentity = null;

        public String response = null;

        public SaveTankMonitorReadingy(TankMonitorEntity vrentity) {
            this.vrentity = vrentity;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(vrentity);
                String userEmail = CommonUtils.getCustomerDetails_backgroundService_PIPE(BackgroundService_AP_PIPE.this).PersonEmail;

                System.out.println("jsonDatajsonDatajsonData" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(vrentity.IMEI_UDID + ":" + userEmail + ":" + "SaveTankMonitorReading");
                response = serverHandler.PostTextData(BackgroundService_AP_PIPE.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("TAG", "SaveTankMonitorReadingy ", ex);
            }
            return null;
        }

    }

    public String GetMacAddressOfProbe(String level) {

        String MacAddress = "";

        try {
            String[] Seperate = level.split(",");

            for (int i = 0; i < 6; i++) {

                String pd = CommonUtils.decimal2hex(Integer.parseInt(Seperate[i].trim()));
                MacAddress = MacAddress + pd;

            }

            System.out.println("MacAddress of probe: " + MacAddress);

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile("\n" + TAG + " GetMacAddressOfProbe ~~~Exception~~" + e);
        }

        return MacAddress;
    }

    public String ConvertToMacAddressFormat(String mac_str) {

        String str = mac_str;
        String mac_address = "";

        List<String> strings = new ArrayList<String>();
        int index = 0;

        while (index < str.length()) {
            strings.add(str.substring(index, Math.min(index + 2, str.length())));

            if (index < 2) {
                mac_address = mac_address + str.substring(index, Math.min(index + 2, str.length()));
            } else {
                mac_address = mac_address + ":" + str.substring(index, Math.min(index + 2, str.length()));
            }

            index += 2;

        }

        return mac_address;
    }

    public String GetProbeReading(String LSB, String MSB) {

        double prove = 0;
        try {

            String lsb_hex = CommonUtils.decimal2hex(Integer.parseInt(LSB));
            String msb_hex = CommonUtils.decimal2hex(Integer.parseInt(MSB));
            String Combine_hex = msb_hex + lsb_hex;
            int finalpd = CommonUtils.hex2decimal(Combine_hex);
            prove = finalpd / 128;

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile("\n" + TAG + " GetProbeReading ~~~Exception~~" + e);
        }
        return String.valueOf(prove);
    }

    public String CalculateTemperature(String Tem_data) {

        int Temp = (int) ((Integer.parseInt(Tem_data) * 0.48876) - 50);

        return String.valueOf(Temp);
    }

    public void ListConnectedHotspotIP_AP_PIPEAsyncCall() {

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


                                listOfConnectedIP_AP_PIPE.add("http://" + ipAddress + ":80/");
                                System.out.println("Details Of Connected HotspotIP" + listOfConnectedIP_AP_PIPE);
                            }


                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  ListConnectedHotspotIP_AP_PIPE 1 --Exception " + e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  ListConnectedHotspotIP_AP_PIPE 2 --Exception " + e);
                    }
                }
            }
        });
        thread.start();

    }

    private void ExitBackgroundService() {

        CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list
        if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " Relay status error");
        stopTimer = false;
        new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
        Constants.FS_1STATUS = "FREE";
        clearEditTextFields();
        stopSelf();
    }

    public void offlineLogic1() {

        TransactionId = "0";
        PhoneNumber = "0";
        FuelTypeId = "0";
        ServerDate = "0";
        //IsTLDCall = "0";

        //settransactionID to FSUNIT
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                String curr_date = AppConstants.currentDateFormat("hhmmss");//"yyyyMMdd hhmmss"
                System.out.println("curr_date"+curr_date);

                OffLastTXNid = "99999999";//+sqlite_id+curr_date;
                System.out.println("OfflineLastTransactionID_BackgroundService" + OffLastTXNid);
                new CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + OffLastTXNid + "}");

            }
        }, 1500);*/


        EntityOffTranz tzc = offcontroller.getTransactionDetailsBySqliteId(sqlite_id);

        VehicleId = tzc.VehicleId;
        PersonId = tzc.PersonId;
        String siteid = tzc.SiteId;

        HashMap<String, String> linkmap = offcontroller.getLinksDetailsBySiteId(siteid);
        PumpOnTime = linkmap.get("PumpOnTime");
        IntervalToStopFuel = linkmap.get("PumpOffTime");
        PulseRatio = linkmap.get("Pulserratio");

        EnablePrinter = offcontroller.getOfflineHubDetails(BackgroundService_AP_PIPE.this).EnablePrinter;



        listOfConnectedIP_AP_PIPE.clear();
        ListConnectedHotspotIP_AP_PIPEAsyncCall();

        minFuelLimit = OfflineConstants.getFuelLimit(BackgroundService_AP_PIPE.this);

        numPulseRatio = Double.parseDouble(PulseRatio);

        System.out.println("numPulseRatio-" + numPulseRatio);

        stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);
    }

    private void SyncOfflineData(){

        if (WelcomeActivity.OnWelcomeActivity && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

            if (cd.isConnecting()) {


                try {
                    //sync offline transactions
                    String off_json = offcontroller.getAllOfflineTransactionJSON(BackgroundService_AP_PIPE.this);
                    JSONObject jobj = new JSONObject(off_json);
                    String offtransactionArray = jobj.getString("TransactionsModelsObj");
                    JSONArray jarrsy = new JSONArray(offtransactionArray);

                    if (jarrsy.length() > 0) {
                        startService(new Intent(BackgroundService_AP_PIPE.this, OffTranzSyncService.class));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
