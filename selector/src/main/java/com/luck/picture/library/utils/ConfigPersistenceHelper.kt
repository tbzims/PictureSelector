package com.luck.picture.library.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.luck.picture.library.config.SelectorConfig
import com.luck.picture.library.customengine.GlideEngine
import java.lang.reflect.Type

class ConfigPersistenceHelper(context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("picture_selector_config", Context.MODE_PRIVATE)
    }

    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.registerTypeAdapter(Class::class.java, ClassTypeAdapter())
        builder.registerTypeAdapter(SelectorConfig::class.java, SelectorConfigTypeAdapter())
        builder.create()
    }

    fun saveConfig(config: SelectorConfig) {
        val jsonString = gson.toJson(config)
        prefs.edit().putString("selector_config_json", jsonString).apply()
    }

    fun restoreConfig(): SelectorConfig? {
        val jsonString = prefs.getString("selector_config_json", null)
        return if (jsonString != null) {
            gson.fromJson(jsonString, SelectorConfig::class.java)
        } else {
            null
        }
    }

    fun clearConfig() {
        prefs.edit().remove("selector_config_json").apply()
    }

    private class ClassTypeAdapter : JsonSerializer<Class<*>>, JsonDeserializer<Class<*>> {
        @Throws(JsonParseException::class)
        override fun serialize(
            src: Class<*>?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return JsonPrimitive(src?.name ?: "")
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Class<*> {
            val className = json?.asString ?: throw JsonParseException("Class name is null")
            return try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                throw JsonParseException("Could not load class: $className", e)
            }
        }
    }

    private class SelectorConfigTypeAdapter : JsonSerializer<SelectorConfig>,
        JsonDeserializer<SelectorConfig> {
        @Throws(JsonParseException::class)
        override fun serialize(
            src: SelectorConfig?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            val jsonObject = JsonObject()
            return Gson().toJsonTree(src)
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): SelectorConfig {
            val config = SelectorConfig()
            val jsonObject = json?.asJsonObject
            config.imageEngine = GlideEngine.create()
            return config
        }
    }
}