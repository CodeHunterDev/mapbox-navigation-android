package com.mapbox.navigation.ui.shield.api

import android.graphics.drawable.VectorDrawable
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.shield.RoadShieldContentManager
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.shield.model.RouteShieldToDownload
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.launch

/**
 * A class that can be used to request route shields using either [BannerComponents.imageBaseUrl]
 * or [BannerComponents.mapboxShield]. The class exposes three different API(s) that allows you to
 * either always use [BannerComponents.imageBaseUrl] or use [BannerComponents.mapboxShield] or allow
 * SDK to decide smartly to use one or the other.
 *
 * [MapboxRouteShieldApi] returns route shield in the form of a SVG wrapped in a [ByteArray].
 * Since this SVG would later have to be converted to vector drawable at runtime for rendering
 * purposes, it is important to note that Android platform supports all [VectorDrawable] features
 * from [Tiny SVG 1.2](https://www.w3.org/TR/SVGTiny12/) except for text.
 * More can be read [here](https://developer.android.com/studio/write/vector-asset-studio#svg-support)
 * about support and restrictions for SVG files.
 *
 * @property options MapboxRouteShieldOptions used to specify the default shields in case shield
 * url is unavailable or failure to download the shields.
 */
class MapboxRouteShieldApi {

    private val contentManager = RoadShieldContentManager()
    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()

    fun getRouteShieldsFrom(
        bannerInstructions: List<BannerInstructions>,
        callback: RouteShieldCallback
    ) {
        val routeShieldToDownload = mutableListOf<RouteShieldToDownload.MapboxLegacy>()
        bannerInstructions.forEach { bannerInstruction ->
            val primaryBannerComponents = bannerInstruction.primary().components()
            val secondaryBannerComponents = bannerInstruction.secondary()?.components()
            val subBannerComponents = bannerInstruction.sub()?.components()
            primaryBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                    )
                }
            }
            secondaryBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                    )
                }
            }
            subBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                    )
                }
            }
        }
        getLegacyRouteShields(routeShieldToDownload, callback)
    }

    fun getLegacyRouteShields(
        shieldsToDownload: List<RouteShieldToDownload.MapboxLegacy>,
        callback: RouteShieldCallback
    ) {
        mainJob.scope.launch {
            if (shieldsToDownload.isNotEmpty()) {
                val result = contentManager.getShields(
                    userId = null,
                    styleId = null,
                    accessToken = null,
                    fallbackToLegacy = false,
                    shieldsToDownload = shieldsToDownload
                )
                callback.onRoadShields(shields = result)
            }
        }
    }

    fun getRouteShieldsFrom(
        userId: String,
        styleId: String,
        accessToken: String,
        fallbackToLegacy: Boolean,
        bannerInstructions: List<BannerInstructions>,
        callback: RouteShieldCallback
    ) {
        val routeShieldToDownload = mutableListOf<RouteShieldToDownload.MapboxDesign>()
        bannerInstructions.forEach { bannerInstruction ->
            val primaryBannerComponents = bannerInstruction.primary().components()
            val secondaryBannerComponents = bannerInstruction.secondary()?.components()
            val subBannerComponents = bannerInstruction.sub()?.components()
            primaryBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val mapboxShield = component.mapboxShield()
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxDesign(
                            mapboxShield = mapboxShield,
                            legacy = RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                        )
                    )
                }
            }
            secondaryBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val mapboxShield = component.mapboxShield()
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxDesign(
                            mapboxShield = mapboxShield,
                            legacy = RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                        )
                    )
                }
            }
            subBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val mapboxShield = component.mapboxShield()
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxDesign(
                            mapboxShield = mapboxShield,
                            legacy = RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                        )
                    )
                }
            }
        }
        getMapboxDesignedRouteShields(
            userId,
            styleId,
            accessToken,
            fallbackToLegacy,
            routeShieldToDownload,
            callback
        )
    }

    fun getMapboxDesignedRouteShields(
        userId: String,
        styleId: String,
        accessToken: String,
        fallbackToLegacy: Boolean,
        shieldsToDownload: List<RouteShieldToDownload.MapboxDesign>,
        callback: RouteShieldCallback
    ) {
        mainJob.scope.launch {
            if (shieldsToDownload.isNotEmpty()) {
                val result = contentManager.getShields(
                    userId = userId,
                    styleId = styleId,
                    accessToken = accessToken,
                    fallbackToLegacy = fallbackToLegacy,
                    shieldsToDownload = shieldsToDownload
                )
                callback.onRoadShields(shields = result)
            }
        }
    }
}
