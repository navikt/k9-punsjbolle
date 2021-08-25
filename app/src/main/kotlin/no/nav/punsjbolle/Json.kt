package no.nav.punsjbolle

import org.json.JSONArray
import org.json.JSONObject

internal object Json {
    internal fun JSONObject.arrayOrEmptyArray(key: String) = when (has(key) && get(key) is JSONArray) {
        true -> getJSONArray(key)
        false -> JSONArray()
    }

    internal fun JSONObject.objectOrEmptyObject(key: String) = when(has(key) && get(key) is JSONObject) {
        true -> getJSONObject(key)
        false -> JSONObject()
    }

    internal fun JSONObject.stringOrNull(key: String) = when (has(key) && get(key) is String) {
        true -> getString(key)
        else -> null
    }
}