package com.lgt.cwm.http

import android.content.Context
import com.google.gson.GsonBuilder
import com.lgt.cwm.db.MyPreference
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 10/14/20.
 */
@Singleton
class HttpClient @Inject constructor(@ApplicationContext private val context: Context,
                                     private val myPreference: MyPreference,
                                     private val debugConfig: DebugConfig) {

    val TAG = HttpClient::class.simpleName.toString()
//    val BASE_URL = "https://api.cwm.com/"
    val BASE_URL = "https://raw.githubusercontent.com/"

    //region Cookie Manager
    /**
     * Add cookie to header
     */
    private val addCookiesInterceptor: Interceptor by lazy {
        Interceptor {chain ->
            val builder = chain.request().newBuilder()
            if(chain.request().url().toString() != BASE_URL + APIRoute.LOGIN){
                val cookieSet = myPreference.getCookies()
                if(cookieSet.isNotEmpty()){
                    for(cookie in cookieSet){
                        builder.addHeader("Cookie",cookie)
                    }
                }
                debugConfig.log(TAG,"addCookiesInterceptor $cookieSet")
            }
            chain.proceed(builder.build())
        }
    }

    private val addJsonAcceptInterceptor : Interceptor by lazy {
        Interceptor {
            val builder = it.request().newBuilder()
            builder.addHeader("Accept","application/json")
            it.proceed(builder.build())
        }
    }

    private val addTokenInterceptor: Interceptor by lazy {
        Interceptor {chain ->
            val builder = chain.request().newBuilder()
            if(chain.request().url().toString() != BASE_URL + APIRoute.LOGIN){
                val token = myPreference.getToken()
                if(!token.isNullOrEmpty()){
                    builder.addHeader("Authorization", "Bearer $token")
                }
                debugConfig.log(TAG,"addTokenInterceptor : Bearer $token")
            }
            chain.proceed(builder.build())
        }
    }

    /**
     * Receive cookie from header and save in share preference
     */
    private val receivedCookiesInterceptor: Interceptor by lazy {
        Interceptor {chain->
            val originalResponse: Response = chain.proceed(chain.request())

            if(originalResponse.headers("Set-Cookie").isNotEmpty()
                && chain.request().url().toString() == (BASE_URL + APIRoute.LOGIN)){
                val cookies = myPreference.getCookies()
                for(header in originalResponse.headers("Set-Cookie")){
                    cookies.clear()
                    cookies.add(header)
                }
                debugConfig.log(TAG,"receivedCookiesInterceptor $cookies")
                myPreference.setCookies(cookies)
            }
            originalResponse
        }
    }

    /**
     * Force caching intercept
     */
    private val forceCacheInterceptor: Interceptor by lazy {
        Interceptor {
            val response = it.proceed(it.request())
            response.newBuilder().header("Cache-Control", "max-age=" + (60 * 60 * 24 * 365)).build()
        }
    }
    //endregion Cookie Manager

    //region Retrofit API
    /**
     * Retrofit API Service for HTTP Network call
     */
    val apiService: ApiService by lazy {
        // enable show log OkHttp
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE

        val client: OkHttpClient = OkHttpClient.Builder()
//                .addInterceptor(addTokenInterceptor)
//                .addInterceptor(addJsonAcceptInterceptor)
                .addInterceptor(loggingInterceptor)
//                .addInterceptor(addCookiesInterceptor)
//                .addInterceptor(receivedCookiesInterceptor)
                .build()

        val getRetrofit: Retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
                .build()
        getRetrofit.create(ApiService::class.java)
    }
    //endregion retrofit API

}

