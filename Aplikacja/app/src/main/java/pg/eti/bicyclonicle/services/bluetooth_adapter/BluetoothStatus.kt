package pg.eti.bicyclonicle.services.bluetooth_adapter

import pg.eti.bicyclonicle.R

enum class BluetoothStatus(val stringResId: Int) {
    BT_ENABLED(R.string.bt_enabled),
    NO_BT_PERMISSIONS(R.string.no_bt_permissions),
    NO_BT_SUPPORT(R.string.no_bt_support),
    BT_DISABLED(R.string.bt_disabled)
}