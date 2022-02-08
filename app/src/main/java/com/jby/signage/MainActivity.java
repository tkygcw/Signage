package com.jby.signage;
//11dd111

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.stetho.Stetho;
import com.jby.signage.alarm.AlarmReceiver;
import com.jby.signage.connection.NetworkAccessChecker;
import com.jby.signage.connection.NetworkSchedulerService;
import com.jby.signage.database.CustomSqliteHelper;
import com.jby.signage.database.FrameworkClass;
import com.jby.signage.database.ResultCallBack;
import com.jby.signage.object.DisplayObject;
import com.jby.signage.shareObject.MySingleton;
import com.jby.signage.sharePreference.SharedPreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.TimeZone;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static com.jby.signage.database.CustomSqliteHelper.TB_GALLERY;
import static com.jby.signage.shareObject.CustomToast.CustomToast;
import static com.jby.signage.shareObject.VariableUtils.REQUEST_WRITE_EXTERNAL_PERMISSION;
import static com.jby.signage.shareObject.VariableUtils.device;
import static com.jby.signage.shareObject.VariableUtils.display;
import static com.jby.signage.shareObject.VariableUtils.galleyPath;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    /*
     * download purpose
     * */
    private BroadcastReceiver downloadReceiver;
    private DownloadManager manager;
    private File directory = new File(Environment.getExternalStorageDirectory() + "/Signage/");
    private String lastDownloadPath = "";
    /*
     * display list
     * */
    private ArrayList<DisplayObject> displayObjectArrayList, playList;
    private int checkingPosition = -1;
    private int playPosition = 0;
    private boolean timerRunning = false;
    private String checkingTime;
    private long refreshTime = 5000;
    private String shutDownTimer = "";

    /*
     * connection
     * */
    private boolean isConnected = false;
    /*
     * progress
     * */
    private LinearLayout progressBar;
    private TextView progressBarLabel, labelCompany;

    private FrameworkClass tbGallery;

    private ImageView imageView;
    private VideoView videoView;
    /*
     * control timer purpose
     * */
    private String timerType = "refresh_timer";
    /*
     * count down timer
     * */
    private boolean isNewDisplay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        objectInitialize();
        objectSetting();
    }

    private void objectInitialize() {
        Stetho.initializeWithDefaults(this);
        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        tbGallery = new FrameworkClass(this, new CustomSqliteHelper(this), TB_GALLERY);

        displayObjectArrayList = new ArrayList<>();
        playList = new ArrayList<>();

        progressBar = findViewById(R.id.progress_bar);
        progressBarLabel = findViewById(R.id.progress_bar_label);
        labelCompany = findViewById(R.id.label_company);

        videoView = findViewById(R.id.video_view);
        imageView = findViewById(R.id.image_view);
    }

    private void objectSetting() {
        videoView.setOnCompletionListener(this);
        videoView.setOnErrorListener(this);
        //screen orientation
        scheduleJob();

    }

    /*
     * request write permission
     * */
    private void requestWritePermission() {
        int hasWriteStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
                    showMessageOKCancel(
                            new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_PERMISSION);
                                    sweetAlertDialog.dismissWithAnimation();
                                }
                            });
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_PERMISSION);
            }
        } else {
            readLocalDisplay();
            setRefreshTimer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                setRefreshTimer();
                onStart();
            } else {
                // Permission Denied
                Toast.makeText(this, "WRITE_EXTERNAL Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showMessageOKCancel(SweetAlertDialog.OnSweetClickListener onSweetClickListener) {
        final SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        dialog.setTitleText("Permission Request");
        dialog.setContentText("You need to allow access to Storage");
        dialog.setConfirmText("OK");
        dialog.setCancelText("Cancel");
        dialog.setConfirmClickListener(onSweetClickListener);
        dialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                requestWritePermission();
                dialog.dismissWithAnimation();
            }
        });
        dialog.show();
    }

    /*-----------------------------------------------------------------------------------donwload, arrange schedule control-------------------------------------------------------------------------*/
    /*
     * request schedule
     * */
    public void requestSchedule() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, display, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.d("MainActivity", "json object: " + jsonObject);
                    if (jsonObject.getString("status").equals("1")) {
                        displayObjectArrayList.clear();
                        /*
                         * default display
                         * */
                        Log.d("MainActivity", "alarm id: " + jsonObject);
                        JSONArray jsonArray = jsonObject.getJSONArray("display");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            displayObjectArrayList.add(new DisplayObject(
                                    jsonArray.getJSONObject(i).getString("link_id"),
                                    jsonArray.getJSONObject(i).getString("path"),
                                    jsonArray.getJSONObject(i).getString("timer"),
                                    jsonArray.getJSONObject(i).getString("priority"),
                                    jsonArray.getJSONObject(i).getString("gallery_id"),
                                    jsonArray.getJSONObject(i).getString("display_type"),
                                    "0",
                                    "default"
                            ));
                        }
                        /*
                         * refresh time
                         * */
                        refreshTime = Long.parseLong(jsonObject.getString("refresh_time"));
                        /*
                         * shut down time
                         * */
                        shutDownTimer = jsonObject.getString("shut_down_time");
                        setShutDownTimer();
                        /*
                         * add next display list and date
                         * */
                        addNextDisplay(jsonObject);

                        if (displayObjectArrayList.size() > 0) {
                            /*
                             * preset every status into 0
                             * */
                            tbGallery.new Update("status", "0")
                                    .perform(new ResultCallBack.OnUpdate() {
                                        @Override
                                        public void updateResult(String status) {
                                            Log.d("hahahaha", "Updated!");
                                            checkingPosition = -1;
                                            checkFileExistence();
                                        }
                                    });
                        }
                    } else {
                        //set refresh time to default when no data
                        refreshTime = 5000;
                        showProgressBar(true, "Data Not Found!");
                        clearAll();
                    }
                    setRefreshTimer();
                } catch (JSONException e) {
                    e.printStackTrace();
                    //set refresh time to default
                    refreshTime = 5000;
                    setRefreshTimer();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //set refresh time to default when error
                refreshTime = 5000;
                setRefreshTimer();
                CustomToast(getApplicationContext(), "Unable Connect to Api!");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                Log.d("MainActivity", "device_id " + SharedPreferenceManager.getDeviceID(MainActivity.this));
                Log.d("MainActivity", "timer_type " + timerType);
                Log.d("MainActivity", "next_display_date " + SharedPreferenceManager.getNextDisplayDate(MainActivity.this));

                params.put("device_id", SharedPreferenceManager.getDeviceID(MainActivity.this));
                params.put("timer_type", timerType);
                params.put("next_display_date", SharedPreferenceManager.getNextDisplayDate(MainActivity.this));
                params.put("read", "1");
                return params;
            }
        };
        MySingleton.getmInstance(this).addToRequestQueue(stringRequest);
    }

    private void addNextDisplay(JSONObject jsonObject) {
        try {
            /*
             * set next display date
             * */
            if (!jsonObject.getString("next_display_date").equals("")) {
                SharedPreferenceManager.setNextDisplayDate(this, jsonObject.getString("next_display_date"));
                //set timer
                setChangeNextDisplayTimer();
            }
            /*
             * next display list
             * */
            JSONArray jsonArray = jsonObject.getJSONArray("next_display");
            for (int i = 0; i < jsonArray.length(); i++) {
                displayObjectArrayList.add(new DisplayObject(
                        jsonArray.getJSONObject(i).getString("link_id"),
                        jsonArray.getJSONObject(i).getString("path"),
                        jsonArray.getJSONObject(i).getString("timer"),
                        jsonArray.getJSONObject(i).getString("priority"),
                        jsonArray.getJSONObject(i).getString("gallery_id"),
                        jsonArray.getJSONObject(i).getString("display_type"),
                        "0",
                        "next"
                ));
            }
        } catch (JSONException e) {
        }
    }

    private void checkFileExistence() {
        checkingPosition++;
        /*
         * check every position
         * */
        if (checkingPosition < displayObjectArrayList.size()) {
            checkingTime = (String) android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date());
            /*
             * checking from local database (prevent duplicate download purpose)
             * */
            tbGallery.new Read("gallery_id")
                    .where("gallery_id = " + displayObjectArrayList.get(checkingPosition).getGalleryID())
                    .perform(new ResultCallBack.OnRead() {
                        @Override
                        public void readResult(String result) {
                            Log.d("hahahaha", "read here!");
                            /*
                             * file not found (new file) so need arrange to download
                             * */
                            if (result.equals("Nothing") || result.equals("Fail")) {
                                //prevent dl twice when (refresh timer + next timer) fire together
                                if (!lastDownloadPath.equals(displayObjectArrayList.get(checkingPosition).getPath())) {
                                    Log.d("hahahaha", "Download here!");
                                    downLoadFile();
                                }
                            }
                            /*
                             * existing file
                             * */
                            else {
                                Log.d("hahahaha", "Existing item!");
                                /*
                                 * if a gallery is use in both default & next display then we have to save it separately
                                 * */
                                tbGallery.new Read("gallery_id")
                                        .where("link_id ='" + displayObjectArrayList.get(checkingPosition).getLinkID() + "' AND gallery_id ='" + displayObjectArrayList.get(checkingPosition).getGalleryID() + "' AND default_display='" + displayObjectArrayList.get(checkingPosition).getDefaultDisplay() + "'")
                                        .perform(new ResultCallBack.OnRead() {
                                            @Override
                                            public void readResult(String result) {
                                                if (result.equals("Nothing") || result.equals("Fail")) {
                                                    addGalleryIntoLocal();
                                                } else {
                                                    /*
                                                     * make a update
                                                     * */
                                                    tbGallery.new Update("priority, display_type, timer, status, updated_at",
                                                            displayObjectArrayList.get(checkingPosition).getPriority() + "," +
                                                                    displayObjectArrayList.get(checkingPosition).getDisplayType() + "," +
                                                                    displayObjectArrayList.get(checkingPosition).getTimer() + "," +
                                                                    "1" + "," +
                                                                    checkingTime)
                                                            .where("gallery_id = ? AND default_display = ?", displayObjectArrayList.get(checkingPosition).getGalleryID() + "," + displayObjectArrayList.get(checkingPosition).getDefaultDisplay())
                                                            .perform(new ResultCallBack.OnUpdate() {
                                                                @Override
                                                                public void updateResult(String status) {
                                                                    displayObjectArrayList.get(checkingPosition).setStatus("1");
                                                                    /*
                                                                     * check is everything downloaded
                                                                     * */
                                                                    if (allDownloaded(false))
                                                                        play();

                                                                    checkFileExistence();
                                                                }
                                                            });
                                                }
                                            }
                                        });

                            }
                        }
                    });
        }
    }

    public void clearAll() {
        tbGallery.new Delete().perform(new ResultCallBack.OnDelete() {
            @Override
            public void deleteResult(String status) {
                playList.clear();
                displayObjectArrayList.clear();
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
                /*
                 * delete local file
                 * */
                File dir = new File(directory.getAbsolutePath());
                if (dir.exists() && dir.isDirectory()) {
                    String[] children = dir.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(dir, children[i]).delete();
                    }
                }
            }
        });
    }

    private void downLoadFile() {
        try {
            lastDownloadPath = displayObjectArrayList.get(checkingPosition).getPath();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(galleyPath + SharedPreferenceManager.getMerchantID(this) + "/" + displayObjectArrayList.get(checkingPosition).getPath()));
            request.setDescription("Downloading");
            request.setTitle("Download");

            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

            if (!directory.exists()) {
                directory.mkdir();
            }

            request.setDestinationInExternalPublicDir("/Signage", "" + displayObjectArrayList.get(checkingPosition).getPath());

            manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
        } catch (Exception ex) {

        }
    }

    private void setDownloadReceiver() {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (downloadId == -1)
                    return;

                // query download status
                Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId));
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // add gallery into local database when download finished
                        addGalleryIntoLocal();
                    } else {
                        // download is assumed cancelled
                    }
                } else {
                    // download is assumed cancelled
                }
            }
        };
        registerReceiver(downloadReceiver, filter);
    }

    private void addGalleryIntoLocal() {
        try {
            tbGallery.new Create("link_id, path, priority, gallery_id, display_type, timer, status, default_display, created_at, updated_at",
                    new String[]{
                            displayObjectArrayList.get(checkingPosition).getLinkID(),
                            displayObjectArrayList.get(checkingPosition).getPath(),
                            displayObjectArrayList.get(checkingPosition).getPriority(),
                            displayObjectArrayList.get(checkingPosition).getGalleryID(),
                            displayObjectArrayList.get(checkingPosition).getDisplayType(),
                            displayObjectArrayList.get(checkingPosition).getTimer(),
                            "1",
                            displayObjectArrayList.get(checkingPosition).getDefaultDisplay(),
                            checkingTime,
                            checkingTime})
                    /*
                     * call back
                     * */
                    .perform(new ResultCallBack.OnCreate() {
                        @Override
                        public void createResult(String status) {
                            if (status.equals("Success")) {
                                displayObjectArrayList.get(checkingPosition).setStatus("1");
                                /*
                                 * check is everything downloaded then play
                                 * */
                                if (allDownloaded(true)) play();
                                checkFileExistence();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("MainActivity", "Checking position out of bound!");
        }
    }

    private boolean allDownloaded(boolean isNewDisplay) {
        Log.d("hahahaha", "Checking position!!!!" + checkingPosition);
        for (int i = 0; i < displayObjectArrayList.size(); i++) {
            Log.d("hahahaha", "status: " + displayObjectArrayList.get(i).getStatus() + "  path: " + displayObjectArrayList.get(i).getPath() + "  display: " + displayObjectArrayList.get(i).getDefaultDisplay());
        }
        for (int i = 0; i < displayObjectArrayList.size(); i++) {
            if (displayObjectArrayList.get(i).getStatus().equals("0")) {
                return false;
            }
        }
        showProgressBar(false, null);
        //if new item is download then set playlist back to 0
        if (isNewDisplay) playPosition = 0;
        //set playlist
        playList = setPlayList();
        //delete file
        if (isConnected) deleteUnusedFile();
        return true;
    }

    private ArrayList<DisplayObject> setPlayList() {
        ArrayList<DisplayObject> arrayList = new ArrayList<>();
        for (int i = 0; i < displayObjectArrayList.size(); i++) {
            if (displayObjectArrayList.get(i).getDefaultDisplay().equals("default")) {
                arrayList.add(new DisplayObject(
                        displayObjectArrayList.get(i).getLinkID(),
                        displayObjectArrayList.get(i).getPath(),
                        displayObjectArrayList.get(i).getTimer(),
                        displayObjectArrayList.get(i).getPriority(),
                        displayObjectArrayList.get(i).getGalleryID(),
                        displayObjectArrayList.get(i).getDisplayType(),
                        "",
                        ""
                ));
            }
        }
        return arrayList;
    }

    /*-----------------------------------------------------------------------------------playlist control-------------------------------------------------------------------------*/
    /*
     * playlist control
     * */
    private void play() {
        /*
         * if nothing is playing
         * */
        if (!videoView.isPlaying() && !timerRunning) {
            try {
                if (!isNewDisplay) {
                    //screen orientation
                    setRequestedOrientation(playList.get(playPosition).getDisplayType().equals("0") ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    //path
                    String filePath = directory.getAbsolutePath() + "/" + playList.get(playPosition).getPath();
                    if (isMP4()) {
                        videoView.setVideoPath(filePath);
                        videoView.start();
                    } else {
                        timerRunning = true;
                        imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
                        Log.d("haha", "play time: " + playList.get(playPosition).getTimer());
                        new CountDownTimer(Long.valueOf(playList.get(playPosition).getTimer()), 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                timerRunning = false;
                                checkPlayPosition();
                            }
                        }.start();
                    }
                }
                /*
                 * count down timer
                 * */
                else {
                    timerRunning = true;
                    videoView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                    new CountDownTimer(5000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            showProgressBar(true, "New file will be display in " + String.valueOf(millisUntilFinished / 1000));
                        }

                        @Override
                        public void onFinish() {
                            timerRunning = false;
                            isNewDisplay = false;
                            playPosition = 0;
                            showProgressBar(false, "");
                            play();
                        }
                    }.start();
                }

            } catch (IndexOutOfBoundsException e) {
                playPosition = 0;
            }
        }
    }

    private void checkPlayPosition() {
        if (playPosition < playList.size() - 1) playPosition++;
        else playPosition = 0;
        play();
    }

    private boolean isMP4() {
        progressBar.setVisibility(View.GONE);
        if (playList.get(playPosition).getPath().endsWith(".mp4")) {
            videoView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            return true;
        } else {
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            return false;
        }
    }

    /*-----------------------------------------------------------------------------------video view purpose-------------------------------------------------------------------------*/
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        checkPlayPosition();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("hahahaha", "OnStart!!");
        setDownloadReceiver();
        registerReceiver(alarmManger, new IntentFilter("alarmManager"));
        //network
        registerReceiver(connection, new IntentFilter("connection"));
        Intent startServiceIntent = new Intent(this, NetworkSchedulerService.class);
        startService(startServiceIntent);

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

    /*-----------------------------------------------------------------------------timer-----------------------------------------------------------------------------------*/
    /*
     * set time interval for device to request from cloud
     * */
    private void setRefreshTimer() {
        //alarm setting
        AlarmManager alarmManager;
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.putExtra("alarm_id", "refresh_timer");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        /*
         * time setting
         * */
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        try {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + refreshTime, pendingIntent);
        } catch (RuntimeException e) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 30000, pendingIntent);
        }
    }

    /*
     * set timer to change display (default to next display)
     * */
    private void setChangeNextDisplayTimer() {
        //alarm setting
        AlarmManager alarmManager;

        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.putExtra("alarm_id", "change_next_timer");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 2, alarmIntent, 0);

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            Date date = formatter.parse(SharedPreferenceManager.getNextDisplayDate(this));
            /*
             * time setting
             * */
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
            } catch (RuntimeException e) {
            }
        } catch (Exception e) {
            Log.d("MainActivity", "unable to set next display timer!");
            e.printStackTrace();
        }
    }

    /*
     * check time range between refresh and next display timer
     * */
    private boolean shouldProceed() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        try {
            Date currentTime = formatter.parse(formatter.format(Calendar.getInstance().getTime()));
            Date nextDisplayTimer = formatter.parse(SharedPreferenceManager.getNextDisplayDate(MainActivity.this));

            long range = nextDisplayTimer.getTime() - currentTime.getTime();
            Log.d("MainActivity", "alarm id range: " + range);
            if (range <= 60000 && range > -60000) return false;

        } catch (ParseException e) {
            return true;
        }
        return true;
    }

    /*
     * set shut down timer
     * */
    private void setShutDownTimer() {
        //alarm setting
        AlarmManager alarmManager;
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.putExtra("alarm_id", "shut_down_timer");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 3, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");

            Date currentTime = formatter.parse(formatter.format(cal.getTime()));
            Date shutDownTime = formatter.parse(shutDownTimer);

            if (currentTime.before(shutDownTime) || currentTime == shutDownTime) {
                String[] timer = shutDownTimer.split(":");
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timer[0]));
                cal.set(Calendar.MINUTE, Integer.parseInt(timer[1]));

                alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
            } else {
                Log.d("MainActivity", "Shut Down Time is over!");
            }

        } catch (Exception e) {
            Log.d("MainActivity", "unable to set shut down timer");
            e.printStackTrace();
        }
    }

    /*
     * when timer is fired
     * */
    BroadcastReceiver alarmManger = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity", "alarm id: " + intent.getExtras().getString("alarm_id"));
            if (intent.getExtras().getString("alarm_id").equals("change_next_timer")) {
                timerType = "change_next_timer";
            } else if (intent.getExtras().getString("alarm_id").equals("refresh_timer")) {
                timerType = "refresh_timer";
                /*
                 * if next timer - refresh < 60000 then stop refresh
                 * */
                if (!shouldProceed()) return;
            } else {
                shutDown();
            }
            checkNetworkAccess();
        }
    };

    /*------------------------------------------------------------------------network-----------------------------------------------------------------------------------------------*/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scheduleJob() {
        JobInfo myJob = new JobInfo.Builder(0, new ComponentName(this, NetworkSchedulerService.class))
                .setRequiresCharging(true)
                .setMinimumLatency(1000)
                .setOverrideDeadline(2000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(myJob);
    }

    /*
     * when receive broadcast from job scheduler then checking network
     * */
    BroadcastReceiver connection = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //when a network is change then set refresh time to 30second(default)
            refreshTime = 5000;
            showProgressBar(true, null);

            isConnected = intent.getExtras().getBoolean("isConnected");
            if (isConnected) {
                //write permission and request from cloud
                requestWritePermission();
            } else {
                readLocalDisplay();
            }
        }
    };

    public void checkNetworkAccess() {
        new NetworkAccessChecker(MainActivity.this).isInternetConnectionAvailable(new NetworkAccessChecker.InternetCheckListener() {
            @Override
            public void onComplete(final boolean connected) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connected) {
                            //write permission and request from cloud
                            requestSchedule();
                        } else {
                            refreshTime = 5000;
                            readLocalDisplay();
                        }
                    }
                });
            }
        });
    }

    /*---------------------------------------------------------------------------read from local display-----------------------------------------------------------------------------------*/
    private void readLocalDisplay() {
        tbGallery
                .new Read("*")
                .where("status = 1")
                .perform(new ResultCallBack.OnRead() {
                    @Override
                    public void readResult(String result) {
                        if (result.equals("Nothing") || result.equals("Fail")) {
                            showProgressBar(true, "Data Not Found!");
                        } else {
                            try {
                                displayObjectArrayList.clear();
                                JSONArray jsonArray = new JSONObject(result).getJSONArray("result");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    displayObjectArrayList.add(new DisplayObject(
                                            jsonArray.getJSONObject(i).getString("link_id"),
                                            jsonArray.getJSONObject(i).getString("path"),
                                            jsonArray.getJSONObject(i).getString("timer"),
                                            jsonArray.getJSONObject(i).getString("priority"),
                                            jsonArray.getJSONObject(i).getString("gallery_id"),
                                            jsonArray.getJSONObject(i).getString("display_type"),
                                            jsonArray.getJSONObject(i).getString("status"),
                                            jsonArray.getJSONObject(i).getString("default_display")
                                    ));
                                }
                                /*
                                 * check is everything downloaded
                                 * */
                                if (allDownloaded(false)) play();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        /*
         * set refresh timer
         * */
        setRefreshTimer();
    }

    /*---------------------------------------------------------------------------delete unused display-----------------------------------------------------------------------------------*/
    private void deleteUnusedFile() {
        Log.d("hahahaha", "Delete method here!");
        /*
         * delete file from directory
         * */
        File dir = new File(directory.getAbsolutePath());
        boolean delete;

        if (dir.exists() && dir.isDirectory()) {
            String[] content = dir.list();

            for (String gallery : content) {
                delete = true;
                for (int j = 0; j < displayObjectArrayList.size(); j++) {
                    if (gallery.equals(displayObjectArrayList.get(j).getPath())) delete = false;
                }
                if (delete) {
                    Log.d("hahahaha", "delete item: " + gallery);
                    new File(dir, gallery).delete();
                    isNewDisplay = true;
                }
            }
        }

        /*
         * delete local database
         * */
        tbGallery.new Delete().where("status = ?", "0")
                .perform(new ResultCallBack.OnDelete() {
                    @Override
                    public void deleteResult(String status) {
                        Log.d("hahahaha", "delete success!");
                        /*
                         * check in
                         * */
                        checkIn();
                    }
                });
    }

    //    --------------------------------------------------check in after finished download, arrange schedule-----------------------------------------------------------------------
    public void checkIn() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, device, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

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
                params.put("check_in", "1");
                params.put("device_id", SharedPreferenceManager.getDeviceID(MainActivity.this));
                return params;
            }
        };
        MySingleton.getmInstance(this).addToRequestQueue(stringRequest);
    }

    //    --------------------------------------------------full screen-----------------------------------------------------------------------
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showProgressBar(boolean show, String label) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        labelCompany.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBarLabel.setText(label == null ? "Loading..." : label);
    }

    //    --------------------------------------------------full screen-----------------------------------------------------------------------
    private void shutDown() {
        try {
            Process proc = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "reboot -p"});
            proc.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Your device is not able to shut down!", Toast.LENGTH_SHORT).show();
        }
    }
}
