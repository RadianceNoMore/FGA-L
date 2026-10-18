package io.github.fate_grand_automata.prefs

import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

fun SharedPreferences.Editor.import(map: Map<String, *>) {
    for ((key, value) in map) {
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is Collection<*> -> putStringSet(key, value.map { it.toString() }.toSet())
        }
    }
}

/**
 * The `.fga` file format: one battle config's whole [SharedPreferences] map as a flat JSON object.
 *
 * [PrefMaker][io.github.fate_grand_automata.prefs.core.PrefMaker] only ever stores strings, ints,
 * booleans and string sets, and those are the types [import] puts back, so those are the types
 * that survive a round trip. Other values are still written out, but importing skips them.
 */
object BattleConfigFile {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val BOOLEAN_KEYS = setOf(
        "should_use_teapots", "auto_choose_target", "use_servant_priority",
        "add_raid_delay", "support_pref_ce_mlb", "support_friends_only",
        "also_check_all", "support_max_ascended", "support_skill_max_1",
        "support_skill_max_2", "support_skill_max_3", "support_grand_servant",
        "support_require_both_normal_and_reward_match",
        "should_limit_runs", "should_limit_ces", "is_favorite",
        "story_intro_skip", "skip_servant_face_card_check",
        "auto_accept_friend_request", "use_critical_star_priority"
    )

    private val INTEGER_KEYS = setOf(
        "limit_teapots", "shuffle_cards_wave", "autoskill_cmd_index",
        "support_np_level", "limit_runs", "limit_ces"
    )

    private val STRING_INT_KEYS = setOf(
        "autoskill_party", "raid_delay_seconds", "usage_count"
    )

    private val STRING_SET_KEYS = setOf(
        "battle_config_mat", "support_friend_names_list",
        "support_pref_servant_list", "support_pref_ce_list"
    )

    fun encode(values: Map<String, *>): String {
        val entries = values.mapNotNull { (key, value) ->
            value.toJson()?.let { key to it }
        }

        return json.encodeToString(JsonObject.serializer(), JsonObject(entries.toMap()))
    }

    /** @throws kotlinx.serialization.SerializationException if [text] is not a JSON object. */
    fun decode(text: String, compatMode: Boolean = false): Map<String, Any> {
        val map = json.parseToJsonElement(text).jsonObject
            .mapNotNull { (key, element) ->
                element.toPrefValue()?.let { key to it }
            }
            .toMap()

        return if (compatMode || isLikelyCompatExport(map)) {
            normalizeCompat(map)
        } else {
            map
        }
    }

    fun decodeCompat(text: String): Map<String, Any> = decode(text, compatMode = true)

    private fun isLikelyCompatExport(map: Map<String, Any>): Boolean {
        if (map.containsKey("grand_servant") ||
            map.containsKey("servant_priority_settings") ||
            map.containsKey("support_pref_ce_list_slot3")
        ) {
            return true
        }
        if (map["should_use_teapots"] is String || map["limit_teapots"] is String) {
            return true
        }
        return map.values.any { it is String && it.contains("[t]") }
    }

    fun normalizeCompat(raw: Map<String, Any>): Map<String, Any> {
        val cleaned = mutableMapOf<String, Any>()

        for ((key, rawVal) in raw) {
            var v: Any = rawVal

            // 1. Handle wave separator [t] in string values
            if (v is String && v.contains("[t]")) {
                v = when (key) {
                    "card_priority", "servant_priority" -> v.replace("[t]", "\n")
                    "auto_skill_brave_chains" -> v.replace("[t]", ",")
                    "auto_skill_rearrange_cards" -> {
                        v.split("[t]").joinToString(",") { p ->
                            if (p.trim().equals("true", ignoreCase = true) || p.trim() == "T") "T" else "F"
                        }
                    }
                    else -> v.replace("[t]", ",")
                }
            }

            // 2. Handle auto_skill_rearrange_cards formatting
            if (key == "auto_skill_rearrange_cards") {
                v = when (v) {
                    is Boolean -> if (v) "T" else "F"
                    is String -> {
                        val trimmed = v.trim()
                        if (trimmed.equals("true", ignoreCase = true) || trimmed == "T") {
                            "T"
                        } else if (trimmed.equals("false", ignoreCase = true) || trimmed == "F") {
                            "F"
                        } else if (trimmed.contains(",")) {
                            trimmed.split(",").joinToString(",") { p ->
                                if (p.trim().equals("true", ignoreCase = true) || p.trim() == "T") "T" else "F"
                            }
                        } else {
                            trimmed
                        }
                    }
                    else -> v
                }
            }

            // 3. Handle grand_servant alias
            if (key == "grand_servant") {
                if (v is String && v.isNotBlank() && !v.equals("None", ignoreCase = true)) {
                    cleaned["support_grand_servant"] = true
                } else if (v is Boolean) {
                    cleaned["support_grand_servant"] = v
                }
            }

            // 4. Handle servant_priority_settings alias
            if (key == "servant_priority_settings") {
                if (v is String && v.contains("true", ignoreCase = true)) {
                    cleaned["use_servant_priority"] = true
                } else if (v is Boolean && v) {
                    cleaned["use_servant_priority"] = true
                }
            }

            // 5. Coerce types for known boolean keys
            if (key in BOOLEAN_KEYS) {
                v = when (v) {
                    is Boolean -> v
                    is String -> v.trim().equals("true", ignoreCase = true) || v.trim() == "1"
                    is Number -> v.toInt() != 0
                    else -> false
                }
            }
            // 6. Coerce types for known integer keys
            else if (key in INTEGER_KEYS) {
                v = when (v) {
                    is Int -> v
                    is Number -> v.toInt()
                    is String -> v.trim().toIntOrNull() ?: 0
                    is Boolean -> if (v) 1 else 0
                    else -> 0
                }
            }
            // 7. Coerce types for string-as-int keys (stores as String)
            else if (key in STRING_INT_KEYS) {
                v = v.toString()
            }
            // 8. Coerce string sets (e.g. from JSON arrays/lists)
            else if (key in STRING_SET_KEYS) {
                v = when (v) {
                    is Collection<*> -> v.map { it.toString() }.toSet()
                    is String -> setOf(v)
                    else -> emptySet<String>()
                }
            }

            cleaned[key] = v
        }

        // 9. Fallback support_pref_ce_list from slot3 if primary is empty
        val prefCEs = cleaned["support_pref_ce_list"] as? Collection<*>
        if (prefCEs.isNullOrEmpty()) {
            val slot3 = raw["support_pref_ce_list_slot3"] as? Collection<*>
            if (!slot3.isNullOrEmpty()) {
                cleaned["support_pref_ce_list"] = slot3.map { it.toString() }.toSet()
            }
        }

        return cleaned
    }

    private fun Any?.toJson(): JsonElement? = when (this) {
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Collection<*> -> JsonArray(map { JsonPrimitive(it.toString()) })
        else -> null
    }

    /*
     * JSON has one number type, so the whole-number cases have to be picked apart by hand:
     * reading them all back as doubles - which is what an untyped parse gives you - would make
     * every int-valued preference unimportable.
     */
    private fun JsonElement.toPrefValue(): Any? = when {
        this is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.content }
        this !is JsonPrimitive || this is JsonNull -> null
        isString -> content
        else -> booleanOrNull ?: intOrNull ?: longOrNull ?: doubleOrNull
    }
}
