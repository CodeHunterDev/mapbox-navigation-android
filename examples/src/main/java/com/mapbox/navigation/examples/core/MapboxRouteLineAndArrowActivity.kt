package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToLineString
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityRoutelineExampleBinding
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.internal.locationsearch.CityHighwayMap
import com.mapbox.navigation.ui.maps.internal.locationsearch.CityHighwayMapApi
import com.mapbox.navigation.ui.maps.internal.locationsearch.LocationSearchUtil
import com.mapbox.navigation.ui.maps.internal.locationsearch.XB01PointToPixelMap
import com.mapbox.navigation.ui.maps.internal.locationsearch.XB03PointToPixelMap
import com.mapbox.navigation.ui.maps.internal.locationsearch.getCityHighwayMapData
import com.mapbox.navigation.ui.maps.internal.locationsearch.getCityHighwayMapImage
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * This class demonstrates the usage of the route line and route arrow API's. There is
 * boiler plate code for a basic navigation experience. The turn by turn navigation is simulated.
 * The route line and arrow specific code is indicated with inline comments.
 */
class MapboxRouteLineAndArrowActivity : AppCompatActivity(), OnMapLongClickListener {
    private val routeClickPadding = Utils.dpToPx(30f)
    private val ONE_HUNDRED_MILLISECONDS = 100
    private val mapboxReplayer = MapboxReplayer()
    private val replayRouteMapper = ReplayRouteMapper()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private var trafficGradientSoft = false

    private val viewBinding: LayoutActivityRoutelineExampleBinding by lazy {
        LayoutActivityRoutelineExampleBinding.inflate(layoutInflater)
    }

    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    private val locationComponent by lazy {
        viewBinding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    private val mapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    private val mapCamera by lazy {
        viewBinding.mapView.camera
    }

    private val chmApi: CityHighwayMapApi by lazy {
        CityHighwayMapApi(chmImageProvider = tmpCHMBitmapProvider).also {
            it.setMap(CityHighwayMap.XB01)
        }
    }

//    private val locationSearchTree by lazy {
//        LocationSearchTree<Supplier<Point>>().also {
//            val points = preRecordedPoints2.map {
//                Supplier<Point> { it }
//            }
//            it.addAll(points)
//        }
//    }

    // RouteLine: Route line related colors can be customized via the RouteLineColorResources.
    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .routeLineTraveledColor(Color.LTGRAY)
            .routeLineTraveledCasingColor(Color.GRAY)
            .build()
    }

