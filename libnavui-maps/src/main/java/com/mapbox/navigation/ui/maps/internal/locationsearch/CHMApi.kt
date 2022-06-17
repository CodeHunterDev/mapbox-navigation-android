package com.mapbox.navigation.ui.maps.internal.locationsearch

class CHMApi(private val locationSearchTree: LocationSearchTree = LocationSearchTree()) {

    private val xb03PointToPixels: List<EnhancedPoint> by lazy {
        val enhancedPoints = XB03PointToPixelMap.getPointCollections()
        enhancedPoints.forEach {
            LocationSearchUtil.interpolateScreenCoordinates(it)
        }
        enhancedPoints.flatten()
    }


    fun getMap(mapType: CityHighwayMap) {
        when (mapType) {
            CityHighwayMap.XB03 -> {
                locationSearchTree.clear()
                locationSearchTree.addAll(xb03PointToPixels)
            }
        }
    }

}
