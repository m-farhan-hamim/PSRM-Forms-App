package com.psrm.forms.psbdx.data.repository

import com.psrm.forms.psbdx.data.local.ResponseDao
import com.psrm.forms.psbdx.data.local.ResponseEntity
import com.psrm.forms.psbdx.data.remote.WordPressApi
import com.psrm.forms.psbdx.data.remote.dto.NewReplyRequest
import com.psrm.forms.psbdx.data.remote.dto.ReplyDto
import com.psrm.forms.psbdx.domain.model.PsrmReply
import com.psrm.forms.psbdx.domain.model.PsrmResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResponsesRepository(
    private val api: WordPressApi,
    private val responseDao: ResponseDao
) {
    private val moshi = Moshi.Builder().build()
    private val replyListType = Types.newParameterizedType(List::class.java, ReplyDto::class.java)
    private val replyListAdapter = moshi.adapter<List<ReplyDto>>(replyListType)
    private val answersType = Types.newParameterizedType(
        Map::class.java, String::class.java, String::class.java
    )
    private val answersAdapter = moshi.adapter<Map<String, String>>(answersType)

    fun observeResponses(formId: Long): Flow<List<PsrmResponse>> =
        responseDao.observeForForm(formId).map { list -> list.map { it.toDomain() } }

    suspend fun refresh(formId: Long): Result<Unit> = runCatching {
        val response = api.listResponses(formId)
        if (!response.isSuccessful) error("Failed to load responses (HTTP ${response.code()})")
        responseDao.upsertAll(response.body().orEmpty().map { dto ->
            ResponseEntity(
                id = dto.id,
                formId = dto.formId,
                ticketId = dto.ticketId,
                status = dto.status,
                reporterName = dto.reporterName,
                reporterEmail = dto.reporterEmail,
                submittedAt = dto.submittedAt,
                answersJson = answersAdapter.toJson(dto.answers),
                repliesEnabled = dto.repliesEnabled,
                repliesJson = replyListAdapter.toJson(dto.replies)
            )
        })
    }

    /** Sends a reply to the respondent — surfaced in the app per spec
     *  ("send email or notification replies... directly through the app"),
     *  and mirrors the notify-by-email behavior the plugin already applies
     *  to agent replies sent from the WP admin. */
    suspend fun sendReply(responseId: Long, message: String, notifyEmail: Boolean): Result<PsrmReply> =
        runCatching {
            val response = api.sendReply(responseId, NewReplyRequest(message, notifyEmail))
            if (!response.isSuccessful) error("Reply failed to send (HTTP ${response.code()})")
            response.body()!!.toDomain()
        }

    private fun ResponseEntity.toDomain(): PsrmResponse = PsrmResponse(
        id = id,
        formId = formId,
        ticketId = ticketId,
        status = status,
        reporterName = reporterName,
        reporterEmail = reporterEmail,
        submittedAt = submittedAt,
        answers = answersAdapter.fromJson(answersJson).orEmpty(),
        repliesEnabled = repliesEnabled,
        replies = replyListAdapter.fromJson(repliesJson).orEmpty().map { it.toDomain() }
    )

    private fun ReplyDto.toDomain(): PsrmReply = PsrmReply(id, authorName, message, sentAt, isFromAgent)
}
