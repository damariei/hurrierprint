package com.usehurrier.hurrierprint;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by damariei on 15-08-28.
 */
public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBluetoothAdapter.enable();
    Intent printIntent = new Intent(context, PrintService.class);
    context.startService(printIntent);
  }
}