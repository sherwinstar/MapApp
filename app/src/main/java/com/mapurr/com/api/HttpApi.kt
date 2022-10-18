package com.mapurr.com.api

import com.google.android.gms.maps.model.LatLng
import com.mapurr.com.model.PlaceInfoEntry
import com.mapurr.com.model.PlaceResultEntry
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface HttpApi {
    @GET("place/nearbysearch/json")
    fun getPlaces(@Query("keyword") keyword: String, @Query("location") location: String, @Query("key") key: String, @Query("radius") radius: Int): Call<PlaceResultEntry<Array<PlaceInfoEntry>>>

}