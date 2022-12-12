package com.lgt.cwm.models

import com.lgt.cwm.db.entity.SignalMsg
import com.lgt.cwm.db.entity.SignalThread

/**
 * Created by giangtpu on 31/08/2022.
 */
data class SignalMsgProcessResult(val signalThread: SignalThread, val signalMsg: SignalMsg?)
