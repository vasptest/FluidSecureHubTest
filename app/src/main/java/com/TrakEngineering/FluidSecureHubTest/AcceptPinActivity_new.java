package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.HFCardGAtt.LeServiceHFCard;
import com.TrakEngineering.FluidSecureHubTest.HFCardGAtt.ServiceHFCard;
import com.TrakEngineering.FluidSecureHubTest.LFCardGAtt.ServiceLFCard;
import com.TrakEngineering.FluidSecureHubTest.MagCardGAtt.ServiceMagCard;
import com.TrakEngineering.FluidSecureHubTest.enity.CheckPinFobEntity;
import com.TrakEngineering.FluidSecureHubTest.enity.VehicleRequireEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHubTest.server.ServerHandler.TEXT;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService_fsnp}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class AcceptPinActivity_new extends AppCompatActivity {

    public int cnt123 = 0;

    OffDBController controller = new OffDBController(AcceptPinActivity_new.this);

    private NetworkReceiver receiver = new NetworkReceiver();
    private TextView tv_fobkey;
    private String mDisableFOBReadingForPin;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceName_hf_trak;
    private String mDeviceAddress_hf_trak;
    private String HFDeviceName;
    private String HFDeviceAddress;
    private String mMagCardDeviceName;
    private String mMagCardDeviceAddress;
    private EditText etInput;
    String LF_FobKey = "", ScreenNameForPersonnel = "PERSONNEL", ScreenNameForVehicle = "VEHICLE", KeyboardType = "2", MagCard_personnel = "";
    int Count = 1, LF_ReaderConnectionCountPin = 0, sec_count = 0;

    private static final String TAG = "DeviceControl_Pin";
    public static double CurrentLat = 0, CurrentLng = 0;
    RelativeLayout footer_keybord;
    LinearLayout Linear_layout_Save_back_buttons;
    TextView tv_fob_Reader, tv_or, tv_return, tv_swipekeybord, tv_fob_number, tv_vehicle_no_below, tv_dont_have_fob, tv_enter_vehicle_no, tv_title;
    LinearLayout Linear_layout_vehicleNumber;
    boolean Istimeout_Sec = true;
    public AcceptPinActivity_new.BroadcastCardReader_dataFromServiceToUI ServiceCardReader_pin = null;

    EditText etPersonnelPin;
    private LinearLayout layout_reader_status;
    TextView tv_enter_pin_no, tv_ok, tv_hf_status, tv_lf_status, tv_mag_status, tv_reader_status;
    Button btnSave, btnCancel, btn_ReadFobAgain;
    String IsPersonHasFob = "", IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "", IsVehicleNumberRequire = "", IsStayOpenGate = "", IsGateHub;
    String TimeOutinMinute;
    Timer t, ScreenOutTime;

    ConnectionDetector cd = new ConnectionDetector(AcceptPinActivity_new.this);
    List<Timer> TimerList = new ArrayList<Timer>();
    List<Timer> ScreenTimerList = new ArrayList<Timer>();

    //BLE Upgrade
    String BLEType = "";
    String BLEFileLocation;
    String IsLFUpdate = "N";
    String IsHFUpdate = "N";

    String HFVersion = "";
    String LFVersion = "";

    String BLEVersionLFServer;
    String BLEVersionHFServer;
    String IsHFUpdateServer = "N";
    String IsLFUpdateServer = "N";
    boolean bleLFUpdateSuccessFlag = false;
    boolean bleHFUpdateSuccessFlag = false;

    String FOLDER_PATH_BLE = null;
    HashMap<String, String> hmapSwitchOfflinepin = new HashMap<>();

    private void clearUI() {

        tv_enter_pin_no.setText("Please wait, processing");
        tv_fobkey.setText("");
        tv_fob_number.setText("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control_pin);

        SharedPreferences sharedPre2 = AcceptPinActivity_new.this.getSharedPreferences("storeBT_FOBDetails", Context.MODE_PRIVATE);
        mDisableFOBReadingForPin = sharedPre2.getString("DisableFOBReadingForPin", "");
        mDeviceName = sharedPre2.getString("LFBluetoothCardReader", "");
        mDeviceAddress = sharedPre2.getString("LFBluetoothCardReaderMacAddress", "");
        HFDeviceName = sharedPre2.getString("BluetoothCardReader", "");
        HFDeviceAddress = sharedPre2.getString("BTMacAddress", "");
        mMagCardDeviceName = sharedPre2.getString("MagneticCardReader", ""); //
        mMagCardDeviceAddress = sharedPre2.getString("MagneticCardReaderMacAddress", ""); //

        mDeviceName_hf_trak = sharedPre2.getString("HFTrakCardReader", ""); //
        mDeviceAddress_hf_trak = sharedPre2.getString("HFTrakCardReaderMacAddress", ""); //
        AppConstants.ACS_READER = sharedPre2.getBoolean("ACS_Reader", false);

        SharedPreferences sharedPrefODO = AcceptPinActivity_new.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        IsVehicleNumberRequire = sharedPrefODO.getString(AppConstants.IsVehicleNumberRequire, "");

        SharedPreferences sharedPrefGatehub = AcceptPinActivity_new.this.getSharedPreferences(Constants.PREF_COLUMN_GATE_HUB, Context.MODE_PRIVATE);
        IsGateHub = sharedPrefGatehub.getString(AppConstants.IsGateHub, "");
        IsStayOpenGate = sharedPrefGatehub.getString(AppConstants.IsStayOpenGate, "");

        /* site id is mismatching
        SharedPreferences sharedPref = AcceptPinActivity_new.this.getSharedPreferences(Constants.PREF_COLUMN_SITE, Context.MODE_PRIVATE);
        String dataSite = sharedPref.getString(Constants.PREF_COLUMN_SITE, "");
        SITE_ID = parseSiteData(dataSite);
        AppConstants.SITE_ID = SITE_ID;
        */

        // Sets up UI references.
        tv_fobkey = (TextView) findViewById(R.id.tv_fobkey);
        etInput = (EditText) findViewById(R.id.etInput);

        getSupportActionBar().setTitle(AppConstants.BrandName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        InItGUI();

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.sharedPref_KeyboardType, 0);
        KeyboardType = myPrefkb.getString("KeyboardTypePerson", "2");
        ScreenNameForPersonnel = myPrefkb.getString("ScreenNameForPersonnel", "Personnel");
        ScreenNameForVehicle = myPrefkb.getString("ScreenNameForVehicle", "Vehicle");

        if (ScreenNameForPersonnel.trim().isEmpty())
            ScreenNameForPersonnel = "Personnel";


        etPersonnelPin = (EditText) findViewById(R.id.etPersonnelPin);
        tv_fob_number = (TextView) findViewById(R.id.tv_fob_number);
        tv_enter_pin_no = (TextView) findViewById(R.id.tv_enter_pin_no);
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
        Linear_layout_Save_back_buttons = (LinearLayout) findViewById(R.id.Linear_layout_Save_back_buttons);
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
        tv_ok = (TextView) findViewById(R.id.tv_ok);
        tv_dont_have_fob = (TextView) findViewById(R.id.tv_dont_have_fob);//Enter your PERSONNEL ID in the green box below
        btn_ReadFobAgain = (Button) findViewById(R.id.btn_ReadFobAgain);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        tv_fob_Reader = (TextView) findViewById(R.id.tv_fob_Reader);
        tv_or = (TextView) findViewById(R.id.tv_or);

        tv_title.setText(ScreenNameForPersonnel.toUpperCase() + " IDENTIFICATION");
        tv_fob_Reader.setText("Present your " + ScreenNameForPersonnel + " Access Device Below");
        String content = "Enter your<br> <b>" + ScreenNameForPersonnel + "</b> in<br> the green box below";
        etPersonnelPin.setText("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
            System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv_dont_have_fob.setText(Html.fromHtml(content));
            System.out.println(Html.fromHtml(content));
        }

        tv_enter_pin_no.setText(ScreenNameForPersonnel + " Number:");

        //BLE upgrade
        SharedPreferences myPrefslo = this.getSharedPreferences("BLEUpgradeInfo", 0);
        BLEType = myPrefslo.getString("BLEType", "");
        BLEFileLocation = myPrefslo.getString("BLEFileLocation", "");
        IsLFUpdate = myPrefslo.getString("IsLFUpdate", "");
        IsHFUpdate = myPrefslo.getString("IsHFUpdate", "");
        FOLDER_PATH_BLE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FSCardReader_" + BLEType + "/";

        if (!IsVehicleNumberRequire.equalsIgnoreCase("True")) {
            CheckForFirmwareUpgrade();
            CommonUtils.LogReaderDetails(AcceptPinActivity_new.this);
        }

        getSupportActionBar().setTitle(R.string.fs_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        etPersonnelPin.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                boolean ps = isKeyboardShown(etPersonnelPin.getRootView());
                if (ps) {
                    footer_keybord.setEnabled(true);
                    footer_keybord.setVisibility(View.VISIBLE);
                } else {
                    footer_keybord.setEnabled(false);
                    footer_keybord.setVisibility(View.INVISIBLE);
                }

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeybord();
                onBackPressed();

            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {


                String pin = etPersonnelPin.getText().toString().trim();
                String FKey = AppConstants.APDU_FOB_KEY;

                //////////common for online offline///////////////////////////////
                HashMap<String, String> hmap = new HashMap<>();

                if (pin != null && !pin.trim().isEmpty()) {
                    hmap = controller.getPersonnelDetailsByPIN(pin);
                    hmapSwitchOfflinepin = hmap;
                    offlinePersonInitialization(hmap);

                } else if (FKey != null && !FKey.trim().isEmpty()) {

                    String fob = AppConstants.APDU_FOB_KEY.replace(":", "");
                    hmap = controller.getPersonnelDetailsByFOBnumber(fob);
                    hmapSwitchOfflinepin = hmap;
                    offlinePersonInitialization(hmap);

                }
                ///////////////////////////////

                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                    if (!MagCard_personnel.isEmpty()) {

                        if (cd.isConnectingToInternet()) {
                            if (!isFinishing()) {
                                new GetPinNuOnFobKeyDetection().execute();
                            }
                        }


                    } else if (FKey.equalsIgnoreCase("")) {
                        if (cd.isConnectingToInternet()) {
                            if (!isFinishing()) {
                                new CallSaveButtonFunctionality().execute();//Press Enter fun
                            }
                        } else {
                            AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
                        }

                    } else if (pin.equalsIgnoreCase("") && !FKey.equalsIgnoreCase("")) {
                        if (cd.isConnectingToInternet()) {
                            if (!isFinishing()) {
                                new GetPinNuOnFobKeyDetection().execute();
                            }
                        } else {
                            AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
                        }

                    } else if (!pin.equalsIgnoreCase("") && !FKey.equalsIgnoreCase("")) {

                        if (cd.isConnectingToInternet()) {
                            if (!isFinishing()) {
                                new GetPinNuOnFobKeyDetection().execute();
                            }
                        } else {
                            AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
                        }
                    }
                } else {

                    AppConstants.AUTH_CALL_SUCCESS = false;
                    if (AppConstants.GenerateLogs) AppConstants.WriteinFile("Offline Pin : " + pin);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Temporary loss of cell service ~Switching to offline mode!!");

                    if (OfflineConstants.isOfflineAccess(AcceptPinActivity_new.this)) {
                        //offline----------
                        if (!pin.isEmpty()) {
                            checkPINvalidation(hmap);
                        } else {
                            checkPINvalidation(hmap);
                        }
                    } else {
                        CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Please check your Offline Access");
                        //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                    }

                }
            }
        });


        try {
            etPersonnelPin.setInputType(Integer.parseInt(KeyboardType));
        } catch (Exception e) {
            System.out.println("keyboard exception");
            etPersonnelPin.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        tv_swipekeybord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int InputTyp = etPersonnelPin.getInputType();
                if (InputTyp == 2) {
                    etPersonnelPin.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for 123");
                } else {

                    etPersonnelPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for ABC");
                }

            }
        });

        btn_ReadFobAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResume();
            }
        });


        tv_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeybord();
            }
        });

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        resetReaderStatus();//BLE reader status reset
        RegisterBroadcastForReader();//BroadcastReciver for MagCard,HF and LF Readers

        Count = 1;
        LF_ReaderConnectionCountPin = 0;
        //Toast.makeText(getApplicationContext(), "FOK_KEY" + AppConstants.APDU_FOB_KEY, Toast.LENGTH_SHORT).show();
        //showKeybord();
        etPersonnelPin.setText("");
        AppConstants.APDU_FOB_KEY = "";
        AppConstants.PinLocal_FOB_KEY = "";
        if (IsGateHub.equalsIgnoreCase("True")) {
            Istimeout_Sec = false;
        } else {
            Istimeout_Sec = true;
        }

        TimeoutPinScreen();

        tv_fobkey.setText("");
        LF_FobKey = "";

        btnSave.setClickable(true);
        //Set/Reset EnterPin text
        etPersonnelPin.setText("");

        DisplayScreenInit();


        t = new Timer();
        TimerList.add(t);
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {

                sec_count++;
                invalidateOptionsMenu();
                UpdateReaderStatusToUI();

                if (!AppConstants.PinLocal_FOB_KEY.equalsIgnoreCase("")) {

                    CancelTimer();
                    System.out.println("Pin FOK_KEY" + AppConstants.APDU_FOB_KEY);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //tv_enter_pin_no.setText("Fob Read Successfully");
                            FobreadSuccess();
                        }
                    });

                } else {

                    checkFor5Seconds();

                }

            }
        };
        t.schedule(tt, 1000, 1000);

    }

    @Override
    protected void onPause() {
        super.onPause();

        UnRegisterBroadcastForReader();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CancelTimer();

        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        AppConstants.PinLocal_FOB_KEY = "";
        //  AppConstants.APDU_FOB_KEY = "";
        CancelTimer();
        ScreenTimer();

    }

    private void CancelTimer() {

        for (int i = 0; i < TimerList.size(); i++) {
            TimerList.get(i).cancel();
        }

    }


    private void ScreenTimer() {

        for (int i = 0; i < ScreenTimerList.size(); i++) {
            ScreenTimerList.get(i).cancel();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        menu.findItem(R.id.mreboot_reader).setVisible(true);
        menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.mconfigure_fsnp).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);
        menu.findItem(R.id.mreconnect_ble_readers).setVisible(true);

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

            menu.findItem(R.id.monline).setVisible(true);
            menu.findItem(R.id.mofline).setVisible(false);

        } else {
            menu.findItem(R.id.monline).setVisible(false);
            menu.findItem(R.id.mofline).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                //Connect Ble readers
                return true;
            case R.id.menu_disconnect:
                //Disconnect Ble readers
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.mreconnect_ble_readers:
                new ReconnectBleReaders().execute();
                return true;
            case R.id.mreboot_reader:
                CustomDilaogForRebootCmd(AcceptPinActivity_new.this, "Please enter a code to continue.", "Message");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void displayData_LF(String data) {

        if (data != null || !data.isEmpty()) {

            String Str_data = data.toString().trim();
            System.out.println("FOK_KEY Pin " + Str_data);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  Response LF: " + Str_data);
            String Str_check = Str_data.replace(" ", "");

            if (Str_data.contains("FFFFFFFFFFFFFFFFFFFF") || Str_data.contains("FF FF FF FF FF FF FF FF FF FF")) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Unable to read fob: " + Str_data);
                CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Unable to read fob.  Please Try again..");

            } else if (CommonUtils.ValidateFobkey(Str_check) && Str_check.length() > 4) {

                try {


                    if (Str_check.contains("\n")) {

                        String last_val = "";
                        String[] Seperate = Str_data.split("\n");
                        if (Seperate.length > 1) {
                            last_val = Seperate[Seperate.length - 1];
                        }
                        LF_FobKey = last_val.replaceAll("\\s", "");
                        tv_fobkey.setText(last_val.replace(" ", ""));


                    } else {

                        LF_FobKey = Str_check.replaceAll("\\s", "");
                        tv_fobkey.setText(Str_check.replace(" ", ""));
                    }

                    if (!LF_FobKey.equalsIgnoreCase("") && LF_FobKey.length() > 5) {//
                        //tv_enter_pin_no.setText("Fob Read Successfully");
                        tv_fob_number.setText("");//"Fob No: " + LF_FobKey
                        AppConstants.PinLocal_FOB_KEY = LF_FobKey;
                        AppConstants.APDU_FOB_KEY = LF_FobKey;
                    }

                } catch (Exception ex) {
                    System.out.println(ex);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  displayData --Exception " + ex);
                }

            }
            //BLE Upgrade
            else if (!Str_data.equals("00 00 00")) {
                LFVersion = Str_data;
                SharedPreferences sharedPref = getSharedPreferences("LFVersionInfo", 0);
                SharedPreferences.Editor editor1 = sharedPref.edit();
                editor1.putString("LFVersion", LFVersion);
                editor1.commit();
                System.out.println("BLEVERSION: " + LFVersion);
                String serverRes = sendVersionToServer(LFVersion);
                try {

                    if (serverRes != null && !serverRes.equals("")) {

                        JSONObject jsonObject = new JSONObject(serverRes);

                        String ResponceMessage = jsonObject.getString("ResponceMessage");


                        System.out.println("ResponceMessage.." + ResponceMessage);

                        if (ResponceMessage.equalsIgnoreCase("Success!")) {
                            BLEVersionLFServer = jsonObject.getString("BLEVersionLF");
                            IsLFUpdateServer = jsonObject.getString("IsLFUpdate");
                            System.out.println("IsLFUpdateServer.." + IsLFUpdateServer);
                        }
                    }
                } catch (Exception e) {
                    Log.d("Ex", e.getMessage());
                }
                if (IsLFUpdateServer.trim().equalsIgnoreCase("Y")) {
                    bleLFUpdateSuccessFlag = true;
                    SharedPreferences sharedPre = getSharedPreferences("BLEUpgradeFlag", 0);
                    SharedPreferences.Editor editor = sharedPre.edit();
                    editor.putString("bleLFUpdateSuccessFlag", "Y");
                    editor.commit();
                } else {
                    bleLFUpdateSuccessFlag = false;
                    SharedPreferences sharedPre = getSharedPreferences("BLEUpgradeFlag", 0);
                    SharedPreferences.Editor editor = sharedPre.edit();
                    editor.putString("bleLFUpdateSuccessFlag", "N");
                    editor.commit();
                }
            }
        }
    }

    private void displayData_HF(String data) {

        if (data != null && !data.isEmpty()) {

            String Str_data = data.toString().trim();
            System.out.println("FOK_KEY pIN " + Str_data);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Response HF: " + Str_data);
            String Str_check = Str_data.replace(" ", "");

            if (Str_data.contains("FFFFFFFFFFFFFFFFFFFF") || Str_data.contains("FF FF FF FF FF FF FF FF FF FF")) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "Unable to read fob: " + Str_data);
                CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Unable to read fob.  Please Try again..");

            } else if (CommonUtils.ValidateFobkey(Str_check) && Str_check.length() > 4) {

                try {

                    if (Str_check.contains("\n")) {

                        String last_val = "";
                        String[] Seperate = Str_data.split("\n");
                        if (Seperate.length > 1) {
                            last_val = Seperate[Seperate.length - 1];
                        }
                        LF_FobKey = last_val.replaceAll("\\s", "");
                        tv_fobkey.setText(last_val.replace(" ", ""));


                    } else {
                        LF_FobKey = Str_check.replaceAll("\\s", "");
                        tv_fobkey.setText(Str_check.replace(" ", ""));
                    }


                    if (!LF_FobKey.equalsIgnoreCase("") && LF_FobKey.length() > 5) {//
                        //tv_enter_pin_no.setText("Fob Read Successfully");
                        tv_fob_number.setText("");//"Fob No: " + LF_FobKey
                        AppConstants.PinLocal_FOB_KEY = LF_FobKey;
                        AppConstants.APDU_FOB_KEY = LF_FobKey;

                    }

                } catch (Exception ex) {
                    System.out.println(ex);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  displayData_HF --Exception " + ex);
                }

            }
            //BLE Upgrade
            else if (!Str_data.trim().equals("00 00 00 00 00 00 00 00 00 00")) {
                HFVersion = Str_data;
                SharedPreferences sharedPref = getSharedPreferences("HFVersionInfo", 0);
                SharedPreferences.Editor editor1 = sharedPref.edit();
                editor1.putString("HFVersion", HFVersion);
                editor1.commit();

                System.out.println("BLEVERSION: " + HFVersion);
                String serverRes = sendVersionToServer(HFVersion);
                try {

                    if (serverRes != null && !serverRes.equals("")) {

                        JSONObject jsonObject = new JSONObject(serverRes);

                        String ResponceMessage = jsonObject.getString("ResponceMessage");


                        System.out.println("ResponceMessage.." + ResponceMessage);

                        if (ResponceMessage.equalsIgnoreCase("Success!")) {
                            BLEVersionHFServer = jsonObject.getString("BLEVersionHF");
                            IsHFUpdateServer = jsonObject.getString("IsHFUpdate");
                            System.out.println("IsLFUpdateServer.." + IsLFUpdateServer);
                        }
                    }
                } catch (Exception e) {
                    Log.d("Ex", e.getMessage());
                }
                if (IsHFUpdateServer.trim().equalsIgnoreCase("Y")) {
                    bleHFUpdateSuccessFlag = true;
                    SharedPreferences sharedPre = getSharedPreferences("BLEUpgradeFlag", 0);
                    SharedPreferences.Editor editor = sharedPre.edit();
                    editor.putString("bleHFUpdateSuccessFlag", "Y");
                    editor.commit();
                } else {
                    bleHFUpdateSuccessFlag = false;
                    SharedPreferences sharedPre = getSharedPreferences("BLEUpgradeFlag", 0);
                    SharedPreferences.Editor editor = sharedPre.edit();
                    editor.putString("bleHFUpdateSuccessFlag", "N");
                    editor.commit();
                }
            }
        }
    }

    private void displayData_MagCard(String data) {

        System.out.println("MagCard data 002----" + data);
        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " displayData_MagCard " + data);


        if (data != null && !data.isEmpty()) {

            String Str_data = data.toString().trim();
            Log.i(TAG, "displayData MagCard:" + Str_data);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  displayData MagCard: " + Str_data);

            String Str_check = Str_data.replace(" ", "");
            if (!CommonUtils.ValidateFobkey(Str_check) || Str_data.contains("FFFFFFFFFFFFFFFFFFFF") || Str_data.contains("FF FF FF FF FF FF FF FF FF FF")) {

                MagCard_personnel = "";
                //CommonUtils.AutoCloseCustomMessageDilaog(DeviceControlActivity_Pin.this, "Message", "Unable to read MagCard.  Please Try again..");

            } else if (Str_check.length() > 5) {

                try {
                    MagCard_personnel = Str_check;
                    tv_fob_number.setText("");//"Fob No: " + LF_FobKey
                    AppConstants.PinLocal_FOB_KEY = MagCard_personnel;
                    //AppConstants.APDU_FOB_KEY = MagCard_personnel;

                } catch (Exception ex) {
                    MagCard_personnel = "";
                    System.out.println(ex);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  displayData Split MagCard  --Exception " + ex);
                }

            }

        } else {
            MagCard_personnel = "";
        }
    }

    private String sendVersionToServer(String bleVersion) {
        SharedPreferences sharedPrefODO = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String PersonId = sharedPrefODO.getString(AppConstants.HubId, "");
        String bleType = BLEType;

        SharedPreferences sharedPref = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");

        String authStringDefTire = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "CheckCurrentBLEVersionOnDemand");
        BleVersionData bleVersionData = new BleVersionData();
        bleVersionData.BLEType = bleType;
        if (BLEType.equals("HF"))
            bleVersionData.VersionHF = bleVersion;
        else
            bleVersionData.VersionHF = "";
        if (BLEType.equals("LF"))
            bleVersionData.VersionLF = bleVersion;
        else
            bleVersionData.VersionLF = "";
        bleVersionData.PersonId = PersonId;
        Gson gson = new Gson();
        final String jsonDataDefTire = gson.toJson(bleVersionData);
        String response = "";
        try {
            if (cd.isConnecting())
                response = new AcceptPinActivity_new.sendBleVersionData().execute(jsonDataDefTire, authStringDefTire).get();

        } catch (Exception e) {
            System.out.println(e);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  response BLE LF version number  --Exception " + e);
        }

        return response;

    }

    public void FobreadSuccess() {

        runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {

                AppConstants.PinLocal_FOB_KEY = "";
                ScreenTimer();

                if (MagCard_personnel != null && !MagCard_personnel.isEmpty()) {

                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
                        if (!isFinishing()) {
                            new GetPinNuOnFobKeyDetection().execute();
                        }
                    } else {
                        //offline---------------
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile("Offline MagCard_personnel Not yet implemented");
                        CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Offline MagCard_personnel Not yet implemented");
                    }


                } else if (AppConstants.APDU_FOB_KEY != null) {

                    String test = AppConstants.APDU_FOB_KEY;
                    tv_fob_number.setText("Access Device No: " + test);

                    try {

                        String fob = AppConstants.APDU_FOB_KEY.replace(":", "").trim();
                        HashMap<String, String> hmap = controller.getPersonnelDetailsByFOBnumber(fob);
                        hmapSwitchOfflinepin = hmap;
                        offlinePersonInitialization(hmap);

                        tv_fobkey.setText(fob);
                        CommonUtils.PlayBeep(AcceptPinActivity_new.this);
                        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
                            if (!isFinishing()) {

                                new GetPinNuOnFobKeyDetection().execute();
                            }
                        } else {

                            if (OfflineConstants.isOfflineAccess(AcceptPinActivity_new.this)) {
                                checkPINvalidation(hmap);
                                String PinNumber = hmap.get("PinNumber");
                                etPersonnelPin.setText(PinNumber);
                            } else {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile("Please check your Offline Access");
                                CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Please check your Offline Access");
                                //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                            }
                        }
                        tv_fob_number.setText("Access Device No: " + test);

                    } catch (Exception e) {
                        System.out.println("Pin FobreadSuccess-" + e.getMessage());
                    }

                } else {
                    AppConstants.colorToastBigFont(getApplicationContext(), "Access Device not found", Color.RED);
                }

            }
        });

    }

    private void InItGUI() {

        Linear_layout_vehicleNumber = (LinearLayout) findViewById(R.id.Linear_layout_vehicleNumber);
        layout_reader_status = (LinearLayout) findViewById(R.id.layout_reader_status);
        tv_fob_Reader = (TextView) findViewById(R.id.tv_fob_Reader);
        tv_or = (TextView) findViewById(R.id.tv_or);
        btnSave = (Button) findViewById(R.id.btnSave);
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
        Linear_layout_Save_back_buttons = (LinearLayout) findViewById(R.id.Linear_layout_Save_back_buttons);
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
        tv_fob_number = (TextView) findViewById(R.id.tv_fob_number);
        tv_vehicle_no_below = (TextView) findViewById(R.id.tv_vehicle_no_below);
        tv_enter_vehicle_no = (TextView) findViewById(R.id.tv_enter_vehicle_no);
        tv_dont_have_fob = (TextView) findViewById(R.id.tv_dont_have_fob);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_hf_status = (TextView) findViewById(R.id.tv_hf_status);
        tv_lf_status = (TextView) findViewById(R.id.tv_lf_status);
        tv_mag_status = (TextView) findViewById(R.id.tv_mag_status);
        tv_reader_status = (TextView) findViewById(R.id.tv_reader_status);

    }

    //============SoftKeyboard enable/disable Detection======
    private boolean isKeyboardShown(View rootView) {
        /* 128dp = 32dp * 4, minimum button height 32dp and generic 4 rows soft keyboard */
        final int SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD = 128;

        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        /* heightDiff = rootView height - status bar height (r.top) - visible frame height (r.bottom - r.top) */
        int heightDiff = rootView.getBottom() - r.bottom;
        /* Threshold size: dp to pixels, multiply with display density */
        boolean isKeyboardShown = heightDiff > SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD * dm.density;

        Log.d(TAG, "isKeyboardShown ? " + isKeyboardShown + ", heightDiff:" + heightDiff + ", density:" + dm.density
                + "root view height:" + rootView.getHeight() + ", rect:" + r);

        return isKeyboardShown;
    }

    public void cancelAction(View v) {

        hideKeybord();
        onBackPressed();
    }

    @Override
    public void onBackPressed() {

        // ActivityHandler.removeActivity(3);
        Istimeout_Sec = false;
        //AppConstants.ClearEdittextFielsOnBack(AcceptPinActivity.this); //Clear EditText on move to welcome activity.
        finish();
    }

    public void DisplayScreenInit() {

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
            SharedPreferences sharedPrefODO = AcceptPinActivity_new.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            IsPersonHasFob = sharedPrefODO.getString(AppConstants.IsPersonHasFob, "false");
        } else {
            IsPersonHasFob = controller.getOfflineHubDetails(AcceptPinActivity_new.this).PersonhasFOB;

            if (IsPersonHasFob.trim().equalsIgnoreCase("y"))
                IsPersonHasFob = "true";
        }


        if (IsPersonHasFob.equalsIgnoreCase("true")) {

            // Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.INVISIBLE);

            int widthi = 0;
            int heighti = 0;
            LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
            parmsi.weight = 0;
            btnSave.setLayoutParams(parmsi);

            int widthp = ActionBar.LayoutParams.MATCH_PARENT;
            int heightp = 0;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(widthp, heightp);
            parms.gravity = Gravity.CENTER;
            etPersonnelPin.setLayoutParams(parms);
            tv_dont_have_fob.setLayoutParams(parms);
            tv_or.setLayoutParams(parms);

            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
            tv_fob_Reader.setVisibility(View.INVISIBLE);
            tv_dont_have_fob.setVisibility(View.INVISIBLE);
            tv_or.setVisibility(View.INVISIBLE);

            hideKeybord();

        } else {


            /*int widthi = 0;
            int heighti = 0;
            LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
            parmsi.weight = 0;
            btnSave.setLayoutParams(parmsi);*/

            btnCancel.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);

            int width = ActionBar.LayoutParams.MATCH_PARENT;
            int height = ActionBar.LayoutParams.WRAP_CONTENT;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
            parms.gravity = Gravity.CENTER;
            etPersonnelPin.setLayoutParams(parms);

            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
            tv_fob_Reader.setVisibility(View.VISIBLE);
            tv_dont_have_fob.setVisibility(View.VISIBLE);
            tv_or.setVisibility(View.VISIBLE);


        }

        int width = 0;
        int height = 0;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        tv_ok.setLayoutParams(parms);

        etPersonnelPin.setEnabled(true);
        btnSave.setEnabled(true);
        tv_fob_number.setText("");
        tv_enter_pin_no.setVisibility(View.INVISIBLE);
        tv_fob_Reader.setVisibility(View.VISIBLE);
        tv_fob_Reader.setVisibility(View.VISIBLE);
        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
        etPersonnelPin.setVisibility(View.VISIBLE);


        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = 0;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        tv_enter_pin_no.setLayoutParams(parmsi);

    }

    public void DisplayScreenFobReadSuccess() {

        int width = ActionBar.LayoutParams.WRAP_CONTENT;
        int height = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        tv_ok.setLayoutParams(parms);

        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        tv_enter_pin_no.setLayoutParams(parmsi);

        //Display on success
        tv_fob_number.setVisibility(View.GONE);
        tv_fob_Reader.setVisibility(View.GONE);
        tv_or.setVisibility(View.GONE);
        tv_enter_pin_no.setVisibility(View.VISIBLE);
        tv_ok.setText("Access Device read successfully");
        tv_dont_have_fob.setVisibility(View.GONE);
        etPersonnelPin.setVisibility(View.GONE);

        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);


        btnCancel.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);


    }

    public class CallSaveButtonFunctionality extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            if (AppConstants.ServerCallLogs)
                Log.w(TAG, "SC_Log CallSaveButtonFunctionality onPreExecute ");
            if (AppConstants.ServerCallLogs)
                AppConstants.WriteinFile(TAG + "SC_Log CallSaveButtonFunctionality onPreExecute ");

            String s = "Please wait...";
            SpannableString ss2 = new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(AcceptPinActivity_new.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {

            String resp = "";
            String vehicleNumber = "";
            if (AppConstants.ServerCallLogs)
                Log.w(TAG, "SC_Log CallSaveButtonFunctionality doInBackground ");
            if (AppConstants.ServerCallLogs)
                AppConstants.WriteinFile(TAG + "SC_Log CallSaveButtonFunctionality doInBackground ");

            try {

                if (!etPersonnelPin.getText().toString().trim().isEmpty()) {

                    if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                        Constants.AccPersonnelPIN_FS1 = etPersonnelPin.getText().toString().trim();

                        vehicleNumber = Constants.AccVehicleNumber_FS1;

                    } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                        Constants.AccPersonnelPIN = etPersonnelPin.getText().toString().trim();

                        vehicleNumber = Constants.AccVehicleNumber;

                    } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                        Constants.AccPersonnelPIN_FS3 = etPersonnelPin.getText().toString().trim();

                        vehicleNumber = Constants.AccVehicleNumber_FS3;
                        Log.i("ps_Vechile no", "Step 3:" + vehicleNumber);
                    } else {
                        Constants.AccPersonnelPIN_FS4 = etPersonnelPin.getText().toString().trim();
                        vehicleNumber = Constants.AccVehicleNumber_FS4;
                    }

                    Istimeout_Sec = false;

                    VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
                    objEntityClass.IMEIUDID = AppConstants.getIMEI(AcceptPinActivity_new.this);
                    objEntityClass.VehicleNumber = vehicleNumber;
                    objEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
                    objEntityClass.SiteId = Integer.parseInt(AppConstants.SITE_ID);
                    objEntityClass.PersonnelPIN = etPersonnelPin.getText().toString().trim();
                    objEntityClass.RequestFromAPP = "AP";
                    objEntityClass.FromNewFOBChange = "Y";
                    objEntityClass.FOBNumber = AppConstants.APDU_FOB_KEY;
                    objEntityClass.IsVehicleNumberRequire = IsVehicleNumberRequire;
                    objEntityClass.Barcode = "";

                    SharedPreferences pref1 = getSharedPreferences("LFVersionInfo", 0);
                    LFVersion = pref1.getString("LFVersion", "");

                    SharedPreferences pref2 = getSharedPreferences("HFVersionInfo", 0);
                    HFVersion = pref2.getString("HFVersion", "");

                    if (HFVersion != "") {
                        if (IsHFUpdateServer.trim().equalsIgnoreCase("Y") || IsHFUpdateServer == "Y") {
                            objEntityClass.HFVersion = HFVersion;
                        } else {
                            objEntityClass.HFVersion = "";
                        }
                    } else {
                        objEntityClass.HFVersion = "";
                    }
                    if (LFVersion != "") {
                        if (IsLFUpdateServer.trim().equalsIgnoreCase("Y") || IsLFUpdateServer == "Y") {
                            objEntityClass.LFVersion = LFVersion;
                        } else {
                            objEntityClass.LFVersion = "";
                        }
                    } else {
                        objEntityClass.LFVersion = "";
                    }

                    /*objEntityClass.HFVersion = "123456";
                    objEntityClass.LFVersion = "456789";*/

                    AppConstants.FOB_KEY_VEHICLE = AppConstants.APDU_FOB_KEY;

                    Log.i(TAG, "VehicleNumber: " + vehicleNumber);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "VehicleNumber: " + vehicleNumber);

                    if (AppConstants.APDU_FOB_KEY.equalsIgnoreCase("")) {
                        Log.i(TAG, "PIN EN Manually: " + etPersonnelPin.getText().toString().trim() + "  Fob:" + AppConstants.APDU_FOB_KEY);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " PIN EN Manually: " + etPersonnelPin.getText().toString().trim() + "  Fob:" + AppConstants.APDU_FOB_KEY);
                    } else {
                        Log.i(TAG, "PIN FOB:" + AppConstants.APDU_FOB_KEY + "  PIN No: " + String.valueOf(etPersonnelPin.getText()));
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "PIN FOB:" + AppConstants.APDU_FOB_KEY + "  PIN No: " + String.valueOf(etPersonnelPin.getText()));
                    }


                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass);
                    String userEmail = CommonUtils.getCustomerDetails(AcceptPinActivity_new.this).PersonEmail;

                    System.out.println("jsonDatajsonDatajsonData" + jsonData);
                    //----------------------------------------------------------------------------------
                    String authString = "Basic " + AppConstants.convertStingToBase64(objEntityClass.IMEIUDID + ":" + userEmail + ":" + "CheckVehicleRequireOdometerEntryAndRequireHourEntry");

                    OkHttpClient client = new OkHttpClient();
                    client.setConnectTimeout(4, TimeUnit.SECONDS);
                    client.setReadTimeout(4, TimeUnit.SECONDS);
                    client.setWriteTimeout(4, TimeUnit.SECONDS);


                    RequestBody body = RequestBody.create(TEXT, jsonData);
                    Request request = new Request.Builder()
                            .url(AppConstants.webURL)
                            .post(body)
                            .addHeader("Authorization", authString)
                            .build();


                    Response response = null;
                    response = client.newCall(request).execute();
                    resp = response.body().string();

                }

            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " CallSaveButtonFunctionality  STE2 " + e);
                GetBackToWelcomeActivity();
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");


            } catch (Exception e) {
                e.printStackTrace();
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");

            }
            return resp;
        }


        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(String serverRes) {

            pd.dismiss();

            if (serverRes != null && !serverRes.isEmpty()) {

                try {

                    if (AppConstants.ServerCallLogs)
                        Log.w(TAG, "SC_Log CallSaveButtonFunctionality onPostExecute ");
                    if (AppConstants.ServerCallLogs)
                        AppConstants.WriteinFile(TAG + "SC_Log CallSaveButtonFunctionality onPostExecute ");

                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage.." + ResponceMessage);


                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" PIN Accepted:" + etPersonnelPin.getText().toString().trim());

                        btnSave.setClickable(false);

                        SharedPreferences sharedPrefODO = AcceptPinActivity_new.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
                        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
                        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
                        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");


                        if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(AcceptPinActivity_new.this, AcceptDeptActivity.class);
                            startActivity(intent);

                        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(AcceptPinActivity_new.this, AcceptOtherActivity.class);
                            startActivity(intent);

                        } else {

                            AcceptServiceCall asc = new AcceptServiceCall();
                            asc.activity = AcceptPinActivity_new.this;
                            asc.checkAllFields();
                        }
                    } else {

                        String ResponceText = jsonObject.getString("ResponceText");
                        String ValidationFailFor = jsonObject.getString("ValidationFailFor");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " PIN rejected:" + etPersonnelPin.getText().toString().trim() + " Error:" + ResponceText);

                        if (ValidationFailFor.equalsIgnoreCase("PinWithFob")) {

                            CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceText);

                        } else if (ValidationFailFor.equalsIgnoreCase("Pin")) {

                            //Clear Pin edit text
                            if (Constants.CurrentSelectedHose.equals("FS1")) {
                                Constants.AccPersonnelPIN_FS1 = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS2")) {
                                Constants.AccPersonnelPIN = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS3")) {
                                Constants.AccPersonnelPIN_FS3 = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS4")) {
                                Constants.AccPersonnelPIN_FS4 = "";
                            }

                            //AppConstants.colorToastBigFont(AcceptPinActivity_new.this, ResponceText, Color.RED);
                            //CommonUtils.AlertDialogAutoClose(AcceptPinActivity_new.this, "Message", ResponceText);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  ValidateFor Pin" + ResponceText);


                            DilaogRecreate(AcceptPinActivity_new.this, "Message", ResponceText);


                        } else if (ValidationFailFor.equalsIgnoreCase("Vehicle")) {

                            AppConstants.colorToastBigFont(AcceptPinActivity_new.this, ResponceText, Color.RED);
                            //CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceText);
                            if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + "  ValidateFor Vehicle" + ResponceText);

                            AppConstants.ClearEdittextFielsOnBack(AcceptPinActivity_new.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(AcceptPinActivity_new.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } else {

                            //AppConstants.colorToastBigFont(AcceptPinActivity_new.this, ResponceText, Color.RED);
                            CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceText);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  ValidateFor Else" + ResponceText);

                            /*AppConstants.colorToastBigFont(this, "Some thing went wrong Please try again..\n"+ResponceText, Color.RED);
                             if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+" Some thing went wrong Please try again..(~else~)\n"+ResponceText);
                            AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_fsnp.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DeviceControlActivity_fsnp.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);*/
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    AppConstants.NETWORK_STRENGTH = false;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");

                }
            } else {

                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "CallSaveButtonFunctionality Temporary loss of cell service ~Switching to offline mode!!");
                if (OfflineConstants.isOfflineAccess(AcceptPinActivity_new.this)) {
                    //offline----------
                    checkPINvalidation(hmapSwitchOfflinepin);

                } else {
                    CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Please check your Offline Access");
                    //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                }

            }

        }
    }

    public class GetPinNuOnFobKeyDetection extends AsyncTask<Void, Void, String> {


        //ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            if (AppConstants.ServerCallLogs)
                Log.w(TAG, "SC_Log GetPinNuOnFobKeyDetection onPreExecute ");
            if (AppConstants.ServerCallLogs)
                AppConstants.WriteinFile(TAG + "SC_Log GetPinNuOnFobKeyDetection onPreExecute ");

            String text = "Please wait..";
            SpannableStringBuilder biggerText = new SpannableStringBuilder(text);
            biggerText.setSpan(new RelativeSizeSpan(2.00f), 0, text.length(), 0);
            Toast.makeText(getApplicationContext(), biggerText, Toast.LENGTH_LONG).show();

        }

        protected String doInBackground(Void... arg0) {

            String resp = "";

            if (AppConstants.ServerCallLogs)
                Log.w(TAG, "SC_Log GetPinNuOnFobKeyDetection doInBackground ");
            if (AppConstants.ServerCallLogs)
                AppConstants.WriteinFile(TAG + "SC_Log GetPinNuOnFobKeyDetection doInBackground ");

            CheckPinFobEntity objEntityClass = new CheckPinFobEntity();
            objEntityClass.IMEIUDID = AppConstants.getIMEI(AcceptPinActivity_new.this);
            objEntityClass.PersonPIN = String.valueOf(etPersonnelPin.getText());
            objEntityClass.PersonFOBNumber = AppConstants.APDU_FOB_KEY;
            objEntityClass.FromNewFOBChange = "Y";
            objEntityClass.MagneticCardNumber = MagCard_personnel;

            SharedPreferences pref1 = getSharedPreferences("LFVersionInfo", 0);
            LFVersion = pref1.getString("LFVersion", "");

            SharedPreferences pref2 = getSharedPreferences("HFVersionInfo", 0);
            HFVersion = pref2.getString("HFVersion", "");

            if (HFVersion != "") {
                if (IsHFUpdateServer.trim().equalsIgnoreCase("Y") || IsHFUpdateServer == "Y") {
                    objEntityClass.HFVersion = HFVersion;
                } else {
                    objEntityClass.HFVersion = "";
                }
            } else {
                objEntityClass.HFVersion = "";
            }
            if (LFVersion != "") {
                if (IsLFUpdateServer.trim().equalsIgnoreCase("Y") || IsLFUpdateServer == "Y") {
                    objEntityClass.LFVersion = LFVersion;
                } else {
                    objEntityClass.LFVersion = "";
                }
            } else {
                objEntityClass.LFVersion = "";
            }

            /*objEntityClass.HFVersion = "pin799";
            objEntityClass.LFVersion = "pin456";*/

            //objEntityClass.IsBothFobAndPinRequired = IsBothFobAndPinRequired_flag;

            System.out.println(TAG + "Personnel PIN: Read FOB:" + AppConstants.APDU_FOB_KEY + "  PIN Number: " + String.valueOf(etPersonnelPin.getText()));
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Personnel PIN: Read FOB:" + AppConstants.APDU_FOB_KEY + "  PIN Number: " + String.valueOf(etPersonnelPin.getText()));


            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(objEntityClass);
                String userEmail = CommonUtils.getCustomerDetails(AcceptPinActivity_new.this).PersonEmail;

                System.out.println("jsonData123" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objEntityClass.IMEIUDID + ":" + userEmail + ":" + "CheckValidPinOrFOBNUmber");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);


                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();


                Response response = null;
                response = client.newCall(request).execute();
                resp = response.body().string();


            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetPinNuOnFobKeyDetection  STE1 " + e);
                GetBackToWelcomeActivity();
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");


            } catch (Exception ex) {
                CommonUtils.LogMessage("TAG", "CheckValidPinOrFOBNUmber ", ex);
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");

            }

            return resp;
        }


        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(String serverRes) {


            try {
                if (AppConstants.ServerCallLogs)
                    Log.w(TAG, "SC_Log GetPinNuOnFobKeyDetection onPostExecute ");
                if (AppConstants.ServerCallLogs)
                    AppConstants.WriteinFile(TAG + "SC_Log GetPinNuOnFobKeyDetection onPostExecute ");


                if (serverRes != null && !serverRes.isEmpty()) {

                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    System.out.println("ResponceMessage..dt.." + ResponceMessage);
                    String PersonFOBNumber = jsonObject.getString("PersonFOBNumber");
                    String PersonPIN = jsonObject.getString("PersonPIN");
                    String IsNewFob = jsonObject.getString("IsNewFob");
                    String IsBothFobAndPinRequired = jsonObject.getString("IsBothFobAndPinRequired");
                    String IsNewMagneticCardReaderNumber = jsonObject.getString("IsNewMagneticCardReaderNumber");

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        DisplayScreenFobReadSuccess();
                        tv_enter_pin_no.setText(ScreenNameForPersonnel + " Number:" + PersonPIN);
                        System.out.println("PersonFOBNumber.." + PersonFOBNumber + "PersonPin" + PersonPIN);
                        etPersonnelPin.setText(PersonPIN);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                InCaseOfGateHub();
                            }
                        }, 1000);


                    } else {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Pin Fob Fail: " + ResponceMessage);
                        if (IsBothFobAndPinRequired.equalsIgnoreCase("yes")) {

                            AcceptPinNumber();

                            InputMethodManager inputMethodManager = (InputMethodManager) etPersonnelPin.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            etPersonnelPin.requestFocus();
                            inputMethodManager.showSoftInput(etPersonnelPin, 0);

                            if (IsPersonHasFob.equalsIgnoreCase("true")) {
                                CommonUtils.SimpleMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                            } else {
                                ResetTimeoutPinScreen();
                                //CommonUtils.showCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                                CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);

                            }


                        } else if (IsNewFob.equalsIgnoreCase("No")) {
                            AppConstants.APDU_FOB_KEY = "";
                            onResume();

                            tv_fob_Reader.setVisibility(View.GONE);

                            /*int width = ActionBar.LayoutParams.WRAP_CONTENT;
                            int height = ActionBar.LayoutParams.WRAP_CONTENT;
                            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
                            parms.gravity = Gravity.CENTER;
                            tv_ok.setLayoutParams(parms);
                            tv_ok.setText("Invalid Access Device or Unassigned FOB");/
*/
                            ResetTimeoutPinScreen();
                            //CommonUtils.showCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                            CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);

                        } else if (IsNewMagneticCardReaderNumber.equalsIgnoreCase("yes")) {

                            AcceptPinNumber();

                            InputMethodManager inputMethodManager = (InputMethodManager) etPersonnelPin.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            etPersonnelPin.requestFocus();
                            inputMethodManager.showSoftInput(etPersonnelPin, 0);

                            if (IsPersonHasFob.equalsIgnoreCase("true")) {
                                CommonUtils.SimpleMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                            } else {
                                ResetTimeoutPinScreen();
                                //CommonUtils.showCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                                CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                            }

                        } else {

                            AcceptPinNumber();

                            InputMethodManager inputMethodManager = (InputMethodManager) etPersonnelPin.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            etPersonnelPin.requestFocus();
                            inputMethodManager.showSoftInput(etPersonnelPin, 0);

                            if (IsPersonHasFob.equalsIgnoreCase("true")) {
                                CommonUtils.SimpleMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                            } else {
                                ResetTimeoutPinScreen();
                                //CommonUtils.showCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                                CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ResponceMessage);
                            }
                        }

                        if (IsGateHub.equalsIgnoreCase("True")) {
                            Istimeout_Sec = false;
                        } else {
                            Istimeout_Sec = true;
                        }
                        TimeoutPinScreen();
                        btnSave.setEnabled(true);
                        tv_fob_number.setText("");
                        tv_fob_number.setVisibility(View.GONE);
                        tv_or.setVisibility(View.GONE);
                        tv_dont_have_fob.setVisibility(View.VISIBLE);
                        String content = "Enter your<br> <b>" + ScreenNameForPersonnel + "</b> in<br> the green box below";

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                            System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            tv_dont_have_fob.setText(Html.fromHtml(content));
                            System.out.println(Html.fromHtml(content));
                        }

                        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
                        etPersonnelPin.setVisibility(View.VISIBLE);
                        etPersonnelPin.setText("");
                    }

                } else {

                    AppConstants.NETWORK_STRENGTH = false;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "GetPinNuOnFobKeyDetection Temporary loss of cell service ~Switching to offline mode!!");
                    if (OfflineConstants.isOfflineAccess(AcceptPinActivity_new.this)) {
                        //offline----------
                        checkPINvalidation(hmapSwitchOfflinepin);

                    } else {
                        CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Please check your Offline Access");
                        //AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                    }
                }

            } catch (Exception ex) {
                Log.e("TAG", ex.getMessage());
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");

            }

        }
    }

    public void TimeoutPinScreen() {
        Log.i("TimeoutPinScreen", "Start");
        SharedPreferences sharedPrefODO = AcceptPinActivity_new.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        AppConstants.HUB_ID = sharedPrefODO.getString(AppConstants.HubId, "");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;

        ScreenOutTime = new Timer();
        ScreenTimerList.add(ScreenOutTime);
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                Log.i("TimeoutPinScreen", "Running..");
                if (Istimeout_Sec) {

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideKeybord();
                                Istimeout_Sec = false;
                                AppConstants.ClearEdittextFielsOnBack(AcceptPinActivity_new.this);


                                Intent i = new Intent(AcceptPinActivity_new.this, WelcomeActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });

                        ScreenTimer();
                    } catch (Exception e) {

                        System.out.println(e);
                    }

                }

            }

            ;
        };
        ScreenOutTime.schedule(ttt, screenTimeOut, 500);


    }

    public void ResetTimeoutPinScreen() {


        ScreenTimer();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TimeoutPinScreen();
    }

    public void hideKeybord() {

        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void showKeybord() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void AcceptPinNumber() {

        tv_fob_Reader.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);

        int width = ActionBar.LayoutParams.MATCH_PARENT;
        int height = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        etPersonnelPin.setLayoutParams(parms);

        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        parmsi.weight = 1;
        btnSave.setLayoutParams(parmsi);


    }

    public String parseSiteData(String dataSite) {
        String ssiteId = "";
        try {
            if (dataSite != null) {
                JSONArray jsonArray = new JSONArray(dataSite);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jo = jsonArray.getJSONObject(i);

                    String SiteId = jo.getString("SiteId");
                    String SiteNumber = jo.getString("SiteNumber");
                    String SiteName = jo.getString("SiteName");
                    String SiteAddress = jo.getString("SiteAddress");
                    String Latitude = jo.getString("Latitude");
                    String Longitude = jo.getString("Longitude");
                    String HoseId = jo.getString("HoseId");
                    String HoseNumber = jo.getString("HoseNumber");
                    String WifiSSId = jo.getString("WifiSSId");
                    String UserName = jo.getString("UserName");
                    String Password = jo.getString("Password");

                    System.out.println("Wifi Password...." + Password);

                    //AppConstants.WIFI_PASSWORD = "";

                    ssiteId = SiteId;
                }
            }
        } catch (Exception ex) {

            CommonUtils.LogMessage(TAG, "", ex);
        }

        return ssiteId;
    }

    public void InCaseOfGateHub() {

        //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" InCaseOfGateHub PIN Accepted:" + etPersonnelPin.getText().toString().trim());

        String vehicleNumber = "";

        if (!etPersonnelPin.getText().toString().trim().isEmpty()) {

            if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                Constants.AccPersonnelPIN_FS1 = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber_FS1;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                Constants.AccPersonnelPIN = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                Constants.AccPersonnelPIN_FS3 = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber_FS3;
            } else {
                Constants.AccPersonnelPIN_FS4 = etPersonnelPin.getText().toString().trim();
                vehicleNumber = Constants.AccVehicleNumber_FS4;
            }
        }

        Istimeout_Sec = false;


        btnSave.setClickable(false);

        SharedPreferences sharedPrefODO = AcceptPinActivity_new.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");


        if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptPinActivity_new.this, AcceptDeptActivity.class);
            startActivity(intent);

        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptPinActivity_new.this, AcceptOtherActivity.class);
            startActivity(intent);

        } else {

            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = AcceptPinActivity_new.this;
            asc.checkAllFields();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void checkPINvalidation(HashMap<String, String> hmap) {
        if (hmap.size() > 0) {

            offlinePersonInitialization(hmap);

            String Authorizedlinks = hmap.get("Authorizedlinks");
            String AssignedVehicles = hmap.get("AssignedVehicles");


            if (!Authorizedlinks.isEmpty() || Authorizedlinks.contains(",")) {
                boolean isAllowed = false;

                String parts[] = Authorizedlinks.split(",");
                for (String allowedId : parts) {
                    if (AppConstants.R_SITE_ID.equalsIgnoreCase(allowedId)) {
                        isAllowed = true;
                        break;
                    }
                }

                if (isAllowed) {

                    boolean isAssigned = false;

                    if (!AssignedVehicles.isEmpty() || AssignedVehicles.contains(",")) {

                        if (AssignedVehicles.trim().equalsIgnoreCase("all")) {
                            isAssigned = true;
                        } else {
                            String parts2[] = AssignedVehicles.split(",");
                            for (String allowedId : parts2) {
                                if (AppConstants.OFF_VEHICLE_ID.equalsIgnoreCase(allowedId)) {
                                    isAssigned = true;
                                    break;
                                }
                            }
                        }

                        if (isAssigned) {
                            Intent ii = new Intent(AcceptPinActivity_new.this, DisplayMeterActivity.class);
                            startActivity(ii);
                        } else {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile("Vehicle is not assigned for this PIN");
                            CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ScreenNameForPersonnel + " not assigned for this PIN");
                            //AppConstants.colorToastBigFont(getApplicationContext(), "Vehicle is not assigned for this PIN", Color.RED);
                        }


                    } else {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile("Personnel is not allowed for selected Vehicle");
                        CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ScreenNameForPersonnel + " not allowed for selected Vehicle");
                        //AppConstants.colorToastBigFont(getApplicationContext(), "Personnel is not allowed for selected Vehicle", Color.RED);
                    }


                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile("Personnel is not allowed for selected Link");
                    CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", ScreenNameForPersonnel + " not allowed for selected Link");
                    //AppConstants.colorToastBigFont(getApplicationContext(), "Personnel is not allowed for selected Link", Color.RED);
                }

            }

        } else {

            if (AppConstants.APDU_FOB_KEY != null && !AppConstants.APDU_FOB_KEY.isEmpty()) {
                String fob = AppConstants.APDU_FOB_KEY.replace(":", "");
                HashMap<String, String> VehicleMap = controller.getVehicleDetailsByFOBNumber(fob.trim());
                if (VehicleMap.size() > 0) {
                    //vehicle fob please present pin fob
                    String msg = "This is " + ScreenNameForVehicle + " Access Device. Please use your " + ScreenNameForPersonnel + " Access Device";
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile("Personnel is not found in offline db");
                    CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", msg);
                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile("Personnel is not found in offline db");
                    CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Invalid Access Device");
                }

            } else if (etPersonnelPin.getText().toString().trim() != null && !etPersonnelPin.getText().toString().trim().isEmpty()){

                String V_Number = etPersonnelPin.getText().toString().trim();
                HashMap<String, String> VehicleMap  = controller.getVehicleDetailsByVehicleNumber(V_Number);
                if (VehicleMap.size() > 0) {
                    //vehicle fob please present pin fob
                    String msg = "This is " + ScreenNameForVehicle + " Number. Please use your " + ScreenNameForPersonnel + "Number";
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile("Personnel is not found in offline db");
                    CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", msg);
                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile("Personnel is not found in offline db");
                    CommonUtils.AutoCloseCustomMessageDilaog(AcceptPinActivity_new.this, "Message", "Invalid Number");
                }
            }


            onResume();
        }
    }

    public void offlinePersonInitialization(HashMap<String, String> hmap) {

        if (hmap != null && hmap.size() > 0) {
            String PersonId = hmap.get("PersonId");
            String PinNumber = hmap.get("PinNumber");
            String FuelLimitPerTxn = hmap.get("FuelLimitPerTxn");
            String FuelLimitPerDay = hmap.get("FuelLimitPerDay");
            String FOBNumber = hmap.get("FOBNumber");
            String RequireHours = hmap.get("RequireHours");


            AppConstants.OFF_PERSON_PIN = PinNumber;

            OfflineConstants.storeCurrentTransaction(AcceptPinActivity_new.this, "", "", "", "", "", PersonId, "", "");

            OfflineConstants.storeFuelLimit(AcceptPinActivity_new.this, "", "", "", PersonId, FuelLimitPerTxn, FuelLimitPerDay);
        }
    }

    public void GetBackToWelcomeActivity() {


        AppConstants.colorToast(getApplicationContext(), "Something went wrong, Please try again", Color.RED);

        Istimeout_Sec = false;
        AppConstants.ClearEdittextFielsOnBack(AcceptPinActivity_new.this);

        Intent i = new Intent(AcceptPinActivity_new.this, WelcomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);

    }

    public void DilaogRecreate(final Activity context, final String title, final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Whatever...
                                recreate();
                            }
                        }).show();
            }

        });

    }

    private class sendBleVersionData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String resp = "";

            System.out.println("Inside sendBleVersionData");
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, strings[0]);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", strings[1])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
                System.out.println("Inside sendBleVersionData response-----" + resp);

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " NETWORK_STRENGTH set to false.");

            }


            return resp;
        }

    }

    private void CheckForFirmwareUpgrade() {

        //LINK UPGRADE
        if (AppConstants.UP_Upgrade) {

            //Check for /FSBin folder if not create one
            File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "FSBin");
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }

            String LocalPath = AppConstants.FOLDER_PATH + AppConstants.UP_Upgrade_File_name;
            File f = new File(LocalPath);
            if (f.exists()) {
                Log.e(TAG, "Link upgrade firmware file already exist. Skip download");
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " Link upgrade firmware file already exist. Skip download");
            }else{
                if (AppConstants.UP_FilePath != null) {
                    new BackgroundServiceDownloadFirmware.DownloadLinkAndReaderFirmware().execute(AppConstants.UP_FilePath, AppConstants.UP_Upgrade_File_name, "UP_Upgrade");
                } else {
                    Log.e(TAG, "Link upgrade File path null");
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " Link upgrade File path null");
                }
            }
        }

        //BLE upgrade is in ServiceLFCard and ServiceHFCard Background service
    }

    @SuppressLint("ResourceAsColor")
    private void UpdateReaderStatusToUI() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                boolean ReaderStatusUI = false;

                //LF reader status on UI
                if (mDeviceName.length() > 0 && !mDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N")) {
                    ReaderStatusUI = true;
                    tv_lf_status.setVisibility(View.VISIBLE);
                    if (Constants.LF_ReaderStatus.equals("LF Connected") || Constants.LF_ReaderStatus.equals("LF Discovered")) {
                        tv_lf_status.setText(Constants.LF_ReaderStatus);
                        tv_lf_status.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tv_lf_status.setText(Constants.LF_ReaderStatus);
                        tv_lf_status.setTextColor(Color.parseColor("#ff0000"));
                    }

                } else {
                    tv_lf_status.setVisibility(View.GONE);
                }

                //Hf reader status on UI
                if (HFDeviceName.length() > 0 && !HFDeviceAddress.isEmpty() && !AppConstants.ACS_READER && mDisableFOBReadingForPin.equalsIgnoreCase("N")) {
                    ReaderStatusUI = true;
                    tv_hf_status.setVisibility(View.VISIBLE);
                    if (Constants.HF_ReaderStatus.equals("HF Connected") || Constants.HF_ReaderStatus.equals("HF Discovered")) {
                        tv_hf_status.setText(Constants.HF_ReaderStatus);
                        tv_hf_status.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tv_hf_status.setText(Constants.HF_ReaderStatus);
                        tv_hf_status.setTextColor(Color.parseColor("#ff0000"));
                    }
                } else {
                    tv_hf_status.setVisibility(View.GONE);
                }

                //Magnetic reader status on UI
                if (mMagCardDeviceAddress.length() > 0 && !mMagCardDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N")) {
                    ReaderStatusUI = true;
                    tv_mag_status.setVisibility(View.VISIBLE);
                    if (Constants.Mag_ReaderStatus.equals("Mag Connected") || Constants.Mag_ReaderStatus.equals("Mag Discovered")) {
                        tv_mag_status.setText(Constants.Mag_ReaderStatus);
                        tv_mag_status.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tv_mag_status.setText(Constants.Mag_ReaderStatus);
                        tv_mag_status.setTextColor(Color.parseColor("#ff0000"));
                    }
                } else {
                    tv_mag_status.setVisibility(View.GONE);
                }

                if (ReaderStatusUI) {
                    tv_reader_status.setText("Reader status: ");
                    layout_reader_status.setVisibility(View.VISIBLE);
                } else {
                    layout_reader_status.setVisibility(View.GONE);
                }


            }
        });

    }

    private void UnRegisterBroadcastForReader() {

        try {


            if (ServiceCardReader_pin != null)
                unregisterReceiver(ServiceCardReader_pin);
            ServiceCardReader_pin = null;

            if (mDeviceName.length() > 0 && !mDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N"))
                stopService(new Intent(AcceptPinActivity_new.this, ServiceLFCard.class));

            if (HFDeviceName.length() > 0 && !HFDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N"))
                stopService(new Intent(AcceptPinActivity_new.this, ServiceHFCard.class));

            if (mMagCardDeviceAddress.length() > 0 && !mMagCardDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N"))
                stopService(new Intent(AcceptPinActivity_new.this, ServiceMagCard.class));


        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "UnRegisterBroadcastForReader Exception:" + e.toString());
        }

    }

    private void RegisterBroadcastForReader() {


        try {

            if (ServiceCardReader_pin == null) {
                ServiceCardReader_pin = new BroadcastCardReader_dataFromServiceToUI();
                IntentFilter intentSFilterPIN = new IntentFilter("ServiceToActivityMagCard");
                registerReceiver(ServiceCardReader_pin, intentSFilterPIN);

                if (mDeviceName.length() > 0 && !mDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N"))
                    startService(new Intent(AcceptPinActivity_new.this, ServiceLFCard.class));

                if (HFDeviceName.length() > 0 && !HFDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N"))
                    startService(new Intent(AcceptPinActivity_new.this, ServiceHFCard.class));

                if (mMagCardDeviceAddress.length() > 0 && !mMagCardDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N"))
                    startService(new Intent(AcceptPinActivity_new.this, com.TrakEngineering.FluidSecureHubTest.MagCardGAtt.ServiceMagCard.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class BroadcastCardReader_dataFromServiceToUI extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();

            try {
                String Action = notificationData.getString("Action");
                if (Action.equals("HFReader")) {

                    String newData = notificationData.getString("HFCardValue");
                    System.out.println("HFCard data 001 pin----" + newData);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " " + Action + " Raw data:" + newData);
                    displayData_HF(newData);

                } else if (Action.equals("LFReader")) {

                    String newData = notificationData.getString("LFCardValue");
                    System.out.println("LFCard data 001 pin----" + newData);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " " + Action + " Raw data:" + newData);
                    displayData_LF(newData);

                } else if (Action.equals("MagReader")) {

                    String newData = notificationData.getString("MagCardValue");
                    System.out.println("MagCard data 002----" + newData);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " " + Action + " Raw data:" + newData);
                    MagCard_personnel = "";
                    displayData_MagCard(newData);

                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Two readers read at the same time", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private class ReconnectBleReaders extends AsyncTask<String, String, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String s = "Reconnectiong..\nPlease present FOB again..";
            SpannableString ss2 = new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(AcceptPinActivity_new.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.show();
        }

        @Override
        protected String doInBackground(String... strings) {

            try {

                UnRegisterBroadcastForReader();

                Thread.sleep(2000);

                RegisterBroadcastForReader();


            } catch (Exception e) {
                pd.dismiss();
                e.printStackTrace();
                if (AppConstants.ServerCallLogs)
                    AppConstants.WriteinFile(TAG + " ReconnectBleReaders Exception: " + e.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                if ((this.pd != null) && this.pd.isShowing()) {
                    this.pd.dismiss();
                }
            } catch (final IllegalArgumentException e) {
                // Handle or log or ignore
            } catch (final Exception e) {
                // Handle or log or ignore
            } finally {
                this.pd = null;
            }
        }
    }

    private void checkFor5Seconds() {

        runOnUiThread(new Runnable() {
            public void run() {
                if (sec_count == 5) {

                    if (!Constants.HF_ReaderStatus.equals("HF Connected") && !HFDeviceAddress.isEmpty() && !AppConstants.ACS_READER && mDisableFOBReadingForPin.equalsIgnoreCase("N")) {
                        new ReconnectBleReaders().execute();
                        //Toast.makeText(getApplicationContext(), "Reconnecting HF reader please wait.."+sec_count, Toast.LENGTH_SHORT).show();
                    } else if (!Constants.LF_ReaderStatus.equals("LF Connected") && !mDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N")) {
                        new ReconnectBleReaders().execute();
                        //Toast.makeText(getApplicationContext(), "Reconnecting LF reader please wait.."+sec_count, Toast.LENGTH_SHORT).show();
                    } else if (!Constants.Mag_ReaderStatus.equals("Mag Connected") && !mMagCardDeviceAddress.isEmpty() && mDisableFOBReadingForPin.equalsIgnoreCase("N")) {
                        new ReconnectBleReaders().execute();
                        //Toast.makeText(getApplicationContext(), "Reconnecting Mag reader please wait.."+sec_count, Toast.LENGTH_SHORT).show();
                    } else {
                       // Toast.makeText(getApplicationContext(), "Reader connected " + sec_count, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    public void resetReaderStatus() {

        sec_count = 0;
        Constants.HF_ReaderStatus = "HF Waiting..";
        Constants.LF_ReaderStatus = "LF Waiting..";
        Constants.Mag_ReaderStatus = "Mag Waiting..";

    }

    private void CustomDilaogForRebootCmd(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge_debugwindow);
        dialogBus.show();

        EditText edt_code = (EditText) dialogBus.findViewById(R.id.edt_code);
        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        Button btnCancel = (Button) dialogBus.findViewById(R.id.btn_cancel);
        edt_message.setText(Html.fromHtml(title));

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String code = edt_code.getText().toString().trim();

                if (code != null && !code.isEmpty() && code.equals(AppConstants.AccessCode)) {
                    AppConstants.RebootHF_reader = true;
                    //Toast.makeText(AcceptPinActivity_new.this, "Done", Toast.LENGTH_SHORT).show();
                    dialogBus.dismiss();
                } else {
                    if (!code.equals(AppConstants.AccessCode)) {
                        Toast.makeText(AcceptPinActivity_new.this, "Code did not match. Please try again", Toast.LENGTH_SHORT).show();
                    }
                    dialogBus.dismiss();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }

            }
        });

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }
            }
        };

        dialogBus.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 20000);

    }
}