package com.progetto.user.speedo;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Array adapter per i peers visibili in wifi direct
 */
class PeersArrayAdapter extends ArrayAdapter<WifiP2pDevice> {
    public static final String TAG = "PEERS-ARRAY-ADAPTER";

    private final StringBuilder stringBuilder;

    private final List<WifiP2pDevice> peers;

    PeersArrayAdapter(Context context, int textViewResourceId,
                      List<WifiP2pDevice> objects) {
        super(context, textViewResourceId, objects);
        peers = objects;
        stringBuilder = new StringBuilder();
    }


    /**
     * Riusa la convert view se è possibile e ne cambia
     * il testo con le informazioni relativa alla nuova posizione
     *
     * @param position posizione dell'informazione richiesta
     * @param convertView vista da convertire
     * @param parent parent della convert view
     * @return convert view con dati aggiornati
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Log.d(TAG, "getView: ");
        if(convertView == null){
            convertView = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_list_item_1,parent,false);
        }
        WifiP2pDevice peer = peers.get(position);
        stringBuilder.delete(0, stringBuilder.capacity());
        stringBuilder.append(peer.deviceName);
        stringBuilder.append(" - ");
        stringBuilder.append(getDeviceStatus(peer.status));
        TextView device = convertView.findViewById(android.R.id.text1);
        device.setText(stringBuilder);
        return convertView;
    }

    /**
     * @param deviceStatus codifica dello status del peer
     * @return status del peer
     */
    private static String getDeviceStatus(int deviceStatus) {
        Log.d("PEERS-ADAPTER", "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}
