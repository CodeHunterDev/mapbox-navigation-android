package com.mapbox.navigation.dropin.component.infopanel

import android.view.View
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesState
import com.mapbox.navigation.ui.app.internal.showRoutePreview
import com.mapbox.navigation.ui.app.internal.startActiveNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelHeaderComponent(
    private val store: Store,
    private val binding: MapboxInfoPanelHeaderLayoutBinding,
    @StyleRes private val routePreviewStyle: Int,
    @StyleRes private val endNavigationStyle: Int,
    @StyleRes private val startNavigationStyle: Int,
) : UIComponent() {

    private val resources get() = binding.root.resources

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        binding.routePreview.updateStyle(routePreviewStyle)
        binding.endNavigation.updateStyle(endNavigationStyle)
        binding.startNavigation.updateStyle(startNavigationStyle)

        // views visibility
        store.select { it.navigation }.observe {
            binding.poiName.isVisible = it == NavigationState.DestinationPreview
            binding.routePreview.isVisible = it == NavigationState.DestinationPreview
            binding.startNavigation.isVisible = it == NavigationState.DestinationPreview ||
                it == NavigationState.RoutePreview
            binding.endNavigation.isVisible = it == NavigationState.ActiveNavigation ||
                it == NavigationState.Arrival
            binding.tripProgressLayout.isVisible = it == NavigationState.ActiveNavigation ||
                it == NavigationState.RoutePreview
            binding.arrivedText.isVisible = it == NavigationState.Arrival
        }

        store.select { it.destination }.observe {
            val placeName = it?.features?.firstOrNull()?.placeName()
            binding.poiName.text = placeName
                ?: resources.getString(R.string.mapbox_drop_in_dropped_pin)
        }

        binding.routePreview.onClick(coroutineScope) {
            store.dispatch(showRoutePreview())
        }

        binding.startNavigation.onClick(coroutineScope) {
            store.dispatch(startActiveNavigation())
        }

        binding.endNavigation.onClick(coroutineScope) {
            store.dispatch(endNavigation())
        }
    }

    private fun View.onClick(scope: CoroutineScope, action: suspend CoroutineScope.(View) -> Unit) {
        // launch one actor
        val eventActor = scope.actor<View>(Dispatchers.Main, capacity = Channel.CONFLATED) {
            for (event in channel) action(event)
        }
        // install a listener to activate this actor
        setOnClickListener { eventActor.trySend(it) }
    }

    /**
     * Dispatch FetchPoints action and wait for RoutesState.Ready.
     * Method returns immediately if already in RoutesState.Ready or RoutesState.Fetching, or if
     * required location or destination data is missing.
     *
     * @return `true` once in RoutesState.Ready state, otherwise `false`
     */
    private suspend fun fetchRouteIfNeeded(): Boolean {
        val storeState = store.state.value
        if (storeState.routes is RoutesState.Ready) return true
        if (storeState.routes is RoutesState.Fetching) return false

        return ifNonNull(
            storeState.location?.enhancedLocation?.toPoint(),
            storeState.destination
        ) { lastPoint, destination ->
            store.dispatch(RoutesAction.FetchPoints(listOf(lastPoint, destination.point)))
            waitForReady()
        } ?: false
    }

    private suspend fun waitForReady(): Boolean {
        store.select { it.routes }.takeWhile { it is RoutesState.Fetching }.collect()
        return store.state.value.routes is RoutesState.Ready
    }
}
