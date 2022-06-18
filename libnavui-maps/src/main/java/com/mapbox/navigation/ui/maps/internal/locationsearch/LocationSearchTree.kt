package com.mapbox.navigation.ui.maps.internal.locationsearch

import android.util.LruCache
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.function.Supplier

class LocationSearchTree<T: Supplier<Point>>(private val capacity: Int = 32, private val distanceCalculationCacheCapacity: Int = 500) {

    private var rootNode: LocationTreeNode<T>? = null
    private val distanceCalculationCache : LruCache<
        CacheResultUtils.CacheResultKey2<
            Point,
            Point,
            Double
            >,
        Double> by lazy { LruCache(distanceCalculationCacheCapacity) }

    fun size() = rootNode?.size() ?: 0

    fun isEmpty() = size() == 0

    fun add(point: T) {
        addAll(listOf(point))
    }

    fun addAll(points: List<T>) {
        if (rootNode == null) {
            rootNode = LocationTreeNode(points.toMutableList(), capacity, distanceCalcFunction)
        } else {
            if (points.isNotEmpty()) {
                points.forEach {
                    rootNode?.add(it)
                }
                rootNode?.initializeNode()
            }
        }
    }

    fun remove(point: T) {
        removeAll(listOf(point))
    }

    fun removeAll(points: List<T>) {
        var pointRemoved = false
        rootNode?.let { theRootNode ->
            points.forEach {
                pointRemoved = theRootNode.remove(it) || pointRemoved
            }

            if (pointRemoved) {
                theRootNode.initializeNode()
            }
        }
    }

    fun clear() {
        rootNode = null
        distanceCalculationCache.evictAll()
    }

    fun getNearestNeighbor(target: Point) = getNearestNeighbors(target, 1).firstOrNull()

    fun getNearestNeighbors(target: Point, maxResults: Int): List<T> {
        return if (rootNode == null) {
            listOf()
        } else {
            val collector: NearestNeighborCollector<T> = NearestNeighborCollector(target, maxResults)
            rootNode?.collectNearestNeighbors(collector)
            collector.toSortedList()
        }
    }

    fun getDistanceCalculationCacheHits() = distanceCalculationCache.hitCount()

    fun getDistanceCalculationCacheMisses() = distanceCalculationCache.missCount()

    private val distanceCalcFunction = { point1: Point, point2: Point ->
        //Log.e("foobar", "distance cache hit count = ${distanceCalculationCache.hitCount()}, miss count = ${distanceCalculationCache.missCount()} size is ${distanceCalculationCache.size()}")
        TurfMeasurement.distance(point1, point2, TurfConstants.UNIT_METERS)
    }.cacheResult(distanceCalculationCache)
}
