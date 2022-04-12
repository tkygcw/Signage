package com.jby.signage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

import com.jby.signage.shareObject.MySingleton;
import com.jby.signage.sharePreference.SharedPreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import pl.droidsonroids.gif.GifImageView;

import static com.jby.signage.shareObject.CustomToast.CustomToast;
import static com.jby.signage.shareObject.VariableUtils.device;

public class SettingActivity extends AppCompatActivity implements TextView.OnEditorActionListener {
    private TextView versionName;
    private EditText deviceName, password;
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
        password = findViewById(R.id.password);
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
        //display version
        String version = "Power By Channel Soft \n" + "Version " + getVersion();
        versionName.setText(version);
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        switch (textView.getId()) {
            case R.id.password:
                checkingInput(null);
                return true;
        }
        return false;
    }

    private void isRegister() {
        if (!SharedPreferenceManager.getDeviceID(this).equals("default")) {
            openDisplayScreen();
        }
    }

    public void openDisplayScreen() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void checkingInput(View view) {
        if (!deviceName.getText().toString().trim().equals("") && !password.getText().toString().trim().equals("")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    login();
                }
            }, 300);
        } else {
            if (deviceName.getText().toString().trim().equals("")) {
                Toast.makeText(this, "Device name can't be blank!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.getText().toString().trim().equals("")) {
                Toast.makeText(this, "Password can't be blank!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * update device name
     * */
    public void login() {
        showProgressBar(true);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, device, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.d("", "login json: " + jsonObject);
                    if (jsonObject.getString("status").equals("1")) {
                        //check status
                        String status = jsonObject.getJSONObject("device_detail").getString("status");
                        if (status.equals("1")) {
                            notFoundDialog();
                            showProgressBar(false);
                            return;
                        }
                        deviceId = jsonObject.getJSONObject("device_detail").getString("device_id");
                        merchantId = jsonObject.getJSONObject("device_detail").getString("merchant_id");

                        SharedPreferenceManager.setDeviceID(getApplicationContext(), deviceId);
                        SharedPreferenceManager.setMerchantID(getApplicationContext(), merchantId);

                        Toast.makeText(SettingActivity.this, "Successfully!", Toast.LENGTH_SHORT).show();
                        openDisplayScreen();
                    }
                    /*
                     * login unsuccessfully
                     * */
                    else {
                        CustomToast(getApplicationContext(), "Invalid Username Or Password!");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    CustomToast(getApplicationContext(), "Something Went Wrong!");
                }
                showProgressBar(false);
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
                params.put("username", deviceName.getText().toString());
                params.put("password", password.getText().toString());
                params.put("login", "1");
                return params;
            }
        };
        MySingleton.getmInstance(this).

                addToRequestQueue(stringRequest);
    }

    private void notFoundDialog() {
        final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE);
        pDialog.setTitleText("Something Went Wrong");
        pDialog.setContentText("Something error with this device! Please contact administrator for further support!");
        pDialog.setConfirmText("I Got IT");
        pDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                pDialog.dismissWithAnimation();
            }
        });
        pDialog.show();
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String getVersion() {
        String version = "-";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "-";
            e.printStackTrace();
        }
        return version;
    }
}
