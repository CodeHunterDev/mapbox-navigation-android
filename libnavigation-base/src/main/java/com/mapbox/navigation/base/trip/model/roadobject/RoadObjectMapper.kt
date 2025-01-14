package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectLocation
import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.base.trip.model.roadobject.bridge.Bridge
import com.mapbox.navigation.base.trip.model.roadobject.custom.Custom
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing.RailwayCrossing
import com.mapbox.navigation.base.trip.model.roadobject.restrictedarea.RestrictedArea
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.Tunnel
import com.mapbox.navigation.base.utils.ifNonNull
import com.mapbox.navigator.AdminInfo
import com.mapbox.navigator.AmenityType
import com.mapbox.navigator.IncidentCongestion
import com.mapbox.navigator.IncidentImpact
import com.mapbox.navigator.IncidentInfo
import com.mapbox.navigator.IncidentType
import com.mapbox.navigator.RailwayCrossingInfo
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.ServiceAreaInfo
import com.mapbox.navigator.ServiceAreaType
import com.mapbox.navigator.TollCollectionInfo
import com.mapbox.navigator.TollCollectionType
import com.mapbox.navigator.TunnelInfo

internal typealias SDKTollCollectionType =
    com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollectionType

internal typealias SDKAmenity =
    com.mapbox.navigation.base.trip.model.roadobject.reststop.Amenity

internal typealias SDKAmenityType =
    com.mapbox.navigation.base.trip.model.roadobject.reststop.AmenityType

internal typealias SDKIncidentType =
    com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentType

internal typealias SDKIncidentInfo =
    com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentInfo

internal typealias SDKIncidentImpact =
    com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentImpact

internal typealias SDKIncidentCongestion =
    com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentCongestion

internal typealias SDKTunnelInfo =
    com.mapbox.navigation.base.trip.model.roadobject.tunnel.TunnelInfo

internal typealias SDKRailwayCrossingInfo =
    com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing.RailwayCrossingInfo

internal fun com.mapbox.navigator.RoadObject.mapToRoadObject(): RoadObject {
    val location = location.mapToRoadObjectLocation()
    val provider = provider.mapToRoadObjectProvider()
    return when (type) {
        RoadObjectType.INCIDENT ->
            Incident(
                id,
                metadata.incidentInfo.toIncidentInfo(),
                length,
                location,
                provider,
                this
            )
        RoadObjectType.TOLL_COLLECTION_POINT ->
            TollCollection(
                id,
                metadata.tollCollectionInfo.toTollCollectionType(),
                metadata.tollCollectionInfo.name,
                length,
                location,
                provider,
                this
            )
        RoadObjectType.BORDER_CROSSING ->
            CountryBorderCrossing(
                id,
                CountryBorderCrossingInfo(
                    metadata.borderCrossingInfo.from.toBorderCrossingAdminInfo(),
                    metadata.borderCrossingInfo.to.toBorderCrossingAdminInfo()
                ),
                length,
                location,
                provider,
                this
            )
        RoadObjectType.TUNNEL ->
            Tunnel(id, metadata.tunnelInfo.toTunnelInfo(), length, location, provider, this)
        RoadObjectType.RESTRICTED_AREA -> RestrictedArea(
            id,
            length,
            location,
            provider,
            this
        )
        RoadObjectType.SERVICE_AREA ->
            RestStop(
                id,
                metadata.serviceAreaInfo.toRestStopType(),
                metadata.serviceAreaInfo.name,
                metadata.serviceAreaInfo.amenities.toAmenities(),
                length,
                location,
                provider,
                this
            )
        RoadObjectType.BRIDGE -> Bridge(id, length, location, provider, this)
        RoadObjectType.CUSTOM -> Custom(id, length, location, provider, this)
        RoadObjectType.RAILWAY_CROSSING -> RailwayCrossing(
            id,
            metadata.railwayCrossingInfo.toRailwayCrossingInfo(),
            length,
            location,
            provider,
            this
        )
        else -> throw IllegalArgumentException("unsupported type: $type")
    }
}

private fun TunnelInfo.toTunnelInfo() =
    SDKTunnelInfo(name)

private fun AdminInfo.toBorderCrossingAdminInfo() =
    CountryBorderCrossingAdminInfo(
        code = iso_3166_1,
        codeAlpha3 = iso_3166_1_alpha3
    )

private fun TollCollectionInfo.toTollCollectionType() =
    when (type) {
        TollCollectionType.TOLL_BOOTH -> SDKTollCollectionType.TOLL_BOOTH
        TollCollectionType.TOLL_GANTRY -> SDKTollCollectionType.TOLL_GANTRY
    }

