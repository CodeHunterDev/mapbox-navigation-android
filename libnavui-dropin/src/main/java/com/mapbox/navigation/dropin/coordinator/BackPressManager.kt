package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StopActiveGuidanceUseCase
import javax.inject.Inject
import javax.inject.Provider

/**
 * Class that manages onBackPressedCallback enabled state
 * and handles onBackPressed event for each NavigationState.
 */
internal class BackPressManager @Inject constructor(
    private val context: DropInNavigationViewContext,
    private val viewModel: DropInNavigationViewModel,
    private val stopActiveGuidanceUseCaseProvider: Provider<StopActiveGuidanceUseCase>
) : UIComponent() {

    private val stopActiveGuidanceUseCase: StopActiveGuidanceUseCase
        get() = stopActiveGuidanceUseCaseProvider.get()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        viewModel.destination.observe {
            context.onBackPressedCallback.isEnabled = it != null
        }

        viewModel.onBackPressedEvent.observe {
            when (viewModel.navigationState.value) {
                NavigationState.FreeDrive -> {
                    viewModel.updateDestination(null)
                }
                NavigationState.RoutePreview -> {
                    mapboxNavigation.setRoutes(emptyList())
                }
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> {
                    stopActiveGuidanceUseCase(Unit)
                }
                else -> Unit
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        context.onBackPressedCallback.isEnabled = false
    }
}