    // RouteLine: Various route line related options can be customized here including applying
    // route line color customizations. If using the default colors the RouteLineColorResources
    // does not need to be set as seen here, the defaults will be used internally by the builder.
    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    // RouteLine: Additional route line options are available through the MapboxRouteLineOptions.
    // Notice here the withRouteLineBelowLayerId option. The map is made up of layers. In this
    // case the route line will be placed below the "road-label" layer which is a good default
    // for the most common Mapbox navigation related maps. You should consider if this should be
    // changed for your use case especially if you are using a custom map style.
    //
    // Also noteworthy is the 'withVanishingRouteLineEnabled' option. This feature will change
    // the color of the route line behind the puck during navigation. The color can be customized
    // using the RouteLineColorResources::routeLineTraveledColor and
    // RouteLineColorResources::routeLineTraveledCasingColor options. The color options support
    // an alpha value to render the line transparent which is the default.
    //
    // To use the vanishing route line feature it is also necessary to register an
    // OnIndicatorPositionChangedListener and a RouteProgressObserver. There may be reasons to use
    // a RouteProgressObserver even if you are not using the vanishing route line feature.
    // The OnIndicatorPositionChangedListener is only useful and required when enabling the
    // vanishing route line feature.
    //
    // Examples are below.
    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(false)
            //.displaySoftGradientForTraffic(trafficGradientSoft)
            .build()
    }

    // RouteLine: This class is responsible for rendering route line related mutations generated
    // by the MapboxRouteLineApi class.
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    // RouteLine: This class is responsible for generating route line related data which must be
    // rendered by the MapboxRouteLineView class in order to visualize the route line on the map.
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    // RouteArrow: This class is responsible for generating data related to maneuver arrows. The
    // data generated must be rendered by the MapboxRouteArrowView in order to apply mutations to
    // the map.
    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    // RouteArrow: Customization of the maneuver arrow(s) can be done using the
    // RouteArrowOptions. Here the above layer ID is used to determine where in the map layer
    // stack the arrows appear. Above the layer of the route traffic line is being used here. Your
    // use case may necessitate adjusting this to a different layer position.
    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    // RouteArrow: This class is responsible for rendering the arrow related mutations generated
    // by the MapboxRouteArrowApi class.
    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    var indexCounterZZ = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        init()

        viewBinding.moveDown.setOnClickListener {
            redCircleYPos += 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.moveLeft.setOnClickListener {
            redCircleXPos -= 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.moveRight.setOnClickListener {
            redCircleXPos += 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.moveUp.setOnClickListener {
            redCircleYPos -= 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.btnDo.setOnClickListener {
            // viewBinding.mapImage.invalidate()
            //
            // val mapImage = BitmapFactory.decodeResource(
            //     getResources(),
            //     R.drawable.xbo3
            // ).copy(Bitmap.Config.ARGB_8888, true)
            //
            //
            // val canvas = Canvas(mapImage)
            // val paint = Paint().apply {
            //     isAntiAlias = true
            //     color = Color.parseColor("#00ff0000")
            //     style = Paint.Style.FILL
            // }
            //
            // //val m: Matrix = viewBinding.mapImage.getImageMatrix()
            // //val drawableRect = RectF(0f, 0f, 606f, 540f)
            // //val viewRect = RectF(0f, 0f, bmWidth, bmHeight)
            // //m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
            //
            //
            // canvas.drawBitmap(mapImage, 0f, 0f, null)
            // canvas.drawCircle(redCircleXPos, (viewBinding.mapImage.height / 2).toFloat() + redCircleYPos, 50f, paint)
            // viewBinding.mapImage.setImageBitmap(mapImage)



            // if (redCircleXPos >= 700) {
            //     redCircleYPos -= 20f
            // }

            if (indexCounterZZ < interpolatedPixelMap.size) {
                updateCHMImage(
                    //redCircleXPos,
                    //redCircleYPos
                    interpolatedPixelMap[indexCounterZZ].second.first,
                    interpolatedPixelMap[indexCounterZZ].second.second
                )
                redCircleXPos = interpolatedPixelMap[indexCounterZZ].second.first
                redCircleYPos = interpolatedPixelMap[indexCounterZZ].second.second

                Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos  point= ${interpolatedPixelMap[indexCounterZZ].first}")

                indexCounterZZ++
            }


            // locationRecorderOn = !locationRecorderOn
            // if (!locationRecorderOn) {
            //     printLocationRecordings()
            // }
        }


        viewBinding.btnDimCHM.setOnClickListener {
            if (viewBinding.mapImage.isVisible) {
                viewBinding.mapImage.visibility = View.INVISIBLE
            } else {
                viewBinding.mapImage.visibility = View.VISIBLE
            }




        //     InternalJobControlFactory.createDefaultScopeJobControl().scope.launch {
        //         val minStart = async {
        //             fillerCoordinates.mapIndexed { index, point ->
        //                 Pair(index, TurfMeasurement.distance(point, Point.fromLngLat(135.256397, 34.443042), TurfConstants.UNIT_METERS))
        //             }.minByOrNull { it.second }
        //         }
        //
        //         val minEnd = async {
        //             fillerCoordinates.mapIndexed { index, point ->
        //                 Pair(index, TurfMeasurement.distance(point, Point.fromLngLat(135.293218, 34.415231), TurfConstants.UNIT_METERS))
        //             }.minByOrNull { it.second }
        //         }
        //
        //         val ms = minStart.await()
        //         val me = minEnd.await()
        //
        //         ifNonNull(ms, me) { m1, m2 ->
        //             val totalDist = TurfMeasurement.distance(fillerCoordinates[m1.first], fillerCoordinates[m2.first], TurfConstants.UNIT_METERS)
        //             val indexDiff = m2.first - m1.first // 255
        //             Log.e("foobar", "$m1, $m2, $indexDiff")
        //
        //             val y = 350
        //             var counter = 0
        //             for (i in (m1.first..m2.first)) {
        //                 Log.e("foobar", "${fillerCoordinates[i]} -- x=185 y=${y + counter++}")
        //
        //             }
        //         }
        //     }
        }

        viewBinding.startNavigation.visibility = View.VISIBLE
    }

    private val tmpCHMBitmapProvider = {
        viewBinding.mapImage.invalidate()

        BitmapFactory.decodeResource(
            resources,
            R.drawable.xb01
        ).copy(Bitmap.Config.ARGB_8888, true)
    }

    fun updateCHMImage(x: Float, y: Float) {
        viewBinding.mapImage.invalidate()

        val mapImage = BitmapFactory.decodeResource(
            resources,
            R.drawable.xb01
        ).copy(Bitmap.Config.ARGB_8888, true)


        val canvas = Canvas(mapImage)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#ffcc00")
            style = Paint.Style.FILL
        }

        // this was to make up for a mistake in the original y value
        //val yShim = 0//742

        canvas.drawBitmap(mapImage, 0f, 0f, null)
        canvas.drawCircle(x, y, 10f, paint)
        viewBinding.mapImage.setImageBitmap(mapImage)

        //Log.e("foobar", "imageHeight div2 is ${viewBinding.mapImage.height / 2} y = $y")
    }



    var redCircleYPos = 350f
    var redCircleXPos = 185f
    val bmWidth = 1080f
    val bmHeight = 1485f

    private fun init() {
        initGradientSelector()
        initNavigation()
        initStyle()
        initListeners()
        locationComponent.locationPuck = LocationPuck2D(
            null,
            ContextCompat.getDrawable(
                this@MapboxRouteLineAndArrowActivity,
                R.drawable.mapbox_navigation_puck_icon
            ),
            null,
            null
        )
    }

    private fun initGradientSelector() {
        viewBinding.gradientOptionHard.setOnClickListener {
            trafficGradientSoft = false
        }
        viewBinding.gradientOptionSoft.setOnClickListener {
            trafficGradientSoft = true
        }
    }

    // RouteLine: This is one way to keep the route(s) appearing on the map in sync with
    // MapboxNavigation. When this observer is called the route data is used to draw route(s)
    // on the map.
    private val routesObserver: RoutesObserver = RoutesObserver { result ->
        // RouteLine: wrap the DirectionRoute objects and pass them
        // to the MapboxRouteLineApi to generate the data necessary to draw the route(s)
        // on the map.
        val routeLines = result.routes.map { RouteLine(it, null) }

        routeLineApi.setRoutes(
            routeLines
        ) { value ->
            // RouteLine: The MapboxRouteLineView expects a non-null reference to the map style.
            // the data generated by the call to the MapboxRouteLineApi above must be rendered
            // by the MapboxRouteLineView in order to visualize the changes on the map.
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    // RouteLine: This listener is necessary only when enabling the vanishing route line feature
    // which changes the color of the route line behind the puck during navigation. If this
    // option is set to `false` (the default) in MapboxRouteLineOptions then it is not necessary
    // to use this listener.
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // RouteLine: This line is only necessary if the vanishing route line feature
        // is enabled.
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }
    }

    // RouteLine: Below is a demonstration of selecting different routes. On a map click, a call
    // will be made to look for a route line on the map based on the map touch point. If a route is
    // found and it is not already the primary route, the selected route will designated the primary
    // route and MapboxNavigation will be updated.
    private val mapClickListener = OnMapClickListener { point ->
        mapboxMap.getStyle()?.apply {
            // Since this listener is reacting to all map touches, if the primary and alternative
            // routes aren't visible it's assumed the touch isn't related to selecting an
            // alternative route.
            // val primaryLineVisibility = routeLineView.getPrimaryRouteVisibility(this)
            // val alternativeRouteLinesVisibility = routeLineView.getAlternativeRoutesVisibility(this)
            // if (
            //     primaryLineVisibility == Visibility.VISIBLE &&
            //     alternativeRouteLinesVisibility == Visibility.VISIBLE
            // ) {
            //     routeLineApi.findClosestRoute(
            //         point,
            //         mapboxMap,
            //         routeClickPadding
            //     ) { result ->
            //         result.onValue { value ->
            //             if (value.route != routeLineApi.getPrimaryRoute()) {
            //                 val reOrderedRoutes = routeLineApi.getRoutes()
            //                     .filter { it != value.route }
            //                     .toMutableList()
            //                     .also {
            //                         it.add(0, value.route)
            //                     }
            //                 mapboxNavigation.setRoutes(reOrderedRoutes)
            //             }
            //         }
            //     }
            // }

            Log.e("foobar", "touchpoint is $point")
            job.scope.launch(Dispatchers.Main) {
                //val start = System.currentTimeMillis()
                val nearestDef = async {
                    //locationSearchTree.getNearestNeighbor(point)
                    chmApi.getCityHighwayMapData(point)
                }
                nearestDef.await().fold({
                    Log.e("foobar", "error getting getNearestNeighbor ${it.errorMessage}")
                },{
                    Log.e("foobar", "nearest neighbor to touch point is ${it.pointPixelData.get()}")
                    highLightPointOnMap(it.pointPixelData.get())
                })

//                nearestDef.await()?.apply {
//                    Log.e("foobar", "my closest point = $this time take is ${System.currentTimeMillis() - start}")
//                    highLightPointOnMap(this.get())
//                }
            }

            // job.scope.launch(Dispatchers.Main) {
            //     val start2 = System.currentTimeMillis()
            //     getClosestPixelCoords(point)?.apply {
            //         Log.e("foobar", "getClosestPixelCoords point = $this time take is ${System.currentTimeMillis() - start2}")
            //     }
            // }
        }
        false
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)

        // The lines below are related to the navigation simulator.
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(4.0)
        mapboxReplayer.play()
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(locationMatcherResult.enhancedLocation)
            //updateCHM(Point.fromLngLat(locationMatcherResult.enhancedLocation.longitude, locationMatcherResult.enhancedLocation.latitude))
            //recordLocation(Point.fromLngLat(locationMatcherResult.enhancedLocation.longitude, locationMatcherResult.enhancedLocation.latitude))
            val currentLocPoint = Point.fromLngLat(locationMatcherResult.enhancedLocation.longitude, locationMatcherResult.enhancedLocation.latitude)

            // job.scope.launch(Dispatchers.Main) {
            //     getClosestPixelCoords(currentLocPoint)?.apply {
            //         updateCHMImage(this.first, this.second)
            //     }
            // }

            job.scope.launch(Dispatchers.Main) {
                //val start = System.currentTimeMillis()




                val nearestDef = async {
                    //locationSearchTree.getNearestNeighbor(currentLocPoint)
                    chmApi.getCityHighwayMapData(currentLocPoint)
                }
                nearestDef.await().fold({
                    Log.e("foobar", "*** ${it.errorMessage}")
                },{
                    highLightPointOnMap(it.pointPixelData.get())
                })

                val bitMapDef = async {
                    chmApi.getCityHighwayMapImage(currentLocPoint)
                }
                bitMapDef.await().apply {
                    this.fold( {
                        Log.e("foobar", "error getting CHM bitmap ${it.errorMessage}")
                    },{
                        viewBinding.mapImage.setImageBitmap(it.bitmap)
                    })
                }


//                nearestDef.await()?.apply {
//                    //Log.e("foobar", "my closest point = $this time take is ${System.currentTimeMillis() - start}")
//                    highLightPointOnMap(this.get())
//
//                    interpolatedPixelMap.firstOrNull { it.first == this.get() }?.apply {
//                        updateCHMImage(this.second.first, this.second.second)
//                    }
//                    //val misses = locationSearchTree.getDistanceCalculationCacheMisses()
//                    //val cacheMissDelta = misses - cacheMisses
//                    //cacheMisses = misses
//                    //Log.e("foobar", "cache hits = ${locationSearchTree.getDistanceCalculationCacheHits()}, miss delta is = $cacheMissDelta")
//                }
            }
        }
    }
    //var cacheMisses = 0

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .pitch(45.0)
                .zoom(12.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE,
            { style: Style ->
                // Get the last known location and move the map to that location.
                // mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                //     locationEngineCallback
                // )


                viewBinding.mapView.gestures.addOnMapLongClickListener(this)
                val route = loadRoute()
                val routeOrigin = preRecordedPoints2.last()// getRouteOrigin(route)
                val lastPoint = getRouteEnd(route)
                val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(14.0).build()
                mapboxMap.setCamera(cameraOptions)
                mapboxNavigation.setRoutes(listOf(route))

                initPointLayer(style)
                initPointHighlightLayer(style)
                initBoundsLayer(style)
                showBounds()

                //addPointToPixelMapPoints(style)
                //addPointToPixelMapPoints(recordedPoints)
                addPointToPixelMapPoints(preRecordedPoints2)

                val pointToHighlightIndex = 0
                highLightPointOnMap(preRecordedPoints2[pointToHighlightIndex])
                Log.e("foobar", "highlighted point is at ${preRecordedPoints2[pointToHighlightIndex]}")

                if (interpolatedPixelMap.isNotEmpty()) {
                    redCircleXPos = interpolatedPixelMap.last().second.first
                    redCircleYPos = interpolatedPixelMap.last().second.second
                    updateCHMImage(redCircleXPos, redCircleYPos)
                }
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        MapboxRouteLineAndArrowActivity::class.java.simpleName,
                        "Error loading map - error type: " +
                            "${eventData.type}, message: ${eventData.message}"
                    )
                }
            }
        )
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        viewBinding.startNavigation.visibility = View.GONE
        viewBinding.optionTrafficGradient.visibility = View.GONE
        val currentLocation = navigationLocationProvider.lastLocation
        //if (currentLocation != null) {
