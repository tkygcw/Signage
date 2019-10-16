package com.jby.signage.sharePreference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wypan on 2/24/2017.
 */

public class SharedPreferenceManager {


    private static String DeviceID = "device_Id";
    private static String MerchantID = "merchant_Id";

    private static SharedPreferences getSharedPreferences(Context context) {
        String SharedPreferenceFileName = "Signage";
        return context.getSharedPreferences(SharedPreferenceFileName, Context.MODE_PRIVATE);
    }

    public static void clear(Context context) {
        getSharedPreferences(context).edit().clear().apply();
    }

    public static String getDeviceID(Context context) {
        return getSharedPreferences(context).getString(DeviceID, "default");
    }

    public static void setDeviceID(Context context, String device_Id) {
        getSharedPreferences(context).edit().putString(DeviceID, device_Id).apply();
    }

    public static String getMerchantID(Context context) {
        return getSharedPreferences(context).getString(MerchantID, "default");
    }

    public static void setMerchantID(Context context, String merchantID) {
        getSharedPreferences(context).edit().putString(MerchantID, merchantID).apply();
    }
}
