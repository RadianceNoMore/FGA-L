package io.github.fate_grand_automata.prefs

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BattleConfigFileTest {
    private fun roundTrip(values: Map<String, *>) =
        BattleConfigFile.decode(BattleConfigFile.encode(values))

    @Test
    fun `every value type a battle config stores survives a round trip`() {
        val decoded = roundTrip(
            mapOf(
                "autoskill_name" to "Farming",
                "shuffle_cards_wave" to 3,
                "use_servant_priority" to true,
                "battle_config_mat" to setOf("Proof", "Bone")
            )
        )

        assertThat(decoded["autoskill_name"]).isEqualTo("Farming")
        assertThat(decoded["shuffle_cards_wave"]).isEqualTo(3)
        assertThat(decoded["use_servant_priority"]).isEqualTo(true)
        assertThat(decoded["battle_config_mat"] as List<*>).containsOnly("Proof", "Bone")
    }

    /*
     * An int decoded as a double is not an Int, and the import side drops what it cannot type,
     * so a widened number would silently reset the preference to its default.
     */
    @Test
    fun `a whole number comes back as an Int`() {
        assertThat(roundTrip(mapOf("shuffle_cards_wave" to 3))["shuffle_cards_wave"])
            .isNotNull().isInstanceOf(Int::class)
    }

    /*
     * PrefMaker.stringAsInt stores numbers as strings, so JSON quoting is the only thing that
     * tells those apart from real int preferences.
     */
    @Test
    fun `a numeric string stays a String`() {
        assertThat(roundTrip(mapOf("autoskill_party" to "-1"))["autoskill_party"])
            .isNotNull().isInstanceOf(String::class)
    }

    @Test
    fun `a string that reads as a boolean stays a String`() {
        assertThat(roundTrip(mapOf("autoskill_notes" to "true"))["autoskill_notes"])
            .isEqualTo("true")
    }

    @Test
    fun `keys this version does not know are kept`() {
        assertThat(roundTrip(mapOf("some_future_pref" to "value"))["some_future_pref"])
            .isEqualTo("value")
    }

    @Test
    fun `a file that is not a JSON object is rejected`() {
        assertFailsWith<SerializationException> {
            BattleConfigFile.decode("not a battle config")
        }
    }

    @Test
    fun `compat export with quoted teapots booleans and ints coerces properly`() {
        val json = """
            {
                "autoskill_name": "3T_NoSwap",
                "should_use_teapots": "false",
                "limit_teapots": "2",
                "autoskill_party": "1",
                "auto_skill_rearrange_cards": "false"
            }
        """.trimIndent()

        val decoded = BattleConfigFile.decodeCompat(json)
        assertThat(decoded["autoskill_name"]).isEqualTo("3T_NoSwap")
        assertThat(decoded["should_use_teapots"]).isEqualTo(false)
        assertThat(decoded["limit_teapots"]).isEqualTo(2)
        assertThat(decoded["autoskill_party"]).isEqualTo("1")
        assertThat(decoded["auto_skill_rearrange_cards"]).isEqualTo("F")
    }

    @Test
    fun `compat export with t separator in card priority and servant priority normalizes to newline and comma`() {
        val json = """
            {
                "autoskill_name": "HOPEWILLCAMELOT",
                "autoskill_cmd": "i2 c2 d b2 h2 l2 g 645,\na 645,\nf e2 645,#,\na 645,\nf e2 645,#,\nf e2 645",
                "card_priority": "WAC, WQC, WBC, WA, WQ, WB, BC, AC, QC, A, B, Q, RA, RB, RQ[t]WAC, WQC, WBC, WA, WQ, WB, BC, AC, QC, A, B, Q, RA, RB, RQ",
                "servant_priority": "3,1,2,4,5,6[t]1,3,2,4,5,6",
                "auto_skill_brave_chains": "WithNP[t]WithNP",
                "auto_skill_rearrange_cards": "false[t]false",
                "grand_servant": "TARGET_ABOVE_LEVEL_100",
                "support_pref_ce_list_slot3": ["Chaldea Teatime"]
            }
        """.trimIndent()

        val decoded = BattleConfigFile.decode(json)
        assertThat(decoded["autoskill_cmd"]).isEqualTo("i2c2db2h2l2g645,a645,fe2645,#,a645,fe2645,#,fe2645")
        assertThat(decoded["card_priority"]).isEqualTo("WA, WQ, WB, B, A, Q, RA, RB, RQ\nWA, WQ, WB, B, A, Q, RA, RB, RQ")
        assertThat(decoded["servant_priority"]).isEqualTo("3,1,2,4,5,6\n1,3,2,4,5,6")
        assertThat(decoded["auto_skill_brave_chains"]).isEqualTo("WithNP,WithNP")
        assertThat(decoded["auto_skill_rearrange_cards"]).isEqualTo("F,F")
        assertThat(decoded["support_grand_servant"]).isEqualTo(true)
        assertThat(decoded["support_pref_ce_list"] as Collection<*>).containsOnly("Chaldea Teatime")
    }
}
