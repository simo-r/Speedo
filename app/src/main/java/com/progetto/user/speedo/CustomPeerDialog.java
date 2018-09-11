package com.progetto.user.speedo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Dialog che mostra i peer disponibili in
 * wifi direct nelle vicinanze
 */
public class CustomPeerDialog extends AppCompatDialogFragment {
    private static final String TAG = "CUSTOM-PEER-DIALOG";
    private static final String KEY_SAVED_PEERS = "com.progetto.user.speedo.PEERS";

    private ArrayList<WifiP2pDevice> peers;
    private PeersArrayAdapter arrayAdapter;

    public static CustomPeerDialog newInstance() {
        return new CustomPeerDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore delle info dopo rotazione dello schermo
            ArrayList<WifiP2pDevice> tmp = savedInstanceState.getParcelableArrayList(KEY_SAVED_PEERS);
            if (tmp != null) peers = tmp;
        }
        if (peers == null) peers = new ArrayList<>();
        arrayAdapter = new PeersArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, peers);
        Log.d(TAG, "onCreate: ");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog: ");
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
        builderSingle.setIcon(R.drawable.ic_run_icon);
        builderSingle.setTitle(R.string.select_device);
        builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CustomPeerDialog.this.dismiss();
            }
        });
        DialogInterface.OnClickListener itemClickListener = createPeerOnClickListener();
        builderSingle.setAdapter(arrayAdapter, itemClickListener);
        return builderSingle.create();
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        peers = null;
        arrayAdapter = null;
    }


    private DialogInterface.OnClickListener createPeerOnClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiP2pDevice peer = arrayAdapter.getItem(which);
                if (peer != null) {
                    final WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = peer.deviceAddress;
                    Log.d(TAG, "onPeersAvailable: CONNETTO A " + peer.deviceName);
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.showDisconnectSnackbar(peer);
                        activity.connectToDevice(config);
                    }
                } else {
                    Log.d(TAG, "onPeersAvailable: NO PEERS");
                }
            }
        };
    }

    /**
     * Aggiorna i peers all'interno del dialog
     * @param refreshedPeers nuovi peers
     */
    public void refreshPeers(Collection<WifiP2pDevice> refreshedPeers) {
        if (peers == null || arrayAdapter == null) {
            Log.d(TAG, "refreshPeers: NULL");
            return;
        }
        if (refreshedPeers.equals(peers)) {
            Log.d(TAG, "refreshPeers: EQUALS");
            return;
        }
        peers.clear();
        peers.addAll(refreshedPeers);
        arrayAdapter.notifyDataSetChanged();
        // TODO [DEBUG]
        for (WifiP2pDevice p : peers) {
            Log.d(TAG, "refreshPeers: PEER " + p.deviceName);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_SAVED_PEERS, peers);
    }
}
