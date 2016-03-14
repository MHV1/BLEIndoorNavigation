package com.mhv.bleindoornavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    private RelativeLayout mainContainer;
	private PointF location;
	private MapPointer mapPointer;

	//Sensor variables
	private SensorManager sensorManager;
	private String compassOrientation = "";
	private TextView orientationDisplay;
	private TextView distanceDisplay;

	private static final int SCAN_INTERVAL = 1000;
	private ArrayList<Integer> rssiScanResults = new ArrayList<>();
	private ArrayList<Double> distances = new ArrayList<>();

	private static final String TAG = "BLE_IN";
	private static final int REQUEST_ENABLE_BLUETOOTH = 1;

	// An aggressive scan for nearby devices that reports immediately.
	private static final ScanSettings SCAN_SETTINGS =
			new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0)
					.build();

	private static final Handler handler = new Handler(Looper.getMainLooper());

	// The Eddystone Service UUID, 0xFEAA.
	private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
			ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

	private BluetoothLeScanner scanner;
	private BeaconArrayAdapter arrayAdapter;
	private int onLostTimeoutMillis = 3000;

	private List<ScanFilter> scanFilters;
	private ScanCallback scanCallback;

	private Map<String /* device address */, Beacon> deviceToBeaconMap = new HashMap<>();

	private double newestDistance = 0.0;
	private double recordedDistance = 0.0;
	private double diference = 0.0;
	private String movement = "Forth";

	private Button done;
	private Button cancel;
	private boolean setup = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		init();

		done = (Button) findViewById(R.id.button_done);
		cancel = (Button) findViewById(R.id.button_cancel);
		done.setVisibility(View.INVISIBLE);
		cancel.setVisibility(View.INVISIBLE);
		done.setEnabled(false);
		cancel.setEnabled(false);

		ArrayList<Beacon> arrayList = new ArrayList<>();
		arrayAdapter = new BeaconArrayAdapter(this, R.layout.beacon_list_item, arrayList);
		scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());

		//Initialize SensorManager
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		orientationDisplay = (TextView) findViewById(R.id.orientationDisplay);

		mainContainer = (RelativeLayout) findViewById(R.id.main_container);
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
                            beaconSetup(tap);
						}

						break;
					case MotionEvent.ACTION_UP:
						break;
				}
				return true;
			}
		});

		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String appName = getApplicationContext().getString(R.string.app_name);
				setTitle(appName);
				done.setVisibility(View.INVISIBLE);
				cancel.setVisibility(View.INVISIBLE);
				done.setEnabled(false);
				cancel.setEnabled(false);
				setup = false;
			}
		});

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

			//Scan error codes
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

	// Checks the frame type and hands off the service data to the validation module.
	private void validateServiceData(String deviceAddress, byte[] serviceData) {
		Beacon beacon = deviceToBeaconMap.get(deviceAddress);
		if (serviceData == null) {
			String err = "Null Eddystone service data";
			beacon.frameStatus.nullServiceData = err;
			logDeviceError(deviceAddress, err);
			return;
		} else {
			//this.deviceAddress.setText("Device Address: " + beacon.deviceAddress);
			rssiScanResults.add(beacon.rssi);
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
			rssiScanResults.clear();
			distances.clear();
			scanner.startScan(null, SCAN_SETTINGS, scanCallback);

			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					scanner.stopScan(scanCallback);

					if (rssiScanResults.size() > 0) {
						for (int i = 0; i < rssiScanResults.size(); i++) {
							distances.add(distanceClaculation(rssiScanResults.get(i)));
						}
					}

					newestDistance = averageDistance();
					diference = newestDistance - recordedDistance;
					//Log.i(TAG, movement + " " + newestDistance + " " + recordedDistance);

					if(diference == 0) {
						movement = "Stay";
					} else if (diference >= 1) {
						movement = "Back";
					} else {
						movement = "Forth";
					}

					recordedDistance = averageDistance();
					runScan();
				}
			}, SCAN_INTERVAL);
		}
	}

	private double distanceClaculation(int rssi) {
		return Math.pow(10, (-62 - rssi) / 20.00);
	}

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

		if(distances.size() > 0) {
			for (int i = 0; i < distances.size(); i++) {
				sum += distances.get(i);
			}
			averageDistance = Math.round((sum / distances.size() * 100) /100);
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
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
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
		cancel.setVisibility(View.VISIBLE);
		done.setEnabled(true);
		cancel.setEnabled(true);
		setup = true;
	}

	public void beaconSetup(PointF beaconPosition) {
        final BeaconView beaconView = new BeaconView(getApplicationContext());
        beaconView.setPosition(beaconPosition);
        mainContainer.addView(beaconView);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.beacon_setup_dialog, null, false);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);

        final EditText input = (EditText) promptView.findViewById(R.id.description_field);

        Button selectBeacon = (Button) promptView.findViewById(R.id.button_beacon_select);
        selectBeacon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBeaconScanner();
            }
        });

        alertDialogBuilder
                .setTitle("New Beacon Setup")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mainContainer.removeView(beaconView);
                                dialog.cancel();
                            }
                        });

        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    private void showBeaconScanner() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle("Beacon Scanner");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.beacon_list, null, false);
        dialog.setContentView(v);
        dialog.setCancelable(true);

        ListView beaconList = (ListView) dialog.findViewById(R.id.listView);
        beaconList.setAdapter(arrayAdapter);
        dialog.show();
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
			showBeaconScanner();

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
	public void onSensorChanged(SensorEvent event) {
		float degree = Math.round(event.values[0]);

		if (degree >= 0 && degree < 90) {
			compassOrientation = "N";
		} else if (degree >= 90 && degree < 180) {
			compassOrientation = "E";
		} else if (degree >= 180 && degree < 270) {
			compassOrientation = "S";
		} else {
			compassOrientation = "W";
		}
		orientationDisplay.setText("Heading: " + Float.toString(degree) + "º" + " " + compassOrientation);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
