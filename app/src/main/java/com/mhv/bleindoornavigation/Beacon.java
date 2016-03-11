package com.mhv.bleindoornavigation;

import java.util.Locale;

/**
 * Modified version of class found @ https://github.com/google/eddystone/blob/master/
 * tools/eddystone-validator/EddystoneValidator/app/src/main/
 * java/com/google/sample/eddystonevalidator/Beacon.java
 */

public class Beacon {
	
	private static final String BULLET = "*";
	final String deviceAddress;
	int rssi;
	
	// Used to remove devices from the listview when they haven't been seen in a while.
	long lastSeenTimestamp = System.currentTimeMillis();
	
	byte[] uidServiceData;
	boolean hasUidFrame;
	UidStatus uidStatus = new UidStatus();
	
	FrameStatus frameStatus = new FrameStatus();
	
	Beacon(String deviceAddress, int rssi) {
		this.deviceAddress = deviceAddress;
		this.rssi = rssi;
	}
	
	//Frame Class
	class FrameStatus {
		String nullServiceData;
		String tooShortServiceData;
		String invalidFrameType;
		
		public String getErrors() {
			StringBuilder sb = new StringBuilder();
			
			if (nullServiceData != null) {
				sb.append(BULLET).append(nullServiceData).append("\n");
			}
			
			if (tooShortServiceData != null) {
				sb.append(BULLET).append(tooShortServiceData).append("\n");
			}

			return sb.toString().trim();
		}
		
		@Override
		public String toString() {
			return getErrors();
		}
	}
	
	//UID Class
	class UidStatus {
		String uidValue;
		int txPower;
		
		String errTx;
		String errUid;
		String errRfu;
		
		public String getErrors() {
			StringBuilder sb = new StringBuilder();
			
			if (errTx != null) {
				sb.append(BULLET).append(errTx).append("\n");
			}
			
			if (errUid != null) {
				sb.append(BULLET).append(errUid).append("\n");
			}
			
			if (errRfu != null) {
				sb.append(BULLET).append(errRfu).append("\n");
			}
			
			return sb.toString().trim();
		}
	}
	
	/**
	 * Performs a case-insensitive "contains" test of "s" on the device address (with or without the
	 * colon separators) and/or the UID value, and/or the URL value.
	 */
	boolean contains(String s) {
		return s == null
				|| s.isEmpty()
				|| deviceAddress.replace(":", "").toLowerCase(Locale.US).contains(s.toLowerCase(Locale.US))
				|| (uidStatus.uidValue != null && uidStatus.uidValue.toLowerCase(Locale.US).contains(s.toLowerCase(Locale.US)));
	}
}
