package com.mapurr.com.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiManager private constructor() {

    private val httpApi: HttpApi

    init {
        val okHttpClient = OkHttpClient()
        val retrofit = Retrofit.Builder().baseUrl(BaseUrl).client(okHttpClient).addConverterFactory(
            GsonConverterFactory.create()).build()
        httpApi = retrofit.create(HttpApi::class.java)
    }

    companion object {
        const val BaseUrl = "https://maps.googleapis.com/maps/api/"
        private var apiManager: ApiManager? = null

        fun getHttpApi(): HttpApi {
            if (apiManager == null)
                apiManager = ApiManager()
            return apiManager!!.httpApi
        }
    }

}