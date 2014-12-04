/**   
 * @Title: BLEPeripheralResponse.java 
 * @Package com.coney.demo.ble.peripheral 
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月15日 下午3:13:19 
 * @version V1.0   
 */
package com.coney.demo.ble.peripheral;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

/**
 * @ClassName: BLEPeripheralResponse
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月15日 下午3:13:19
 * 
 */
public abstract class BLEPeripheralResponse {
	public static enum RequestType {
		READ, WRITE
	}

	public BluetoothGattCharacteristic mCharacteristic;
	public RequestType mRequestType;

	public BLEPeripheralResponse(BluetoothGattCharacteristic characteristic,
			RequestType requestType) {
		mCharacteristic = characteristic;
		mRequestType = requestType;
	}

	/**
	 * @return any data sent in this response for caching purposes. Large
	 *         requests/responses will be packetized over several requests, but
	 *         BLEPeripheral will deliver the final recombined result.
	 */
	public abstract byte[] respondToRequest(
			BluetoothGattServer localPeripheral, BluetoothDevice remoteCentral,
			int requestId, BluetoothGattCharacteristic characteristic,
			boolean preparedWrite, boolean responseNeeded, byte[] value);
}
