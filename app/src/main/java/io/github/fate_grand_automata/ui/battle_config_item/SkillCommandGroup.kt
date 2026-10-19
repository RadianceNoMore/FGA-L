package io.github.fate_grand_automata.ui.battle_config_item

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.prefs.core.BattleConfigCore
import io.github.fate_grand_automata.scripts.models.AutoSkillCommand
import io.github.fate_grand_automata.ui.DimmedIcon
import io.github.fate_grand_automata.ui.icon
import io.github.fate_grand_automata.ui.prefs.Preference
import io.github.fate_grand_automata.ui.prefs.PreferenceTextEditor
import io.github.fate_grand_automata.ui.prefs.remember
import io.github.fate_grand_automata.ui.skill_maker.SkillMakerEntry
import io.github.fate_grand_automata.ui.skill_maker.colorRes

import io.github.fate_grand_automata.prefs.core.Pref

@Composable
fun SkillCommandField(
    title: String = stringResource(R.string.p_battle_config_cmd),
    commandPref: Pref<String>,
    parsedCommand: List<SkillMakerEntry>,
    openSkillMaker: (() -> Unit)? = null
) {
    var cmd by commandPref.remember()
    var editing by remember { mutableStateOf(false) }

    val invalidCommandMessage = stringResource(R.string.battle_config_cmd_invalid)

    if (editing) {
        var errorMessage by remember { mutableStateOf("") }

        Column {
            PreferenceTextEditor(
                label = title,
                prefill = cmd,
                onSubmit = {
                    try {
                        // Check if parses correctly
                        AutoSkillCommand.parse(it)

                        cmd = it
                        editing = false
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = invalidCommandMessage.format(e.cause?.message ?: e.message ?: "")
                    }
                },
                onCancel = { editing = false },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )

            if (errorMessage.isNotBlank()) {
                PreferenceError(errorMessage)
            }
        }
    } else {
        Preference(
            title = { Text(title) },
            summary = if (parsedCommand.isNotEmpty()) {
                { SkillCommandSummary(parsedCommand) }
            } else {
                {
                    Text(
                        stringResource(R.string.battle_config_cmd_not_set),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            onClick = openSkillMaker ?: { editing = true }
        ) {
            IconButton(
                onClick = { editing = true }
            ) {
                DimmedIcon(
                    icon(R.drawable.ic_terminal),
                    contentDescription = "Show Textbox for editing Skill command"
                )
            }
        }
    }
}

@Composable
fun SkillCommandGroup(
    config: BattleConfigCore,
    vm: BattleConfigScreenViewModel,
    openSkillMaker: () -> Unit
) {
    val parsedCommand by vm.skillCommand.collectAsState(listOf())
    SkillCommandField(
        title = stringResource(R.string.p_battle_config_cmd),
        commandPref = config.skillCommand,
        parsedCommand = parsedCommand,
        openSkillMaker = openSkillMaker
    )
}

/*
 * A plain scrolling Row rather than a LazyRow: this is the supporting content of a ListItem,
 * whose measure policy asks for the child's intrinsic height, and a LazyRow is a
 * SubcomposeLayout, which throws on any intrinsic measurement. A command is short enough that
 * composing every entry costs nothing.
 */
@Composable
fun SkillCommandSummary(skillCommand: List<SkillMakerEntry>) {
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        skillCommand.forEach {
            Card(
                colors = cardColors(
                    containerColor = colorResource(it.colorRes)
                ),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    it.toString(),
                    color = Color.White,
                    modifier = Modifier
                        .padding(2.dp, 1.dp)
                )
            }
        }
    }
}

@Composable
fun PreferenceError(error: String) {
    Text(
        error,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(16.dp, 2.dp)
            .padding(bottom = 5.dp)
    )
}