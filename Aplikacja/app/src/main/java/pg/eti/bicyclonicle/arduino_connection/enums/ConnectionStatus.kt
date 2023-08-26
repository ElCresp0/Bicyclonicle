package pg.eti.bicyclonicle.arduino_connection.enums

import pg.eti.bicyclonicle.R

// todo: strings
// todo: all this needed?
enum class ConnectionStatus(val stringResId: Int) {
    CONNECTION_STATUS(R.string.connection_status),
    CONNECTED(R.string.connected),
    NOT_CONNECTED(R.string.not_connected),
    MESSAGE_READ(R.string.message_read)
}