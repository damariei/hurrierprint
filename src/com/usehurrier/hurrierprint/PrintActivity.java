package com.usehurrier.hurrierprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.zj.btsdk.BluetoothService;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrintActivity extends Activity {
  Button btnSet;
  Button btnService;
  Button btnTestPrint;
  EditText txtPrinter;
  TextView txtStatus;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // Start register service for GCM.
    Intent gcmIntent = new Intent(this, GCMRegistrationIntentService.class);
    startService(gcmIntent);

    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
        new IntentFilter("print-service-event"));
  }

  @Override
  protected void onDestroy() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    setServiceStatus();
  }

  @Override
  public void onStart() {
    super.onStart();

    btnSet = (Button) this.findViewById(R.id.btnSet);
    btnSet.setOnClickListener(new ClickEvent());
    btnService = (Button) this.findViewById(R.id.btnService);
    btnService.setOnClickListener(new ClickEvent());
    btnTestPrint = (Button) this.findViewById(R.id.btnTestPrint);
    btnTestPrint.setOnClickListener(new ClickEvent());
    txtPrinter = (EditText) findViewById(R.id.txtPrinter);
    txtStatus = (TextView) findViewById(R.id.txtStatus);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    txtPrinter.setText(sharedPreferences.getString("printer-mac-address", ""));

    setServiceStatus();
  }

  private void setServiceStatus() {
    if (PrintService.instance==null) {
      btnService.setText(R.string.start_service);
      btnTestPrint.setEnabled(false);
      btnSet.setEnabled(true);
      txtPrinter.setEnabled(true);
      txtStatus.setText(R.string.service_disconnected);
      txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    } else {
      btnService.setText(R.string.stop_service);
      btnTestPrint.setEnabled(true);
      btnSet.setEnabled(false);
      txtPrinter.setEnabled(false);
      txtStatus.setText(R.string.service_connected);
      txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }
  }

  class ClickEvent implements View.OnClickListener {
    public void onClick(View v) {
      if (v == btnService) {

        if (PrintService.instance==null) {
          // Start print service.
          Intent printIntent = new Intent(getApplicationContext(), PrintService.class);
          startService(printIntent);
        } else {
          // Stop print service.
          AlertDialog.Builder builder = new AlertDialog.Builder(PrintActivity.this);
          builder.setTitle("Service Password");
          final EditText input = new EditText(getApplicationContext());
          input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
          builder.setView(input);

          builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              String password = input.getText().toString();

              if (password.equals("printstop")) {
                stopService(new Intent(getApplicationContext(), PrintService.class));
              } else {
                Toast.makeText(getApplicationContext(), "Service Password Invalid", Toast.LENGTH_SHORT).show();
              }
            }
          });
          builder.show();
        }

      } else if (v == btnSet) {
        String printAddress = txtPrinter.getText().toString();

        if (printAddress!=null && printAddress.length()==17) {
          SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
          sharedPreferences.edit().putString("printer-mac-address", printAddress).apply();
          Toast.makeText(getApplicationContext(), "Printer Set", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(getApplicationContext(), "Printer Address Invalid", Toast.LENGTH_SHORT).show();
        }
      } else if (v == btnTestPrint) {
        Intent intent = new Intent("print-event");
        intent.putExtra("message", "PRINTER TEST\n"+(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
      }
    }
  }

  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      setServiceStatus();
    }
  };
}
