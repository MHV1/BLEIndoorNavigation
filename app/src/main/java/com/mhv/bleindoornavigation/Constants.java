package com.mhv.bleindoornavigation;

/**
 * Class found @ https://github.com/google/eddystone/blob/master/
 * tools/eddystone-validator/EddystoneValidator/app/src/main/
 * java/com/google/sample/eddystonevalidator/Constants.java
 */

public class Constants {
	
	private Constants() {}
	
	/**
	 * Eddystone-UID frame type value.
	 */
	static final byte UID_FRAME_TYPE = 0x00;
	
	/**
	 * Eddystone-URL frame type value.
	 */
	static final byte URL_FRAME_TYPE = 0x10;
	
	/**
	 * Eddystone-TLM frame type value.
	 */
	static final byte TLM_FRAME_TYPE = 0x20;

	/**
	 * Minimum expected Tx power (in dBm) in UID and URL frames.
	 */
	static final int MIN_EXPECTED_TX_POWER = -100;
	
	/**
	 * Maximum expected Tx power (in dBm) in UID and URL frames.
	 */
	static final int MAX_EXPECTED_TX_POWER = 20;
	
}
