package com.craiovadata.groupmap.model

import java.util.*

class CurrentMemberStatus {

    var state = STATE_SIGNED_OFF

    companion object {
        const val STATE_JOINED = 1
        const val STATE_NOT_JOINED = 2
        const val STATE_BANNED = 3
        const val STATE_ADMIN = 4
        const val STATE_SIGNED_OFF = 5
    }

}