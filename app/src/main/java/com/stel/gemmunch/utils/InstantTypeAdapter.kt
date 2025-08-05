package com.stel.gemmunch.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.time.Instant

/**
 * A custom Gson TypeAdapter to correctly serialize and deserialize java.time.Instant.
 * It converts an Instant to its epoch millisecond representation (a Long) for storage
 * and converts it back when reading.
 */
class InstantTypeAdapter : TypeAdapter<Instant>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
            return
        }
        // Serialize Instant as its epoch millisecond value
        out.value(value.toEpochMilli())
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Instant? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        // Deserialize epoch millisecond value back to an Instant
        return Instant.ofEpochMilli(`in`.nextLong())
    }
}