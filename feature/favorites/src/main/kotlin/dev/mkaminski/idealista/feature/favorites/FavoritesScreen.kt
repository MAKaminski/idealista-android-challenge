package dev.mkaminski.idealista.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.mkaminski.idealista.designsystem.Formatters
import dev.mkaminski.idealista.model.Ad

/**
 * The Compose-only screen. The two mandatory screens stay XML; this one demonstrates Compose
 * "alongside xml" as a whole destination (ADR-0006).
 */
@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        FavoritesUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }

        FavoritesUiState.Empty -> Box(modifier.fillMaxSize(), Alignment.Center) {
            Text(
                text = stringResource(R.string.favorites_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(32.dp).testTag(TAG_EMPTY),
            )
        }

        is FavoritesUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize().testTag(TAG_LIST),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.favorites, key = { it.propertyCode }) { ad ->
                FavoriteRow(ad = ad, onRemove = { onRemoveFavorite(ad.propertyCode) })
            }
        }
    }
}

@Composable
private fun FavoriteRow(ad: Ad, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ad.thumbnailUrl,
                contentDescription = stringResource(R.string.favorites_thumbnail, ad.address),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Text(
                    text = Formatters.price(ad.price, ad.currencySuffix),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = ad.address, style = MaterialTheme.typography.bodyMedium)
                // The requirement, in the third toolkit as well: when it was favorited.
                ad.favoritedAt?.let { favoritedAt ->
                    Text(
                        text = stringResource(
                            R.string.favorite_saved_on,
                            Formatters.favoritedDate(favoritedAt),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(
                        dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite_filled,
                    ),
                    contentDescription = stringResource(R.string.favorites_remove, ad.address),
                )
            }
        }
    }
}

internal const val TAG_EMPTY = "favorites_empty"
internal const val TAG_LIST = "favorites_list"
