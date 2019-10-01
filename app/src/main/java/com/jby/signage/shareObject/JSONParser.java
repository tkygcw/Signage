package com.jby.signage.shareObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by User on 29/7/2016.
 */
class JSONParser {

    static InputStream is = null;
    static JSONObject jobj = null;
    static String json ="";

    JSONParser(){}

    JSONObject getJSONFromUrl(String url, String data) {
        try{
            //拿到的URL 做成一个新的URL
            URL link = new URL(url);
            URLConnection conn = link.openConnection(); //打开连接
            conn.setDoOutput(true);//出口开
            //新的outputStreamWriter holding url 的 outputdata
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data); //这个 outputStreamWriter holding url 写data 进去
            //data example：Pang Chee Weng = username
            //              22 = Age
            wr.flush();
            //这里主要是让 json holding data
            //Buffer 是做checking 看下一行有没有 没有就stop
            //有的话 就把data 存进去 StringBuilder 里面
            //没有下一行 吧string builder 的 data to string store 在 json variable
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line).append("\n");
            }
            wr.close();
            rd.close();
            json = sb.toString();
            //让 JSON jobj holding 刚才的 data
            try {
                jobj = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jobj;
    }
}

