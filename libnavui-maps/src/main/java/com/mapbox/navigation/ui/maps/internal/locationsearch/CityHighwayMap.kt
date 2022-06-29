package com.mapbox.navigation.ui.maps.internal.locationsearch

sealed class CityHighwayMap {
    object XB01: CityHighwayMap()
    object XB03: CityHighwayMap() // todo come up with a better naming scheme
}
