package com.lgt.cwm.business.account

import android.graphics.Bitmap
import com.lgt.cwm.http.FormDataField
import com.lgt.cwm.http.HttpClient
import com.lgt.cwm.http.request.APIUploadProfile
import com.lgt.cwm.http.response.BaseResponse
import com.lgt.cwm.http.response.TestPlan
import com.lgt.cwm.util.Result
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 6/29/22.
 */
@Singleton
class AccountHttpDataSource @Inject constructor(val httpClient: HttpClient){


    suspend fun uploadProfile(apiUploadProfile: APIUploadProfile): Result<BaseResponse>{
        val result = try {
            val namePart = RequestBody.create(MultipartBody.FORM, apiUploadProfile.name)
            val phonePart = RequestBody.create(MultipartBody.FORM, apiUploadProfile.phone)

            var idCardPart: RequestBody? = null
            var idFrontPart: MultipartBody.Part? = null
            var idBackPart: MultipartBody.Part? = null
            var selfiePart: MultipartBody.Part? = null
            apiUploadProfile.idCard?.let {
                idCardPart = RequestBody.create(MultipartBody.FORM, apiUploadProfile.idCard)
            }
            apiUploadProfile.idFront?.let {
                val bos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                idFrontPart = MultipartBody.Part.createFormData(
                    FormDataField.id_front, "idFront_${System.currentTimeMillis()}", RequestBody.create(
                    MultipartBody.FORM, bos.toByteArray()))
            }

            apiUploadProfile.idBack?.let {
                val bos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                idBackPart = MultipartBody.Part.createFormData(FormDataField.id_back, "idBack_${System.currentTimeMillis()}", RequestBody.create(
                    MultipartBody.FORM, bos.toByteArray()))
            }
            apiUploadProfile.selfie?.let {
                val bos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                selfiePart = MultipartBody.Part.createFormData(FormDataField.selfie, "selfie_${System.currentTimeMillis()}", RequestBody.create(
                    MultipartBody.FORM, bos.toByteArray()))
            }

            val baseResponse = httpClient.apiService.uploadProfileAPI(namePart, phonePart,
                idCardPart, idFrontPart, idBackPart, selfiePart)

            Result.Success(baseResponse)
        } catch(e: Exception) {
            Result.Error(Exception("Network request failed"))
        }

        return result

    }


    suspend fun testApi(): Result<List<TestPlan>>{
        val result = try {
            val listTestPlan = httpClient.apiService.test()
            Result.Success(listTestPlan)
        } catch(e: Exception) {
            Result.Error(Exception("Network request failed"))
        }

        return result

    }
}