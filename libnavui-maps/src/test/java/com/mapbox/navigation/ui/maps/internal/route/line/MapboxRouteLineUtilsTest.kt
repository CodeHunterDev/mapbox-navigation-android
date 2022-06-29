package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Logger
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToLineString
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.CLOSURE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LOW_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.RouteInterface
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class MapboxRouteLineUtilsTest {

    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicWidth } returns 24
            every { intrinsicHeight } returns 24
        }
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun getTrafficLineExpressionTest() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"

        val expressionDatas = listOf(
            RouteLineExpressionData(0.0, -11097861, 0),
            RouteLineExpressionData(0.015670907645820537, -11097861, 0),
            RouteLineExpressionData(0.11898525632162987, -11097861, 0)
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            -11097861,
            expressionDatas
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionDuplicateOffsetsRemoved() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.7964017663976524, [rgba, 255.0, 0.0, 0.0, 1.0]]"
        val expressionDatas = listOf(
            RouteLineExpressionData(0.7868200761181402, -11097861, 0),
            RouteLineExpressionData(0.7930120224665551, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7964017663976524, Color.RED, 0)
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            -11097861,
            expressionDatas
        )

        assertEquals(result.toString(), expectedExpression)
    }

    @Test
    fun getFilteredRouteLineExpressionDataDuplicateOffsetsRemoved() {
        val expressionDatas = listOf(
            RouteLineExpressionData(0.7868200761181402, -11097861, 0),
            RouteLineExpressionData(0.7930120224665551, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7964017663976524, -11097861, 0)
        )

        val result = MapboxRouteLineUtils.getFilteredRouteLineExpressionData(
            0.0,
            expressionDatas,
            -11097861
        )

        assertEquals(2, expressionDatas.count { it.offset == 0.7932530928525063 })
        assertEquals(1, result.count { it.offset == 0.7932530928525063 })
    }

    @Test
    fun getRestrictedSectionExpressionData() {
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(14, result.size)
        assertTrue(result[4].isInRestrictedSection)
        assertFalse(result[5].isInRestrictedSection)
        assertTrue(result[6].isInRestrictedSection)
    }

    @Test
    fun getRestrictedLineExpression() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.2, [rgba, 0.0, 0.0, 0.0, 0.0], 0.44865144220494346, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0], 0.468779750455607, [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.5032854217424586, [rgba, 255.0, 255.0, 255.0, 1.0], 0.5207714038134984, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"

        val route = loadRoute("route-with-restrictions.json")
        val expData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            -1,
            expData
        )

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpressionProducer() {
        val colorResources = RouteLineColorResources.Builder()
            .restrictedRoadColor(Color.CYAN)
            .build()
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.2, [rgba, 0.0, 0.0, 0.0, 0.0], 0.44865144220494346, " +
            "[rgba, 0.0, 255.0, 255.0, 1.0], 0.468779750455607, [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.5032854217424586, [rgba, 0.0, 255.0, 255.0, 1.0], 0.5207714038134984," +
            " [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("route-with-restrictions.json")

        val expression = MapboxRouteLineUtils.getRestrictedLineExpressionProducer(
            route,
            0.2,
            0,
            colorResources
        ).generateExpression()

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getDisabledRestrictedLineExpressionProducer() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.0, [rgba, 0.0, 0.0, 0.0, 0.0]]"

        val expression = MapboxRouteLineUtils.getDisabledRestrictedLineExpressionProducer(
            0.0,
            0,
            1
        ).generateExpression()

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpression_whenNoRestrictionsInRoute() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.2, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("short_route.json")
        val expData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            -1,
            expData
        )

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getVanishingRouteLineExpressionTest() {
        val expectedExpression = "[step, [line-progress], [rgba, 255.0, 77.0, 77.0, 1.0]" +
            ", 3.0, [rgba, 86.0, 168.0, 251.0, 1.0]]"

        val result = MapboxRouteLineUtils.getRouteLineExpression(3.0, -45747, -11097861)

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun calculateDistance() {
        val result = MapboxRouteLineUtils.calculateDistance(
            Point.fromLngLat(-122.525212, 37.974092),
            Point.fromLngLat(-122.52509389295653, 37.974569579999944)
        )

        assertEquals(0.0000017145850113848236, result, 0.0)
    }

    @Test
    fun layersAreInitialized() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns true
        }
        val style = mockk<Style> {
            every { styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
            } returns true
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RESTRICTED_ROAD_LAYER_ID) }
    }

    @Test
    fun `layersAreInitialized without restricted roads`() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns false
        }
        val style = mockk<Style> {
            every { styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
            } returns false
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) }
        verify(exactly = 0) {
            style.styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
        }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
            every { styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
            } returns true
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
            every { styleSourceExists(WAYPOINT_SOURCE_ID) } returns false
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
    }

    @Test
    fun calculateRouteGranularDistances_whenInputNull() {
        val result = MapboxRouteLineUtils.calculateRouteGranularDistances(listOf())

        assertNull(result)
    }

    @Test
    fun getBelowLayerIdToUse() {
        val style = mockk<Style> {
            every { styleLayerExists("foobar") } returns true
        }

        val result = MapboxRouteLineUtils.getBelowLayerIdToUse("foobar", style)

        assertEquals("foobar", result)
    }

    @Test
    fun getBelowLayerIdToUse_whenLayerIdNotFoundReturnsNull() {
        mockkStatic(Logger::class)
        every { Logger.e(any(), any()) } just Runs
        val style = mockk<Style> {
            every { styleLayerExists("foobar") } returns false
        }

        val result = MapboxRouteLineUtils.getBelowLayerIdToUse("foobar", style)

        assertNull(result)
        unmockkStatic(Logger::class)
    }

    @Test
    fun getBelowLayerIdToUse_whenLayerIdNotSpecified() {
        val style = mockk<Style>()

        val result = MapboxRouteLineUtils.getBelowLayerIdToUse(null, style)

        assertNull(result)
    }

    @Test
    fun buildScalingExpression() {
        val expectedExpression = "[interpolate, [exponential, 1.5], [zoom], 4.0, [*, 3.0, 1.0]," +
            " 10.0, [*, 4.0, 1.0], 13.0, [*, 6.0, 1.0], 16.0, [*, 10.0, 1.0], 19.0, " +
            "[*, 14.0, 1.0], 22.0, [*, 18.0, 1.0]]"
        val values = listOf(
            RouteLineScaleValue(4f, 3f, 1f),
            RouteLineScaleValue(10f, 4f, 1f),
            RouteLineScaleValue(13f, 6f, 1f),
            RouteLineScaleValue(16f, 10f, 1f),
            RouteLineScaleValue(19f, 14f, 1f),
            RouteLineScaleValue(22f, 18f, 1f)
        )

        val result = MapboxRouteLineUtils.buildScalingExpression(values)

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getRouteLineTrafficExpressionDataWhenUniqueStreetClassDataExists() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-unique-road-classes.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val distances = route.legs()!!.mapNotNull { it.annotation()!!.distance() }.flatten()
        val distancesSum = distances.subList(0, distances.lastIndex).sum()
        val roadClasses = route.legs()?.asSequence()
            ?.mapNotNull { it.steps() }
            ?.flatten()
            ?.mapNotNull { it.intersections() }
            ?.flatten()
            ?.filter {
                it.geometryIndex() != null && it.mapboxStreetsV8()?.roadClass() != null
            }
            ?.map { it.mapboxStreetsV8()!!.roadClass() }
            ?.toList()

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(distances.size, result.size)
        assertEquals(distances.first(), result[1].distanceFromOrigin, 0.0)
        assertEquals(result[0].roadClass, roadClasses!!.first())
        assertEquals(result[2].distanceFromOrigin, distances.subList(0, 2).sum(), 0.0)
        assertEquals(distancesSum, result.last().distanceFromOrigin, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    // The route used here for testing produced an erroneous edge case. The
    // getRouteLineTrafficExpressionData method was always producing a distanceFromOrigin value
    // of 0.0 for the beginning of each route leg. This could cause an error when creating the
    // traffic expression because the distanceFromOrigin value is used to determine the
    // percentage of distance traveled. These values need to be in ascending order to create a
    // valid line gradient expression. This error won't occur in single leg routes and will
    // only occur in multileg routes when there is a traffic congestion change at the first point in
    // the leg. This is because duplicate traffic congestion values are dropped. The route
    // used in the test below has a traffic change at the first point in the second leg and
    // the distance annotation is 0.0 which would have caused an error prior to the fix this
    // test is checking for.
    @Test
    fun getRouteLineTrafficExpressionDataMultiLegRouteWithTrafficChangeAtWaypoint() {
        val route = loadRoute("multileg-route-two-legs.json")

        val trafficExpressionData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(1, trafficExpressionData.count { it.distanceFromOrigin == 0.0 })
        assertEquals(0.0, trafficExpressionData[0].distanceFromOrigin, 0.0)
    }

    @Test
    fun getRouteLineTrafficExpressionWithRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(10, result.size)
        assertEquals(1300.0000000000002, result.last().distanceFromOrigin, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson =
            FileUtils.loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(5, result.size)
        assertEquals(0.0, result[0].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[0].trafficCongestionIdentifier)
        assertEquals("motorway", result[0].roadClass)
        assertEquals(3.7, result[1].distanceFromOrigin, 0.0)
        assertEquals("severe", result[1].trafficCongestionIdentifier)
        assertEquals("motorway", result[1].roadClass)
        assertEquals(27.5, result[2].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[2].trafficCongestionIdentifier)
        assertEquals("motorway", result[2].roadClass)
        assertEquals(39.9, result[3].distanceFromOrigin, 0.0)
        assertEquals("severe", result[3].trafficCongestionIdentifier)
        assertEquals("motorway", result[3].roadClass)
        assertEquals(99.6, result[4].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[4].trafficCongestionIdentifier)
        assertEquals("motorway", result[4].roadClass)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesOnMotorway() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture("motorway-route-with-road-classes.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(1, result.size)
    }

    @Test
    fun getRouteLineExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-1)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()

        val routeAsJsonJson =
            FileUtils.loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertEquals(5, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(-1, result[0].segmentColor)
        assertEquals(0.002337691548550063, result[1].offset, 0.0)
        assertEquals(33, result[1].segmentColor)
        assertEquals(0.01737473448246668, result[2].offset, 0.0)
        assertEquals(-1, result[2].segmentColor)
        assertEquals(0.025209160212742564, result[3].offset, 0.0)
        assertEquals(33, result[3].segmentColor)
        assertEquals(0.06292812925286113, result[4].offset, 0.0)
        assertEquals(-1, result[4].segmentColor)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesAndClosures() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(99)
            .routeClosureColor(-21)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-closure.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("tertiary")
        )

        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(-1, result[0].segmentColor)
        assertEquals(0.5467690917306824, result[1].offset, 0.0)
        assertEquals(-21, result[1].segmentColor)
        assertEquals(0.8698599186624492, result[2].offset, 0.0)
        assertEquals(99, result[2].segmentColor)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithOutStreetClassesDuplicatesRemoved() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(5, result.size)
        assertEquals(1188.7000000000003, result.last().distanceFromOrigin, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertNull(result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithStreetClassesDuplicatesRemoved() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        assertEquals("service", trafficExpressionData[0].roadClass)
        assertEquals("street", trafficExpressionData[1].roadClass)
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[0].trafficCongestionIdentifier
        )
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[1].trafficCongestionIdentifier
        )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("street")
        )

        assertEquals(-9, result[0].segmentColor)
        assertEquals(7, result.size)
        assertEquals(0.016404052025563352, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenDoesNotHaveStreetClasses() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf()
        )

        assertEquals(5, result.size)
        assertEquals(0.23460041526970057, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
    }

    @Test
    fun getTrafficExpressionWithStreetClassOverrideOnMotorwayWhenChangeOutsideOfIntersections() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(-2)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture(
            "motorway-route-with-road-classes-unknown-not-on-intersection.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertEquals(-2, result[0].segmentColor)
        assertNotEquals(-9, result[1].segmentColor)
        assertEquals(-1, result[1].segmentColor)
        assertEquals(-2, result[2].segmentColor)
    }

    @Test
    fun getRouteLineTrafficExpressionDataMissingRoadClass() {
        val routeAsJsonJson = FileUtils.loadJsonFixture(
            "route-with-missing-road-classes.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(7, result.size)
        assertEquals(0.0, result[0].distanceFromOrigin, 0.0)
        assertEquals("severe", result[0].trafficCongestionIdentifier)
        assertEquals("motorway", result[0].roadClass)
        assertEquals(3.7, result[1].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[1].trafficCongestionIdentifier)
        assertEquals("motorway", result[1].roadClass)
        assertEquals(27.5, result[2].distanceFromOrigin, 0.0)
        assertEquals("severe", result[2].trafficCongestionIdentifier)
        assertEquals("motorway", result[2].roadClass)
        assertEquals(271.8, result[3].distanceFromOrigin, 0.0)
        assertEquals("severe", result[3].trafficCongestionIdentifier)
        assertEquals("intersection_without_class_fallback", result[3].roadClass)
        assertEquals(305.2, result[4].distanceFromOrigin, 0.0)
        assertEquals("severe", result[4].trafficCongestionIdentifier)
        assertEquals("motorway", result[4].roadClass)
        assertEquals(545.6, result[5].distanceFromOrigin, 0.0)
        assertEquals("severe", result[5].trafficCongestionIdentifier)
        assertEquals("intersection_without_class_fallback", result[5].roadClass)
        assertEquals(1168.3000000000002, result[6].distanceFromOrigin, 0.0)
        assertEquals("severe", result[6].trafficCongestionIdentifier)
        assertEquals("motorway", result[6].roadClass)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesOnMotorwayMultiLeg() {
        // test case for overlapping geometry indices across multiple legs
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val routeAsJsonJson = FileUtils.loadJsonFixture(
            "motorway-with-road-classes-multi-leg.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(2, result.size)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithClosures() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-closure.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        assertEquals(0.0, trafficExpressionData[0].distanceFromOrigin, 0.0)
        assertEquals("low", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(145.20000000000002, trafficExpressionData[1].distanceFromOrigin, 0.0)
        assertEquals("closed", trafficExpressionData[1].trafficCongestionIdentifier)
        assertEquals(231.0, trafficExpressionData[2].distanceFromOrigin, 0.0)
        assertEquals("severe", trafficExpressionData[2].trafficCongestionIdentifier)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithRestrictedSections() {
        val route = loadRoute("route-with-restrictions.json")
        val expectedDistanceFromOriginIndex3 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 3).sum()
        val expectedDistanceFromOriginIndex17 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 17).sum()
        val expectedDistanceFromOriginIndex18 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 18).sum()
        val expectedDistanceFromOriginIndex19 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 19).sum()
        val expectedDistanceFromOriginIndex20 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 20).sum()

        val trafficExpressionData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(0.0, trafficExpressionData[0].offset, 0.0)
        assertEquals(true, trafficExpressionData[0].isLegOrigin)

        assertEquals(
            expectedDistanceFromOriginIndex3,
            trafficExpressionData[3].offset * route.distance(),
            0.0
        )
        assertEquals(0, trafficExpressionData[3].legIndex)
        assertEquals(false, trafficExpressionData[3].isInRestrictedSection)
        assertEquals(false, trafficExpressionData[3].isLegOrigin)

        assertEquals(
            expectedDistanceFromOriginIndex17,
            trafficExpressionData[17].offset * route.distance(),
            0.0
        )
        assertEquals(true, trafficExpressionData[17].isInRestrictedSection)

        assertEquals(
            expectedDistanceFromOriginIndex18,
            trafficExpressionData[18].offset * route.distance(),
            0.0
        )
        assertEquals(false, trafficExpressionData[18].isInRestrictedSection)

        assertEquals(
            expectedDistanceFromOriginIndex19,
            trafficExpressionData[19].offset * route.distance(),
            0.000000000001
        )
        assertEquals(true, trafficExpressionData[19].isInRestrictedSection)

        assertEquals(
            expectedDistanceFromOriginIndex20,
            trafficExpressionData[20].offset * route.distance(),
            0.0
        )
        assertEquals(false, trafficExpressionData[20].isInRestrictedSection)
    }

    @Test
    fun getRouteLineTrafficExpressionData_whenFirstDistanceInSecondLegIsZero() {
        val route = loadRoute("multileg-route-two-legs.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(19, result.size)
        assertEquals(478.70000000000005, result[7].distanceFromOrigin, 0.0)
        assertTrue(result[7].isLegOrigin)
        assertEquals(529.9000000000001, result[8].distanceFromOrigin, 0.0)
        assertFalse(result[8].isLegOrigin)
    }

    @Test
    fun `extractRouteData with multiple distance entries of zero`() {
        val route = loadRoute("artificially_wrong_two_leg_route.json")
        val comparisonRoute = loadRoute("multileg-route-two-legs.json")
        val expectedResult = MapboxRouteLineUtils.extractRouteData(
            comparisonRoute,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        // In this case the two routes being used are the same except for an additional
        // distance value of 0 being added to test the implementation. The result of the
        // calls should be the same to prove that distance values of 0 in the route
        // are ignored.
        val listItemsAreEqual = listElementsAreEqual(expectedResult, result) { item1, item2 ->
            compareValuesBy(
                item1,
                item2,
                { it.distanceFromOrigin },
                { it.isInRestrictedSection },
                { it.isLegOrigin },
                { it.offset },
                { it.trafficCongestionIdentifier }
            ) == 0
        }
        assertTrue(listItemsAreEqual)
        assertFalse(result[16].isLegOrigin)
        assertTrue(result[17].isLegOrigin)
        assertFalse(result[18].isLegOrigin)
        assertEquals(478.70000000000005, result[17].distanceFromOrigin, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRoute() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val route = loadRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        assertEquals(20, result.size)
        assertEquals(0.039793906743275334, result[1].offset, 0.0)
        assertEquals(0.989831291992653, result.last().offset, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRouteFirstDistanceValueAboveMinimumOffset() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val route = loadRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        assertTrue(result[1].offset > .001f)
    }

    @Test
    fun calculateRouteLineSegments_whenNoTrafficExpressionData() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeOptions = mockk<RouteOptions>(relaxed = true) {
            every {
                annotationsList()
            } returns listOf(DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC)
        }
        val route = mockk<DirectionsRoute> {
            every { legs() } returns listOf()
            every { routeOptions() } returns routeOptions
        }

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(colorResources.routeDefaultColor, result[0].segmentColor)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionLow() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            LOW_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeLowCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionModerate() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            MODERATE_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeModerateCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionHeavy() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                HEAVY_CONGESTION_VALUE,
                true,
                resources
            )

        assertEquals(resources.routeHeavyCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionSevere() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                SEVERE_CONGESTION_VALUE,
                true,
                resources
            )

        assertEquals(resources.routeSevereCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionUnknown() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                UNKNOWN_CONGESTION_VALUE,
                true,
                resources
            )

        assertEquals(resources.routeUnknownCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionDefault() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            "foobar",
            true,
            resources
        )

        assertEquals(resources.routeDefaultColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionClosure() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            CLOSURE_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeClosureColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionRestricted() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RESTRICTED_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.restrictedRoadColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionLow() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                LOW_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteLowCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionModerate() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                MODERATE_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteModerateCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionHeavy() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                HEAVY_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteHeavyCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionSevere() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                SEVERE_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteSevereCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionUnknown() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                UNKNOWN_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteUnknownCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionDefault() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            "foobar",
            false,
            resources
        )

        assertEquals(resources.alternativeRouteDefaultColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionRestricted() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RESTRICTED_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteRestrictedRoadColor, result)
    }

    @Test
    fun getRestrictedRouteLegRangesTest() {
        val route = loadRoute("route-with-restrictions.json")
        val coordinates = LineString.fromPolyline(
            route.geometry() ?: "",
            Constants.PRECISION_6
        )

        val result = MapboxRouteLineUtils.getRestrictedRouteLegRanges(route.legs()!!.first())

        assertEquals(2, result.size)
        assertEquals(37.971947, coordinates.coordinates()[result[0].first].latitude(), 0.0)
        assertEquals(-122.526159, coordinates.coordinates()[result[0].first].longitude(), 0.0)
        assertEquals(37.971947, coordinates.coordinates()[result[0].last].latitude(), 0.0)
        assertEquals(-122.526159, coordinates.coordinates()[result[0].last].longitude(), 0.0)
        assertEquals(37.972037, coordinates.coordinates()[result[1].first].latitude(), 0.0)
        assertEquals(-122.526951, coordinates.coordinates()[result[1].first].longitude(), 0.0)
        assertEquals(37.972037, coordinates.coordinates()[result[1].last].latitude(), 0.0)
        assertEquals(-122.526951, coordinates.coordinates()[result[1].last].longitude(), 0.0)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionClosure() {
        val expectedColor = Color.parseColor("#ffcc00")
        val resources = RouteLineColorResources.Builder()
            .alternativeRouteClosureColor(expectedColor)
            .build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            CLOSURE_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteClosureColor, result)
    }

    @Test
    fun buildWayPointFeatureCollection() {
        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val routesCount =
                JSONObject(this.firstArg<String>())
                    .getJSONArray("routes")
                    .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeId } returns "$it"
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }

        val route = loadRoute("multileg_route.json").toNavigationRoute()

        val result = MapboxRouteLineUtils.buildWayPointFeatureCollection(route)

        assertEquals(3, result.features()!!.size)
        assertEquals(
            Point.fromLngLat(-77.157347, 38.783004),
            result.features()!![0].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.167276, 38.775717),
            result.features()!![1].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.153468, 38.77091),
            result.features()!![2].geometry() as Point
        )
    }

    @Test
    fun getLayerVisibility() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val layer = mockk<Layer>(relaxed = true) {
            every { visibility } returns Visibility.VISIBLE
        }

        val style = mockk<Style> {
            every { getLayer("foobar") } returns layer
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertEquals(Visibility.VISIBLE, result)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun getLayerVisibility_whenLayerNotFound() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val style = mockk<Style> {
            every { getLayer("foobar") } returns null
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertNull(result)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun parseRoutePoints() {
        val route = loadRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.parseRoutePoints(route)!!

        assertEquals(128, result.flatList.size)
        assertEquals(15, result.nestedList.flatten().size)
        assertEquals(result.flatList[1].latitude(), result.flatList[2].latitude(), 0.0)
        assertEquals(result.flatList[1].longitude(), result.flatList[2].longitude(), 0.0)
        assertEquals(result.flatList[126].latitude(), result.flatList[127].latitude(), 0.0)
        assertEquals(result.flatList[126].longitude(), result.flatList[127].longitude(), 0.0)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveLowCongestionNumeric() {
        val lowCongestionNumeric = 4
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            lowCongestionNumeric,
            congestionResource
        )

        assertEquals(LOW_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveModerateCongestionNumeric() {
        val moderateCongestionNumeric = 45
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            moderateCongestionNumeric,
            congestionResource
        )

        assertEquals(MODERATE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveHeavyCongestionNumeric() {
        val heavyCongestionNumeric = 65
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            heavyCongestionNumeric,
            congestionResource
        )

        assertEquals(HEAVY_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveSevereCongestionNumeric() {
        val severeCongestionNumeric = 85
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            severeCongestionNumeric,
            congestionResource
        )

        assertEquals(SEVERE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveUnknownCongestionNumeric() {
        val unknownCongestionNumeric = null
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            unknownCongestionNumeric,
            congestionResource
        )

        assertEquals(UNKNOWN_CONGESTION_VALUE, result)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithCongestionNumeric() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture("route-with-congestion-numeric.json")
        val route = DirectionsRoute.fromJson(routeAsJson)
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                annotationProvider
            )

        MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)
        )
        MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)
        )

        assertEquals("low", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals("moderate", trafficExpressionData[1].trafficCongestionIdentifier)
        assertEquals("severe", trafficExpressionData[2].trafficCongestionIdentifier)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithNoRouteOptions() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-congestion-numeric-no-route-options.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteData(
                route,
                annotationProvider
            )

        assertEquals("unknown", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(21, trafficExpressionData.size)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithNoCongestionOrCongestionNumeric() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-no-congestion-annotation.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteData(
                route,
                annotationProvider
            )

        assertEquals("unknown", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(21, trafficExpressionData.size)
    }

    @Test
    fun getAnnotationProvider_whenNumericTrafficSource_matchesDistances() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-congestion-numeric.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)

        val result =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)

        assertEquals(
            route.legs()!!.first().annotation()!!.distance()!!.size,
            result(route.legs()!!.first()).size
        )
    }

    @Test
    fun getAnnotationProvider_whenNoRouteOptions() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-congestion-numeric-no-route-options.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val expected = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider

        val result =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        assertEquals(expected, result)
    }

    @Test
    fun routeHasRestrictions_whenHasRestrictions() {
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.routeHasRestrictions(route)

        assertTrue(result)
    }

    @Test
    fun routeHasRestrictions_whenNotHasRestrictions() {
        val route = loadRoute("motorway-with-road-classes-multi-leg.json")

        val result = MapboxRouteLineUtils.routeHasRestrictions(route)

        assertFalse(result)
    }

    @Test
    fun getRouteRestrictedSectionsExpressionData() {
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(40, result.size)
        assertTrue(result.first().isLegOrigin)
        assertFalse(result[16].isInRestrictedSection)
        assertTrue(result[17].isInRestrictedSection)
        assertFalse(result[18].isInRestrictedSection)
        assertTrue(result[19].isInRestrictedSection)
        assertFalse(result[20].isInRestrictedSection)
    }

    @Test
    fun getRouteRestrictedSectionsExpressionData_multiLegRoute() {
        val route = loadRoute("two-leg-route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(45, result.size)
        assertTrue(result.first().isLegOrigin)
        assertFalse(result[1].isInRestrictedSection)
        assertTrue(result[2].isInRestrictedSection)
        assertTrue(result[3].isInRestrictedSection)
        assertTrue(result[4].isInRestrictedSection)
        assertFalse(result[5].isInRestrictedSection)
        assertTrue(result[17].isLegOrigin)
        assertFalse(result[36].isInRestrictedSection)
        assertTrue(result[37].isInRestrictedSection)
        assertEquals(result[37].roadClass, "tertiary")
        assertFalse(result[38].isInRestrictedSection)
    }

    @Test
    fun `extractRouteData with null congestion provider`() {
        val route = loadRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { null }) {
            assertEquals(UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with empty congestion provider`() {
        val route = loadRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { emptyList() }) {
            assertEquals(UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with short congestion provider`() {
        val route = loadRoute("short_route.json")
        val extractedData = MapboxRouteLineUtils.extractRouteData(route) { leg ->
            val distance = requireNotNull(leg.annotation()?.distance()?.takeIf { it.size > 1 })
            List(distance.size - 1) { "low" }
        }
        for (index in 0 until extractedData.lastIndex) {
            assertEquals("low", extractedData[index].trafficCongestionIdentifier)
        }
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            extractedData.last().trafficCongestionIdentifier,
        )
    }

    @Test
    fun `getRoadClassArray when route has step intersections with incorrect geometry indexes`() {
        mockkStatic(Logger::class)
        every { Logger.e(any(), any()) } returns Unit
        val route = loadRoute("route-with-incorrect-geometry-indexes.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(0, result.size)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `featureCollectionHasProperty when FeatureCollection is null`() {
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            null,
            0,
            ""
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when features null`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns null
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            0,
            ""
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when features empty`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf()
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            0,
            ""
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when index equal to features size`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockk())
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            1,
            ""
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when index greater than features size`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockk())
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            5,
            ""
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when feature has property`() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns true
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            0,
            "someProperty"
        )

        assertTrue(result)
    }

    @Test
    fun getMatchingColors() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns true
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }
        val styleDescriptors = listOf(RouteStyleDescriptor("someProperty", 1, 2))

        val result = MapboxRouteLineUtils.getMatchingColors(
            mockFeatureCollection,
            styleDescriptors,
            4,
            5
        )

        assertEquals(1, result.first)
        assertEquals(2, result.second)
    }

    @Test
    fun `getMatchingColors when no match`() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }
        val styleDescriptors = listOf(RouteStyleDescriptor("someProperty", 1, 2))

        val result = MapboxRouteLineUtils.getMatchingColors(
            mockFeatureCollection,
            styleDescriptors,
            4,
            5
        )

        assertEquals(4, result.first)
        assertEquals(5, result.second)
    }

    @Test
    fun `getMatchingColors when feature collection is null`() {
        val styleDescriptors = listOf(RouteStyleDescriptor("someProperty", 1, 2))

        val result = MapboxRouteLineUtils.getMatchingColors(
            null,
            styleDescriptors,
            4,
            5
        )

        assertEquals(4, result.first)
        assertEquals(5, result.second)
    }

    @Test
    fun `getMatchingColors when route descriptors empty`() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns true
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }

        val result = MapboxRouteLineUtils.getMatchingColors(
            mockFeatureCollection,
            listOf(),
            4,
            5
        )

        assertEquals(4, result.first)
        assertEquals(5, result.second)
    }

    private fun <T> listElementsAreEqual(
        first: List<T>,
        second: List<T>,
        equalityFun: (T, T) -> Boolean
    ): Boolean {
        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (x, y) ->
            equalityFun(x, y)
        }
    }

    @Test
    fun generatePreRecordedPoints() = runBlockingTest {

        val route = loadRoute("temp-delete-me-route.json")
        val routeGeometry = route.completeGeometryToLineString()
        val newPoints = mutableListOf<Point>(routeGeometry.coordinates().first())

        val replayRouteMapper = ReplayRouteMapper()
        val replayData: List<ReplayEventBase> = replayRouteMapper.mapDirectionsRouteGeometry(route)

        replayData.forEach {
            if (it is ReplayEventUpdateLocation) {
                val thisPoint = Point.fromLngLat(it.location.lon, it.location.lat)
                if (newPoints.isEmpty()) {
                    newPoints.add(thisPoint)
                } else {
                    val dist = TurfMeasurement.distance(newPoints.last(), thisPoint, TurfConstants.UNIT_METERS)
                    if (dist >= 500.0) {
                        newPoints.add(thisPoint)
                    }
                }
            }
        }
        newPoints.forEach {
            println("Point.fromLngLat(${it.longitude()}, ${it.latitude()}),")
        }

    }

    @Test
    fun temp2() {
        val preRecordedPointsStartPoint = 73
        val locationPointCovered = 3
        val xPositionStart : Float= 845f
        val xPositionEnd: Float = 845f
        val xPositionDelta: Float = xPositionEnd - xPositionStart

        val yPositionStart: Float = 1020f
        val yPositionEnd: Float = 1005f
        val yPositionDelta = yPositionEnd - yPositionStart

        val granularLocationXDeltas: Float = xPositionDelta / locationPointCovered
        val granularLocationYDeltas: Float = yPositionDelta / locationPointCovered

        var xPos = xPositionStart
        var yPos = yPositionStart
        repeat(locationPointCovered + 1) { index ->
            println("Pair(Point.fromLngLat(${preRecordedPoints[preRecordedPointsStartPoint + index].longitude()}, ${preRecordedPoints[preRecordedPointsStartPoint + index].latitude()}), Pair(${xPos}f, ${yPos}f)),")
            //println("x = $xPos  y = $yPos")
            xPos += granularLocationXDeltas
            yPos += granularLocationYDeltas
        }
    }
//351268908143, 34.64010685015972]
    private val preRecordedPoints by lazy {
        listOf(
            // Point.fromLngLat(135.263263, 34.43838),
            // Point.fromLngLat(135.2592237703461, 34.44150361991096),
            Point.fromLngLat(135.26354888814373, 34.438500471880225),
            Point.fromLngLat(135.26729722216783, 34.43489747681673),
            Point.fromLngLat(135.27143269522125, 34.43169180777495),
            Point.fromLngLat(135.27550312908502, 34.428617005239126),
            Point.fromLngLat(135.2795732634925, 34.42554206785929),
            Point.fromLngLat(135.28364309848703, 34.422466995660905),
            Point.fromLngLat(135.28771263411198, 34.41939178866937),
            Point.fromLngLat(135.2917818704107, 34.41631644691015),
            Point.fromLngLat(135.296376, 34.413709),
            Point.fromLngLat(135.30153571191934, 34.415502500803086),
            Point.fromLngLat(135.30698698267128, 34.41623354196272),
            Point.fromLngLat(135.31254604370773, 34.4171395936453),
            Point.fromLngLat(135.31770275656007, 34.41913698290124),
            Point.fromLngLat(135.32209657672965, 34.42211206480083),
            Point.fromLngLat(135.3261666280631, 34.425452525714384),
            Point.fromLngLat(135.32978210516993, 34.42912703967447),
            Point.fromLngLat(135.33271005055866, 34.432921488449374),
            Point.fromLngLat(135.33541607553065, 34.436993669875044),
            Point.fromLngLat(135.3375814382039, 34.44120845855389),
            Point.fromLngLat(135.34233757614805, 34.443772467101724),
            Point.fromLngLat(135.34757166120423, 34.44538980221373),
            Point.fromLngLat(135.35172072901352, 34.448411693460926),
            Point.fromLngLat(135.35491999308022, 34.45231132854109),
            Point.fromLngLat(135.35781935472207, 34.4561750433119),
            Point.fromLngLat(135.36071005673364, 34.46024379720794),
            Point.fromLngLat(135.3638212126413, 34.46419186540004),
            Point.fromLngLat(135.367274, 34.467891),
            Point.fromLngLat(135.372093, 34.470181),
            Point.fromLngLat(135.37556553078954, 34.47393702541184),
            Point.fromLngLat(135.37763756696697, 34.478165135886364),
            Point.fromLngLat(135.37727658607497, 34.482674474856374),
            Point.fromLngLat(135.37846799417682, 34.48708282150821),
            Point.fromLngLat(135.37987166989663, 34.49167308855809),
            Point.fromLngLat(135.38125883185123, 34.496287127330405),
            Point.fromLngLat(135.38411024257616, 34.50033566876203),
            Point.fromLngLat(135.38836542646942, 34.5031906513975),
            Point.fromLngLat(135.3924609932627, 34.506199709364644),
            Point.fromLngLat(135.3972101816279, 34.5086433829396),
            Point.fromLngLat(135.4027267055564, 34.509666720826445),
            Point.fromLngLat(135.40787501454807, 34.51170531036246),
            Point.fromLngLat(135.41265382949538, 34.5138982810049),
            Point.fromLngLat(135.4166698623002, 34.51715286622886),
            Point.fromLngLat(135.42004521533246, 34.52091406849284),
            Point.fromLngLat(135.42372098475894, 34.524549437135775),
            Point.fromLngLat(135.42613014645548, 34.528744338318134),
            Point.fromLngLat(135.428662, 34.532777),
            Point.fromLngLat(135.431641957322, 34.536730710529525),
            Point.fromLngLat(135.43440708500913, 34.54061624668257),
            Point.fromLngLat(135.43693382241486, 34.544743206388254),
            Point.fromLngLat(135.43980844776218, 34.5487473008449),
            Point.fromLngLat(135.4437509959282, 34.55204672901478),
            Point.fromLngLat(135.4453917319862, 34.55643045317768),
            Point.fromLngLat(135.44884249890123, 34.56006836407924),
            Point.fromLngLat(135.452953, 34.563207),
            Point.fromLngLat(135.45539642245308, 34.56730267405715),
            Point.fromLngLat(135.45706721763298, 34.57168874601794),
            Point.fromLngLat(135.45859594523313, 34.576176275243675),
            Point.fromLngLat(135.45800853419814, 34.58073834209151),
            Point.fromLngLat(135.46075849587007, 34.58480882568937),
            Point.fromLngLat(135.46355467379263, 34.588697943636475),
            Point.fromLngLat(135.46332605505924, 34.5933506955903),
            Point.fromLngLat(135.462748, 34.597988),
            Point.fromLngLat(135.461395, 34.602538),
            Point.fromLngLat(135.45759276884212, 34.605769057839105),
            Point.fromLngLat(135.4532119777108, 34.608573951285244),
            Point.fromLngLat(135.44794307119042, 34.61045284132922),
            Point.fromLngLat(135.44252060051764, 34.61196288140486),
            Point.fromLngLat(135.43716885965827, 34.61344655043803),
            Point.fromLngLat(135.43541857873296, 34.617869471779855),
            Point.fromLngLat(135.43366052803458, 34.62238046669404),
            Point.fromLngLat(135.4320750816063, 34.62669964452119),
            Point.fromLngLat(135.434718, 34.630798),
            Point.fromLngLat(135.43422546876656, 34.63542926719152),
            Point.fromLngLat(135.4351268908143, 34.64010685015972),//
            Point.fromLngLat(135.43767350436244, 34.644094148606506),
            Point.fromLngLat(135.44034202514675, 34.64827498171898),
            Point.fromLngLat(135.442021, 34.652583),
            Point.fromLngLat(135.43772220525543, 34.65545767075316),
            Point.fromLngLat(135.43353995208128, 34.65855613855892),
            Point.fromLngLat(135.4297624373702, 34.66185494520802),
            Point.fromLngLat(135.42491943782437, 34.66418327103825),
            Point.fromLngLat(135.42147644754107, 34.66775765160598),
            Point.fromLngLat(135.4195353901103, 34.67207416532465),
            Point.fromLngLat(135.41764367699446, 34.67632796131387),
            Point.fromLngLat(135.4157477905149, 34.68059935614752),
            Point.fromLngLat(135.41385170843316, 34.68487072162431),
            Point.fromLngLat(135.41180633403846, 34.689051000154784),
            Point.fromLngLat(135.4085670017836, 34.692791000730736),
            Point.fromLngLat(135.404270772023, 34.695821784837534),
            Point.fromLngLat(135.3991044234251, 34.69760153842241),
            Point.fromLngLat(135.39349178318247, 34.697239280802066),
            Point.fromLngLat(135.3883840375837, 34.69519621571538),
            Point.fromLngLat(135.3829833028119, 34.69657143040748),
            Point.fromLngLat(135.37722733967075, 34.69664972496831),
            Point.fromLngLat(135.371554, 34.696653),
            Point.fromLngLat(135.36593296958566, 34.69719920699183),
            Point.fromLngLat(135.3610502018518, 34.69947474891815),
            Point.fromLngLat(135.3572323119788, 34.7027147418947),
            Point.fromLngLat(135.3533519043536, 34.70599524171251),
            Point.fromLngLat(135.34948350387725, 34.70922216606029),
            Point.fromLngLat(135.345449662829, 34.7124811215806),
            Point.fromLngLat(135.34034559842812, 34.71457425994662),
            Point.fromLngLat(135.3350472167275, 34.716153403764714),
            Point.fromLngLat(135.329447, 34.716838),
            Point.fromLngLat(135.323797380371, 34.715918074978),
            Point.fromLngLat(135.31840699243008, 34.714852860921994),
            Point.fromLngLat(135.31306830864108, 34.71379943256003),
            Point.fromLngLat(135.3074546944639, 34.71269502012801),
            Point.fromLngLat(135.3019416263332, 34.71167600316799),
            Point.fromLngLat(135.296405, 34.710871),
            Point.fromLngLat(135.2908352921629, 34.70974754427635),
            Point.fromLngLat(135.28526861044196, 34.70860456528489),
            Point.fromLngLat(135.27954719458808, 34.70792658266585),
            Point.fromLngLat(135.2745724594273, 34.70588858834762),
            Point.fromLngLat(135.27358603414842, 34.70134176784736),
            Point.fromLngLat(135.2732434287031, 34.69668412888626),
        )
    }

    private tailrec fun getFillPoint(startPoint: Point, endPoint: Point): Point {
        // rough estimate 60 mph is roughly 26 meters per second
        // the rational being route progress is every second
        val lowerDistThreshold = 200
        val distThreshold = 250
        val distance = TurfMeasurement.distance(startPoint, endPoint, TurfConstants.UNIT_METERS)

        return if (distance.toInt() in lowerDistThreshold..distThreshold) {
            endPoint
        } else {
            if (distance < 200) {
                val newEndpoint = TurfMeasurement.destination(endPoint, 49.0, TurfMeasurement.bearing(startPoint, endPoint), TurfConstants.UNIT_METERS)
                getFillPoint(startPoint, newEndpoint)
            } else {
                getFillPoint(startPoint, TurfMeasurement.midpoint(startPoint, endPoint))
            }
        }
    }
}
