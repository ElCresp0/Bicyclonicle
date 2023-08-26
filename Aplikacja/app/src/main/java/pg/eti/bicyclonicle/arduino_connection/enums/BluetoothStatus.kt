package pg.eti.bicyclonicle.arduino_connection.enums

enum class BluetoothStatus {
    NO_BT_SUPPORT,
    NO_BT_PERMISSIONS,
    BT_DISABLED,
    BT_ENABLED_NOT_PAIRED,
    BT_PAIRED_WITH_ARDUINO
}