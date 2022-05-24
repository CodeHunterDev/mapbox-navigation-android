package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils.initializeLayers
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowAddedValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowVisibilityChangeValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.RemoveArrowValue
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import com.mapbox.navigation.ui.maps.util.MapboxRouteLineUtils
import com.mapbox.navigation.utils.internal.logE

/**
 * Responsible for rendering state data generated by the MapboxRouteArrowApi class. The
 * state data will alter the appearance of the maneuver arrow(s) on the map.
 *
 * Each [Layer] added to the map by this class is a persistent layer - it will survive style changes.
 * This means that if the data has not changed, it does not have to be manually redrawn after a style change.
 * See [Style.addPersistentStyleLayer].
 *
 * @param options the options used for determining the rendering appearance and/or behavior.
 */
class MapboxRouteArrowView(private val options: RouteArrowOptions) {

    private companion object {
        private const val LOG_CATEGORY = "MapboxRouteArrowView"
    }

    /**
     * Renders an [ArrowVisibilityChangeValue] applying view side effects based on the data
     * it contains.
     *
     * @param style a valid map style object
     * @param visibilityChange a state containing data for applying the view side effects.
     */
    fun render(style: Style, visibilityChange: ArrowVisibilityChangeValue) {
        initializeLayers(style, options)

        visibilityChange.layerVisibilityModifications.forEach {
            updateLayerVisibility(style, it.first, it.second)
        }
    }

    /**
     * Renders an [Expected<InvalidPointError, UpdateManeuverArrowValue>] applying view side
     * effects based on the data it contains.
     *
     * @param style a valid map style object
     * @param expectedValue a value containing data for applying the view side effects.
     */
    fun renderManeuverUpdate(
        style: Style,
        expectedValue: Expected<InvalidPointError, UpdateManeuverArrowValue>
    ) {
        initializeLayers(style, options)

        expectedValue.onError {
            logE(it.errorMessage, LOG_CATEGORY)
        }
        expectedValue.onValue { value ->
            value.layerVisibilityModifications.forEach {
                updateLayerVisibility(style, it.first, it.second)
            }
            value.arrowHeadFeature?.apply {
                updateSource(style, RouteLayerConstants.ARROW_HEAD_SOURCE_ID, this)
            }
            value.arrowShaftFeature?.apply {
                updateSource(style, RouteLayerConstants.ARROW_SHAFT_SOURCE_ID, this)
            }
        }
    }

    /**
     * Renders an [ArrowAddedValue]
     *
     * @param style a valid map style object
     * @param arrowAdded a state containing data for applying the view side effects.
     */
    fun render(style: Style, arrowAdded: ArrowAddedValue) {
        initializeLayers(style, options)

        updateSource(
            style,
            RouteLayerConstants.ARROW_SHAFT_SOURCE_ID,
            arrowAdded.arrowShaftFeatureCollection
        )
        updateSource(
            style,
            RouteLayerConstants.ARROW_HEAD_SOURCE_ID,
            arrowAdded.arrowHeadFeatureCollection
        )
    }

    /**
     * Renders the maneuver arrow data
     *
     * @param style a valid map style object
     * @param expectedValue a state containing data for applying the view side effects.
     */
    fun render(style: Style, expectedValue: Expected<InvalidPointError, ArrowAddedValue>) {
        expectedValue.fold(
            { error ->
                logE(error.errorMessage, LOG_CATEGORY)
            },
            { value ->
                render(style, value)
            }
        )
    }

    /**
     * Renders a [RemoveArrowValue]
     *
     * @param style a valid map style object
     * @param state a state containing data for applying the view side effects.
     */
    fun render(style: Style, state: RemoveArrowValue) {
        initializeLayers(style, options)

        updateSource(
            style,
            RouteLayerConstants.ARROW_SHAFT_SOURCE_ID,
            state.arrowShaftFeatureCollection
        )
        updateSource(
            style,
            RouteLayerConstants.ARROW_HEAD_SOURCE_ID,
            state.arrowHeadFeatureCollection
        )
    }

    /**
     * Renders a [ClearArrowsValue]
     *
     * @param style a valid map style object
     * @param state a state containing data for applying the view side effects.
     */
    fun render(style: Style, state: ClearArrowsValue) {
        initializeLayers(style, options)

        updateSource(
            style,
            RouteLayerConstants.ARROW_SHAFT_SOURCE_ID,
            state.arrowShaftFeatureCollection
        )
        updateSource(
            style,
            RouteLayerConstants.ARROW_HEAD_SOURCE_ID,
            state.arrowHeadFeatureCollection
        )
    }

    /**
     * Returns the maneuver arrow visibility.
     *
     * @param style a valid map style object
     *
     * @return the visibility of the map layers used for rendering the maneuver arrow
     */
    fun getVisibility(style: Style): Visibility? {
        return MapboxRouteLineUtils.getLayerVisibility(
            style,
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID
        )
    }

    private fun updateLayerVisibility(style: Style, layerId: String, visibility: Visibility) {
        style.getLayer(layerId)?.visibility(visibility)
    }

    private fun updateSource(style: Style, sourceId: String, feature: Feature) {
        if (style.styleSourceExists(sourceId)) {
            style.getSourceAs<GeoJsonSource>(sourceId)?.feature(feature)
        }
    }

    private fun updateSource(style: Style, sourceId: String, featureCollection: FeatureCollection) {
        if (style.styleSourceExists(sourceId)) {
            style.getSourceAs<GeoJsonSource>(sourceId)?.featureCollection(featureCollection)
        }
    }
}
