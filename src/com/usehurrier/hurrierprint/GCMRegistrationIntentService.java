package com.usehurrier.hurrierprint;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by damariei on 15-08-13.
 */

public class GCMRegistrationIntentService extends IntentService {

  private static final String TAG = "RegIntentService";
  private static final String[] TOPICS = {"global"};

  public GCMRegistrationIntentService() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    try {
      // In the (unlikely) event that multiple refresh operations occur simultaneously,
      // ensure that they are processed sequentially.
      synchronized (TAG) {
        // [START register_for_gcm]
        // Initially this call goes out to the network to retrieve the token, subsequent calls
        // are local.
        // [START get_token]
        InstanceID instanceID = InstanceID.getInstance(this);
        String token = instanceID.getToken(getString(R.string.gcm_SenderId),
            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        // [END get_token]
        Log.i(TAG, "GCM Registration Token: " + token);

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(token);

        // You should store a boolean that indicates whether the generated token has been
        // sent to your server. If the boolean is false, send the token to your server,
        // otherwise your server should have already received the token.
        sharedPreferences.edit().putBoolean("sentTokenToServer", true).apply();
        // [END register_for_gcm]
      }
    } catch (Exception e) {
      Log.d(TAG, "Failed to complete token refresh", e);
      // If an exception happens while fetching the new token or updating our registration data
      // on a third-party server, this ensures that we'll attempt the update at a later time.
      sharedPreferences.edit().putBoolean("sentTokenToServer", false).apply();
    }
  }

  /**
   * Persist registration to third-party servers.
   *
   * Modify this method to associate the user's GCM registration token with any server-side account
   * maintained by your application.
   *
   * @param token The new token.
   */
  private void sendRegistrationToServer(String token) {
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost("http://api.cloud.appcelerator.com/v1/push_notification/subscribe_token.json?key="+
        getString(R.string.titanium_api_key));

    List<NameValuePair> data = new ArrayList<NameValuePair>(2);
    data.add(new BasicNameValuePair("type", "android"));
    data.add(new BasicNameValuePair("channel", "printing"));
    data.add(new BasicNameValuePair("device_token", token));

    //Encoding POST data
    try {
      httpPost.setEntity(new UrlEncodedFormEntity(data));
    } catch (UnsupportedEncodingException e) {
      Log.e("Subscription HTTP Error", "ERROR: " + e.getMessage());
    }

    try {
      HttpResponse response = httpClient.execute(httpPost);
    } catch (ClientProtocolException e) {
      // Log exception
      e.printStackTrace();
    } catch (IOException e) {
      // Log exception
      e.printStackTrace();
    }
  }

}