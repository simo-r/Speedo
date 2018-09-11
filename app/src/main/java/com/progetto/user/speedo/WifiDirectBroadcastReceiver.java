package com.progetto.user.speedo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Riceve i broadcast che riguardano il wifi direct.
 * Registrato e deregistrato nella onResume() e onPause()
 * dell'activity.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WIFI-BROADCAST";

    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private final MainActivity activity;

    WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity activity) {

        this.mManager = mManager;
        this.mChannel = mChannel;
        this.activity = activity;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action;
        if (intent == null || (action = intent.getAction()) == null) return;

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: WIFI STATE CHANGED");
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mManager != null) mManager.requestPeers(mChannel, activity);
            Log.d(TAG, "P2P peers changed");

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: CONNECTION CHANGED");
            if (mManager == null) return;
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                Log.d(TAG, "onReceive: E' CONNESSO ");
                mManager.requestConnectionInfo(mChannel, activity);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: INFO SUL MIO DEVICE CAMBIATE");
        }
    }


}
