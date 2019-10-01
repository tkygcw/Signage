package com.jby.signage.shareObject;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

//    implementation 'com.mcxiaoke.volley:library:1.0.19'
public class MySingleton {
    private static MySingleton mInstance;
    private RequestQueue requestQueue;
    private static Context mContext;

    private MySingleton(Context context){
        mContext = context;
        requestQueue = getRequestQueue();
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null)
            requestQueue =  Volley.newRequestQueue(mContext.getApplicationContext());
        return  requestQueue;
    }

    public static synchronized MySingleton getmInstance(Context context){
        if(mInstance == null)
        {
            mInstance = new MySingleton(context);
        }
        return  mInstance;
    }

    public<T>  void addToRequestQueue(Request request){
        getRequestQueue().add(request);
    }

}
