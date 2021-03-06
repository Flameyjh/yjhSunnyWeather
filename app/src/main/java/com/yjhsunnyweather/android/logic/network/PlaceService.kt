package com.yjhsunnyweather.android.logic.network

import com.yjhsunnyweather.android.SunnyWeatherApplication
import com.yjhsunnyweather.android.logic.model.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

//定义一个用于访问地点信息API的Retrofit接口

interface PlaceService {
    @GET("v2/place?token=${SunnyWeatherApplication.TOKEN}&lang=zh_CN")
    fun searchPlaces(@Query("query") query: String): Call<PlaceResponse>
}