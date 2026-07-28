package dev.mkaminski.idealista.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mkaminski.idealista.model.AdCharacteristics
import dev.mkaminski.idealista.model.EnergyCertificate

/**
 * Compose *inside* the XML detail screen, hosted by a `ComposeView` in `fragment_ad_detail.xml`.
 *
 * This is the incremental-adoption half of ADR-0006: the surrounding screen is still XML with
 * ViewBinding, and only this panel is Compose.
 */
@Composable
internal fun CharacteristicsPanel(
    labels: List<String>,
    certificate: EnergyCertificate?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            SuggestionChip(onClick = {}, label = { Text(label) }, enabled = false)
        }

        val consumption = certificate?.consumptionType
        val emissions = certificate?.emissionsType
        if (consumption != null || emissions != null) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Row {
                        Text(
                            text = listOfNotNull(
                                consumption?.let { stringResource(R.string.detail_energy_consumption, it.uppercase()) },
                                emissions?.let { stringResource(R.string.detail_energy_emissions, it.uppercase()) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                modifier = Modifier.padding(top = 0.dp),
            )
        }
    }
}

/** Builds the chip labels from the merged detail, keeping formatting out of the composable. */
internal fun characteristicLabels(
    characteristics: AdCharacteristics,
    roomsLabel: (Int) -> String,
    bathsLabel: (Int) -> String,
    areaLabel: (Int) -> String,
    floorLabel: (String) -> String,
    liftLabel: (Boolean) -> String,
    communityCostsLabel: (Double) -> String,
): List<String> = buildList {
    characteristics.roomNumber?.let { add(roomsLabel(it)) }
    characteristics.bathNumber?.let { add(bathsLabel(it)) }
    characteristics.constructedAreaSquareMeters?.let { add(areaLabel(it)) }
    characteristics.floor?.let { add(floorLabel(it)) }
    characteristics.hasLift?.let { add(liftLabel(it)) }
    characteristics.communityCosts?.let { add(communityCostsLabel(it)) }
}
