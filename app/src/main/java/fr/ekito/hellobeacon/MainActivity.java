package fr.ekito.hellobeacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimbal.android.Beacon;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Communication;
import com.gimbal.android.CommunicationListener;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    // constante RSSI mesuréee à 1m
    public static final int TX_POWER = -60;

    private PlaceManager placeManager;
    private PlaceEventListener placeEventListener;
    private Map<String, BeaconSighting> data = new HashMap<>();
    private ArrayAdapter<String> listAdapter;
    private ListView listView;
    private TextView textView;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1);
        listView = (ListView) findViewById(R.id.list);
        textView = (TextView) findViewById(R.id.text);
        listView.setAdapter(listAdapter);

        Gimbal.setApiKey(this.getApplication(), "81de0756-daa3-45dc-90d5-3580f07a101f");

        placeEventListener = new PlaceEventListener() {
            @Override
            public void onVisitStart(Visit visit) {
                Toast.makeText(getApplicationContext(), String.format("Start Visit for %s", visit.getPlace().getName()), Toast.LENGTH_LONG);
            }

            @Override
            public void onVisitEnd(Visit visit) {
                Toast.makeText(getApplicationContext(), String.format("End Visit for %s", visit.getPlace().getName()), Toast.LENGTH_LONG);
            }

            @Override
            public void onBeaconSighting(BeaconSighting beaconSighting, List<Visit> list) {
                data.put(beaconSighting.getBeacon().getName(), beaconSighting);
                textView.setVisibility(View.GONE);

                listAdapter.clear();
                List<String> collection = asString(data.values());
                listAdapter.addAll(collection);
                listAdapter.notifyDataSetChanged();
            }
        };

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //  BT no activated
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            textView.setTextColor(getResources().getColor(R.color.red));
            textView.setText(getString(R.string.ble_not_supported));
        } else if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            textView.setTextColor(getResources().getColor(R.color.red));
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            textView.setText(getString(R.string.ble_not_activated));
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

            placeManager = PlaceManager.getInstance();
            placeManager.addListener(placeEventListener);
            placeManager.startMonitoring();

            CommunicationManager.getInstance().addListener(new CommunicationListener() {
                @Override
                public Collection<Communication> presentNotificationForCommunications(Collection<Communication> collection, Visit visit) {
                    return super.presentNotificationForCommunications(collection, visit);
                }
            });
            CommunicationManager.getInstance().startReceivingCommunications();
        }
    }

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    private List<String> asString(Collection<BeaconSighting> v) {
        List<BeaconSighting> values = new ArrayList<>(v);
        Collections.sort(values, new Comparator<BeaconSighting>() {
            @Override
            public int compare(BeaconSighting lhs, BeaconSighting rhs) {
                return rhs.getRSSI().compareTo(lhs.getRSSI());
            }
        });
        List<String> list = new ArrayList<>();
        for (BeaconSighting beaconSighting : values) {
            Beacon beacon = beaconSighting.getBeacon();
            double accuracy = calculateAccuracy(TX_POWER, beaconSighting.getRSSI());
            DecimalFormat df = new DecimalFormat("#.00");
            String format = String.format("Name:%S - ID:%s\nRange ~%sm (%sdb)", beacon.getName(), beacon.getIdentifier(), df.format(accuracy), beaconSighting.getRSSI());
            list.add(format);
//            list.add("Name: " + beacon.getName() + " - ID:" + beacon.getIdentifier() + "\nRange: " + beaconSighting.getRSSI() + "("+accuracy+")");
        }
        return list;
    }

//    private String renderRange(Integer rssi) {
//        if (rssi < -100) return "    ";
//        else if (rssi > -100 && rssi <= -80) return "+   ";
//        else if (rssi > -80 && rssi <= -55) return "++  ";
//        else if (rssi > -55 && rssi <= -35) return "+++ ";
//        else return "++++";
//    }

}
