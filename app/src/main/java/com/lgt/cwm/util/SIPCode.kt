package com.lgt.cwm.util

/**
 * Created by giangtpu on 7/7/22.
 */
enum class SIPCode(val code: Int){
    OK(200),
    UnAuthorized(401),
    Forbidden(403),
    BusyHere(486),
    RequestTimeout(408),
    TemporarilyUnavailable(480),
    RequestTerminated(487),
    NotAcceptableHere(488)
}