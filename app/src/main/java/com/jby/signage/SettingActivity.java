package com.jby.signage;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.jby.signage.object.DisplayObject;
import com.jby.signage.shareObject.MySingleton;
import com.jby.signage.sharePreference.SharedPreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import pl.droidsonroids.gif.GifImageView;

import static com.jby.signage.shareObject.CustomToast.CustomToast;
import static com.jby.signage.shareObject.VariableUtils.device;
import static com.jby.signage.shareObject.VariableUtils.display;

public class SettingActivity extends AppCompatActivity implements TextView.OnEditorActionListener {
    private TextView versionName;
    private EditText deviceName;
    private GifImageView progressBar;

    private String deviceId, merchantId;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        objectInitialize();
        objectSetting();
    }

    private void objectInitialize() {
        deviceName = findViewById(R.id.device_name);
        progressBar = findViewById(R.id.progress_bar);
        versionName = findViewById(R.id.version_name);
        handler = new Handler();
    }

    private void objectSetting() {
        Window window = getWindow();
        WindowManager.LayoutParams winParams = window.getAttributes();
        winParams.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        window.setAttributes(winParams);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        deviceName.setOnEditorActionListener(this);
        isRegister();
        displayVersion();
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        switch (textView.getId()) {
            case R.id.device_name:
                checkingInput(null);
                return true;
        }
        return false;
    }

    private void isRegister() {
        if (SharedPreferenceManager.getDeviceID(this).equals("default")) {
            checkDeviceNameFromCloud();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /*
     * check device availability
     * */
    public void checkDeviceNameFromCloud() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, device, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                showProgressBar(false);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("status").equals("1")) {
                        deviceId = jsonObject.getJSONArray("device").getJSONObject(0).getString("device_id");
                        merchantId = jsonObject.getJSONArray("device").getJSONObject(0).getString("merchant_id");
                        String device = jsonObject.getJSONArray("device").getJSONObject(0).getString("name");
                        /*
                         * if device is registered before
                         * */
                        if (!device.equals("Unknown")) {
                            SharedPreferenceManager.setMerchantID(getApplicationContext(), merchantId);
                            SharedPreferenceManager.setDeviceID(getApplicationContext(), deviceId);
                            isRegister();
                        }
                    }
                    /*
                     * if device serial_no not found
                     * */
                    else {
                        notFoundDialog();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showProgressBar(false);
                CustomToast(getApplicationContext(), "Unable Connect to Api!");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("serial_no", getSerialNumber());
                params.put("read", "1");
                return params;
            }
        };
        MySingleton.getmInstance(this).addToRequestQueue(stringRequest);
    }

    public void checkingInput(View view) {
        if (!deviceName.getText().toString().trim().equals("")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateDeviceName();
                }
            }, 300);
        } else {
            Toast.makeText(this, "Device name can't be blank!", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * update device name
     * */
    public void updateDeviceName() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, device, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("status").equals("1")) {
                        Toast.makeText(SettingActivity.this, "Successfully!", Toast.LENGTH_SHORT).show();
                        SharedPreferenceManager.setDeviceID(getApplicationContext(), deviceId);
                        SharedPreferenceManager.setMerchantID(getApplicationContext(), merchantId);
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    } else if (jsonObject.getString("status").equals("3")) {
                        Toast.makeText(SettingActivity.this, "This device is existed!", Toast.LENGTH_SHORT).show();
                    }
                    /*
                     * if device serial_no not found
                     * */
                    else {
                        notFoundDialog();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                CustomToast(getApplicationContext(), "Unable Connect to Api!");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", deviceName.getText().toString().trim());
                params.put("device_id", deviceId);
                params.put("merchant_id", merchantId);
                params.put("update", "1");
                return params;
            }
        };
        MySingleton.getmInstance(this).addToRequestQueue(stringRequest);
    }

    private void notFoundDialog() {
        final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE);
        pDialog.setTitleText("Unknown Device");
        pDialog.setContentText("This device is not registered yet!");
        pDialog.setConfirmText("I Got IT");
        pDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                finish();
                pDialog.dismissWithAnimation();
            }
        });
        pDialog.show();
    }

    public static String getSerialNumber() {
        String serialNumber;
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);

            serialNumber = (String) get.invoke(c, "gsm.sn1");
            if (serialNumber.equals(""))
                serialNumber = (String) get.invoke(c, "ril.serialnumber");
            if (serialNumber.equals(""))
                serialNumber = (String) get.invoke(c, "ro.serialno");
            if (serialNumber.equals(""))
                serialNumber = (String) get.invoke(c, "sys.serialnumber");
            if (serialNumber.equals(""))
                serialNumber = Build.SERIAL;

            // If none of the methods above worked
            if (serialNumber.equals(""))
                serialNumber = null;
        } catch (Exception e) {
            e.printStackTrace();
            serialNumber = null;
        }
        return serialNumber;
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void displayVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = "Power By Channel Soft \n" + "Version " + pInfo.versionName + "\n" + "Serial: " + getSerialNumber();
            versionName.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
