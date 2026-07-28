package dev.mkaminski.idealista.data

import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdDetail
import kotlinx.coroutines.flow.Flow

interface AdRepository {

    /**
     * Cached ads joined with their favorite state. Every screen observes this, so a favorite toggle
     * can never leave two screens disagreeing about whether an ad is favorited or when.
     */
    fun observeAds(): Flow<List<Ad>>

    /** Fetches `list.json` into the cache. Failure leaves the cache — and the UI — intact. */
    suspend fun refreshAds(): Result<Unit>

    /**
     * The detail of [propertyCode]. Identity comes from the cached ad, rich content from the detail
     * endpoint — which ignores the id and always returns ad 1. See ADR-0005.
     */
    fun observeAdDetail(propertyCode: String): Flow<AdDetail>

    /** Favorites the ad if it is not, un-favorites it if it is. Stamps the date on favoriting. */
    suspend fun toggleFavorite(propertyCode: String)
}