private fun ServiceAreaInfo.toRestStopType() =
    when (type) {
        ServiceAreaType.REST_AREA -> RestStopType.REST_AREA
        ServiceAreaType.SERVICE_AREA -> RestStopType.SERVICE_AREA
    }

private fun List<com.mapbox.navigator.Amenity>.toAmenities(): List<SDKAmenity> =
    map { amenity ->
        SDKAmenity(
            type = amenity.type.toAmenityType(),
            name = amenity.name,
            brand = amenity.brand
        )
    }

private fun AmenityType.toAmenityType(): String =
    when (this) {
        AmenityType.ATM -> SDKAmenityType.ATM
        AmenityType.BABY_CARE -> SDKAmenityType.BABY_CARE
        AmenityType.COFFEE -> SDKAmenityType.COFFEE
        AmenityType.ELECTRIC_CHARGING_STATION -> SDKAmenityType.ELECTRIC_CHARGING_STATION
        AmenityType.FAX -> SDKAmenityType.FAX
        AmenityType.FACILITIES_FOR_DISABLED -> SDKAmenityType.FACILITIES_FOR_DISABLED
        AmenityType.GAS_STATION -> SDKAmenityType.GAS_STATION
        AmenityType.HOTEL -> SDKAmenityType.HOTEL
        AmenityType.HOTSPRING -> SDKAmenityType.HOTSPRING
        AmenityType.INFO -> SDKAmenityType.INFO
        AmenityType.POST -> SDKAmenityType.POST
        AmenityType.PICNIC_SHELTER -> SDKAmenityType.PICNIC_SHELTER
        AmenityType.RESTAURANT -> SDKAmenityType.RESTAURANT
        AmenityType.SHOP -> SDKAmenityType.SHOP
        AmenityType.SHOWER -> SDKAmenityType.SHOWER
        AmenityType.SNACK -> SDKAmenityType.SNACK
        AmenityType.TELEPHONE -> SDKAmenityType.TELEPHONE
        AmenityType.TOILET -> SDKAmenityType.TOILET
        AmenityType.UNDEFINED -> SDKAmenityType.UNDEFINED
    }

private fun IncidentInfo.toIncidentInfo() =
    SDKIncidentInfo(
        id,
        type.toIncidentType(),
        impact.toIncidentImpact(),
        congestion?.toIncidentCongestion(),
        roadClosed,
        creationTime,
        startTime,
        endTime,
        description,
        subType,
        subTypeDescription,
        alertcCodes,
        iso_3166_1_alpha2,
        iso_3166_1_alpha3,
        lanesBlocked,
        longDescription,
        lanesClearDesc,
        numLanesBlocked,
        affectedRoadNames,
    )

private fun IncidentType.toIncidentType(): Int =
    when (this) {
        IncidentType.ACCIDENT -> SDKIncidentType.ACCIDENT
        IncidentType.CONGESTION -> SDKIncidentType.CONGESTION
        IncidentType.CONSTRUCTION -> SDKIncidentType.CONSTRUCTION
        IncidentType.DISABLED_VEHICLE -> SDKIncidentType.DISABLED_VEHICLE
        IncidentType.LANE_RESTRICTION -> SDKIncidentType.LANE_RESTRICTION
        IncidentType.MASS_TRANSIT -> SDKIncidentType.MASS_TRANSIT
        IncidentType.MISCELLANEOUS -> SDKIncidentType.MISCELLANEOUS
        IncidentType.OTHER_NEWS -> SDKIncidentType.OTHER_NEWS
        IncidentType.PLANNED_EVENT -> SDKIncidentType.PLANNED_EVENT
        IncidentType.ROAD_CLOSURE -> SDKIncidentType.ROAD_CLOSURE
        IncidentType.ROAD_HAZARD -> SDKIncidentType.ROAD_HAZARD
        IncidentType.WEATHER -> SDKIncidentType.WEATHER
    }

private fun IncidentCongestion?.toIncidentCongestion() =
    ifNonNull(this) { congestion ->
        SDKIncidentCongestion(congestion.value)
    }

private fun IncidentImpact.toIncidentImpact(): String =
    when (this) {
        IncidentImpact.UNKNOWN -> SDKIncidentImpact.UNKNOWN
        IncidentImpact.CRITICAL -> SDKIncidentImpact.CRITICAL
        IncidentImpact.MAJOR -> SDKIncidentImpact.MAJOR
        IncidentImpact.MINOR -> SDKIncidentImpact.MINOR
        IncidentImpact.LOW -> SDKIncidentImpact.LOW
    }

private fun RailwayCrossingInfo.toRailwayCrossingInfo() =
    SDKRailwayCrossingInfo()
