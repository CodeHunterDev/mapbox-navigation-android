package com.mapbox.navigation.ui.app.internal

import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.extension.ThunkAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesState
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/**
 * End Navigation ThunkAction creator.
 */
fun endNavigation() = ThunkAction { store ->
    store.dispatch(RoutesAction.SetRoutes(emptyList()))
    store.dispatch(DestinationAction.SetDestination(null))
    store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
}

/**
 * Show Route Preview ThunkAction creator.
 */
fun CoroutineScope.showRoutePreview() = fetchRouteAndContinue { store ->
    store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
}

/**
 * Start Active Navigation ThunkAction creator.
 */
fun CoroutineScope.startActiveNavigation() = fetchRouteAndContinue { store ->
    store.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
}

private fun CoroutineScope.fetchRouteAndContinue(continuation: (Store) -> Unit) =
    ThunkAction { store ->
        launch {
            if (fetchRouteIfNeeded(store)) {
                continuation(store)
            }
        }
    }

/**
 * Dispatch FetchPoints action and wait for RoutesState.Ready.
 * Method returns immediately if already in RoutesState.Ready or RoutesState.Fetching, or if
 * required location or destination data is missing.
 *
 * @return `true` once in RoutesState.Ready state, otherwise `false`
 */
private suspend fun fetchRouteIfNeeded(store: Store): Boolean {
    val storeState = store.state.value
    if (storeState.routes is RoutesState.Ready) return true
    if (storeState.routes is RoutesState.Fetching) return false

    return ifNonNull(
        storeState.location?.enhancedLocation?.toPoint(),
        storeState.destination
    ) { lastPoint, destination ->
        store.dispatch(RoutesAction.FetchPoints(listOf(lastPoint, destination.point)))
        store.waitWhileFetching()
        store.state.value.routes is RoutesState.Ready
    } ?: false
}

private suspend fun Store.waitWhileFetching() {
    select { it.routes }.takeWhile { it is RoutesState.Fetching }.collect()
}
