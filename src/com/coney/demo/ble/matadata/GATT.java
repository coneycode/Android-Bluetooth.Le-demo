/**   
* @Title: GATT.java 
* @Package com.coney.demo.ble.matadata 
* @Description: TODO
* @author coney Geng
* @date 2014年11月15日 下午3:32:58 
* @version V1.0   
*/
package com.coney.demo.ble.matadata;

/** 
 * @ClassName: GATT 
 * @Description: TODO
 * @author coney Geng
 * @date 2014年11月15日 下午3:32:58 
 *  
 */
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

/** MeshChat UUIDs */
public class GATT {

    /** Service */
    public static final UUID SERVICE_UUID = UUID.fromString("96F22BCA-F08C-43F9-BF7D-EEBC579C94D2");

    /** Characteristic */
    public static final UUID IDENTITY_READ_UUID = UUID.fromString("21C7DE8E-B0D0-4A41-9B22-78221277E2AA");
    public static final UUID IDENTITY_WRITE_UUID = UUID.fromString("00E12465-2E2F-4C6B-9FD2-E84A8A088C68");
    public static final UUID MESSAGES_READ_UUID = UUID.fromString("A109B433-96A0-463A-A070-542C5A15E177");
    public static final UUID MESSAGES_WRITE_UUID = UUID.fromString("6EAEC220-5EB0-4181-8858-D40E1EE072F6");

    /** Descriptors */
    private static final UUID MYSTERY_DESCRIPTOR_UUID = UUID.fromString("00002900-0000-1000-8000-00805f9b34fb");

    /** Properties */
    // Received from iOS app
    private static final int CHARACTERISTIC_READABLE_PROPERTY = 34;
    private static final int CHARACTERISTIC_WRITABLE_PROPERTY = 136;

    /** Permissions */
    // Received from iOS app
    private static final int PERMISSION = 0;


    public static final BluetoothGattCharacteristic IDENTITY_READ =
            new BluetoothGattCharacteristic(IDENTITY_READ_UUID,
//                                            CHARACTERISTIC_READABLE_PROPERTY,
                                            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE,
//                                            PERMISSION);
                                            BluetoothGattCharacteristic.PERMISSION_READ);

    public static final BluetoothGattCharacteristic MESSAGES_READ =
            new BluetoothGattCharacteristic(MESSAGES_READ_UUID,
//                                            CHARACTERISTIC_READABLE_PROPERTY,
                                            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE,
//                                            PERMISSION);
                                            BluetoothGattCharacteristic.PERMISSION_READ);

    public static final BluetoothGattCharacteristic IDENTITY_WRITE =
            new BluetoothGattCharacteristic(IDENTITY_WRITE_UUID,
//                                            CHARACTERISTIC_WRITABLE_PROPERTY,
                                            BluetoothGattCharacteristic.PROPERTY_WRITE,
//                                            PERMISSION);
                                            BluetoothGattCharacteristic.PERMISSION_WRITE);

    public static final BluetoothGattCharacteristic MESSAGES_WRITE =
            new BluetoothGattCharacteristic(MESSAGES_WRITE_UUID,
//                                            CHARACTERISTIC_WRITABLE_PROPERTY,
                                            BluetoothGattCharacteristic.PROPERTY_WRITE,
//                                            PERMISSION);
                                            BluetoothGattCharacteristic.PERMISSION_WRITE);

    private static final BluetoothGattDescriptor READ_DESCRIPTOR = new BluetoothGattDescriptor(MYSTERY_DESCRIPTOR_UUID,
                                                                                               0);
                                                                                               //BluetoothGattDescriptor.PERMISSION_READ);
    private static final BluetoothGattDescriptor WRITE_DESCRIPTOR = new BluetoothGattDescriptor(MYSTERY_DESCRIPTOR_UUID,
                                                                                                0);
                                                                                                //BluetoothGattDescriptor.PERMISSION_WRITE);


    static {
        IDENTITY_READ.addDescriptor(READ_DESCRIPTOR);
        IDENTITY_WRITE.addDescriptor(WRITE_DESCRIPTOR);
        MESSAGES_READ.addDescriptor(READ_DESCRIPTOR);
        MESSAGES_WRITE.addDescriptor(WRITE_DESCRIPTOR);
    }

    public static String getNameForCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(IDENTITY_READ_UUID)) {
            return "identity read";
        }
        else if (characteristic.getUuid().equals(IDENTITY_WRITE_UUID)) {
            return "identity write";
        }
        else if (characteristic.getUuid().equals(MESSAGES_READ_UUID)) {
            return "messages read";
        }
        else if (characteristic.getUuid().equals(MESSAGES_WRITE_UUID)) {
            return "messages write";
        }
        return "?";
    }
}
