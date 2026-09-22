package com.psrm.forms.psbdx.data.remote

import com.psrm.forms.psbdx.data.remote.dto.FormDto
import com.psrm.forms.psbdx.data.remote.dto.FormsFieldsPayload
import com.psrm.forms.psbdx.data.remote.dto.NewReplyRequest
import com.psrm.forms.psbdx.data.remote.dto.ReplyDto
import com.psrm.forms.psbdx.data.remote.dto.ResponseDto
import com.psrm.forms.psbdx.data.remote.dto.WpMeDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * NOTE ON SCOPE: the `psbdx-srm/v1/*` routes below are NOT part of the
 * current plugin (which drives its admin UI over admin-ajax.php, not a
 * REST namespace). They're the contract this app is written against —
 * a small `WP_REST_Controller` addition to the plugin (forms CRUD,
 * responses list/detail, reply-send) is the natural Task-1-adjacent
 * follow-up to make this app functional against a real site. Until
 * that controller exists, every call below will 404. `wp/v2/users/me`
 * is real WP core and works today.
 */
interface WordPressApi {

    @GET("wp-json/wp/v2/users/me?context=edit")
    suspend fun getCurrentUser(): Response<WpMeDto>

    // ── Forms ──
    @GET("wp-json/psbdx-srm/v1/forms")
    suspend fun listForms(): Response<List<FormDto>>

    @GET("wp-json/psbdx-srm/v1/forms/{id}")
    suspend fun getForm(@Path("id") id: Long): Response<FormDto>

    @POST("wp-json/psbdx-srm/v1/forms")
    suspend fun createForm(@Body form: FormDto): Response<FormDto>

    @PUT("wp-json/psbdx-srm/v1/forms/{id}/fields")
    suspend fun updateFormFields(
        @Path("id") id: Long,
        @Body payload: FormsFieldsPayload
    ): Response<FormDto>

    @DELETE("wp-json/psbdx-srm/v1/forms/{id}")
    suspend fun deleteForm(@Path("id") id: Long): Response<Unit>

    // ── Responses ──
    @GET("wp-json/psbdx-srm/v1/forms/{id}/responses")
    suspend fun listResponses(@Path("id") formId: Long): Response<List<ResponseDto>>

    @GET("wp-json/psbdx-srm/v1/responses/{id}")
    suspend fun getResponse(@Path("id") id: Long): Response<ResponseDto>

    @PATCH("wp-json/psbdx-srm/v1/responses/{id}/status")
    suspend fun updateResponseStatus(
        @Path("id") id: Long,
        @Body status: Map<String, String>
    ): Response<ResponseDto>

    @POST("wp-json/psbdx-srm/v1/responses/{id}/replies")
    suspend fun sendReply(
        @Path("id") id: Long,
        @Body body: NewReplyRequest
    ): Response<ReplyDto>
}
