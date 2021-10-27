package com.jby.signage.connection;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class NetworkAccessChecker extends AsyncTask<Void, Void, Void> {
    private Activity activity;
    private InternetCheckListener listener;

    public NetworkAccessChecker(Activity x) {
        activity = x;
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean b = hasInternetAccess();
        listener.onComplete(b);

        return null;
    }

    public void isInternetConnectionAvailable(InternetCheckListener x) {
        listener = x;
        execute();
    }


    private boolean hasInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://clients3.google.com/generate_204").openConnection());
            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500);
            urlc.connect();
            return (urlc.getResponseCode() == 204 &&
                    urlc.getContentLength() == 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public interface InternetCheckListener {
        void onComplete(boolean connected);
    }

}