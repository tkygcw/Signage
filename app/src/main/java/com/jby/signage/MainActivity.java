package com.jby.signage;

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
import com.jby.signage.connection.NetworkSchedulerService;
import com.jby.signage.database.CustomSqliteHelper;
import com.jby.signage.database.FrameworkClass;
import com.jby.signage.database.ResultCallBack;
import com.jby.signage.object.DisplayObject;
import com.jby.signage.alarm.AlarmReceiver;
import com.jby.signage.shareObject.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static com.jby.signage.database.CustomSqliteHelper.TB_GALLERY;
import static com.jby.signage.shareObject.CustomToast.CustomToast;
import static com.jby.signage.shareObject.VariableUtils.REQUEST_WRITE_EXTERNAL_PERMISSION;
import static com.jby.signage.shareObject.VariableUtils.display;
import static com.jby.signage.shareObject.VariableUtils.galleyPath;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {
    /*
     * download purpose
     * */
    private BroadcastReceiver downloadReceiver;
    private DownloadManager manager;
    private File directory = new File(Environment.getExternalStorageDirectory() + "/Signage/");
    /*
     * display list
     * */
    private ArrayList<DisplayObject> displayObjectArrayList, playList;
    private int checkingPosition = -1;
    private int playPosition = 0;
    private boolean timerRunning = false;
    private String checkingTime;
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
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_PERMISSION);
            }
        } else {
            requestSchedule();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                requestSchedule();
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

    /*
     * request schedule
     * */
    public void requestSchedule() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, display, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("status").equals("1")) {
                        displayObjectArrayList.clear();
                        JSONArray jsonArray = jsonObject.getJSONArray("display");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            displayObjectArrayList.add(new DisplayObject(
                                    jsonArray.getJSONObject(i).getString("path"),
                                    jsonArray.getJSONObject(i).getString("timer"),
                                    jsonArray.getJSONObject(i).getString("priority"),
                                    jsonArray.getJSONObject(i).getString("gallery_id"),
                                    jsonArray.getJSONObject(i).getString("display_type"),
                                    jsonArray.getJSONObject(i).getString("refresh_time"),
                                    "0"
                            ));
                        }
                        if (displayObjectArrayList.size() > 0) {
                            /*
                             * preset every status into 0
                             * */
                            tbGallery.new Update("status", "0")
                                    .perform(new ResultCallBack.OnUpdate() {
                                        @Override
                                        public void updateResult(String status) {
                                            checkingPosition = -1;
                                            checkFileExistence();
                                        }
                                    });
                        }
                    } else showProgressBar(true, "Data Not Found!");

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
                params.put("serial_no", getSerialNumber());
                params.put("read", "1");
                return params;
            }
        };
        MySingleton.getmInstance(this).addToRequestQueue(stringRequest);
    }

    private void checkFileExistence() {
        checkingPosition++;
        /*
         * check every position
         * */
        if (checkingPosition < displayObjectArrayList.size()) {
            checkingTime = (String) android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date());
            /*
             * checking from local database
             * */
            tbGallery.new Read("gallery_id")
                    .where("gallery_id = " + displayObjectArrayList.get(checkingPosition).getGalleryID())
                    .perform(new ResultCallBack.OnRead() {
                        @Override
                        public void readResult(String result) {
                            /*
                             * file not found (new file) so need arrange to download
                             * */
                            if (result.equals("Nothing") || result.equals("Fail")) {
                                downLoadFile();
                            }
                            /*
                             * existing file
                             * */
                            else {
                                /*
                                 * make a update
                                 * */
                                tbGallery.new Update("priority, display_type, timer, refresh_time, status, updated_at",
                                        displayObjectArrayList.get(checkingPosition).getPriority() + "," +
                                                displayObjectArrayList.get(checkingPosition).getDisplayType() + "," +
                                                displayObjectArrayList.get(checkingPosition).getTimer() + "," +
                                                displayObjectArrayList.get(checkingPosition).getRefreshTime() + "," +
                                                "1" + "," +
                                                checkingTime)
                                        .where("gallery_id = ?", displayObjectArrayList.get(checkingPosition).getGalleryID())
                                        /*
                                         * call back
                                         * */
                                        .perform(new ResultCallBack.OnUpdate() {
                                            @Override
                                            public void updateResult(String status) {
                                                displayObjectArrayList.get(checkingPosition).setStatus("1");
                                                checkFileExistence();
                                                /*
                                                 * check is everything downloaded
                                                 * */
                                                if (allDownloaded(false)) play();
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    public void clearDB(View view) {
        tbGallery.new Delete().perform(new ResultCallBack.OnDelete() {
            @Override
            public void deleteResult(String status) {
                displayObjectArrayList.clear();
                Toast.makeText(MainActivity.this, "Clear Successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downLoadFile() {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(galleyPath + displayObjectArrayList.get(checkingPosition).getPath()));
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
                        // download is successful
                        tbGallery.new Create("path, priority, gallery_id, display_type, timer, refresh_time, status, created_at, updated_at",
                                new String[]{
                                        displayObjectArrayList.get(checkingPosition).getPath(),
                                        displayObjectArrayList.get(checkingPosition).getPriority(),
                                        displayObjectArrayList.get(checkingPosition).getGalleryID(),
                                        displayObjectArrayList.get(checkingPosition).getDisplayType(),
                                        displayObjectArrayList.get(checkingPosition).getTimer(),
                                        displayObjectArrayList.get(checkingPosition).getRefreshTime(),
                                        "1",
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
                                            checkFileExistence();
                                            /*
                                             * check is everything downloaded
                                             * */
                                            if (allDownloaded(true)) play();
                                        }
                                    }
                                });

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

    private boolean allDownloaded(boolean isNewDisplay) {
        for (int i = 0; i < displayObjectArrayList.size(); i++) {
            if (displayObjectArrayList.get(i).getStatus().equals("0")) {
                return false;
            }
        }
        showProgressBar(false, null);
        //if new item is download then set playlist back to 0
        if (isNewDisplay) playPosition = 0;
        playList = displayObjectArrayList;
        //delete file
        deleteUnusedFile();
        return true;
    }

    /*
     * playlist control
     * */
    private void play() {
        /*
         * if nothing is playing
         * */
        if (!videoView.isPlaying() && !timerRunning) {
            try {
                //screen orientation
                setRequestedOrientation(playList.get(playPosition).getDisplayType().equals("0") ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                //path
                String filePath = directory.getAbsolutePath() + "/" + playList.get(playPosition).getPath();
                if (isMP4()) {
                    /*
                     * if file is downloaded then play
                     * */
                    if (playList.get(playPosition).getStatus().equals("1")) {
                        videoView.setVideoPath(filePath);
                        videoView.start();
                    }
                } else {
                    timerRunning = true;
                    imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
                    videoView.setVisibility(View.GONE);

                    new CountDownTimer(10000, Long.valueOf(playList.get(playPosition).getTimer())) {
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
            } catch (IndexOutOfBoundsException e) {
                playPosition = 0;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        checkPlayPosition();

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

    @Override
    protected void onPause() {
        super.onPause();
        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
            unregisterReceiver(alarmManger);
            //network control
            unregisterReceiver(connection);
            stopService(new Intent(this, NetworkSchedulerService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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
     * timer setting
     * */
    private void setRefreshTimer() {
        //alarm setting
        AlarmManager alarmManager;
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        /*
         * time setting
         * */
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        try {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Long.valueOf(displayObjectArrayList.get(checkingPosition).getRefreshTime()), pendingIntent);

        } catch (RuntimeException e) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 300000, pendingIntent);
        }
    }

    /*
     * when timer is fired
     * */
    BroadcastReceiver alarmManger = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            requestSchedule();
            if (isConnected) setRefreshTimer();
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
     * when timer is fired
     * */
    BroadcastReceiver connection = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showProgressBar(true, null);
            isConnected = intent.getExtras().getBoolean("isConnected");
            if (isConnected) {
                //write permission and request from cloud
                requestWritePermission();
                //timer
                setRefreshTimer();
            } else {
                readLocalDisplay();
            }
        }
    };

    /*---------------------------------------------------------------------------read from local display-----------------------------------------------------------------------------------*/
    private void readLocalDisplay() {
        tbGallery
                .new Read("*")
                .where("status = 1")
                .orderByDesc("priority")
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
                                            jsonArray.getJSONObject(i).getString("path"),
                                            jsonArray.getJSONObject(i).getString("timer"),
                                            jsonArray.getJSONObject(i).getString("priority"),
                                            jsonArray.getJSONObject(i).getString("gallery_id"),
                                            jsonArray.getJSONObject(i).getString("display_type"),
                                            jsonArray.getJSONObject(i).getString("refresh_time"),
                                            jsonArray.getJSONObject(i).getString("status")
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
    }

    /*---------------------------------------------------------------------------delete unused display-----------------------------------------------------------------------------------*/
    private void deleteUnusedFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                tbGallery.new Read("gallery_id, path")
                        .where("status != 1")
                        .perform(new ResultCallBack.OnRead() {
                            @Override
                            public void readResult(String result) {
                                /*
                                 * if item found
                                 * */
                                if (!result.equals("Nothing") && !result.equals("Fail")) {
                                    try {
                                        JSONArray jsonArray = new JSONObject(result).getJSONArray("result");
                                        File dir = new File(directory.getAbsolutePath());
                                        /*
                                         * check directory existence
                                         * */
                                        if (dir.exists() && dir.isDirectory())
                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                new File(dir, jsonArray.getJSONObject(i).getString("path")).delete();
                                                /*
                                                 * delete from local database
                                                 * */
                                                tbGallery.new Delete().where("gallery_id = ?", jsonArray.getJSONObject(i).getString("gallery_id")).perform(new ResultCallBack.OnDelete() {
                                                    @Override
                                                    public void deleteResult(String status) {

                                                    }
                                                });
                                            }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
            }
        }).start();
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
}
