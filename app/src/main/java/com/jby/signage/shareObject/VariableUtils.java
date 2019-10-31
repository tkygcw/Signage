package com.jby.signage.shareObject;

public class VariableUtils {
    public static int REQUEST_UPDATE = 1;
    public static int REQUEST_WRITE_EXTERNAL_PERMISSION = 2;

    private static String domain = "https://www.channelsoft.com.my/";
    private static String prefix = "signage/";
    /*
    * api
    * */
    public static String display = domain + prefix + "display/display.php";
    public static String device = domain + prefix + "device/device.php";
    public static String galleyPath = domain + prefix + "gallery/resource/";

    /*
    * network
    * */
    public static final String CONNECT_TO_WIFI = "WIFI";
    public static final String CONNECT_TO_MOBILE = "MOBILE";
    public static final String NOT_CONNECT = "NOT_CONNECT";
    public final static String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
}
