package com.mapurr.com.model

data class PlaceInfoEntry(
    var icon: String? = null,
    var icon_background_color: String? = null,
    var name: String? = null,
    var place_id: String? = null,
    var business_status: String? = null,
    var rating: Int? = null,
    var geometry: Geometry? = null
)
