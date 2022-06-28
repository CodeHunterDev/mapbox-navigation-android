package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc

object XB01PointToPixelMap {
    // Tokyo

    // CHM bounds
    /*
     Point.fromLngLat(139.4967988, 35.9198808), // north west
     Point.fromLngLat(140.121725, 35.9198808), // north east
     Point.fromLngLat(140.121725, 35.3366116), // south east
     Point.fromLngLat(139.4967988, 35.3366116), // south west
     */

    fun getBounds(): Feature {
        return Feature.fromGeometry(
            LineString.fromLngLats(
            listOf(
                Point.fromLngLat(139.4967988, 35.9198808), // north west
                Point.fromLngLat(140.121725, 35.9198808), // north east
                Point.fromLngLat(140.121725, 35.3366116), // south east
                Point.fromLngLat(139.4967988, 35.3366116), // south west
                Point.fromLngLat(139.4967988, 35.9198808) // close to north west
            )
        ))
    }

    fun getPointCollections(): List<List<EnhancedPoint>> = listOf(
        pointToPixelForInterpolation,
    )

    val pointToPixelForInterpolation: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.791604, 35.518599), Pair(900f, 1285f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.79687440512663, 35.51668904475685)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.80152965349603, 35.513964812634036)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.80583687545993, 35.5110305352397)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8101437826156, 35.508096104758536)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.81445037500376, 35.50516152121836)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.818756652665, 35.50222678464692)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8230626156401, 35.49929189507197)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.82736826396967, 35.496356852521274)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.83167359769445, 35.49342165702257)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.83598280442143, 35.49048773715759)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84029767860105, 35.487555704680986)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84461223792735, 35.484623518616466)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84892648244087, 35.48169117899189)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.85324041218226, 35.478758685835054)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.85755402719204, 35.4758260391738)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8618673275108, 35.47289323903596)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8661803131791, 35.46996028544934)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87049298423747, 35.46702717844176)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87480375713338, 35.46409237605334)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87913907581674, 35.46119533653849)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.88346225402267, 35.45829623530968)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.88775526208119, 35.455364299198386)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8920479572645, 35.45243221111089)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.89634033961306, 35.44949997107472)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.90063240916737, 35.44656757911739)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.90492416596788, 35.443635035266446)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9092156100551, 35.44070233954942)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.91374762703344, 35.437950361185365)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.918648196875, 35.43588061367041)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.92317650147504, 35.432935750606745)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9270987183858, 35.42941903824267)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9308763029487, 35.426008200692344)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9348705583603, 35.42284997220032)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.93959931928333, 35.42021983323077)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.94449083381068, 35.41807310406041)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.94943350807029, 35.415856306286145)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.954532, 35.4136)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.959174, 35.410904)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96294117019883, 35.407493947671846)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96582, 35.403493)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96764933355004, 35.39901715803431)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96868785032123, 35.39456771258702)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97048852114594, 35.39012845821664)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97344676560846, 35.38615831280797)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9767107385583, 35.382391349153)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97919834597246, 35.378154406386415)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.98118778952576, 35.373797034390975)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.984449, 35.369946)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.98905800044037, 35.367266667047474)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9943627921376, 35.36577622893744)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.999428, 35.363816)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.00364590633762, 35.360847727715175)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0083501827117, 35.35809595186307)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.01377373520697, 35.3568093052269)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.01958725496127, 35.35706012374683)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.02532277569773, 35.35773140742848)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0309116535604, 35.357863538690204)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.03654699750254, 35.35771762176733)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.04205179498572, 35.35855422244616)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.04763354362038, 35.35921599860255)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.053327, 35.359233)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.05901440483842, 35.35912474467167)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.06459997307456, 35.3590512193889)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.07033924321615, 35.35908682881253)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.07588275326572, 35.3597574449574)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.08128673791177, 35.36101634155877)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0868933786049, 35.36218333129008)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.09254472171568, 35.362697566347705)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.09830325764898, 35.36259580629173)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.10405092649754, 35.362314976329664)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.10960146567032, 35.362074640943234)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.11518433330278, 35.36190900050119)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(140.122126, 35.363022), Pair(1090f,1470f))
        )
    }
}
