package com.bronze.ble

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bronze.ble.adapter.DeviceManagerAdapter
import com.bronze.ble.impl.BleListener
import com.bronze.kutil.view.LProgressDialog
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.scan.ScanFilter
import com.polidea.rxandroidble.scan.ScanSettings
import com.tbruyelle.rxpermissions2.RxPermissions
import org.jetbrains.anko.bluetoothManager
import rx.Subscription


/**
 * properties:
 * maxConnection
 * listWithAlias
 */
class DeviceManagerFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, BleListener {

    //properties
    var maxConnection = 1
    var listWithAlias = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            connectedAdapter.listWithAlias = field
            nearAdapter.listWithAlias = field
        }

    private val rxPermission by lazy { RxPermissions(this.activity!!) }
    private val rxBleClient by lazy { RxBleClient.create(context!!) }
    private val srl by lazy { view?.findViewById<SwipeRefreshLayout>(R.id.srlBleScanning) }
    private val rvConnected by lazy { view?.findViewById<RecyclerView>(R.id.rvConnectedBle) }
    private val rvNear by lazy { view?.findViewById<RecyclerView>(R.id.rvNearBle) }
    private var scanning: Subscription? = null
    private var bleServiceStartedCallbacks = HashSet<() -> Unit>()

    private val connectingDialog by lazy {
        val temp = LProgressDialog(context!!)
        temp.setMessage(R.string.ble_connecting)
        temp
    }
    private val connectedAdapter = DeviceManagerAdapter {
        BleService.instance?.disconnect(it)
        BleService.instance?.lastConnectAddresses?.removeAll { m -> m == it.macAddress }
        BleService.instance?.saveLastConnectAddresses()
        refreshConnectedDevices()
    }
    private val nearAdapter = DeviceManagerAdapter {
        if (BleService.instance?.connectedDevices?.size ?: 0 >= maxConnection) {
            return@DeviceManagerAdapter
        }
        connectingDialog.show()
        BleService.instance?.connect(it)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        BleService.start(context!!) {
            bleServiceStartedCallbacks.forEach { it.invoke() }
            bleServiceStartedCallbacks.clear()
        }
        return inflater.inflate(R.layout.fragment_device_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typedValue = TypedValue()
        activity?.theme?.resolveAttribute(android.R.attr.colorAccent, typedValue, true)

        srl?.setColorSchemeColors(typedValue.data)
        srl?.setOnRefreshListener(this)
        rvConnected?.adapter = connectedAdapter
        rvNear?.adapter = nearAdapter
        waitForBleServiceStart {
            srl?.isRefreshing = true
            BleService.start(context!!) {
                BleService.instance?.addListener(this)
                onRefresh()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BleService.instance?.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        BleService.instance?.removeListener(this)
    }

    override fun onDeviceConnected(device: RxBleDevice) {
        connectingDialog.dismissByUser()
        refreshConnectedDevices()
        refreshNearDevices()
    }

    override fun onDeviceDisconnected(device: RxBleDevice) {
        connectingDialog.dismissByUser()
        refreshConnectedDevices()
    }

    private fun refreshConnectedDevices() {
        connectedAdapter.dataList.clear()
        connectedAdapter.dataList.addAll(BleService.instance?.connectedDevices ?: emptyList())
        connectedAdapter.notifyDataSetChanged()
    }

    private fun refreshNearDevices() {
        nearAdapter.dataList.clear()
        nearAdapter.dataList.addAll(BleService.instance?.nearDevices ?: emptyList())
        nearAdapter.notifyDataSetChanged()
    }

    override fun onRefresh() {
        if (activity?.bluetoothManager?.adapter == null) {
            return
        }
        if (!activity!!.bluetoothManager.adapter.isEnabled) {
            srl?.isRefreshing = false
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 123)
            return
        }
        rxPermission.request(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe {
                    if (it) {
                        BleService.instance?.nearDevices?.clear()
                        nearAdapter.dataList.clear()
                        nearAdapter.notifyDataSetChanged()
                        refreshConnectedDevices()
                        srl?.isRefreshing = true
                        val scanFilterBuilder = ScanFilter.Builder()
                        BleService.instance?.serviceUUID?.let {
                            scanFilterBuilder.setServiceUuid(ParcelUuid(it))
                        }
                        scanning = rxBleClient.scanBleDevices(
                                ScanSettings.Builder()
                                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                        .build(),
                                scanFilterBuilder.build())
                                .subscribe(scanning@{ result ->
                                    if (BleService.instance?.nearDevices?.any {
                                                it.macAddress == result.bleDevice.macAddress
                                            } == true) {
                                        return@scanning
                                    }
                                    if (BleService.instance?.connectedDevices?.any {
                                                it.macAddress == result.bleDevice.macAddress
                                            } == true) {
                                        return@scanning
                                    }
                                    BleService.instance?.nearDevices?.add(result.bleDevice)
                                    onDeviceFound()
                                }, {
                                    if (it is BleScanException &&
                                            it.reason == BleScanException.LOCATION_SERVICES_DISABLED) {
                                        AlertDialog.Builder(context)
                                                .setTitle(R.string.ble_title_location_service)
                                                .setMessage(R.string.ble_content_location_service)
                                                .setPositiveButton(R.string.ble_go_to_settings) { _, _ ->
                                                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                                }
                                                .setNegativeButton(R.string.ble_got_it) { _, _ -> }
                                                .show()
                                    }
                                    srl?.isRefreshing = false
                                    Log.e("DeviceManagerFragment", "scanningErr!!!", it)
                                })
                        srl?.postDelayed({
                            scanning?.unsubscribe()
                            srl?.isRefreshing = false
                        }, 5000)
                    } else {
                        srl?.isRefreshing = false
                    }
                }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanning?.unsubscribe()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == 123) {
            onRefresh()
        }
    }

    private fun onDeviceFound() {
        refreshNearDevices()
    }

    fun waitForBleServiceStart(started: () -> Unit) {
        BleService.instance?.let {
            started.invoke()
            return
        }
        bleServiceStartedCallbacks.add(started)
    }
}
