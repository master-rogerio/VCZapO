package com.pdm.vczap_o.chatRoom.data.local.typeconverters

import androidx.room.TypeConverter
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.Location

class LocationConverter {
    @TypeConverter
    fun fromString(value: String?): Location? {
        if (value == null) {
            return null
        }
        return try {
            val parts = value.split(",")
            val location = Location(parts[0].toDouble(), parts[1].toDouble())
            location
        } catch (e: Exception) {
            logger(
                "LocationConverter",
                "fromString: Error converting string '$value' to Location $e",
            )
            null
        }
    }

    @TypeConverter
    fun toString(location: Location?): String? {
        val result = location?.let { "${it.latitude},${it.longitude}" }
        return result
    }
}