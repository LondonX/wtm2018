package com.bronze.ble.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.bronze.ble.BleService
import com.bronze.ble.R
import com.bronze.ble.extensions.alias
import com.bronze.kutil.widget.SimpleAdapter2
import com.polidea.rxandroidble.RxBleDevice
import org.jetbrains.anko.textResource

/**
 * Created by London on 2017/12/11.
 * device list
 */
class DeviceManagerAdapter(private val operateClick: (RxBleDevice) -> Unit) :
        SimpleAdapter2<RxBleDevice>(R.layout.item_ble_device) {

    var listWithAlias = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            notifyDataSetChanged()
        }

    override fun bindData2(context: Context, itemView: View, position: Int, data: RxBleDevice) {

        itemView.findViewById<TextView>(R.id.tvBleDisplayName)?.text = data.alias.takeIf { !it.isBlank() } ?: "UNKNOWN"
        itemView.findViewById<TextView>(R.id.tvBleMacAddress)?.text = data.macAddress
        itemView.findViewById<TextView>(R.id.tvBleOperate)?.setOnClickListener { operateClick.invoke(data) }
        if (data in BleService.instance?.connectedDevices ?: emptyList<BleService>()) {
            itemView.findViewById<TextView>(R.id.tvBleOperate)?.textResource = R.string.ble_disconnect
            itemView.findViewById<TextView>(R.id.tvBleAlias)?.visibility =
                    if (listWithAlias) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
        } else {
            itemView.findViewById<TextView>(R.id.tvBleOperate)?.textResource = R.string.ble_connect
            itemView.findViewById<TextView>(R.id.tvBleAlias)?.visibility = View.GONE
        }
        itemView.findViewById<TextView>(R.id.tvBleAlias)?.setOnClickListener {
            val etAlias = EditText(context)
            etAlias.setText(data.alias)
            etAlias.setHint(R.string.ble_set_alias)
            AlertDialog.Builder(context)
                    .setTitle(R.string.ble_set_alias)
                    .setView(etAlias)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        data.alias = etAlias.text.toString()
                        notifyItemChanged(position)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        }
    }
}