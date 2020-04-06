package com.TrakEngineering.FluidSecureHubTest.offline;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.Aes_Encryption;
import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.CommonUtils;
import com.TrakEngineering.FluidSecureHubTest.ConnectionDetector;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class OffBackgroundService extends Service {

    //https://github.com/smanikandan14/ThinDownloadManager

    OffDBController controller = new OffDBController(OffBackgroundService.this);

    ConnectionDetector cd = new ConnectionDetector(OffBackgroundService.this);
    private static final String TAG = OffBackgroundService.class.getSimpleName();
    private static ArrayList<HashMap<String, String>> AllDataFilePathList = new ArrayList<>();
    private boolean clearVehicleOldSqliteData = false, clearLinkOldSqliteData = false, clearpersonnelOldSqliteData = false;

    Timer timer;
    TimerTask repeatedTask;

    public OffBackgroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        deleteAllDownloadedFiles();
        AllDataFilePathList.clear();

        Log.i(TAG, " Started offline data downloading..onStartCommand");
        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " Started offline data downloading..onStartCommand");

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("storeOfflineAccess", Context.MODE_PRIVATE);
        String isOffline = sharedPref.getString("isOffline", "");
        String OFFLineDataDwnldFreq = sharedPref.getString("OFFLineDataDwnldFreq", "Weekly");
        int DayOfWeek = sharedPref.getInt("DayOfWeek", 2);
        int HourOfDay = sharedPref.getInt("HourOfDay", 2);


        //Check step 1
        if (cd.isConnecting() && isOffline.equalsIgnoreCase("True") && checkSharedPrefOfflineData(getApplicationContext())) {

            Log.i(TAG, " Started offline data downloading.all true.Day-" + OFFLineDataDwnldFreq + " " + DayOfWeek + ":Hr-" + HourOfDay);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Started offline data downloading.all true.Day-" + OFFLineDataDwnldFreq + " " + DayOfWeek + ":Hr-" + HourOfDay);

            new GetAPIToken().execute();

        } else {
            Log.i(TAG, " Started offline data downloading..if condition fail");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Started offline data downloading..if condition fail");

            stopSelf();

        }

        return super.onStartCommand(intent, flags, startId);
    }

    public class GetAPIToken extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;

                String formData = "username=" + Email + "&" +
                        "password=FluidSecure*123&" +
                        "grant_type=password&" +
                        "FromApp=y";


                OkHttpClient client = new OkHttpClient();


                RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), formData);


                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_TOKEN)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPIToken InBackG Ex:" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            if (result != null && !result.isEmpty()) {

                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String access_token = jsonObject.getString("access_token");
                    String token_type = jsonObject.getString("token_type");
                    String expires_in = jsonObject.getString("expires_in");
                    String refresh_token = jsonObject.getString("refresh_token");

                    Log.i(TAG, "Started offline data downloading..API token success");
                    AppConstants.WriteinFile(TAG + " Started offline data downloading..API token success");

                    controller.storeOfflineToken(OffBackgroundService.this, access_token, token_type, expires_in, refresh_token);


                    if (cd.isConnecting()) {
                        Log.i(TAG, "GetAPIHubDetails execute");
                        new GetAPIHubDetails().execute();

                    } else {
                        Log.i(TAG, "GetAPIToken InPost NoInternet");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " GetAPIToken InPost NoInternet");
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " GetAPIToken InPost Ex:" + e.getMessage());
                }

            } else {
                Log.i(TAG, "GetAPIToken InPost Result err:" + result);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPIToken InPost Result err:" + result);
            }

        }


    }

    public class GetAPIHubDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";

            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_HUB + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPIHubDetails InBackG Ex:" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        clearVehicleOldSqliteData = true;
                        clearLinkOldSqliteData = true;
                        clearpersonnelOldSqliteData = true;

                        JSONObject HubDataObj = jsonObject.getJSONObject("HubDataObj");

                        String AllowedLinks = HubDataObj.getString("AllowedLinks");
                        String PersonnelPINNumberRequired = HubDataObj.getString("PersonnelPINNumberRequired");
                        String VehicleNumberRequired = HubDataObj.getString("VehicleNumberRequired");
                        String PersonhasFOB = HubDataObj.getString("PersonhasFOB");
                        String VehiclehasFOB = HubDataObj.getString("VehiclehasFOB");
                        String WiFiChannel = HubDataObj.getString("WiFiChannel");
                        String BluetoothCardReader = HubDataObj.getString("BluetoothCardReader");
                        String BluetoothCardReaderMacAddress = HubDataObj.getString("BluetoothCardReaderMacAddress");
                        String LFBluetoothCardReader = HubDataObj.getString("LFBluetoothCardReader");
                        String LFBluetoothCardReaderMacAddress = HubDataObj.getString("LFBluetoothCardReaderMacAddress");
                        String PrinterMacAddress = HubDataObj.getString("PrinterMacAddress");
                        String PrinterName = HubDataObj.getString("PrinterName");

                        String HubId = HubDataObj.getString("HubId");
                        String EnablePrinter = HubDataObj.getString("EnablePrinter");

                        String VehicleDataFilePath = HubDataObj.getString("VehicleDataFilePath");
                        String PersonnelDataFilePath = HubDataObj.getString("PersonnelDataFilePath");
                        String LinkDataFilePath = HubDataObj.getString("LinkDataFilePath");

                        String VehicleFileCount = HubDataObj.getString("VehicleDataFilesCount");
                        String PersonnelFileCount = HubDataObj.getString("PersonDataFilesCount");
                        String LinkFileCount = HubDataObj.getString("LinkDataFilesCount");

                        controller.storeOfflineHubDetails(OffBackgroundService.this, HubId, AllowedLinks, PersonnelPINNumberRequired, VehicleNumberRequired, PersonhasFOB, VehiclehasFOB, WiFiChannel,
                                BluetoothCardReader, BluetoothCardReaderMacAddress, LFBluetoothCardReader, LFBluetoothCardReaderMacAddress,
                                PrinterMacAddress, PrinterName, EnablePrinter, VehicleDataFilePath, PersonnelDataFilePath, LinkDataFilePath, VehicleFileCount, PersonnelFileCount, LinkFileCount);

                        if (cd.isConnecting()) {

                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " GetAPIHubDetails success ");


                            new GetAPILinkDetails().execute();

                            new GetAPIVehicleDetails().execute();

                            new GetAPIPersonnelPinDetails().execute();


                            AppConstants.clearSharedPrefByName(OffBackgroundService.this, "DownloadFileStatus");
                            AppConstants.clearSharedPrefByName(OffBackgroundService.this, "InsertSqLiteFileStatus");

                            startDownloadTimerTask();

                        } else {
                            Log.i(TAG, "GetAPIHubDetails InPost NoInternet");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " GetAPIHubDetails InPost NoInternet");
                        }

                    } else {
                        Log.i(TAG, "GetAPIHubDetails InPost Response fail" + result);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " GetAPIHubDetails InPost Response fail" + result);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " GetAPIHubDetails InPost Ex:" + e.getMessage());
                }

            } else {
                Log.i(TAG, "GetAPIHubDetails InPost Response err:" + result);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPIHubDetails InPost Response err:" + result);
            }

        }
    }

    public void startDownloadTimerTask() {

        EntityHub obj = controller.getOfflineHubDetails(OffBackgroundService.this);

        String VehicleDataFilePath = obj.VehicleDataFilePath;
        String PersonnelDataFilePath = obj.PersonnelDataFilePath;
        String LinkDataFilePath = obj.LinkDataFilePath;

        String VehicleFileCount = obj.VehicleFileCount;
        String PersonnelFileCount = obj.PersonnelFileCount;
        String LinkFileCount = obj.LinkFileCount;

        //http://sierravistatest.cloudapp.net/offlinedatafiles/FS_AP/HUB00000245/vehicledata_1.txt
        //http://sierravistatest.cloudapp.net/offlinedatafiles/FS_AP/HUB00000245/personneldata_1.txt
        //http://sierravistatest.cloudapp.net/offlinedatafiles/FS_AP/HUB00000245/linkdata_1.txt

        //Create vehicle url
        if (VehicleFileCount != null || !VehicleFileCount.isEmpty()) {

            for (int i = 1; i <= Integer.parseInt(VehicleFileCount); i++) {
                //Add it to array list
                String VehicleFilePath = VehicleDataFilePath;
                String VehicleFileName = "vehicledata_" + i;
                VehicleFilePath = VehicleFilePath.replace("vehicledata_1", "vehicledata_" + i);

                HashMap<String, String> map = new HashMap<>();
                map.put("file_name", VehicleFileName);
                map.put("file_url", VehicleFilePath);
                AllDataFilePathList.add(map);
            }
        }

        //Create Personnel url
        if (PersonnelFileCount != null || !PersonnelFileCount.isEmpty()) {

            for (int i = 1; i <= Integer.parseInt(PersonnelFileCount); i++) {
                //Add it to array list
                String PersonnelFileName = "personneldata_" + i;
                String PersonnelFilePath = PersonnelDataFilePath;
                PersonnelFilePath = PersonnelFilePath.replace("personneldata_1", "personneldata_" + i);

                HashMap<String, String> map = new HashMap<>();
                map.put("file_name", PersonnelFileName);
                map.put("file_url", PersonnelFilePath);
                AllDataFilePathList.add(map);
            }
        }

        //Create Link url
        if (LinkFileCount != null || !LinkFileCount.isEmpty()) {

            for (int i = 1; i <= Integer.parseInt(LinkFileCount); i++) {
                //Add it to array list
                String LinkFileName = "linkdata_" + i;
                String LinkFilePath = LinkDataFilePath;
                LinkFilePath = LinkFilePath.replace("linkdata_1", "linkdata_" + i);

                HashMap<String, String> map = new HashMap<>();
                map.put("file_name", LinkFileName);
                map.put("file_url", LinkFilePath);
                AllDataFilePathList.add(map);
            }
        }

        repeatedTask = new TimerTask() {
            public void run() {

                boolean downloading_pending = false;
                boolean AllInsertedInToSqliteDb = true;

                //Downloading files using downloadLibrary
                if (AllDataFilePathList != null || !AllDataFilePathList.isEmpty()) {
                    for (int i = 0; i < AllDataFilePathList.size(); i++) {

                        String fileName = AllDataFilePathList.get(i).get("file_name");
                        String filePath = AllDataFilePathList.get(i).get("file_url");
                        String status = getDownloadFileStatus(fileName);
                        if (status.isEmpty() || status.equalsIgnoreCase("2")) {

                            downloadLibrary(filePath, fileName);
                            downloading_pending = true;

                        }else if (status.isEmpty() || status.equalsIgnoreCase("3")){
                           downloading_pending = true;
                       }

                    }
                } else {
                    Log.i(TAG, "AllDataFilePathList is Empty..");
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "AllDataFilePathList is Empty..");
                }


                if (!downloading_pending) {

                    //-----------------------------------------------------------
                    //Check for progress of encryption from file and putting it into Sqlite data base
                    if (AllDataFilePathList != null || !AllDataFilePathList.isEmpty()){

                        for (int i = 0; i < AllDataFilePathList.size(); i++) {

                            String fileName = AllDataFilePathList.get(i).get("file_name");
                            String status = getSqLiteFileStatus(fileName);
                            if (status.isEmpty() || status.equalsIgnoreCase("2")){

                                AllInsertedInToSqliteDb = false;
                                //Retry Encrypt and insert into db
                                reAttemptToreadEncryptedFileParseJsonInSqlite(fileName);

                            }else if (status.isEmpty() || status.equalsIgnoreCase("3")){

                                AllInsertedInToSqliteDb = false;
                                //Sqlite Insertion operation in progress
                            }
                        }

                    }else{
                        Log.i(TAG,"AllDataFilePathList is Empty..");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "AllDataFilePathList is Empty..");
                    }

                    if (AllInsertedInToSqliteDb){

                        setSharedPrefOfflineData(getApplicationContext());
                        if (timer != null)
                            timer.cancel();

                        Log.i(TAG,"All files downloaded successfully and Inserted into Sqlite Data Base");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "All files downloaded successfully and Inserted into Sqlite Data Base");
                        //Check if download checksum and Sqlite records match

                    }else{

                        Log.i(TAG,"All files downloaded successfully. Waiting for Sqlite process to complete..");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "All files downloaded successfully. Waiting for Sqlite process to complete..");

                    }

                    /*//----------------------------Original-------------------------------
                    setSharedPrefOfflineData(getApplicationContext());
                    if (timer != null)
                        timer.cancel();

                    Log.i(TAG, "All files downloaded successfully. Pending timer stopped");
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "All files downloaded successfully.");
                    //-------------------------------------------------------------------*/
                }

            }
        };

        long delay = 5000L;
        long period = 60000L;
        timer = new Timer("TimerOffDownload");

        timer.scheduleAtFixedRate(repeatedTask, delay, period);

    }


    public class GetAPILinkDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(4, TimeUnit.SECONDS);
                client.setReadTimeout(4, TimeUnit.SECONDS);
                client.setWriteTimeout(4, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_LINK + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPILinkDetails InBackG Ex:" + e.toString());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            return;

        }
    }

    public void linkJsonParsing(String result, String file_name) {

        if (result != null && !result.isEmpty()) {

            try {

                long InsetFT = -1, InsetLD = -1, ldObj_length = -1, PreviousCount = 0;
                JSONObject jsonObject = new JSONObject(result);

                String ResponceMessage = jsonObject.getString("ResponceMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    JSONArray jsonArr = jsonObject.getJSONArray("LinkDataObj");
                    ldObj_length = jsonArr.length();

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " GetAPILinkDetails Json length=" + jsonArr.length());

                    if (jsonArr != null && jsonArr.length() > 0) {
                        for (int j = 0; j < jsonArr.length(); j++) {
                            JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                            String SiteId = jsonObj.getString("SiteId");
                            String WifiSSId = jsonObj.getString("WifiSSId");
                            String PumpOnTime = jsonObj.getString("PumpOnTime");
                            String PumpOffTime = jsonObj.getString("PumpOffTime");
                            String AuthorizedFuelingDays = jsonObj.getString("AuthorizedFuelingDays");
                            String Pulserratio = jsonObj.getString("Pulserratio");
                            String MacAddress = jsonObj.getString("MacAddress");
                            String IsTLDCall = jsonObj.getString("IsTLDCall");

                            JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                            if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                    JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                    String FromTime = oj.getString("FromTime");
                                    String ToTime = oj.getString("ToTime");

                                    InsetFT = controller.insertFuelTimings(SiteId, "", FromTime, ToTime);

                                    if (InsetFT == -1)
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " GetAPILinkDetails Something went wrong inserting FuelTimings");

                                }
                            }

                            /*if (SiteId.equalsIgnoreCase("155")){
                                //Skip
                            }else {}*/
                                InsetLD = controller.insertLinkDetails(SiteId, WifiSSId, PumpOnTime, PumpOffTime, AuthorizedFuelingDays, Pulserratio, MacAddress, IsTLDCall);

                            //Get previously inserted count
                            if (j == 0 && InsetLD != -1) {
                                PreviousCount = InsetLD - 1;
                            }

                        }
                    }

                    if (InsetLD == -1) {
                        insertSqLiteFileStatus(file_name, "2");
                        Log.i(TAG, " LFileName:" + file_name + " something went wrong inserting sqlite db LinkObjectLength:" + ldObj_length + " Inserted:" + InsetLD);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " something went wrong inserting sqlite db LinkObjectLength:" + ldObj_length + " Inserted:" + InsetLD);
                    } else {

                        long RecordsInsertedCurrentFile = 0;
                        if (PreviousCount == 0) {
                            RecordsInsertedCurrentFile = InsetLD;
                        } else {
                            RecordsInsertedCurrentFile = InsetLD - PreviousCount;
                        }

                        if (ldObj_length == RecordsInsertedCurrentFile) {
                            insertSqLiteFileStatus(file_name, "1");
                        } else {
                            insertSqLiteFileStatus(file_name, "2");
                        }

                        Log.i(TAG, " LFileName:" + file_name + " Offline data inserted in sqlite db LinkObjectLength:" + ldObj_length + " Inserted:" + RecordsInsertedCurrentFile);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " SOffline data inserted in sqlite db LinkObjectLength:" + ldObj_length + " Inserted:" + RecordsInsertedCurrentFile);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Offline data Link download process completed Successfully");
                    }

                } else {
                    insertSqLiteFileStatus(file_name, "2");
                    Log.i(TAG, " LFileName:" + file_name + " PFileName:" + file_name + " InPost Response fail" + result);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " InPost Response fail" + result);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                insertSqLiteFileStatus(file_name, "2");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " InPost Ex:" + e.toString());
            }

        } else {
            insertSqLiteFileStatus(file_name, "2");
            Log.i(TAG, " LFileName:" + file_name + " PFileName:" + file_name + " InPost Result err" + result);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " InPost Result err" + result);
        }
    }

    public class GetAPIVehicleDetails extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(4, TimeUnit.SECONDS);
                client.setReadTimeout(4, TimeUnit.SECONDS);
                client.setWriteTimeout(4, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_VEHICLE + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPIVehicleDetails InBack Ex:" + e.toString());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            return;
        }
    }

    public void vehicleJsonParsing(String result, String file_name) {
        if (result != null && !result.isEmpty()) {

            try {
                long InsertVD = -1, VehicleDataObj_length = -1, PreviousCount = 0;
                JSONObject jsonObject = new JSONObject(result);

                String ResponceMessage = jsonObject.getString("ResponceMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    JSONArray jsonArr = jsonObject.getJSONArray("VehicleDataObj");
                    VehicleDataObj_length = jsonArr.length();

                    if (jsonArr != null && jsonArr.length() > 0) {
                        for (int j = 0; j < jsonArr.length(); j++) {
                            JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                            String VehicleId = jsonObj.getString("VehicleId");
                            String VehicleNumber = jsonObj.getString("VehicleNumber");
                            String CurrentOdometer = jsonObj.getString("CurrentOdometer");
                            String CurrentHours = jsonObj.getString("CurrentHours");
                            String RequireOdometerEntry = jsonObj.getString("RequireOdometerEntry");
                            String RequireHours = jsonObj.getString("RequireHours");
                            String FuelLimitPerTxn = jsonObj.getString("FuelLimitPerTxn");
                            String FuelLimitPerDay = jsonObj.getString("FuelLimitPerDay");
                            String FOBNumber = jsonObj.getString("FOBNumber");
                            String AllowedLinks = jsonObj.getString("AllowedLinks");
                            String Active = jsonObj.getString("Active");

                            String CheckOdometerReasonable = jsonObj.getString("CheckOdometerReasonable");
                            String OdometerReasonabilityConditions = jsonObj.getString("OdometerReasonabilityConditions");
                            String OdoLimit = jsonObj.getString("OdoLimit");
                            String HoursLimit = jsonObj.getString("HoursLimit");
                            String BarcodeNumber = jsonObj.getString("Barcode");
                            String IsExtraOther = jsonObj.getString("IsExtraOther");
                            String ExtraOtherLabel = jsonObj.getString("ExtraOtherLabel");


                            /*if (VehicleNumber.equalsIgnoreCase("4") || VehicleNumber.equalsIgnoreCase("8") || VehicleNumber.equalsIgnoreCase("9")){
                                //skip
                            }else{}*/
                                InsertVD = controller.insertVehicleDetails(VehicleId, VehicleNumber, CurrentOdometer, CurrentHours, RequireOdometerEntry, RequireHours, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, AllowedLinks, Active,
                                        CheckOdometerReasonable, OdometerReasonabilityConditions, OdoLimit, HoursLimit, BarcodeNumber, IsExtraOther, ExtraOtherLabel);


                            //Get previously inserted count
                            if (j == 0 && InsertVD != -1) {
                                PreviousCount = InsertVD - 1;
                            }

                        }
                    }


                    if (InsertVD == -1) {

                        insertSqLiteFileStatus(file_name, "2");
                        Log.i(TAG, " VFileName:" + file_name + " *Offline data something went wrong sqlite db VehicleDataObj_length:" + VehicleDataObj_length + "previous Inserted" + InsertVD);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " Offline data something went wrong sqlite db VehicleDataObj_length:" + VehicleDataObj_length + "previous Inserted" + InsertVD);
                    } else {

                        long RecordsInsertedCurrentFile = 0;
                        if (PreviousCount == 0) {
                            RecordsInsertedCurrentFile = InsertVD;
                        } else {
                            RecordsInsertedCurrentFile = InsertVD - PreviousCount;
                        }

                        if (VehicleDataObj_length == RecordsInsertedCurrentFile) {
                            insertSqLiteFileStatus(file_name, "1");
                        } else {
                            insertSqLiteFileStatus(file_name, "2");
                        }

                        Log.i(TAG, " VFileName:" + file_name + " Offline data inserted in sqlite db VehicleDataObj_length:" + VehicleDataObj_length + " Inserted" + RecordsInsertedCurrentFile);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "FileName:" + file_name + " Offline data inserted in sqlite db VehicleDataObj_length:" + VehicleDataObj_length + " Inserted" + RecordsInsertedCurrentFile);
                    }

                } else {
                    insertSqLiteFileStatus(file_name, "2");
                    Log.i(TAG, " VFileName:" + file_name + " InPost Responce fail" + result);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " InPost Responce fail" + result);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                insertSqLiteFileStatus(file_name, "2");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " InPost Ex:" + e.toString());
            }

        } else {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " InPost Result err:" + result);
        }
    }

    public class GetAPIPersonnelPinDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(4, TimeUnit.SECONDS);
                client.setReadTimeout(4, TimeUnit.SECONDS);
                client.setWriteTimeout(4, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_PERSONNEL + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetAPIPersonnelPinDetails InBack Ex:" + e.toString());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            return;
        }

    }

    public void personnelJsonParsing(String result, String file_name) {

        if (result != null && !result.isEmpty()) {

            try {

                long InsertPD = -1, pdObj_length = -1, PreviousCount = 0;
                JSONObject jsonObject = new JSONObject(result);

                String ResponceMessage = jsonObject.getString("ResponceMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {


                    JSONArray jsonArr = jsonObject.getJSONArray("PersonDataObj");
                    pdObj_length = jsonArr.length();

                    if (jsonArr != null && jsonArr.length() > 0) {
                        for (int j = 0; j < jsonArr.length(); j++) {
                            JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                            String PersonId = jsonObj.getString("PersonId");
                            String PinNumber = jsonObj.getString("PinNumber");
                            String FuelLimitPerTxn = jsonObj.getString("FuelLimitPerTxn");
                            String FuelLimitPerDay = jsonObj.getString("FuelLimitPerDay");
                            String FOBNumber = jsonObj.getString("FOBNumber");
                            String Authorizedlinks = jsonObj.getString("Authorizedlinks");
                            String AssignedVehicles = jsonObj.getString("AssignedVehicles");

                            JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                            if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                    JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                    String FromTime = oj.getString("FromTime");
                                    String ToTime = oj.getString("ToTime");

                                    controller.insertFuelTimings("", PersonId, FromTime, ToTime);
                                }
                            }

                            /*if (PinNumber.equalsIgnoreCase("123") || PinNumber.equalsIgnoreCase("xyz") || PinNumber.equalsIgnoreCase("32131")){
                                //Skip
                            }else{}*/
                                InsertPD = controller.insertPersonnelPinDetails(PersonId, PinNumber, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, Authorizedlinks, AssignedVehicles);


                            //Get previously inserted count
                            if (j == 0 && InsertPD != -1) {
                                PreviousCount = InsertPD - 1;
                            }

                        }

                        String SaveDate = CommonUtils.getDateInString();
                        CommonUtils.SaveOfflineDbSizeDateTime(OffBackgroundService.this, SaveDate);

                    }


                    if (InsertPD == -1) {
                        insertSqLiteFileStatus(file_name, "2");
                        Log.i(TAG, " PFileName:" + file_name + " Offline data something went wrong sqlite db pdObj_length:" + pdObj_length + "previous Inserted:" + InsertPD);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " Offline data something went wrong sqlite db pdObj_length:" + pdObj_length + "previous Inserted:" + InsertPD);
                    } else {

                        long RecordsInsertedCurrentFile = 0;
                        if (PreviousCount == 0) {
                            RecordsInsertedCurrentFile = InsertPD;
                        } else {
                            RecordsInsertedCurrentFile = InsertPD - PreviousCount;
                        }

                        if (pdObj_length == RecordsInsertedCurrentFile) {
                            insertSqLiteFileStatus(file_name, "1");
                        } else {
                            insertSqLiteFileStatus(file_name, "2");
                        }

                        Log.i(TAG, " PFileName:" + file_name + " Offline data inserted in sqlite db pdObj_length:" + pdObj_length + " Inserted:" + RecordsInsertedCurrentFile);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " Offline data inserted in sqlite db pdObj_length:" + pdObj_length + " Inserted:" + RecordsInsertedCurrentFile);
                    }

                } else {
                    insertSqLiteFileStatus(file_name, "2");
                    Log.i(TAG, " PFileName:" + file_name + " PFileName:" + file_name + " InPost Response fail:" + result);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " InPost Response fail:" + result);

                }

            } catch (JSONException e) {
                e.printStackTrace();
                insertSqLiteFileStatus(file_name, "2");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " InPost Ex:" + e.toString());
            }

        } else {
            Log.i(TAG, " PFileName:" + file_name + " InPost Result err:" + result);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " InPost Result err:" + result);
        }
    }

    public static boolean checkSharedPrefOfflineData(Context myctx) {
        SharedPreferences sharedPrefODO = myctx.getSharedPreferences("OfflineData", Context.MODE_PRIVATE);
        String last_date = sharedPrefODO.getString("last_date", "");


        String curr_date = AppConstants.currentDateFormat("dd/MM/yyyy");

        if (curr_date.trim().equalsIgnoreCase(last_date.trim())) {
            return false;
        } else
            return true;

    }

    private static void setSharedPrefOfflineData(Context myctx) {
        SharedPreferences sharedPref = myctx.getSharedPreferences("OfflineData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("last_date", AppConstants.currentDateFormat("dd/MM/yyyy"));
        editor.apply();

    }

    public void insertDownloadFileStatus(String filename, String status) {

        SharedPreferences sharedPref = OffBackgroundService.this.getSharedPreferences("DownloadFileStatus", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(filename, status);
        editor.commit();

    }

    public String getDownloadFileStatus(String filename) {

        SharedPreferences sharedPrefODO = OffBackgroundService.this.getSharedPreferences("DownloadFileStatus", Context.MODE_PRIVATE);
        String status = sharedPrefODO.getString(filename, "");

        return status;
    }

    public void insertSqLiteFileStatus(String filename, String status) {

        SharedPreferences sharedPref = OffBackgroundService.this.getSharedPreferences("InsertSqLiteFileStatus", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(filename, status);
        editor.commit();

    }

    public String getSqLiteFileStatus(String filename) {

        SharedPreferences sharedPrefODO = OffBackgroundService.this.getSharedPreferences("InsertSqLiteFileStatus", Context.MODE_PRIVATE);
        String status = sharedPrefODO.getString(filename, "");

        return status;
    }


    public void downloadLibrary(String downloadUrl, String fileName) {

        try {
            //https://github.com/smanikandan14/ThinDownloadManager
            Log.i(TAG, "Started downloading file:" + fileName);

            ThinDownloadManager downloadManager = new ThinDownloadManager();


            Uri downloadUri = Uri.parse(downloadUrl);
            Uri destinationUri = Uri.parse(Environment.getExternalStorageDirectory() + "/FSdata/" + fileName + ".txt");
            DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                    //.addCustomHeader("Auth-Token", "YourTokenApiKey")
                    .setRetryPolicy(new DefaultRetryPolicy())
                    .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                    .setDownloadResumable(true)
                    //.setDownloadContext(downloadContextObject)//Optional
                    .setDownloadListener(new DownloadStatusListener() {
                        @Override
                        public void onDownloadComplete(int id) {

                            Log.i(TAG, "Offline db File downloaded successfully." + fileName);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Offline db File downloaded successfully." + fileName);

                            insertDownloadFileStatus(fileName, "1");
                            insertSqLiteFileStatus(fileName, "3");//encrypt file and insert into Sqlite process starts
                            readEncryptedFileParseJsonInSqlite(fileName);

                        }

                        @Override
                        public void onDownloadFailed(int id, int errorCode, String errorMessage) {
                            AppConstants.WriteinFile("download-Failed--" + fileName + " " + errorCode + " " + errorMessage);

                            Log.i(TAG, "Failed to download Offline db File retrying.." + fileName);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Failed to download Offline db File retrying.." + fileName);

                            insertDownloadFileStatus(fileName, "2");

                            if (errorCode == 416) {
                                insertDownloadFileStatus(fileName, "1");
                                insertSqLiteFileStatus(fileName, "3");//encrypt file and insert into Sqlite process starts
                                readEncryptedFileParseJsonInSqlite(fileName);
                            }

                        }

                        @Override
                        public void onProgress(int id, long totalBytes, long downlaodedBytes, int progress) {

                            insertDownloadFileStatus(fileName, "3");
                            //AppConstants.WriteinFile("download-onProgress--" + fileName + " " + totalBytes + " " + downlaodedBytes + " " + progress);
                        }
                    });


            int downloadId = downloadManager.add(downloadRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readEncryptedFileParseJsonInSqlite(String file_name) {

        File file = new File(Environment.getExternalStorageDirectory() + "/FSdata/" + file_name + ".txt");

        //File file = new File(file_pathrul);

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String secretKey = "(fs@!<(t!8*N+^e9";
        String decryptedJson = "";

        try {

            String normal = text.toString();
            String withoutFirstCharacter = normal.substring(1);

            if (withoutFirstCharacter != null && !withoutFirstCharacter.trim().isEmpty()) {

                byte[] base64normal = Base64.decode(withoutFirstCharacter, Base64.DEFAULT);

                Aes_Encryption as = new Aes_Encryption();
                byte[] dsd = as.decrypt(base64normal, secretKey.getBytes(), secretKey.getBytes());
                decryptedJson = new String(dsd);
                //Log.i(TAG,"file_name:"+file_name+" decryptedJson: "+decryptedJson);

                if (file_name.contains("vehicledata")) {

                    if (clearVehicleOldSqliteData) {
                        Log.i(TAG, "Deleted old Vehicle offline Sqlite data");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Deleted old Vehicle offline Sqlite data");
                        clearVehicleOldSqliteData = false;
                        controller.deleteTableData(OffDBController.TBL_VEHICLE);
                    }
                    vehicleJsonParsing(decryptedJson, file_name);

                } else if (file_name.contains("personneldata")) {

                    if (clearpersonnelOldSqliteData) {
                        Log.i(TAG, "Deleted old Personnel offline Sqlite data");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Deleted old Personnel offline Sqlite data");
                        controller.deleteTableData(OffDBController.TBL_PERSONNEL);
                        clearpersonnelOldSqliteData = false;
                    }
                    personnelJsonParsing(decryptedJson, file_name);

                } else if (file_name.contains("linkdata")) {

                    if (clearLinkOldSqliteData) {
                        Log.i(TAG, "Deleted old Link offline Sqlite data");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Deleted old Link offline Sqlite data");
                        controller.deleteTableData(OffDBController.TBL_LINK);
                        clearLinkOldSqliteData = false;
                    }
                    linkJsonParsing(decryptedJson, file_name);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void reAttemptToreadEncryptedFileParseJsonInSqlite(String file_name) {

        File file = new File(Environment.getExternalStorageDirectory() + "/FSdata/" + file_name + ".txt");

        //File file = new File(file_pathrul);

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String secretKey = "(fs@!<(t!8*N+^e9";
        String decryptedJson = "";

        try {

            String normal = text.toString();
            String withoutFirstCharacter = normal.substring(1);

            if (withoutFirstCharacter != null && !withoutFirstCharacter.trim().isEmpty()) {

                byte[] base64normal = Base64.decode(withoutFirstCharacter, Base64.DEFAULT);

                Aes_Encryption as = new Aes_Encryption();
                byte[] dsd = as.decrypt(base64normal, secretKey.getBytes(), secretKey.getBytes());
                decryptedJson = new String(dsd);
                //Log.i(TAG,"file_name:"+file_name+" reattempt decryptedJson: "+decryptedJson);

                if (file_name.contains("vehicledata")) {

                    reAttemptvehicleJsonParsing(decryptedJson, file_name);

                } else if (file_name.contains("personneldata")) {

                    reAttemptpersonnelJsonParsing(decryptedJson,file_name);

                } else if (file_name.contains("linkdata")) {

                    reAttemptlinkJsonParsing(decryptedJson,file_name);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void reAttemptvehicleJsonParsing(String result, String file_name) {

        if (result != null && !result.isEmpty()) {

            try {

                insertSqLiteFileStatus(file_name, "1");
                long InsertVD = 0;
                JSONObject jsonObject = new JSONObject(result);
                String ResponceMessage = jsonObject.getString("ResponceMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    JSONArray jsonArr = jsonObject.getJSONArray("VehicleDataObj");
                    long VehicleDataObj_length = jsonArr.length();

                    if (jsonArr != null && jsonArr.length() > 0) {
                        for (int j = 0; j < jsonArr.length(); j++) {
                            JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                            String VehicleId = jsonObj.getString("VehicleId");
                            String VehicleNumber = jsonObj.getString("VehicleNumber");
                            String CurrentOdometer = jsonObj.getString("CurrentOdometer");
                            String CurrentHours = jsonObj.getString("CurrentHours");
                            String RequireOdometerEntry = jsonObj.getString("RequireOdometerEntry");
                            String RequireHours = jsonObj.getString("RequireHours");
                            String FuelLimitPerTxn = jsonObj.getString("FuelLimitPerTxn");
                            String FuelLimitPerDay = jsonObj.getString("FuelLimitPerDay");
                            String FOBNumber = jsonObj.getString("FOBNumber");
                            String AllowedLinks = jsonObj.getString("AllowedLinks");
                            String Active = jsonObj.getString("Active");

                            String CheckOdometerReasonable = jsonObj.getString("CheckOdometerReasonable");
                            String OdometerReasonabilityConditions = jsonObj.getString("OdometerReasonabilityConditions");
                            String OdoLimit = jsonObj.getString("OdoLimit");
                            String HoursLimit = jsonObj.getString("HoursLimit");
                            String BarcodeNumber = jsonObj.getString("Barcode");
                            String IsExtraOther = jsonObj.getString("IsExtraOther");
                            String ExtraOtherLabel = jsonObj.getString("ExtraOtherLabel");


                            InsertVD = controller.insertWithOnConflictVehicleDetails(VehicleId, VehicleNumber, CurrentOdometer, CurrentHours, RequireOdometerEntry, RequireHours, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, AllowedLinks, Active,
                                    CheckOdometerReasonable, OdometerReasonabilityConditions, OdoLimit, HoursLimit, BarcodeNumber, IsExtraOther, ExtraOtherLabel);
                            Log.i(TAG, " VFileName:" + file_name + " insertWithOnConflictVehicleDetails:"+InsertVD);


                        }
                    }


                    if (InsertVD != -1) {
                        Log.i(TAG, " VFileName:" + file_name + " ReAttempt Inserted successfully No:"+InsertVD);
                    }

                } else {
                    Log.i(TAG, " VFileName:" + file_name + " ReAttempt fail InPost Responce fail" + result);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " ReAttempt fail InPost Responce fail" + result);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " ReAttempt fail InPost Ex:" + e.toString());
            }

        } else {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " VFileName:" + file_name + " ReAttempt fail InPost Result err:" + result);
        }
    }

    public void reAttemptpersonnelJsonParsing(String result, String file_name) {

        if (result != null && !result.isEmpty()) {

            try {

                insertSqLiteFileStatus(file_name, "1");
                long InsertPD = -1, pdObj_length = -1, PreviousCount = 0;
                JSONObject jsonObject = new JSONObject(result);

                String ResponceMessage = jsonObject.getString("ResponceMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {


                    JSONArray jsonArr = jsonObject.getJSONArray("PersonDataObj");
                    pdObj_length = jsonArr.length();

                    if (jsonArr != null && jsonArr.length() > 0) {
                        for (int j = 0; j < jsonArr.length(); j++) {
                            JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                            String PersonId = jsonObj.getString("PersonId");
                            String PinNumber = jsonObj.getString("PinNumber");
                            String FuelLimitPerTxn = jsonObj.getString("FuelLimitPerTxn");
                            String FuelLimitPerDay = jsonObj.getString("FuelLimitPerDay");
                            String FOBNumber = jsonObj.getString("FOBNumber");
                            String Authorizedlinks = jsonObj.getString("Authorizedlinks");
                            String AssignedVehicles = jsonObj.getString("AssignedVehicles");

                            JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                            if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                    JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                    String FromTime = oj.getString("FromTime");
                                    String ToTime = oj.getString("ToTime");

                                    controller.insertFuelTimings("", PersonId, FromTime, ToTime);
                                }
                            }

                            InsertPD = controller.insertWithOnConflictPersonnelDetails(PersonId, PinNumber, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, Authorizedlinks, AssignedVehicles);
                            Log.i(TAG, " VFileName:" + file_name + " insertWithOnConflictVehicleDetails:"+InsertPD);

                        }

                        String SaveDate = CommonUtils.getDateInString();
                        CommonUtils.SaveOfflineDbSizeDateTime(OffBackgroundService.this, SaveDate);

                    }

                    if (InsertPD != -1) {
                        Log.i(TAG, " PFileName:" + file_name + " ReAttempt Inserted successfully No:"+InsertPD);
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " ReAttempt Inserted successfully No:"+InsertPD);
                    }


                } else {
                    Log.i(TAG, " PFileName:" + file_name + " PFileName:" + file_name + " ReAttempt InPost Response fail:" + result);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " ReAttempt InPost Response fail:" + result);

                }

            } catch (JSONException e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " ReAttempt InPost Ex:" + e.toString());
            }

        } else {
            Log.i(TAG, " PFileName:" + file_name + " ReAttempt InPost Result err:" + result);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " PFileName:" + file_name + " ReAttempt InPost Result err:" + result);
        }
    }

    public void reAttemptlinkJsonParsing(String result, String file_name) {

        if (result != null && !result.isEmpty()) {

            try {
                insertSqLiteFileStatus(file_name, "1");
                long InsetFT = -1, InsetLD = -1, ldObj_length = -1, PreviousCount = 0;
                JSONObject jsonObject = new JSONObject(result);

                String ResponceMessage = jsonObject.getString("ResponceMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    JSONArray jsonArr = jsonObject.getJSONArray("LinkDataObj");
                    ldObj_length = jsonArr.length();

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " GetAPILinkDetails Json length=" + jsonArr.length());

                    if (jsonArr != null && jsonArr.length() > 0) {
                        for (int j = 0; j < jsonArr.length(); j++) {
                            JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                            String SiteId = jsonObj.getString("SiteId");
                            String WifiSSId = jsonObj.getString("WifiSSId");
                            String PumpOnTime = jsonObj.getString("PumpOnTime");
                            String PumpOffTime = jsonObj.getString("PumpOffTime");
                            String AuthorizedFuelingDays = jsonObj.getString("AuthorizedFuelingDays");
                            String Pulserratio = jsonObj.getString("Pulserratio");
                            String MacAddress = jsonObj.getString("MacAddress");
                            String IsTLDCall = jsonObj.getString("IsTLDCall");

                            JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                            if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                    JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                    String FromTime = oj.getString("FromTime");
                                    String ToTime = oj.getString("ToTime");

                                    InsetFT = controller.insertFuelTimings(SiteId, "", FromTime, ToTime);

                                    if (InsetFT == -1)
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " GetAPILinkDetails Something went wrong inserting FuelTimings");

                                }
                            }

                            InsetLD = controller.insertWithOnConflictLinkDetails(SiteId, WifiSSId, PumpOnTime, PumpOffTime, AuthorizedFuelingDays, Pulserratio, MacAddress, IsTLDCall);

                        }
                    }

                    if (InsetLD != -1) {
                        Log.i(TAG, " LFileName:" + file_name + " ReAttempt Inserted successfully No:"+InsetLD);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " ReAttempt Inserted successfully No:"+InsetLD);
                    }

                } else {
                    Log.i(TAG, " LFileName:" + file_name + " PFileName:" + file_name + " ReAttempt InPost Response fail" + result);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " ReAttempt InPost Response fail" + result);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " ReAttempt InPost Ex:" + e.toString());
            }

        } else {
            Log.i(TAG, " LFileName:" + file_name + " PFileName:" + file_name + " ReAttempt InPost Result err" + result);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " LFileName:" + file_name + " ReAttempt InPost Result err" + result);
        }
    }

    public void deleteAllDownloadedFiles() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/FSdata");
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
            }
        } catch (Exception e) {
            AppConstants.WriteinFile("deleteAllDownloadedFiles-" + e.getMessage());
        }
    }

}