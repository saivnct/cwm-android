package com.lgt.cwm.http

import com.lgt.cwm.http.APIRoute.AUTH
import com.lgt.cwm.http.APIRoute.LOGIN
import com.lgt.cwm.http.APIRoute.PROFILE
import com.lgt.cwm.http.response.BaseResponse
import com.lgt.cwm.http.response.TestPlan
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by giangtpu on 6/29/22.
 */
interface ApiService {

    //region LOGIN
    @FormUrlEncoded
    @POST(LOGIN)
    suspend fun loginAPI(
            @Field(FormDataField.email) email: String,
            @Field(FormDataField.password) password: String,
    ): BaseResponse

    @GET(AUTH)
    suspend fun authAPI(): BaseResponse
    //endregion

    //region PROFIDE
    @GET(PROFILE)
    suspend fun profiledAPI(): BaseResponse

    @Multipart
    @POST(PROFILE)
    suspend fun uploadProfileAPI(@Part(FormDataField.name) name: RequestBody,
                                 @Part(FormDataField.phone) phone: RequestBody,
                                 @Part(FormDataField.id_card) idCard: RequestBody?,
                                 @Part imgFront: MultipartBody.Part?,
                                 @Part imgBack: MultipartBody.Part?,
                                 @Part imgSelfie: MultipartBody.Part?): BaseResponse
    //endregion


    //region TEST
    @GET("googlecodelabs/kotlin-coroutines/master/advanced-coroutines-codelab/sunflower/src/main/assets/plants.json")
    suspend fun test(): List<TestPlan>
    //endregion
}