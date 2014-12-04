/**   
 * @Title: DeviceAdsAcitivity.java 
 * @Package com.coney.demo.ble 
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月14日 下午4:41:20 
 * @version V1.0   
 */
package com.coney.demo.ble.peripheral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.coney.demo.ble.R;
import com.coney.demo.ble.matadata.GATT;
import com.coney.demo.util.BleUtil;
import com.coney.demo.util.DataUtil;
import com.coney.util.MyLogger;

/**
 * @ClassName: DeviceAdsAcitivity
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月14日 下午4:41:20
 * 
 */
public class DeviceAdsAcitivity extends Activity {
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGattServerCallback mGattCallback;
	private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
	private BluetoothGattServer mGattServer;
	private BLEPeripheralConnectionGovernor mConnectionGovernor;

	/** Map of Responses to perform keyed by request characteristic & type pair */
	private HashMap<Pair<UUID, BLEPeripheralResponse.RequestType>, BLEPeripheralResponse> mResponses = new HashMap<>();
	/** Set of connected device addresses */
	private HashSet<String> mConnectedDevices = new HashSet<>();

	private HashMap<Pair<UUID, BLEPeripheralResponse.RequestType>, byte[]> mCachedResponsePayloads = new HashMap<>();

	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	// Device scan callback.
	private AdvertiseCallback mLeScanCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			if (settingsInEffect != null) {
				MyLogger.i("Advertise success TxPowerLv="
						+ settingsInEffect.getTxPowerLevel() + " mode="
						+ settingsInEffect.getMode());
			} else {
				MyLogger.i("Advertise success");
			}
		}

		@Override
		public void onStartFailure(int errorCode) {
			MyLogger.i("Advertising failed with code " + errorCode);
		}
	};

	public interface BLEPeripheralConnectionGovernor {
		public boolean shouldConnectToCentral(BluetoothDevice potentialPeer);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_devices);
		mHandler = new Handler();

		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
			MyLogger.i("11");

			MyLogger.i(mBluetoothLeAdvertiser + "");
			mBluetoothLeAdvertiser = mBluetoothAdapter
					.getBluetoothLeAdvertiser();
		}
		MyLogger.i("22");

		MyLogger.i(mBluetoothLeAdvertiser + ""
				+ mBluetoothAdapter.isMultipleAdvertisementSupported());
		MyLogger.i(mBluetoothAdapter.getBluetoothLeAdvertiser() + "   "
				+ mBluetoothAdapter.isEnabled());
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(
					R.layout.actionbar_indeterminate_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			advertsingDevice(true);
			break;
		case R.id.menu_stop:
			advertsingDevice(false);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		// if (!mBluetoothAdapter.isEnabled()) {
		// if (!mBluetoothAdapter.isEnabled()) {
		// Intent enableBtIntent = new
		// Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		// startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		// }
		// }

		// Initializes list view adapter.
		// setListAdapter(mLeDeviceListAdapter);
		advertsingDevice(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		advertsingDevice(false);
	}


	public void addDefaultBLEPeripheralResponse(BLEPeripheralResponse response) {
		Pair<UUID, BLEPeripheralResponse.RequestType> requestFilter = new Pair<>(
				response.mCharacteristic.getUuid(), response.mRequestType);
		mResponses.put(requestFilter, response);
		MyLogger.i(String.format("Registered %s response for %s",
				requestFilter.second, requestFilter.first));
	}

	private void advertsingDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothLeAdvertiser.stopAdvertising(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);
			startGattServer();
			mScanning = true;
			mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(),
					createAdvData(), mLeScanCallback);
		} else {
			mScanning = false;
			startGattServer();
			mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(),
					createAdvData(), mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	private void startGattServer() {
		BluetoothManager manager = BleUtil.getManager(this);
		if (mGattCallback == null)
			mGattCallback = new BluetoothGattServerCallback() {
				@Override
				public void onConnectionStateChange(BluetoothDevice device,
						int status, int newState) {
					if (newState == BluetoothProfile.STATE_CONNECTED) {
						if (mConnectedDevices.contains(device.getAddress())) {
							// We're already connected (should never happen).
							// Cancel connection
							MyLogger.i("Denied connection. Already connected to "
									+ device.getAddress());
							mGattServer.cancelConnection(device);
							return;
						}

						if (mConnectionGovernor != null
								&& !mConnectionGovernor
										.shouldConnectToCentral(device)) {
							// The ConnectionGovernor denied the connection.
							// Cancel connection
							MyLogger.i("Denied connection. ConnectionGovernor denied "
									+ device.getAddress());
							mGattServer.cancelConnection(device);
							return;
						} else {
							// Allow connection to proceed. Mark device
							// connected
							MyLogger.i("Accepted connection to "
									+ device.getAddress());
							mConnectedDevices.add(device.getAddress());
						}
					} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
						// We've disconnected
						MyLogger.i("Disconnected from " + device.getAddress());
						mConnectedDevices.remove(device.getAddress());
					}
					super.onConnectionStateChange(device, status, newState);
				}

				@Override
				public void onServiceAdded(int status,
						BluetoothGattService service) {
					Log.i("onServiceAdded", service.toString());
					super.onServiceAdded(status, service);
				}

				@Override
				public void onCharacteristicReadRequest(
						BluetoothDevice remoteCentral, int requestId,
						int offset, BluetoothGattCharacteristic characteristic) {
					MyLogger.i(String
							.format("onCharacteristicReadRequest for request %d on characteristic %s with offset %d",
									requestId, characteristic.getUuid()
											.toString().substring(0, 3), offset));

					BluetoothGattCharacteristic localCharacteristic = mGattServer
							.getService(GATT.SERVICE_UUID).getCharacteristic(
									characteristic.getUuid());
					if (localCharacteristic != null) {
						Pair<UUID, BLEPeripheralResponse.RequestType> requestKey = new Pair<>(
								characteristic.getUuid(),
								BLEPeripheralResponse.RequestType.READ);
						if (offset > 0) {
							// This is a framework-generated follow-up request
							// for another section of data
							byte[] cachedResponse = mCachedResponsePayloads
									.get(requestKey);
							byte[] toSend = new byte[cachedResponse.length
									- offset];
							System.arraycopy(cachedResponse, offset, toSend, 0,
									toSend.length);
							MyLogger.i(String
									.format("Sending extended response chunk for offset %d : %s",
											offset, DataUtil.bytesToHex(toSend)));
							mGattServer.sendResponse(remoteCentral, requestId,
									BluetoothGatt.GATT_SUCCESS, offset, toSend);
						} else if (mResponses.containsKey(requestKey)) {
							// This is a fresh request with a registered
							// response
							byte[] cachedResponse = mResponses.get(requestKey)
									.respondToRequest(mGattServer,
											remoteCentral, requestId,
											characteristic, false, true, null);
							if (cachedResponse != null)
								mCachedResponsePayloads.put(requestKey,
										cachedResponse);
						} else {
							MyLogger.i(String
									.format("No %s response registered for characteristic %s",
											requestKey.second, characteristic
													.getUuid().toString()));
							// No response registered for this request. Send
							// GATT_FAILURE
							mGattServer.sendResponse(remoteCentral, requestId,
									BluetoothGatt.GATT_FAILURE, 0,
									new byte[] { 0x00 }); // Got NPE if sending
															// null value
						}
					} else {
						MyLogger.i("CharacteristicReadRequest. Unrecognized characteristic "
								+ characteristic.getUuid().toString());
						// Request for unrecognized characteristic. Send
						// GATT_FAILURE
						mGattServer.sendResponse(remoteCentral, requestId,
								BluetoothGatt.GATT_FAILURE, 0,
								new byte[] { 0x00 });
					}
					super.onCharacteristicReadRequest(remoteCentral, requestId,
							offset, characteristic);
				}

				@Override
				public void onCharacteristicWriteRequest(
						BluetoothDevice remoteCentral, int requestId,
						BluetoothGattCharacteristic characteristic,
						boolean preparedWrite, boolean responseNeeded,
						int offset, byte[] value) {
					MyLogger.i(String
							.format("onCharacteristicWriteRequest for request %d on characteristic %s with offset %d",
									requestId, characteristic.getUuid()
											.toString().substring(0, 3), offset));

					BluetoothGattCharacteristic localCharacteristic = mGattServer
							.getService(GATT.SERVICE_UUID).getCharacteristic(
									characteristic.getUuid());
					if (localCharacteristic != null) {
						Pair<UUID, BLEPeripheralResponse.RequestType> requestKey = new Pair<>(
								characteristic.getUuid(),
								BLEPeripheralResponse.RequestType.WRITE);
						if (offset == 0) {
							// This is a fresh write request so start recording
							// data. This will work in a bit of an opposite way
							// from the readrequest batching
							mCachedResponsePayloads.put(requestKey, value); // Cache
																			// the
																			// payload
																			// data
																			// in
																			// case
																			// more
																			// is
																			// coming
							MyLogger.i(String
									.format("onCharacteristicWriteRequest had %d bytes, offset : %d",
											value == null ? 0 : value.length,
											offset));
							if (characteristic.getUuid().equals(
									GATT.IDENTITY_WRITE_UUID)) {
								// We know this is a one-packet response
								if (mResponses.containsKey(requestKey)) {
									// This is a fresh request
									mResponses.get(requestKey)
											.respondToRequest(mGattServer,
													remoteCentral, requestId,
													characteristic,
													preparedWrite,
													responseNeeded, value);
									MyLogger.i("Sent identity write to response handler");
								}
							}
							if (responseNeeded) {
								// Signal we received the write
								mGattServer.sendResponse(remoteCentral,
										requestId, BluetoothGatt.GATT_SUCCESS,
										offset, value);
								MyLogger.i("Notifying central we received data");
							}
						} else {
							// this is a subsequent request with more data
							byte[] cachedResponse = mCachedResponsePayloads
									.get(requestKey);
							int cachedResponseLength = (cachedResponse == null ? 0
									: cachedResponse.length);
							// if (cachedResponse.length != offset)
							// MyLogger.i(String.format("Got more data. Original payload len %d. offset %d (should be equal)",
							// cachedResponse.length, offset));
							byte[] updatedData = new byte[cachedResponseLength
									+ value.length];
							if (cachedResponseLength > 0)
								System.arraycopy(cachedResponse, 0,
										updatedData, 0, cachedResponseLength);

							System.arraycopy(value, 0, updatedData,
									cachedResponseLength, value.length);
							MyLogger.i(String.format(
									"Got %d bytes for write request",
									updatedData.length));
							if (characteristic.getUuid().equals(
									GATT.MESSAGES_WRITE_UUID)
									// && updatedData.length ==
									// BLEProtocol.MESSAGE_RESPONSE_LENGTH) {
									&& updatedData.length == 309) {
								// We've reconstructed a complete message
								mResponses.get(requestKey).respondToRequest(
										mGattServer, remoteCentral, requestId,
										characteristic, preparedWrite,
										responseNeeded, updatedData);
								MyLogger.i("Sent message write to response handler");
								mCachedResponsePayloads.remove(requestKey); // Clear
																			// cached
																			// data
							} else if (characteristic.getUuid().equals(
									GATT.MESSAGES_WRITE_UUID)
									&& responseNeeded) {
								// Signal we received the write and are ready
								// for more data
								mGattServer.sendResponse(remoteCentral,
										requestId, BluetoothGatt.GATT_SUCCESS,
										offset, value);
								MyLogger.i("Notifying central we received data");
								mCachedResponsePayloads.put(requestKey,
										updatedData);
							}
						}
						// else if (mResponses.containsKey(requestKey)) {
						// // This is a fresh request
						// mResponses.get(requestKey).respondToRequest(mGattServer,
						// remoteCentral, requestId, characteristic,
						// preparedWrite, responseNeeded, value);
						// }
						// else {
						// // No response registered for this request. Send
						// GATT_FAILURE
						// MyLogger.i(String.format("No %s response registered for characteristic %s",
						// requestKey.second,
						// characteristic.getUuid().toString()));
						// mGattServer.sendResponse(remoteCentral, requestId,
						// BluetoothGatt.GATT_FAILURE, 0, new byte[] { 0x00 });
						// }
					} else {
						MyLogger.i("CharacteristicWriteRequest. Unrecognized characteristic "
								+ characteristic.getUuid().toString());
						// Request for unrecognized characteristic. Send
						// GATT_FAILURE
						mGattServer.sendResponse(remoteCentral, requestId,
								BluetoothGatt.GATT_FAILURE, 0,
								new byte[] { 0x00 });
					}
					super.onCharacteristicWriteRequest(remoteCentral,
							requestId, characteristic, preparedWrite,
							responseNeeded, offset, value);
				}

				@Override
				public void onDescriptorReadRequest(BluetoothDevice device,
						int requestId, int offset,
						BluetoothGattDescriptor descriptor) {
					Log.i("onDescriptorReadRequest", descriptor.toString());
					super.onDescriptorReadRequest(device, requestId, offset,
							descriptor);
				}

				@Override
				public void onDescriptorWriteRequest(BluetoothDevice device,
						int requestId, BluetoothGattDescriptor descriptor,
						boolean preparedWrite, boolean responseNeeded,
						int offset, byte[] value) {
					Log.i("onDescriptorWriteRequest", descriptor.toString());
					super.onDescriptorWriteRequest(device, requestId,
							descriptor, preparedWrite, responseNeeded, offset,
							value);
				}

				@Override
				public void onExecuteWrite(BluetoothDevice device,
						int requestId, boolean execute) {
					MyLogger.i("onExecuteWrite" + device.toString());
					super.onExecuteWrite(device, requestId, execute);
				}
			};

		mGattServer = manager.openGattServer(this, mGattCallback);
		setupGattServer();
	}

	private void setupGattServer() {
		if (mGattServer != null) {
			BluetoothGattService chatService = new BluetoothGattService(
					GATT.SERVICE_UUID,
					BluetoothGattService.SERVICE_TYPE_PRIMARY);

			chatService.addCharacteristic(GATT.IDENTITY_READ);
			chatService.addCharacteristic(GATT.IDENTITY_WRITE);

			chatService.addCharacteristic(GATT.MESSAGES_READ);
			chatService.addCharacteristic(GATT.MESSAGES_WRITE);
			mGattServer.addService(chatService);
		}
	}

	private static AdvertiseSettings createAdvSettings() {
		AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
		builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		// builder.setType(AdvertiseSettings.ADVERTISE_TYPE_CONNECTABE);
		builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
		return builder.build();
	}

	private static AdvertiseData createAdvData() {
		// iPad:
		// 4c 00 01 00 00000000 00000010 00000000 000000
		final byte[] manufacturerData = new byte[] {
				// 4c 00 01 00
				(byte) 0x4c, (byte) 0x00, (byte) 0x01, (byte) 0x00, // fix
				// 00 00 00 00
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // uuid
				// 00 00 00 10
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, // uuid
				// 00 00 00 00
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // uuid
				// 00 00 00
				(byte) 0x00, (byte) 0x00, (byte) 0x00 };
		AdvertiseData.Builder builder = new AdvertiseData.Builder();
		// Service UUIDS
		// Generic Access
		// Generic Attribute
		// Battery Service
		// Current Time Service
		// D0611E78-BBB4-4591-A5F8-487910AE4366
		// 8667556C-9A37-4C91-84ED-54EE27D90049
		// 7905F431-B5CE-4E99-A40F-4B1E122D00D0
		// 69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9
		// 9FBF120D-6301-42D9-8C58-25E699A21DBD
		// 22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB
		// 217D750E-7B58-4152-A1EB-F2711BB38350
		// DD4ED52E-58D3-448A-8B9B-44DF52784B5B
		// 89D3502B-0F36-433A-8EF4-C502AD55F8DC
		// 9B3C81D8-57B1-4A8A-B8DF-0E56F7CA51C2
		// 2F7CABCE-808D-411F-9A0C-BB92BA96C102
		// C6B2F38C-23AB-46D8-A6AB-A3A870BBD5D7

		// uuidList.add(ParcelUuid.fromString("D0611E78-BBB4-4591-A5F8-487910AE4366"));
		// uuidList.add(ParcelUuid.fromString("7905F431-B5CE-4E99-A40F-4B1E122D00D0"));
		// uuidList.add(ParcelUuid.fromString("217D750E-7B58-4152-A1EB-F2711BB38350"));
		// uuidList.add(ParcelUuid.fromString("89D3502B-0F36-433A-8EF4-C502AD55F8DC"));
		// List<ParcelUuid> uuidList = new ArrayList<ParcelUuid>();
		// uuidList.add(new ParcelUuid(GATT.SERVICE_UUID));
		// builder.setServiceUuid(seruuidListviceUuid);
		builder.addServiceUuid(new ParcelUuid(GATT.SERVICE_UUID));
		builder.setIncludeTxPowerLevel(false);
		builder.addManufacturerData(0x1234578, manufacturerData);
		return builder.build();
	}

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
}