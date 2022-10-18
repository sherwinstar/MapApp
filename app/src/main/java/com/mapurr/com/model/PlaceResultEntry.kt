package com.mapurr.com.model

data class PlaceResultEntry<T>(
    var status :String? = null,
    var results: T? = null
)
