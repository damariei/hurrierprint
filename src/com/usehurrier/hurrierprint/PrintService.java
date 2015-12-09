package com.usehurrier.hurrierprint;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.zj.btsdk.BluetoothService;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Timer;

/**
 * Created by damariei on 15-08-14.
 */
public class PrintService extends Service {
  public static PrintService instance = null;

  BluetoothService btService = null;
  BluetoothDevice printer = null;

  // LocalBinder, mBinder and onBind() allow other Activities to bind to this service.
  public class LocalBinder extends Binder {
    public PrintService getService() {
      return PrintService.this;
    }
  }

  private final LocalBinder mBinder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onCreate()
  {
    Log.i("PrintService", "Started Print Service");
    instance = this;
    btService = new BluetoothService(this, printHandler);
    if(!btService.isAvailable() || !btService.isBTopen()) {
      Toast.makeText(getApplicationContext(), "Bluetooth Unavailable", Toast.LENGTH_LONG).show();
      stopBTService();
      stopSelf();
    }
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("print-event"));

    Intent intent = new Intent("print-service-event");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }


  @Override
  public void onDestroy() {
    Log.i("PrintService", "Stopped Print Service");

    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

    Intent intent = new Intent("print-service-event");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    instance = null;
    stopBTService();

    AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent cancel_intent = new Intent(getApplicationContext(), PrintService.class);
    PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, cancel_intent, 0);
    alarm.cancel(pintent);

    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i("PrintService", "onStartCommand");
    connectPrinter();
    return android.app.Service.START_STICKY;
  }

  public void printMessage(String message) {
    try {
      if (message.length() > 0) {
        String[] lines = message.split("\n");
        for (String line : lines) {
          if (line.startsWith("!!")) {
            byte[] cmd = new BigInteger(line.substring(2), 16).toByteArray();
            btService.write(cmd);
          } else {
            if (line.length()==0) {
              btService.sendMessage(" ", "UTF-8");
            } else {
              btService.sendMessage(line, "UTF-8");
            }
          }
        }
        btService.write(new BigInteger("1B40", 16).toByteArray());
        btService.sendMessage("\n", "UTF-8");
      }
    } catch (Exception e) {
      Log.e("printMessage",e.getMessage());
    }
  }

  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      // Extract data included in the Intent
      printMessage(intent.getStringExtra("message"));
    }
  };

  private void connectPrinter() {
    try {
      if (btService != null) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        printer = btService.getDevByMac(sharedPreferences.getString("printer-mac-address", ""));
        btService.connect(printer);
      }
    } catch (Exception e) {
      Log.e("connectPrinter",e.getMessage());
    }
  }

  private void stopBTService() {
    if (btService != null) {
      try {
        btService.stop();
      } catch (Exception e) {
      }
      btService = null;
    }
    stopService(new Intent(getApplicationContext(), BluetoothService.class));
  }

  private final Handler printHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case BluetoothService.MESSAGE_STATE_CHANGE:
          switch (msg.arg1) {
            case BluetoothService.STATE_CONNECTED:
              Log.i("PrintHandler","Connect successful");
              break;
            case BluetoothService.STATE_CONNECTING:
              Log.i("PrintHandler","Connecting...");
              break;
            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
              break;
          }
          break;
        case BluetoothService.MESSAGE_CONNECTION_LOST:
          Log.i("PrintHandler", "Device connection was lost");
          connectPrinter();
          break;
        case BluetoothService.MESSAGE_UNABLE_CONNECT:
          Log.i("PrintHandler", "Unable to connect device");
          if (PrintService.instance!=null) {
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(), PrintService.class);
            PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
            alarm.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis()+5000, pintent);
          }
          break;
      }
    }
  };
}
