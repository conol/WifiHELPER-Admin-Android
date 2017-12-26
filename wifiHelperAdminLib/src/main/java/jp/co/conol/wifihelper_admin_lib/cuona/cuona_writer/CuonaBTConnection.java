package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class CuonaBTConnection {

    private static final int CUONA_MID = 0x6f63; // MAGIC 'co', LSB first
    private static final int CUONA_MID_VERSION = 1; // MID version

    private static final UUID CUONA_SERVICE_UUID = uuid16(0xff00);

    static final UUID CUONA_CHAR_UUID_SYSTEM_STATUS = uuid16(0xff01);
    static final UUID CUONA_CHAR_UUID_WIFI_SSID_PW = uuid16(0xff02); // protected
    static final UUID CUONA_CHAR_UUID_SERVER_HOST = uuid16(0xff03); // protected
    static final UUID CUONA_CHAR_UUID_SERVER_PATH = uuid16(0xff04); // protected
    static final UUID CUONA_CHAR_UUID_NET_REQUEST = uuid16(0xff05);
    static final UUID CUONA_CHAR_UUID_NET_RESPONSE = uuid16(0xff06);
    static final UUID CUONA_CHAR_UUID_OTA_CTRL = uuid16(0xff07); // protected
    static final UUID CUONA_CHAR_UUID_PLAIN_JSON = uuid16(0xff08); // protected, legacy
    static final UUID CUONA_CHAR_UUID_NFC_DATA = uuid16(0xff09); // protected, secure
    static final UUID CUONA_CHAR_UUID_PWPROTECT = uuid16(0xff0a); // for protection

    private Context context;
    private byte[] deviceId;
    private CuonaBTCallback callback;

    private final CuonaScanCallback scanCallback = new CuonaScanCallback();
    private final CuonaGattCallback gattCallback = new CuonaGattCallback();

    private final BluetoothAdapter btAdapter;
    private final BluetoothLeScanner bleScanner;
    private BluetoothDevice cuonaDevice = null;
    private BluetoothGatt cuonaGatt = null;
    private List<BluetoothGattService> serviceList = null;
    private final Map<UUID,BluetoothGattCharacteristic> charMap = new HashMap<>();

    class CuonaScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            switch (callbackType) {
                case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                    Log.d("nfc", "onScanResult: CALLBACK_TYPE_ALL_MATCHES");
                    break;
                case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                    Log.d("nfc", "onScanResult: CALLBACK_TYPE_FIRST_MATCH");
                    break;
                case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                    Log.d("nfc", "onScanResult: CALLBACK_TYPE_MATCH_LOST");
                    break;
                default:
                    Log.d("nfc", "onScanResult: " + callbackType);
                    break;
            }
            if (result == null || result.getDevice() == null) {
                return;
            }
            ScanRecord scanRecord = result.getScanRecord();
            Log.d("nfc", "onScanResult: scanRecord=" + scanRecord);
            byte[] mdata = scanRecord.getManufacturerSpecificData(CUONA_MID);
            if (mdata == null) {
                return;
            }
            if (mdata.length != deviceId.length + 1 || mdata[0] != CUONA_MID_VERSION) {
                return;
            }
            for (int i = 0; i < deviceId.length; i++) {
                if (mdata[i + 1] != deviceId[i]) {
                    return;
                }
            }
            Log.d("nfc", "Found CUONA!");

            bleScanner.stopScan(scanCallback);

            if (cuonaDevice == null) {
                cuonaFound(result.getDevice());
            }
        }
    }

    class CuonaGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("nfc", "GATT: onConnectionStateChange: newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.requestMtu(512);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                callback.onCuonaBTDisconnected();
                cuonaGatt.close();
                cuonaGatt = null;
                cuonaDevice = null;
            }
        }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d("nfc", "onMtuChanged mtu=" + mtu);
            gatt.discoverServices();
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            serviceList = gatt.getServices();
            for (BluetoothGattService service: serviceList) {
                UUID uuid = service.getUuid();
                Log.d("nfc", "Service: uuid=" + uuid);
                if (uuid.equals(CUONA_SERVICE_UUID)) {
                    List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                    charMap.clear();
                    for (BluetoothGattCharacteristic ch: chars) {
                        Log.d("nfc", "  Characteristic: uuid=" + ch.getUuid());
                        charMap.put(ch.getUuid(), ch);
                    }
                    callback.onCuonaBTConnected();
                }
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d("nfc", "onCharacteristicRead: uuid=" + characteristic.getUuid() +
                    ", status=" + status);
            byte[] data = characteristic.getValue();
            callback.onCuonaBTRead(characteristic.getUuid(),
                    status == BluetoothGatt.GATT_SUCCESS,
                    data);
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            Log.d("nfc", "onCharacteristicWrite: uuid=" + characteristic.getUuid() +
                    ", status=" + status);
            callback.onCuonaBTWrite(characteristic.getUuid(),
                    status == BluetoothGatt.GATT_SUCCESS);
        }
    }

    public static final UUID uuid16(int n) {
        return new UUID((((long) n) << 32) | 0x1000, 0x800000805f9b34fbL);
    }

    private static CuonaBTConnection theInstance;

    private CuonaBTConnection(Context context) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
        Log.d("nfc", "btAdapter=" + btAdapter);
        btAdapter.enable();
        bleScanner = btAdapter.getBluetoothLeScanner();
        Log.d("nfc", "bleScanner=" + bleScanner);
    }

    static CuonaBTConnection getInstance(Context context, byte[] deviceId,
                                         CuonaBTCallback callback) {
        if (theInstance == null) {
            theInstance = new CuonaBTConnection(context);
        }
        theInstance.setup(context, deviceId, callback);
        return theInstance;
    }

    private void setup(Context context, byte[] deviceId, CuonaBTCallback callback) {
        this.context = context;
        this.deviceId = deviceId;
        this.callback = callback;

        if (cuonaGatt != null) {
            cuonaGatt.close();
        }
        cuonaGatt = null;
        cuonaDevice = null;
    }

    void startScanning() {
        bleScanner.startScan(scanCallback);
    }

    void disconnectRequest() {
        cuonaGatt.disconnect();
    }

    boolean readRequest(UUID uuid) {
        if (cuonaGatt == null) {
            return false;
        }

        BluetoothGattCharacteristic characteristic = charMap.get(uuid);
        if (characteristic == null) {
            return false;
        }

        return cuonaGatt.readCharacteristic(characteristic);
    }

    boolean writeRequest(UUID uuid, byte[] data) {
        if (cuonaGatt == null) {
            return false;
        }

        BluetoothGattCharacteristic characteristic = charMap.get(uuid);
        if (characteristic == null) {
            return false;
        }

        characteristic.setValue(data);
        return cuonaGatt.writeCharacteristic(characteristic);
    }

    private void cuonaFound(BluetoothDevice device) {
        cuonaDevice = device;

        cuonaGatt = cuonaDevice.connectGatt(context, false, gattCallback);
        cuonaGatt.connect();
    }


}

