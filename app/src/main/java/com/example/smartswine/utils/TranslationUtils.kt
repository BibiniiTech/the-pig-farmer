package com.example.smartswine.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.smartswine.SmartSwineApplication
import com.example.smartswine.model.FeedIngredient
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "🇺🇸"),
    FRENCH("fr", "Français", "🇫🇷"),
    CHINESE("zh", "中文", "🇨🇳"),
    SPANISH("es", "Español", "🇲🇽"),
    CASTILIAN("es-es", "Español (Castellano)", "🇪🇸"),
    DOMINICAN_SPANISH("es-do", "Español (Dominicano)", "🇩🇴"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    JAPANESE("ja", "日本語", "🇯🇵"),
    PORTUGUESE("pt", "Português", "🇵🇹"),
    FILIPINO("tl", "Filipino", "🇵🇭"),
    VIETNAMESE("vi", "Tiếng Việt", "🇻🇳"),
    THAI("th", "ไทย", "🇹🇭"),
    INDONESIAN("id", "Bahasa Indonesia", "🇮🇩"),
    HINDI("hi", "हिन्दी", "🇮🇳"),
    SWAHILI("sw", "Kiswahili", "🇰🇪"),
    LUGANDA("lg", "Oluganda", "🇺🇬"),
    KINYARWANDA("rw", "Ikinyarwanda", "🇷🇼"),
    CREOLE("ht", "Kreyòl Ayisyen", "🇭🇹"),
    BURMESE("my", "မြန်မာ", "🇲🇲"),
    TOK_PISIN("tpi", "Tok Pisin", "🇵🇬"),
    TAMAZIGHT("zgh", "Tamaziɣt", "🇲🇦"),
    AFRIKAANS("af", "Afrikaans", "🇿🇦");

    fun toLocale(): Locale = Locale.forLanguageTag(code)
}

object Translator {
    private const val TAG = "Translator"

    @Volatile
    var currentLanguageCode: String = "en"

    @SuppressLint("StaticFieldLeak")
    private var contextRef: Context? = null
    private val cache = ConcurrentHashMap<String, Map<String, String>>()

    fun init(context: Context) {
        contextRef = context.applicationContext
        // Pre-warm English fallback asynchronously or on first access
        loadLanguage("en")
    }

    private fun getContext(): Context? {
        return contextRef ?: SmartSwineApplication.appContext
    }

    private fun loadLanguage(languageCode: String): Map<String, String> {
        cache[languageCode]?.let { return it }

        val context = getContext()
        if (context == null) {
            Log.w(TAG, "Context not initialized yet for language: $languageCode")
            return emptyMap()
        }

        return try {
            val assetPath = "locales/${languageCode.lowercase(Locale.ROOT)}.json"
            val inputStream = context.assets.open(assetPath)
            val jsonString = InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val map = HashMap<String, String>(jsonObject.length())
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.optString(key, "")
            }
            cache[languageCode] = map
            map
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load locale asset for $languageCode", e)
            emptyMap()
        }
    }

    fun getString(key: String, languageCode: String, vararg args: Any): String {
        val langMap = cache[languageCode] ?: loadLanguage(languageCode)
        val enMap = cache["en"] ?: loadLanguage("en")

        val template = langMap[key] ?: enMap[key] ?: key
        return if (args.isNotEmpty()) {
            try {
                String.format(Locale.forLanguageTag(languageCode), template, *args)
            } catch (_: Exception) {
                template
            }
        } else {
            template
        }
    }

    fun getStringWithDefault(key: String, languageCode: String, default: String): String {
        val langMap = cache[languageCode] ?: loadLanguage(languageCode)
        val enMap = cache["en"] ?: loadLanguage("en")
        val value = langMap[key] ?: enMap[key]
        return if (!value.isNullOrBlank() && value != key) value else default
    }

    fun translateError(errorMsg: String?, languageCode: String): String? {
        if (errorMsg == null) return null
        val lower = errorMsg.lowercase(Locale.ROOT)
        if (lower.contains("no credentials available") ||
            lower.contains("nocredentialexception")
        ) {
            return getString("google_sign_in_no_accounts", languageCode)
        }
        if (lower.contains("network error") ||
            lower.contains("timeout") ||
            lower.contains("interrupted connection") ||
            lower.contains("unreachable host") ||
            lower.contains("unable to resolve host") ||
            lower.contains("failed to connect")
        ) {
            return getString("check_internet_connection", languageCode)
        }
        return errorMsg
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun stringResource(key: String, vararg args: Any): String {
    val language = LocalAppLanguage.current
    Translator.currentLanguageCode = language.code
    val translated = Translator.getString(key, language.code, *args)
    if (translated == key && key.contains("_")) {
        return key.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
    return translated
}

/**
 * Returns the translated activity type for a given internal type name.
 */
@Composable
fun getTranslatedActivityType(type: String): String {
    return when (type) {
        "Vaccination" -> stringResource("vaccination")
        "Deworming" -> stringResource("deworming")
        "Medication" -> stringResource("medication")
        "Weight Check" -> stringResource("weight_check")
        "Culling" -> stringResource("culling")
        "Teeth Clipping" -> stringResource("teeth_clipping")
        "Tail Docking" -> stringResource("tail_docking")
        "Iron Injection" -> stringResource("iron_injection")
        "Weaning" -> stringResource("weaning")
        "Castration" -> stringResource("castration")
        "Heat Detection" -> stringResource("heat_detection")
        "Breeding/Mating" -> stringResource("breeding_mating")
        "Pregnancy Check", "Confirm Pregnancy" -> stringResource("pregnancy_check")
        "Farrowing" -> stringResource("farrowing")
        "Custom" -> stringResource("custom")
        else -> type
    }
}

fun getIngredientNameKey(name: String): String {
    return name.lowercase(Locale.ROOT).replace(" ", "_").replace(",", "").replace("&", "")
}

/**
 * Returns a key that can be used with Translator to get the translated name of an ingredient.
 */
fun getIngredientNameKey(ingredient: FeedIngredient, context: Context? = null): String {
    return if (ingredient.nameResourceId != 0 && context != null) {
        try {
            context.resources.getResourceEntryName(ingredient.nameResourceId)
        } catch (_: Exception) {
            ingredient.name.lowercase(Locale.ROOT).replace(" ", "_").replace(",", "").replace("&", "")
        }
    } else {
        ingredient.name.lowercase(Locale.ROOT).replace(" ", "_").replace(",", "").replace("&", "")
    }
}

/**
 * Returns a key for a main category.
 */
fun getCategoryKey(category: String): String {
    return category.lowercase(Locale.ROOT)
        .replace("/", "_")
        .replace(", ", "_")
        .replace(" & ", "_")
        .replace(" ", "_")
}

@Composable
fun getTranslatedIngredientName(ingredient: FeedIngredient): String {
    val language = LocalAppLanguage.current
    val customTranslation = ingredient.nameTranslations[language.code]
    if (!customTranslation.isNullOrBlank()) {
        return customTranslation
    }
    val context = LocalContext.current
    return stringResource(getIngredientNameKey(ingredient, context))
}
