package pg.eti.bicyclonicle.services.bluetooth_adapter

import pg.eti.bicyclonicle.R

enum class BluetoothStatus(val stringResId: Int) {
    BT_ENABLED_NOT_CONNECTED(R.string.bt_enabled_not_paired),
    NO_BT_PERMISSIONS(R.string.no_bt_permissions),
    NO_BT_SUPPORT(R.string.no_bt_support),
    BT_DISABLED(R.string.bt_disabled),
    BT_CONNECTED_TO_ARDUINO(R.string.bt_connected_to_arduino),
    BT_NOT_CONNECTED_TO_ARDUINO(R.string.bt_not_connected_to_arduino)
}