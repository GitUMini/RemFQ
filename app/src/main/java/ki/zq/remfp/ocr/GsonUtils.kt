package ki.zq.remfp.ocr

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/**
 * Json工具类.
 */
object GsonUtils {
    private val gson = GsonBuilder().create()
    fun toJson(value: Any?): String {
        return gson.toJson(value)
    }

    @Throws(JsonParseException::class)
    fun <T> fromJson(json: String?, classOfT: Class<T>?): T {
        return gson.fromJson(json, classOfT)
    }

    @Throws(JsonParseException::class)
    fun <T> fromJson(json: String?, typeOfT: Type?): T {
        return gson.fromJson(json, typeOfT)
    }
}