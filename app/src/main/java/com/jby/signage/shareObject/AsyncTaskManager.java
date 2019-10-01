package com.jby.signage.shareObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Created by wypan on 2/14/2017.
 */

public class AsyncTaskManager extends AsyncTask<String, String, JSONObject> {

    private Context context;
    private String url;
    private String dataPost;

    public AsyncTaskManager(Context context, String url, String dataPost) {
        this.context = context;
        this.url = url;
        this.dataPost = dataPost;
    }

    @Override
    protected void onPreExecute() {
//        System.out.println("on pre execute");
        super.onPreExecute();

        if (NetworkUtils.getConnectivityStatus(this.context) == NetworkUtils.TYPE_NOT_CONNECTED)
            cancel(true);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        //        Log.i("JSONObjectResponse", JSONObjectResponse.toString());
        return new JSONParser().getJSONFromUrl(this.url, this.dataPost);
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
//        System.out.println("on post execute");
    }

    @Override
    protected void onCancelled(JSONObject jsonObject) {
        super.onCancelled(jsonObject);
        Toast.makeText(this.context, "Please Connect To A Network", Toast.LENGTH_SHORT).show();
        Log.i("AsyncTask Cancel", "Connection Timeout");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

    }
}
