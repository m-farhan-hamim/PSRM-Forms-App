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
    val order: Int,
    val choices: List<String>? = null
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

/**
 * The field types the mobile builder can create/edit — a deliberate subset
 * of the plugin's full field library (get_field_library() in
 * class-psbdx-srm-form-builder.php has 22 types total). Excluded here:
 * `grid`, `checkbox_grid`, `linear_scale` — each needs extra structured
 * config (matrix rows/columns, a numeric scale range) this simple
 * label+required(+choices) dialog doesn't have room for yet. Offering them
 * without that config would create a field the mobile app itself can't
 * finish setting up, so they're left for the PC editor for now rather than
 * shipped half-working.
 */
enum class PsrmFieldType(val key: String, val displayName: String, val needsChoices: Boolean = false) {
    NAME("name", "Name (First & Last)"),
    EMAIL("email", "Email"),
    MOBILE("mobile", "Mobile Number"),
    TEXT("text", "Text (Single Line)"),
    PARAGRAPH("paragraph", "Paragraph"),
    NUMBER("number", "Number"),
    SELECT("select", "Drop-down / Select", needsChoices = true),
    RADIO("radio", "Radio Buttons", needsChoices = true),
    CHECKBOX("checkbox", "Checkboxes", needsChoices = true),
    DATE("date", "Date"),
    TIME("time", "Time"),
    URL("url", "Website / URL"),
    CONSENT("consent", "Agreement / Consent"),
    TITLE("title", "Title / Heading"),
    SECTION("section", "Section Break"),
    ATTACHMENT("attachment", "Attachment"),
    REVIEW("review", "Review (Star Rating)");

    companion object {
        fun fromKey(key: String): PsrmFieldType? = entries.find { it.key == key }
    }
}
