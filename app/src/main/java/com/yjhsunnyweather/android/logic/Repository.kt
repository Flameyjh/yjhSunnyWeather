package com.yjhsunnyweather.android.logic

import androidx.lifecycle.liveData
import com.yjhsunnyweather.android.logic.dao.PlaceDao
import com.yjhsunnyweather.android.logic.model.Place
import com.yjhsunnyweather.android.logic.model.Weather
import com.yjhsunnyweather.android.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext

//Result.success()和Result.failure()是kotlin内置的包装函数
//Result.success(XX)包装返回的结果就是函数返回的livedata

object Repository {

    //数据持久化
    //其实这里不标准，即使是对SharedPreference文件进行读写操作，也不建议在主线程中。最好开启一个线程来做
    fun savePlace(place: Place) = PlaceDao.savePlace(place)
    fun getSavedPlace() = PlaceDao.getSavePlace()
    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

    fun searchPlaces(query: String) = fire(Dispatchers.IO) {
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok") {
            val place = placeResponse.places
            Result.success(place)
        } else {
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    //async函数必须在协程里调用
    fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
            }
            val deferredDaily = async {
                SunnyWeatherNetwork.getDailyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDaily.await()
            if (dailyResponse.status == "ok" && realtimeResponse.status == "ok") {
                val weather =
                    Weather(realtimeResponse.result.realtime, dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(
                    RuntimeException(
                        "realtime response status is ${realtimeResponse.status}" +
                                "daily response status is ${dailyResponse.status}"
                    )
                )
            }
        }
    }

    //fire函数把处理网络请求的try-catch提出来了，免得每次请求都要写一遍
    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }
}