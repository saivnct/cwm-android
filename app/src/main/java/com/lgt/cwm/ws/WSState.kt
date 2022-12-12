package com.lgt.cwm.ws

/**
 * Created by giangtpu on 04/07/2022.
 */
enum class WSState {
    RECONNECT_ATTEMPT,
    CONNECTING,
    DISCONNECTED,
    CONNECTED,
    ERROR,
}