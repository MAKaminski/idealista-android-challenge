package dev.mkaminski.idealista.data

import dev.mkaminski.idealista.data.di.IoDispatcher
import dev.mkaminski.idealista.data.local.AdDao
import dev.mkaminski.idealista.data.local.Converters
import dev.mkaminski.idealista.data.local.FavoriteDao
import dev.mkaminski.idealista.data.local.FavoriteEntity
import dev.mkaminski.idealista.data.local.toDomain
import dev.mkaminski.idealista.data.local.toEntity
import dev.mkaminski.idealista.data.network.IdealistaApi
import dev.mkaminski.idealista.data.network.toDomain
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdDetail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultAdRepository @Inject constructor(
    private val api: IdealistaApi,
    private val adDao: AdDao,
    private val favoriteDao: FavoriteDao,
    private val converters: Converters,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AdRepository {

    /**
     * The single join of ads and favorites. Both the list and the detail screen are built on this,
     * which is what makes "favorited on <date>" identical everywhere without any screen knowing how
     * favorites are stored.
     */
    override fun observeAds(): Flow<List<Ad>> =
        combine(adDao.observeAll(), favoriteDao.observeAll()) { ads, favorites ->
            val favoritedAt: Map<String, Instant> = favorites.associate {
                it.propertyCode to Instant.ofEpochMilli(it.favoritedAt)
            }
            ads.map { entity ->
                entity.toDomain(
                    images = converters.jsonToImages(entity.images),
                    favoritedAt = favoritedAt[entity.propertyCode],
                )
            }
        }.flowOn(ioDispatcher)

    override suspend fun refreshAds(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val ads = api.getAds().map { it.toDomain() }
            adDao.upsertAll(ads.map { it.toEntity(images = converters.imagesToJson(it.images)) })
        }
    }

    /**
     * Fetches the detail payload once per collection, then re-emits whenever the cached ad changes —
     * so favoriting from the detail screen updates its own date without a refetch.
     *
     * The response's `adid`, `priceInfo` and `ubication` are discarded: identity comes from the
     * cached ad. Trusting the response would render ad 1 on every screen (ADR-0005).
     */
    override fun observeAdDetail(propertyCode: String): Flow<AdDetail> = flow {
        val detailDto = api.getAdDetail()
        emitAll(
            observeAds()
                .map { ads -> ads.firstOrNull { it.propertyCode == propertyCode } }
                .distinctUntilChanged()
                .mapNotNull { it }
                .map { cachedAd -> detailDto.toDomain(listAd = cachedAd) },
        )
    }.flowOn(ioDispatcher)

    override suspend fun toggleFavorite(propertyCode: String) = withContext(ioDispatcher) {
        if (favoriteDao.isFavorite(propertyCode)) {
            favoriteDao.deleteByCode(propertyCode)
        } else {
            favoriteDao.upsert(
                FavoriteEntity(
                    propertyCode = propertyCode,
                    favoritedAt = clock.instant().toEpochMilli(),
                ),
            )
        }
    }
}