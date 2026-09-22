package com.psrm.forms.psbdx.domain.model

/**
 * The authenticated WordPress user — surfaced in the app header/toolbar
 * per spec so it's always visible which site + account is active.
 */
data class WpSession(
    val siteUrl: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val roles: List<String>,
    val canManageForms: Boolean,
    val canViewResponses: Boolean
)

data class PsrmForm(
    val id: Long,
    val title: String,
    val status: String,          // publish | draft | private
    val shortcode: String,       // e.g. [psbdx_report id="42"]
    val shareUrl: String,        // direct popup-link URL
    val responseCount: Int,
    val fields: List<PsrmField>,
    val modifiedAt: String
)

data class PsrmField(
    val id: String,
    val type: String,
    val label: String,
    val required: Boolean,
    val handle: String?,
    val order: Int
) {
    /** Mirrors the PC editor's duplicateField(): new id, "(Copy)" label, re-derived handle. */
    fun duplicated(newId: String): PsrmField = copy(
        id = newId,
        label = "$label (Copy)",
        handle = handle?.let { "${it}_${newId.takeLast(4)}" }
    )
}

data class PsrmResponse(
    val id: Long,
    val formId: Long,
    val ticketId: String,
    val status: String,          // open | in_progress | solved
    val reporterName: String?,
    val reporterEmail: String?,
    val submittedAt: String,
    val answers: Map<String, String>,
    val repliesEnabled: Boolean,
    val replies: List<PsrmReply>
)

data class PsrmReply(
    val id: Long,
    val authorName: String,
    val message: String,
    val sentAt: String,
    val isFromAgent: Boolean
)
