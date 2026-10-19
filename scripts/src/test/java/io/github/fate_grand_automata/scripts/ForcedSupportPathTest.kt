package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.fate_grand_automata.scripts.models.AutoSkillCommand
import io.github.fate_grand_automata.scripts.models.NPUsage
import io.github.fate_grand_automata.scripts.models.battle.BattleState
import io.github.fate_grand_automata.scripts.modules.AutoSkill
import io.github.fate_grand_automata.scripts.modules.Caster
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.mockk.mockk
import kotlin.test.Test

class ForcedSupportPathTest {
    private val api = mockk<IFgoAutomataApi>(relaxed = true)
    private val caster = mockk<Caster>(relaxed = true)

    @Test
    fun autoSkillExecutesNormalPathWhenNotForcedSupport() {
        val normalCmd = AutoSkillCommand.parse("4, #, 5, #, 6")
        val battleConfig = mockk<IBattleConfig>(relaxed = true) {
            io.mockk.every { useForcedSupportPath } returns true
            io.mockk.every { skillCommandForcedSupport } returns "5, #, 6"
        }
        val state = BattleState().apply {
            isForcedSupport = false
        }

        val autoSkill = AutoSkill(
            api = api,
            skillCommand = normalCmd,
            battleConfig = battleConfig,
            state = state,
            caster = caster
        )

        // Wave 0, Turn 0 -> normalCmd has NP 4
        val usage = autoSkill.execute(stage = 0, turn = 0)
        assertThat(usage.nps.map { it.autoSkillCode }).isEqualTo(listOf('4'))
    }

    @Test
    fun autoSkillExecutesForcedSupportPathWhenDetectedAndEnabled() {
        val normalCmd = AutoSkillCommand.parse("4, #, 5, #, 6")
        val battleConfig = mockk<IBattleConfig>(relaxed = true) {
            io.mockk.every { useForcedSupportPath } returns true
            io.mockk.every { skillCommandForcedSupport } returns "5, #, 6"
        }
        val state = BattleState().apply {
            isForcedSupport = true
        }

        val autoSkill = AutoSkill(
            api = api,
            skillCommand = normalCmd,
            battleConfig = battleConfig,
            state = state,
            caster = caster
        )

        // Wave 0, Turn 0 -> forcedCmd has NP 5
        val usage = autoSkill.execute(stage = 0, turn = 0)
        assertThat(usage.nps.map { it.autoSkillCode }).isEqualTo(listOf('5'))
    }

    @Test
    fun autoSkillFallsBackToNormalWhenForcedSupportPathDisabled() {
        val normalCmd = AutoSkillCommand.parse("4, #, 5, #, 6")
        val battleConfig = mockk<IBattleConfig>(relaxed = true) {
            io.mockk.every { useForcedSupportPath } returns false
            io.mockk.every { skillCommandForcedSupport } returns "5, #, 6"
        }
        val state = BattleState().apply {
            isForcedSupport = true
        }

        val autoSkill = AutoSkill(
            api = api,
            skillCommand = normalCmd,
            battleConfig = battleConfig,
            state = state,
            caster = caster
        )

        // Wave 0, Turn 0 -> should fall back to normalCmd (NP 4)
        val usage = autoSkill.execute(stage = 0, turn = 0)
        assertThat(usage.nps.map { it.autoSkillCode }).isEqualTo(listOf('4'))
    }
}
