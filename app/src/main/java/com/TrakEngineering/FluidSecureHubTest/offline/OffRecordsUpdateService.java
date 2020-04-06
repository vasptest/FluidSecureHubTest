package com.TrakEngineering.FluidSecureHubTest.offline;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.CommonUtils;
import com.TrakEngineering.FluidSecureHubTest.ConnectionDetector;
import com.TrakEngineering.FluidSecureHubTest.SplashActivity;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class OffRecordsUpdateService extends Service {

    OffDBController controller = new OffDBController(OffRecordsUpdateService.this);

    ConnectionDetector cd = new ConnectionDetector(OffRecordsUpdateService.this);
    private static final String TAG = OffRecordsUpdateService.class.getSimpleName();

    public OffRecordsUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences pref = OffRecordsUpdateService.this.getSharedPreferences(AppConstants.sharedPref_OfflineRecordUpdates, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("date", AppConstants.currentDateFormat("MM-dd-yyyy HH:mm"));
        editor.commit();



        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " Started RecordsUpdate data downloading..onStartCommand");


        //Check step 1
        if (cd.isConnecting()) {

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Started RecordsUpdate data downloading.");

            new GetAPIToken().execute();

        } else {

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Started RecordsUpdate data downloading..if condition fail");

            stopSelf();

        }

        return super.onStartCommand(intent, flags, startId);
    }

    public class GetAPIToken extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String Email = CommonUtils.getCustomerDetailsCC(OffRecordsUpdateService.this).PersonEmail;

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

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {


                    JSONObject jsonObject = new JSONObject(result);

                    String access_token = jsonObject.getString("access_token");
                    String token_type = jsonObject.getString("token_type");
                    String expires_in = jsonObject.getString("expires_in");
                    String refresh_token = jsonObject.getString("refresh_token");

                    System.out.println("access_token:" + access_token);

                    AppConstants.WriteinFile(TAG + " Started RecordsUpdate data downloading..API token success");

                    controller.storeOfflineToken(OffRecordsUpdateService.this, access_token, token_type, expires_in, refresh_token);


                    if (cd.isConnecting()) {

                        new GetAPIVehicleDetails().execute();

                        new GetAPIPersonnelPinDetails().execute();

                    }
                } catch (JSONException e) {
                    AppConstants.WriteinFile(TAG + " Started RecordsUpdate data downloading..API token FAIL");
                }

            }


        }


    }


    public class GetAPIVehicleDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffRecordsUpdateService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffRecordsUpdateService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffRecordsUpdateService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_VEHICLE + "?Email=" + Email + "&IMEI=" + IMEI + "&onlyUpdated=y")
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        JSONArray jsonArr = jsonObject.getJSONArray("VehicleDataObj");

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


                                if (controller.isVehicleExistsByVehicleId(VehicleId)) {
                                    //update vehicles
                                    controller.updateVehicleDetailsByVehicleId(VehicleId, VehicleNumber, CurrentOdometer, CurrentHours, RequireOdometerEntry, RequireHours, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, AllowedLinks, Active,
                                            CheckOdometerReasonable, OdometerReasonabilityConditions, OdoLimit, HoursLimit, BarcodeNumber, IsExtraOther, ExtraOtherLabel);
                                } else {
                                    //insert vehicles
                                    controller.insertVehicleDetails(VehicleId, VehicleNumber, CurrentOdometer, CurrentHours, RequireOdometerEntry, RequireHours, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, AllowedLinks, Active,
                                            CheckOdometerReasonable, OdometerReasonabilityConditions, OdoLimit, HoursLimit, BarcodeNumber, IsExtraOther, ExtraOtherLabel);
                                }
                            }
                        }


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }

    public class GetAPIPersonnelPinDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffRecordsUpdateService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffRecordsUpdateService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffRecordsUpdateService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_PERSONNEL + "?Email=" + Email + "&IMEI=" + IMEI + "&onlyUpdated=y")
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {


                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    String ResponceText = jsonObject.getString("ResponceText");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        JSONArray jsonArr = jsonObject.getJSONArray("PersonDataObj");

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

                                    //if fuel timings changed then delete and then insert
                                    if (controller.isPersonExistsByPersonId(PersonId))
                                        controller.deletePersonFuelTimingsByPersonId(PersonId);

                                    for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                        JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                        String FromTime = oj.getString("FromTime");
                                        String ToTime = oj.getString("ToTime");

                                        controller.insertFuelTimings("", PersonId, FromTime, ToTime);
                                    }
                                }

                                if (controller.isPersonExistsByPersonId(PersonId)) {
                                    controller.updatePersonnelDetailsByPersonId(PersonId, PinNumber, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, Authorizedlinks, AssignedVehicles);
                                } else {
                                    controller.insertPersonnelPinDetails(PersonId, PinNumber, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, Authorizedlinks, AssignedVehicles);
                                }


                            }
                            Log.i(TAG, " RecordsUpdate data downloaded Successfully");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " RecordsUpdate data downloaded Successfully");

                            GetOfflineDatabaseSize();
                            stopSelf();
                        }


                    }
                    else {
                        Log.i(TAG, "Person records update failed");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + ResponceText);
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }

    private void GetOfflineDatabaseSize() {

        String SaveDate = CommonUtils.getDateInString();
        String Size = "";

        File f = OffRecordsUpdateService.this.getDatabasePath("FSHubOffline.db");
        // Get length of file in bytes
        long fileSizeInBytes = f.length();
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        long fileSizeInKB = fileSizeInBytes / 1024;
        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
        long fileSizeInMB = fileSizeInKB / 1024;

        if (fileSizeInMB < 1) {
            Size = fileSizeInKB + "KB";
        } else {
            Size = fileSizeInMB + "MB";
        }
        Log.i(TAG, String.valueOf("SizeInBytes: " + fileSizeInBytes) + " SizeInMB: " + fileSizeInMB);

        CommonUtils.SaveOfflineDbSize(getApplicationContext(), Size, SaveDate);

    }

    public static boolean checkSharedPrefOfflineData(Context myctx) {
        SharedPreferences sharedPrefODO = myctx.getSharedPreferences("OfflineData", Context.MODE_PRIVATE);
        String last_date = sharedPrefODO.getString("last_date", "");


        String curr_date = AppConstants.currentDateFormat("dd/MM/yyyy");

        System.out.println(last_date + "  -" + "-  " + curr_date);

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


    /*public class GetAPIHubDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffRecordsUpdateService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffRecordsUpdateService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffRecordsUpdateService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_HUB + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        setSharedPrefOfflineData(getApplicationContext());

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

                        controller.storeOfflineHubDetails(OffRecordsUpdateService.this, HubId, AllowedLinks, PersonnelPINNumberRequired, VehicleNumberRequired, PersonhasFOB, VehiclehasFOB, WiFiChannel,
                                BluetoothCardReader, BluetoothCardReaderMacAddress, LFBluetoothCardReader, LFBluetoothCardReaderMacAddress,
                                PrinterMacAddress, PrinterName, EnablePrinter,"","","");


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }*/

    /*
    public class GetAPILinkDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffRecordsUpdateService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffRecordsUpdateService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffRecordsUpdateService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_LINK + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        controller.deleteTableData(OffDBController.TBL_LINK);

                        JSONArray jsonArr = jsonObject.getJSONArray("LinkDataObj");

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

                                JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                                if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                    for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                        JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                        String FromTime = oj.getString("FromTime");
                                        String ToTime = oj.getString("ToTime");

                                        controller.insertFuelTimings(SiteId, "", FromTime, ToTime);
                                    }
                                }

                                controller.insertLinkDetails(SiteId, WifiSSId, PumpOnTime, PumpOffTime, AuthorizedFuelingDays, Pulserratio, MacAddress);

                            }
                        }


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }*/

}