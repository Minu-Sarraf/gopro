package com.examples.user.or_gopro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "gopro";
    public static final String PREFS_NAME = "MyPrefsFile";
    // Turn on camera : http://<ip>/bacpac/PW?t=<password>&p=%01
    WifiManager wifiManager;
    WifiReceiver wifiReceiver;
    Button wifiButton;
    TextView log;


    String wifiSSID;
    String wifiPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = (TextView) findViewById(R.id.log);

        wifiButton = (Button) findViewById(R.id.wifi);
        wifiButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                refreshWifi();
            }
        });

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        wifiSSID = settings.getString("wifiSSID", "");
        wifiPass = settings.getString("wifiPass", "");

        if (wifiSSID.equals("") || wifiPass.equals("")) {
            displaySettingPopup();
        }

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        log.setText(wifiInfo.getSSID());
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            log.setText("Disconnected");
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + wifiSSID + "\"";
        conf.preSharedKey = "\"" + wifiPass + "\"";
        wifiManager.addNetwork(conf);

        refreshWifi();

    }

    private void displaySettingPopup() {
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.setting,
                (ViewGroup) getCurrentFocus());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialoglayout);

        builder.setTitle("Setting");
        final EditText ssidIN = (EditText) dialoglayout
                .findViewById(R.id.ssidIN);
        final EditText pwIN = (EditText) dialoglayout.findViewById(R.id.pwIN);

        final SharedPreferences preferences = getSharedPreferences(PREFS_NAME,
                0);
        ssidIN.setText(preferences.getString("wifiSSID", "SSID"));
        pwIN.setText(preferences.getString("wifiPass", "password"));

        builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("wifiSSID", ssidIN.getText()
                                .toString());
                        wifiSSID = ssidIN.getText().toString();
                        editor.putString("wifiPass", pwIN.getText().toString());
                        wifiPass = pwIN.getText().toString();

                        // commit the edits
                        editor.commit();

                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Setting");
        return super.onCreateOptionsMenu(menu);
    }


   /* public boolean onMenuItemSelected(int featureId, MenuItem item) {
        displaySettingPopup();
        return super.onMenuItemSelected(featureId, item);
    }*/


    protected void refreshWifi() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo.getSSID() == null || !wifiInfo.getSSID().equals(wifiSSID)) {

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + wifiSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();

                    break;
                }
            }
        }

    }

    public void onToggleClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        URL url;
        try {
            if (on) {
                // Enable streaming
                url = new URL("http://10.5.5.9/camera/PV?t=" + wifiPass
                        + "&p=%02");

            } else {
                // Disable streaming
                url = new URL("http://10.5.5.9/camera/PV?t=" + wifiPass
                        + "&p=%01");
            }

            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                // readStream(in);
            } finally {
                urlConnection.disconnect();
            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void onPause() {
        unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(wifiReceiver, new IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION));
        super.onResume();
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            NetworkInfo wifiNetworkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            Log.v(TAG, "mWifiNetworkInfo: " + wifiNetworkInfo.toString());

            if (wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                log.setText("Connected to: "
                        + wifiManager.getConnectionInfo().getSSID());
                if (wifiManager != null
                        && wifiManager.getConnectionInfo() != null
                        && wifiManager.getConnectionInfo().getSSID() != null
                        && !wifiManager.getConnectionInfo().getSSID()
                        .equals(wifiSSID)) {
                    Log.v(TAG, wifiManager.getConnectionInfo().getSSID());

                    refreshWifi();
                }
            } else if (wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTING) {
                log.setText("Connecting...");
            } else if (wifiNetworkInfo.getState() == NetworkInfo.State.DISCONNECTING) {
                log.setText("Disconnecting...");
            } else if (wifiNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                log.setText("Disconnected");
            }

        }


    }
}