package com.lhn.favs_list.shared.ids

import java.util.UUID

fun interface UuidGenerator {
    fun randomUuid(): UUID
}
