package com.usehurrier.hurrierprint;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by damariei on 15-08-13.
 */
public class CustomGCMListenerService extends GcmListenerService {
  private static final String TAG = "MyGcmListenerService";

  @Override
  public void onCreate () {
    Log.i("GCM LISTENER","CREATED");
  }

  @Override
  public void onDestroy() {
    Log.i("GCM LISTENER","DESTROYED");
    super.onDestroy();
  }

  /**
   * Called when message is received.
   *
   * @param from SenderID of the sender.
   * @param data Data bundle containing message data as key/value pairs.
   *             For Set of keys use data.keySet().
   */
  @Override
  public void onMessageReceived(String from, Bundle data) {
    try {
      String message = new JSONObject(data.getString("payload")).getJSONObject("android").getString("alert");

      HttpClient httpClient = new DefaultHttpClient();
      HttpGet httpGet = new HttpGet(message);

      try {
        HttpResponse response = httpClient.execute(httpGet);
        String orderMessage = EntityUtils.toString(response.getEntity(), "UTF-8");
        Intent intent = new Intent("print-event");
        intent.putExtra("message", orderMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (JSONException e) {
      Log.d("JSON Parse Error", e.getMessage());
    }
  }

}