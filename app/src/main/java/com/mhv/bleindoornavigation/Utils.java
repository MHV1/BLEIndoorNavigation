
package com.mhv.bleindoornavigation;

import java.util.Locale;

/**
 * Class found @ https://github.com/google/eddystone/blob/master/
 * tools/eddystone-validator/EddystoneValidator/app/src/main/
 * java/com/google/sample/eddystonevalidator/Utils.java
 */

public class Utils {
	
	private static final char[] HEX = "0123456789ABCDEF".toCharArray();
	
	static String toHexString(byte[] bytes) {
		if (bytes.length == 0) {
			return "";
		}
		
		char[] chars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int c = bytes[i] & 0xFF;
			chars[i * 2] = HEX[c >>> 4];
			chars[i * 2 + 1] = HEX[c & 0x0F];
		}
		return new String(chars).toLowerCase(Locale.US);
	}
	
	static boolean isZeroed(byte[] bytes) {
		for (byte b : bytes) {
			if (b != 0x00) {
				return false;
			}
		}
		return true;
	}
}
