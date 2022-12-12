package com.lgt.cwm.models

import com.lgt.cwm.db.entity.Contact

/**
 * Created by giangtpu on 7/19/22.
 */
data class SyncContactData (var listContactAll: List<Contact>, var listContactAdd: List<Contact>, var listContactUpdate: List<Contact>, var listContactRemove: List<Contact>)