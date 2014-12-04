/**   
 * @Title: BleUtil.java 
 * @Package com.coney.demo.util 
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月15日 下午3:28:22 
 * @version V1.0   
 */
package com.coney.demo.util;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * @ClassName: BleUtil
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月15日 下午3:28:22
 * 
 */
public class BleUtil {
	public static final String TAG = "BLEUtil";

	private BleUtil() {
		// Util
	}

	/** check if BLE Supported device */
	public static boolean isBLESupported(Context context) {
		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE);
	}

	/** get BluetoothManager */
	public static BluetoothManager getManager(Context context) {
		BluetoothManager manager = (BluetoothManager) context
				.getSystemService(Context.BLUETOOTH_SERVICE);
		return manager;
	}

	/**
	 * Return whether Bluetooth is currently enabled. If request is true, prompt
	 * for permission to enable. If so host should be configured to evaluate the
	 * result via a call to
	 * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
	 */
	public static boolean isBluetoothEnabled(Context context) {
		boolean enabled = getManager(context).getAdapter().isEnabled();
		return enabled;
	}

}
