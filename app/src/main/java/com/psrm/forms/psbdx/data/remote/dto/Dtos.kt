package com.psrm.forms.psbdx.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** GET /wp-json/wp/v2/users/me — used right after login to confirm the
 *  Application Password works and to read the display name + roles for
 *  the header and for gating the CRUD/response screens by capability. */
@JsonClass(generateAdapter = true)
data class WpMeDto(
    val id: Long,
    val name: String,
    val slug: String,
    val roles: List<String> = emptyList(),
    val capabilities: Map<String, Boolean> = emptyMap(),
    @Json(name = "avatar_urls") val avatarUrls: Map<String, String>? = null
)

/** A form, as exposed by the plugin's own REST namespace
 *  (`psbdx-srm/v1/forms`) — mirrors what the PC Form Builder edits. */
@JsonClass(generateAdapter = true)
data class FormDto(
    val id: Long,
    val title: String,
    val status: String,
    val shortcode: String,
    @Json(name = "share_url") val shareUrl: String,
    @Json(name = "response_count") val responseCount: Int,
    val fields: List<FieldDto> = emptyList(),
    @Json(name = "modified_at") val modifiedAt: String
)

@JsonClass(generateAdapter = true)
data class FieldDto(
    val id: String,
    val type: String,
    val label: String,
    val required: Boolean,
    val handle: String?,
    val order: Int
)

@JsonClass(generateAdapter = true)
data class FormsFieldsPayload(
    val fields: List<FieldDto>
)

@JsonClass(generateAdapter = true)
data class ResponseDto(
    val id: Long,
    @Json(name = "form_id") val formId: Long,
    @Json(name = "ticket_id") val ticketId: String,
    val status: String,
    @Json(name = "reporter_name") val reporterName: String?,
    @Json(name = "reporter_email") val reporterEmail: String?,
    @Json(name = "submitted_at") val submittedAt: String,
    val answers: Map<String, String> = emptyMap(),
    @Json(name = "replies_enabled") val repliesEnabled: Boolean,
    val replies: List<ReplyDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReplyDto(
    val id: Long,
    @Json(name = "author_name") val authorName: String,
    val message: String,
    @Json(name = "sent_at") val sentAt: String,
    @Json(name = "is_from_agent") val isFromAgent: Boolean
)

@JsonClass(generateAdapter = true)
data class NewReplyRequest(
    val message: String,
    @Json(name = "notify_email") val notifyEmail: Boolean = true
)
