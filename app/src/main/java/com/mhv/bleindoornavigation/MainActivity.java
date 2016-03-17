package com.mhv.bleindoornavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Modified version of class found @ https://github.com/google/eddystone/blob/master/
 * tools/eddystone-validator/EddystoneValidator/app/src/main/
 * java/com/google/sample/eddystonevalidator/MainActivityFragment.java
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "BLE_IN";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private RelativeLayout mainContainer;
    private MapPointer mapPointer;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] accelValues;
    private float[] magnetValues;
    private String compassOrientation = "";
    private TextView orientationDisplay;

    //The Eddystone Service UUID, 0xFEAA.
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    //An aggressive scan for nearby devices that reports immediately.
    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0)
                    .build();

    private static final int SCAN_INTERVAL = 1000;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private BeaconArrayAdapter arrayAdapter;
    private int onLostTimeoutMillis = 3000;
    private Map<String /* device address */, Beacon> deviceToBeaconMap = new HashMap<>();
    private Map<String, Beacon> validatedBeacons = new HashMap<>();

    private ArrayList<Double> distances = new ArrayList<>();
    private double newestDistance = 0.0;
    private double recordedDistance = 0.0;
    private double distanceDifference = 0.0;
    private String movement = "Forth";

    private boolean setup = false;
    private Button done;
    private List<BeaconView> configuredBeacons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        init();

        //Initialize sensors used for compass.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        orientationDisplay = (TextView) findViewById(R.id.orientationDisplay);

        //Set up the list used to displayed beacon scan results.
        ArrayList<Beacon> arrayList = new ArrayList<>();
        arrayAdapter = new BeaconArrayAdapter(this, R.layout.beacon_list_item, arrayList);
        new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build();

        //Main relative layout containing all views.
        mainContainer = (RelativeLayout) findViewById(R.id.main_container);
        initMapPointer();
        mainContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (!setup) {
                            PointF tap = new PointF(event.getX(), event.getY());
                            Coordinates position = new Coordinates(tap.x, tap.y);
                            mapPointer.setPointerPosition(position);
                            mapPointer.postInvalidate();

                        } else {
                            PointF tap = new PointF(event.getX(), event.getY());
                            BeaconView beaconView = new BeaconView(getApplicationContext());
                            beaconView.setPosition(tap);
                            mainContainer.addView(beaconView);
                            beaconSetup(beaconView);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        break;
                }
                return true;
            }
        });

        //Checks if there are any already configured beacons.
        //TODO: Planning to save beacon configuration for future uses.
        if(configuredBeacons.isEmpty()) {
            showSetupAlertDialog("No beacons found",
                    "Beacons must be configured before proceeding.");
        } else {
            runScan();
        }

        //Apply setup changes and exit.
        done = (Button) findViewById(R.id.button_done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(configuredBeacons.isEmpty()) {
                    showSetupAlertDialog("No beacons found",
                            "Beacons must be configured before proceeding.");

                } else {
                    exitSetupMode();
                }
            }
        });
        done.setVisibility(View.INVISIBLE);
        done.setEnabled(false);

        //BLE scanner Callback.
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    return;
                }

                String deviceAddress = result.getDevice().getAddress();
                Beacon beacon;
                if (!deviceToBeaconMap.containsKey(deviceAddress)) {
                    beacon = new Beacon(deviceAddress, result.getRssi());
                    deviceToBeaconMap.put(deviceAddress, beacon);
                    arrayAdapter.add(beacon);

                } else {
                    deviceToBeaconMap.get(deviceAddress).lastSeenTimestamp = System.currentTimeMillis();
                    deviceToBeaconMap.get(deviceAddress).rssi = result.getRssi();
                }
                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
                validateServiceData(deviceAddress, serviceData);
            }

            //Scan error codes.
            @Override
            public void onScanFailed(int errorCode) {
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        logErrorAndShowToast("SCAN_FAILED_ALREADY_STARTED");
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        logErrorAndShowToast("SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        logErrorAndShowToast("SCAN_FAILED_FEATURE_UNSUPPORTED");
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        logErrorAndShowToast("SCAN_FAILED_INTERNAL_ERROR");
                        break;
                    default:
                        logErrorAndShowToast("Scan failed, unknown error code");
                        break;
                }
            }
        };
    }


    // Attempts to create the scanner.
    private void init() {
        BluetoothManager manager = (BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        if (btAdapter == null) {
            showFinishingAlertDialog("Bluetooth Error", "Bluetooth not detected on device");
        } else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            scanner = btAdapter.getBluetoothLeScanner();
        }
    }


    //Show map pointer on the screen (user position).
    private void initMapPointer() {
        mapPointer = new MapPointer(getApplicationContext());
        mainContainer.addView(mapPointer);

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    mapPointer.move(compassOrientation, movement);
                    Log.i(TAG, "" + mapPointer.move(compassOrientation, movement));
                    mapPointer.postInvalidate();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "InterruptedException");
                    }
                }
            }
        }).start();
    }


    // Pops an AlertDialog that quits the app on OK.
    private void showFinishingAlertDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).show();
    }


    // Used when no beacons have been configured
    private void showSetupAlertDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        initSetupMode();
                    }
                }).show();
    }


    // Checks the frame type and hands off the service data to the validation module.
    private void validateServiceData(String deviceAddress, byte[] serviceData) {
        Beacon beacon = deviceToBeaconMap.get(deviceAddress);
        if (serviceData == null) {
            String err = "Null Eddystone service data";
            beacon.frameStatus.nullServiceData = err;
            logDeviceError(deviceAddress, err);
            return;

        } else {
            validatedBeacons.put(deviceAddress, beacon);
        }

        Log.v(TAG, deviceAddress + " " + Utils.toHexString(serviceData));
        switch (serviceData[0]) {

            case Constants.UID_FRAME_TYPE:
                UidValidator.validate(deviceAddress, serviceData, beacon);
                break;

            default:
                String err = String.format("Invalid frame type byte %02X", serviceData[0]);
                beacon.frameStatus.invalidFrameType = err;
                logDeviceError(deviceAddress, err);
                break;
        }
        arrayAdapter.notifyDataSetChanged();
    }


    public void runScan() {
        if (scanner != null) {
            distances.clear();
            scanner.startScan(null, SCAN_SETTINGS, scanCallback);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(scanCallback);

                    if (!configuredBeacons.isEmpty()) {
                        for (BeaconView bw : configuredBeacons) {
                            Beacon beacon = validatedBeacons.get(bw.getBeaconAddress());

                            Log.i(TAG, "TEST " + beacon.deviceAddress + beacon.rssi);
                            distances.add(distanceCalculation(beacon.rssi));

                            newestDistance = averageDistance();
                            distanceDifference = newestDistance - recordedDistance;
                            //Log.i(TAG, movement + " " + newestDistance + " " + recordedDistance);

                            if (distanceDifference == 0) {
                                movement = "Stay";
                            } else if (distanceDifference >= 1) {
                                movement = "Back";
                            } else {
                                movement = "Forth";
                            }

                            recordedDistance = averageDistance();
                            bw.setDistanceToUser("" + recordedDistance + " m");
                        }
                    }

                    runScan();
                }
            }, SCAN_INTERVAL);
        }
    }


    private double distanceCalculation(int rssi) {
        return Math.pow(10, (-62 - rssi) / 20.00);
    }


    //Used for testing calculation accuracy.
	/*private int averageRSSI() {
        int averageRSSI = 0;
		int sum = 0;

		if(rssiScanResults.size() > 0) {
			for (int i = 0; i < rssiScanResults.size(); i++) {
				sum += rssiScanResults.get(i);
			}
			averageRSSI = sum / rssiScanResults.size();
		}
		return averageRSSI;
	}*/


    private double averageDistance() {
        double averageDistance = 0;
        double sum = 0;

        if (distances.size() > 0) {
            for (int i = 0; i < distances.size(); i++) {
                sum += distances.get(i);
            }
            averageDistance = Math.round((sum / distances.size() * 100) / 100);
        }
        return averageDistance;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                BluetoothManager manager = (BluetoothManager) this.getApplicationContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter btAdapter = manager.getAdapter();
                if (btAdapter == null) {
                    showFinishingAlertDialog("Bluetooth Error", "Bluetooth not detected on device");
                } else if (!btAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                } else {
                    scanner = btAdapter.getBluetoothLeScanner();
                }
            } else {
                finish();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        /*Deprecated:
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);*/
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        handler.removeCallbacksAndMessages(null);
        setOnLostRunnable();
        runScan();
    }


    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (scanner != null) {
            scanner.stopScan(scanCallback);
        }
    }


    private void setOnLostRunnable() {
        Runnable removeLostDevices = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                Iterator<Map.Entry<String, Beacon>> itr = deviceToBeaconMap.entrySet().iterator();

                while (itr.hasNext()) {
                    Beacon beacon = itr.next().getValue();
                    if ((time - beacon.lastSeenTimestamp) > onLostTimeoutMillis) {
                        itr.remove();
                        arrayAdapter.remove(beacon);
                    }
                }
                handler.postDelayed(this, onLostTimeoutMillis);
            }
        };
        handler.postDelayed(removeLostDevices, onLostTimeoutMillis);
    }


    public void initSetupMode() {
        final String appName = getApplicationContext().getString(R.string.app_name);
        setTitle(appName + " (Setup)");
        done.setVisibility(View.VISIBLE);
        done.setEnabled(true);
        setup = true;
    }


    public void exitSetupMode() {
        final String appName = getApplicationContext().getString(R.string.app_name);
        setTitle(appName);
        done.setVisibility(View.INVISIBLE);
        done.setEnabled(false);
        setup = false;
    }


    public void beaconSetup(final BeaconView beaconView) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.beacon_setup_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);

        final TextView beaconSelected = (TextView) dialogView.findViewById(R.id.beaconSelected);
        final EditText input = (EditText) dialogView.findViewById(R.id.description_field);

        Button selectBeacon = (Button) dialogView.findViewById(R.id.button_beacon_select);
        selectBeacon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setTitle("Beacon Scanner");
                ListView beaconList = new ListView(MainActivity.this);
                beaconList.setAdapter(arrayAdapter);
                beaconList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String beaconAddress = arrayAdapter.getItem(position).deviceAddress;
                        beaconSelected.setText("Beacon: " + beaconAddress);
                        beaconView.setBeaconAddress(beaconAddress);
                        dialog.dismiss();
                    }
                });
                dialog.setContentView(beaconList);
                dialog.setCancelable(true);
                dialog.show();
            }
        });

        dialogBuilder
                .setTitle("New Beacon Setup")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (input.length() > 0) {
                            beaconView.setBeaconDescription("Description: " + input.getText().toString());
                        }

                        if(beaconView.getBeaconAddress() != null) {
                            configuredBeacons.add(beaconView);

                        } else {
                            Toast.makeText(MainActivity.this, "Please, select a beacon", Toast.LENGTH_SHORT).show();
                            beaconSetup(beaconView);
                        }

                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mainContainer.removeView(beaconView);
                                dialog.cancel();
                            }
                        });

        dialogBuilder.create().show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_beacon_scanner) {
            final Dialog dialog = new Dialog(this);
            dialog.setTitle("Beacon Scanner");
            ListView beaconList = new ListView(this);
            beaconList.setAdapter(arrayAdapter);
            dialog.setContentView(beaconList);
            dialog.setCancelable(true);
            dialog.show();

        } else if (id == R.id.action_beacon_setup) {
            initSetupMode();

        } else if (id == R.id.attachment) {
            Dialog dialog = new Dialog(this);
            dialog.setTitle("Beacon Details");
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.beacon_details, null, false);
            dialog.setContentView(v);
            dialog.setCancelable(true);
            dialog.show();
        }
        return true;
    }


    private void logErrorAndShowToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }


    private void logDeviceError(String deviceAddress, String err) {
        Log.e(TAG, deviceAddress + ": " + err);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        /*Deprecated:
		float degree = Math.round(event.values[0]);*/
        float rotation[] = new float[9];
        float orientation[] = new float[3];
        float orientationDegree = 0f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accelValues = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magnetValues = event.values;

        if (accelValues != null && magnetValues != null) {
            if (SensorManager.getRotationMatrix(rotation, null, accelValues, magnetValues)) {
                SensorManager.getOrientation(rotation, orientation);
                float azimuthDegree = (float) (Math.toDegrees(orientation[0]) + 360) % 360;
                orientationDegree = Math.round(azimuthDegree);
            }
        }

        if (orientationDegree >= 0 && orientationDegree < 90) {
            compassOrientation = "N";
        } else if (orientationDegree >= 90 && orientationDegree < 180) {
            compassOrientation = "E";
        } else if (orientationDegree >= 180 && orientationDegree < 270) {
            compassOrientation = "S";
        } else {
            compassOrientation = "W";
        }
        orientationDisplay.setText("Heading: " + Float.toString(orientationDegree) + "º" + " " + compassOrientation);
    }
}
