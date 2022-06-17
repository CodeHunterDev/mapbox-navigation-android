package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.navigation.utils.internal.ifNonNull

object LocationSearchUtil {

    fun interpolateScreenCoordinates(pointToPixelList: List<EnhancedPoint>) {
        val position = 0
        var nextKeyPoint = getNextKeyPoint(position, pointToPixelList)
        var followingKeyPoint = getNextKeyPoint(position + 1, pointToPixelList)

        while (nextKeyPoint != null && followingKeyPoint != null) {
            ifNonNull(nextKeyPoint, followingKeyPoint) { nPoint, fPoint ->
                val fillerRange = (fPoint.first + 1) - nPoint.first
                val xDelta = ((fPoint.second.getChmCoordinates()?.first
                    ?: 0f) - (nPoint.second.getChmCoordinates()?.first ?: 0f)) / fillerRange
                val yDelta = ((fPoint.second.getChmCoordinates()?.second
                    ?: 0f) - (nPoint.second.getChmCoordinates()?.second ?: 0f)) / fillerRange

                var xPoint = (nPoint.second.getChmCoordinates()?.first ?: 0f) + xDelta
                var yPoint = (nPoint.second.getChmCoordinates()?.second ?: 0f) + yDelta

                for (index in nPoint.first + 1 until fPoint.first) {
                    val item = pointToPixelList[index]
                    if (item is EnhancedPoint.MapPoint) {
                        item.setChmCoordinates(Pair(xPoint, yPoint))
                    }

                    xPoint += xDelta
                    yPoint += yDelta
                }
            }

            nextKeyPoint = followingKeyPoint
            followingKeyPoint =
                getNextKeyPoint(nextKeyPoint.first + 1, pointToPixelList)
        }
    }

    private fun getNextKeyPoint(offset: Int, pointToPixelList: List<EnhancedPoint>): Pair<Int, EnhancedPoint>? {
        val nextKeyPointIndex = pointToPixelList.drop(offset).indexOfFirst { it is EnhancedPoint.KeyPoint }
        return if (nextKeyPointIndex >= 0) {
            Pair(nextKeyPointIndex + offset, pointToPixelList[nextKeyPointIndex + offset])
        } else {
            null
        }
    }
}
