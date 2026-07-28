package dev.mkaminski.idealista.data.network

import dev.mkaminski.idealista.data.network.dto.AdDetailDto
import dev.mkaminski.idealista.data.network.dto.AdListItemDto
import retrofit2.http.GET

internal interface IdealistaApi {

    @GET("list.json")
    suspend fun getAds(): List<AdListItemDto>

    /**
     * Takes no id **on purpose**: the mock endpoint is a static file that returns ad 1 for every
     * request. Adding a path or query parameter would imply a contract the API does not honour.
     * The caller reconciles this in the repository — see ADR-0005.
     */
    @GET("detail.json")
    suspend fun getAdDetail(): AdDetailDto
}
