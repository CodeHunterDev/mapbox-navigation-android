package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin

sealed class RoutesState {
    object Empty : RoutesState()
    data class Fetching internal constructor(val requestId: Long) : RoutesState()
    data class Ready internal constructor(val routes: List<NavigationRoute>) : RoutesState()
    data class Canceled internal constructor(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutesState()
    data class Failed internal constructor(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutesState()
}
