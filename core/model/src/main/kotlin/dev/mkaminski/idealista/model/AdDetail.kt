package dev.mkaminski.idealista.model

import java.time.Instant

/**
 * The detail screen's model.
 *
 * [ad] carries the **identity** of the ad the user actually opened, taken from the cached list ad.
 * Everything else comes from the detail endpoint, which always returns the same payload regardless
 * of which ad was requested — see docs/DECISIONS/ADR-0005-detail-merge-strategy.md.
 */
data class AdDetail(
    val ad: Ad,
    val comment: String,
    val characteristics: AdCharacteristics,
    val energyCertificate: EnergyCertificate?,
    val gallery: List<AdImage>,
)

data class AdCharacteristics(
    val communityCosts: Double?,
    val roomNumber: Int?,
    val bathNumber: Int?,
    val exterior: Boolean?,
    val housingFurnitures: String?,
    val energyCertificationType: String?,
    val flatLocation: String?,
    val modificationDate: Instant?,
    val constructedAreaSquareMeters: Int?,
    val hasLift: Boolean?,
    val hasBoxroom: Boolean?,
    val isDuplex: Boolean?,
    val floor: String?,
    val status: String?,
)

data class EnergyCertificate(
    val title: String?,
    val consumptionType: String?,
    val emissionsType: String?,
)
