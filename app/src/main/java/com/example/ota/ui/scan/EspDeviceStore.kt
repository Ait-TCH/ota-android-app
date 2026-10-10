package com.example.ota.ui.scan

import android.content.Context

/** 跨页存储 BLE 扫描结果（连接时用） */
object EspDeviceStore {
    private val bleDevices = HashMap<String, android.bluetooth.BluetoothDevice>()
    private val serviceUuids = HashMap<String, String>()

    fun setBleDevice(ctx: Context, name: String, device: android.bluetooth.BluetoothDevice) {
        bleDevices[name] = device
    }
    fun getBleDevice(name: String): android.bluetooth.BluetoothDevice? = bleDevices[name]
    fun setServiceUuid(ctx: Context, name: String, uuid: String) { serviceUuids[name] = uuid }
    fun getServiceUuid(name: String): String? = serviceUuids[name]
}