//            val originPoint = Point.fromLngLat(
//                currentLocation.longitude,
//                currentLocation.latitude
//            )
            val originPoint = Point.fromLngLat(139.796358940977, 35.63784439924975)//preRecordedPoints2.last()

            findRoute(originPoint, point)
            viewBinding.routeLoadingProgressBar.visibility = View.VISIBLE
        //}
        return false
    }

    fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback: RouterCallback = object : RouterCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
            mapboxNavigation.setRoutes(routes)
            if (routes.isNotEmpty()) {
                viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
                viewBinding.startNavigation.visibility = View.VISIBLE
            }
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    ONE_HUNDRED_MILLISECONDS.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(ONE_HUNDRED_MILLISECONDS.toLong())
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        viewBinding.startNavigation.setOnClickListener {
            val route = mapboxNavigation.getRoutes().firstOrNull()
            if (route != null) {
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.startTripSession()
                viewBinding.startNavigation.visibility = View.INVISIBLE
                locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)

                // RouteLine: Hiding the alternative routes when navigation starts.
                mapboxMap.getStyle()?.apply {
                    routeLineView.hideAlternativeRoutes(this)
                }

                startSimulation(route)
            }
        }
        viewBinding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }

    // Starts the navigation simulator
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData: List<ReplayEventBase> = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    override fun onStop() {
        super.onStop()
        locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    private val locationEngineCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: MapboxRouteLineAndArrowActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<MapboxRouteLineAndArrowActivity> by lazy {
            WeakReference(activity)
        }

        override fun onSuccess(result: LocationEngineResult?) {
            val location = result?.lastLocation
            val activity = activityRef.get()
            if (location != null && activity != null) {
                val point = Point.fromLngLat(location.longitude, location.latitude)
                val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                activity.mapboxMap.setCamera(cameraOptions)
                activity.navigationLocationProvider.changePosition(location, listOf(), null, null)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }


    fun getRouteOrigin(route: DirectionsRoute) =
        route.completeGeometryToLineString().coordinates().first()

    fun getRouteEnd(route: DirectionsRoute) =
        route.completeGeometryToLineString().coordinates().last()

    fun loadRoute(): DirectionsRoute {
        val routeAsJson = readRawFileText(this, R.raw.temp_delete_me_route)
        return DirectionsRoute.fromJson(routeAsJson)
    }

    fun readRawFileText(context: Context, res: Int): String =
        context.resources.openRawResource(res).bufferedReader().use { it.readText() }

    fun getRouteFillerCoordinates(route: DirectionsRoute): List<Point> {
        val distThreshold = 76
        val routeGeometry = route.completeGeometryToLineString()
        val newPoints = mutableListOf<Point>(routeGeometry.coordinates().first())
        var endPointIndex = 1
        while (endPointIndex < routeGeometry.coordinates().size) {

            val lastFillPoint = getFillPoint(newPoints.last(), routeGeometry.coordinates()[endPointIndex])
            newPoints.add(lastFillPoint)
            if (TurfMeasurement.distance(lastFillPoint, routeGeometry.coordinates()[endPointIndex], TurfConstants.UNIT_METERS) <= distThreshold) {
                endPointIndex++
            }
        }
        return newPoints
    }

    private tailrec fun getFillPoint(startPoint: Point, endPoint: Point): Point {
        // rough estimate 60 mph is roughly 26 meters per second
        // the rational being route progress is every second
        val distThreshold = 76
        return if (TurfMeasurement.distance(startPoint, endPoint, TurfConstants.UNIT_METERS) <= distThreshold) {
            endPoint
        } else {
            getFillPoint(startPoint, TurfMeasurement.midpoint(startPoint, endPoint))
        }
    }

    private val job by lazy {
        InternalJobControlFactory.createDefaultScopeJobControl()
    }

    private val interpolatedPixelMap: List<Pair<Point, Pair<Float, Float>>>  by lazy {
        val intermediatePoints = XB01PointToPixelMap.getPointCollections().flatten().distinct()
        LocationSearchUtil.interpolateScreenCoordinates(intermediatePoints)
        intermediatePoints.map {
            if (it.getChmCoordinates() == null) {
                Log.e("foobar", "chm coordinates null, are the first and last items KeyPoint?")
            }
            Pair(it.get(), it.getChmCoordinates()!!) // this shouldn't be null but I want to know if it is
        }
    }




    private val preRecordedPoints2 by lazy {
        listOf(
            Point.fromLngLat(139.7957892431928, 35.63947950042642),
            Point.fromLngLat(139.79278366760937, 35.6433180002178),
            Point.fromLngLat(139.7896157573235, 35.64731316188828),
            Point.fromLngLat(139.78615644387276, 35.65090638810382),
            Point.fromLngLat(139.783036, 35.653966)
        )
    }

//
    private val LINE_END_LAYER_ID = "DRAW_UTIL_LINE_END_LAYER_ID"
    private val LINE_END_SOURCE_ID = "DRAW_UTIL_LINE_END_SOURCE_ID"
    private fun initPointLayer(style: Style) {
        if (!style.styleSourceExists(LINE_END_SOURCE_ID)) {
            geoJsonSource(LINE_END_SOURCE_ID) {}.bindTo(style)
        }

        if (!style.styleLayerExists(LINE_END_LAYER_ID)) {
            CircleLayer(LINE_END_LAYER_ID, LINE_END_SOURCE_ID)
                .circleRadius(5.0)
                .circleOpacity(1.0)
                .circleColor(Color.BLACK)
                .bindTo(style)
        }
    }


    private val POINT_HIGHLIGHT_LAYER_ID = "POINT_HIGHLIGHT_LAYER_ID"
    private val POINT_HIGHLIGHT_SOURCE_ID = "POINT_HIGHLIGHT_SOURCE_ID"
    private fun initPointHighlightLayer(style: Style) {
        if (!style.styleSourceExists(POINT_HIGHLIGHT_SOURCE_ID)) {
            geoJsonSource(POINT_HIGHLIGHT_SOURCE_ID) {}.bindTo(style)
        }

        if (!style.styleLayerExists(POINT_HIGHLIGHT_LAYER_ID)) {
            CircleLayer(POINT_HIGHLIGHT_LAYER_ID, POINT_HIGHLIGHT_SOURCE_ID)
                .circleRadius(5.0)
                .circleOpacity(1.0)
                .circleColor(Color.RED)
                .bindTo(style)
        }
    }

    private val BOUNDS_LAYER = "BOUNDS_LAYER"
    private val BOUNDS_SOURCE = "BOUNDS_SOURCE"
    private fun initBoundsLayer(style: Style) {
        if (!style.styleSourceExists(BOUNDS_SOURCE)) {
            geoJsonSource(BOUNDS_SOURCE) {}.bindTo(style)
        }

        if (!style.styleLayerExists(BOUNDS_LAYER)) {
            LineLayer(BOUNDS_LAYER, BOUNDS_SOURCE)
                .lineColor(Color.BLUE)
                .lineWidth(5.0)
                .bindTo(style)
        }
    }

    private fun showBounds() {
        val feature = XB01PointToPixelMap.getBounds()

//        Feature.fromGeometry(LineString.fromLngLats(
//            listOf(
//                Point.fromLngLat(134.8722536, 35.0032252), // north west
//                Point.fromLngLat(135.7471644, 35.0032252), // north east
//                Point.fromLngLat(135.7471644, 34.253293), // south east
//                Point.fromLngLat(134.8722536, 34.253293), // south west
//                Point.fromLngLat(134.8722536, 35.0032252) // close to north west
//            )
//        ))
        (mapboxMap.getStyle()!!.getSource(BOUNDS_SOURCE) as GeoJsonSource).feature(feature)
    }

    private fun highLightPointOnMap(point: Point) {
        (mapboxMap.getStyle()!!.getSource(POINT_HIGHLIGHT_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(point))))
        }
    }

    private fun addPointToPixelMapPoints(points: List<Point>) {
        val features = points.map { Feature.fromGeometry(it) }

        (mapboxMap.getStyle()!!.getSource(LINE_END_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }

    private fun recordLocation(point: Point) {
        if (locationRecorderOn) {
            if (locationRecordings.isEmpty()) {
                locationRecordings.add(point)
            } else {
                val dist = TurfMeasurement.distance(locationRecordings.last(), point, TurfConstants.UNIT_METERS)
                if (dist >= 300.0) {
                    locationRecordings.add(point)
                }
            }
            updateRecordingsOnMap()
        }
    }

    private fun updateRecordingsOnMap() {
        val features = locationRecordings.map { Feature.fromGeometry(it) }
        (mapboxMap.getStyle()!!.getSource(LINE_END_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }
    var locationRecordings = mutableListOf<Point>()
    var locationRecorderOn = false

    private fun printLocationRecordings() {
        locationRecordings.forEach {
            Log.e("foobar", "Point.fromLngLat(${it.longitude()}, ${it.latitude()}),")
        }
    }

}
