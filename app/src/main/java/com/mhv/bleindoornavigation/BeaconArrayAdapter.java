package com.mhv.bleindoornavigation;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Modified version of class found @ https://github.com/google/eddystone/blob/master/
 * tools/eddystone-validator/EddystoneValidator/app/src/main/
 * java/com/google/sample/eddystonevalidator/BeaconArrayAdapter.java
 */

public class BeaconArrayAdapter extends ArrayAdapter<Beacon> implements Filterable {
	
	private static final int DARK_GREEN = Color.argb(255, 0, 150, 0);
	private static final int DARK_RED = Color.argb(255, 150, 0, 0);
	
	private List<Beacon> allBeacons;
	private List<Beacon> filteredBeacons;
	
	public BeaconArrayAdapter(Context context, int resource, List<Beacon> allBeacons) {
		super(context, resource, allBeacons);
	    this.allBeacons = allBeacons;
	    this.filteredBeacons = allBeacons;
	}
	
	@Override
	public int getCount() {
		return filteredBeacons.size();
	}
	
	@Override
	public Beacon getItem(int position) {
		return filteredBeacons.get(position);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext())
					.inflate(R.layout.beacon_list_item, parent, false);
		}
		
		// Note: this is a listView and the convertView object here is likely to be
	    // a recycled view of some other row that isn't in view. You need to set every
	    // field regardless of emptiness to avoid displaying erroneous data.
		
		final Beacon beacon = getItem(position);

	    TextView deviceAddress = (TextView) convertView.findViewById(R.id.deviceAddress);
	    deviceAddress.setText(beacon.deviceAddress);

	    TextView rssi = (TextView) convertView.findViewById(R.id.rssi);
	    rssi.setText(String.valueOf(beacon.rssi));

	    TextView uidLabel = (TextView) convertView.findViewById(R.id.uidLabel);
	    TextView uidNamespace = (TextView) convertView.findViewById(R.id.uidNamespace);
	    TextView uidInstance = (TextView) convertView.findViewById(R.id.uidInstance);
	    TextView uidTxPower = (TextView) convertView.findViewById(R.id.uidTxPower);
	    View uidErrorGroup = convertView.findViewById(R.id.uidErrorGroup);

	    //UID field data
	    View uidGroup = convertView.findViewById(R.id.uidGroup);
	    if (!beacon.hasUidFrame) {
	    	grey(uidLabel);
	    	uidGroup.setVisibility(View.GONE);
	    } else {
	    	if (beacon.uidStatus.getErrors().isEmpty()) {
	    		green(uidLabel);
	    		uidErrorGroup.setVisibility(View.GONE);
	    	} else {
	    		red(uidLabel);
	    		uidErrorGroup.setVisibility(View.VISIBLE);
	    		((TextView) convertView.findViewById(R.id.uidErrors)).setText(beacon.uidStatus.getErrors());
	    	}
	    	uidNamespace.setText(beacon.uidStatus.uidValue.substring(0, 20));
	    	uidInstance.setText(beacon.uidStatus.uidValue.substring(20, 32));
	    	uidTxPower.setText(String.valueOf(beacon.uidStatus.txPower));
	    	uidGroup.setVisibility(View.VISIBLE);
	    }
	    
	    //Frame data
	    LinearLayout frameStatusGroup = (LinearLayout) convertView.findViewById(R.id.frameStatusGroup);
	    if (!beacon.frameStatus.getErrors().isEmpty()) {
	    	TextView frameStatus = (TextView) convertView.findViewById(R.id.frameStatus);
	    	frameStatus.setText(beacon.frameStatus.toString());
	    	frameStatusGroup.setVisibility(View.VISIBLE);
	    } else {
	    	frameStatusGroup.setVisibility(View.GONE);
	    }

	    return convertView;
	}
	
	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				List<Beacon> filteredBeacons;
				
				if (constraint != null && constraint.length() != 0) {
					filteredBeacons = new ArrayList<>();
					for (Beacon beacon : allBeacons) {
						if (beacon.contains(constraint.toString())) {
							filteredBeacons.add(beacon);
						}
					}
				} else {
					filteredBeacons = allBeacons;
				}
				results.count = filteredBeacons.size();
				results.values = filteredBeacons;
				return results;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				filteredBeacons = (List<Beacon>) results.values;
				if (results.count == 0) {
					notifyDataSetInvalidated();
				} else {
					notifyDataSetChanged();
				}
			}
		};
	}
	
	private void green(TextView v) {
		v.setTextColor(DARK_GREEN);
	}
	
	private void red(TextView v) {
		v.setTextColor(DARK_RED);
	}
	
	private void grey(TextView v) {
		v.setTextColor(Color.GRAY);
	}
}
