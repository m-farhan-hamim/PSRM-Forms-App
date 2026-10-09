package com.psrm.forms.psbdx.data.repository

import com.psrm.forms.psbdx.data.local.FormDao
import com.psrm.forms.psbdx.data.local.FormEntity
import com.psrm.forms.psbdx.data.remote.WordPressApi
import com.psrm.forms.psbdx.data.remote.dto.FieldDto
import com.psrm.forms.psbdx.data.remote.dto.FormDto
import com.psrm.forms.psbdx.data.remote.dto.FormsFieldsPayload
import com.psrm.forms.psbdx.data.remote.dto.NewFormRequest
import com.psrm.forms.psbdx.domain.model.PsrmField
import com.psrm.forms.psbdx.domain.model.PsrmForm
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FormsRepository(
    private val api: WordPressApi,
    private val formDao: FormDao
) {
    private val moshi = Moshi.Builder().build()
    private val fieldListType = Types.newParameterizedType(List::class.java, FieldDto::class.java)
    private val fieldListAdapter = moshi.adapter<List<FieldDto>>(fieldListType)

    /** Room is the single source of truth for the list UI — always observe
     *  this, and call [refresh] to pull the latest from the server into it. */
    fun observeForms(): Flow<List<PsrmForm>> =
        formDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh(): Result<Unit> = runCatching {
        val response = api.listForms()
        if (!response.isSuccessful) {
            error(
                if (response.code() == 404) {
                    "This site's WordPress plugin doesn't expose the psbdx-srm/v1 REST routes yet " +
                        "— update to plugin version 2.0.0 or later, which adds them."
                } else {
                    "Failed to load forms (HTTP ${response.code()})"
                }
            )
        }
        formDao.upsertAll(response.body().orEmpty().map { it.toEntity() })
    }

    suspend fun deleteForm(id: Long): Result<Unit> = runCatching {
        val response = api.deleteForm(id)
        if (!response.isSuccessful) error("Delete failed (HTTP ${response.code()})")
        formDao.deleteById(id)
    }

    /**
     * Creates a blank draft form on the server (the Forms list's + button)
     * and caches it locally so it shows up immediately.
     */
    suspend fun createForm(title: String): Result<PsrmForm> = runCatching {
        val response = api.createForm(NewFormRequest(title))
        if (!response.isSuccessful) error("Couldn't create the form (HTTP ${response.code()})")
        cache(response.body()!!)
    }

    /**
     * Duplicate a field within a form's schema — the mobile counterpart to
     * the PC editor's duplicateField(): same "(Copy)" label + re-derived
     * handle convention, so a form edited from either the site or the app
     * behaves identically. Persists the whole field list back to the
     * server (the plugin's schema is form-scoped, not per-field).
     */
    suspend fun duplicateField(formId: Long, fieldId: String): Result<PsrmForm> = runCatching {
        val fields = currentFields(formId)
        val index = fields.indexOfFirst { it.id == fieldId }
        if (index == -1) error("Field not found")

        val copy = fields[index].duplicated(newFieldId())
        val updated = fields.toMutableList().apply { add(index + 1, copy) }

        saveFields(formId, updated)
    }

    /**
     * Appends a brand-new field to the end of a form's schema — the mobile
     * builder's "+" action. The field ID is generated client-side (same
     * convention duplicateField() already used) rather than left for the
     * server, since the REST payload always carries a complete field
     * object and the PHP sanitizer only auto-generates an ID when the key
     * is entirely absent, not when it's empty.
     */
    suspend fun addField(
        formId: Long,
        type: String,
        label: String,
        required: Boolean,
        choices: List<String>? = null
    ): Result<PsrmForm> = runCatching {
        val fields = currentFields(formId)
        val newField = PsrmField(
            id = newFieldId(),
            type = type,
            label = label,
            required = required,
            handle = deriveHandle(label),
            order = fields.size,
            choices = choices
        )

        saveFields(formId, fields + newField)
    }

    /**
     * Edits an existing field in place. The handle is deliberately left
     * unchanged even if the label changes — same behavior as the PC
     * editor, where a handle is only ever derived once, at field creation,
     * so already-submitted answers keyed by that handle stay valid.
     */
    suspend fun updateField(
        formId: Long,
        fieldId: String,
        type: String,
        label: String,
        required: Boolean,
        choices: List<String>? = null
    ): Result<PsrmForm> = runCatching {
        val fields = currentFields(formId)
        val index = fields.indexOfFirst { it.id == fieldId }
        if (index == -1) error("Field not found")

        val updated = fields.toMutableList().apply {
            this[index] = this[index].copy(type = type, label = label, required = required, choices = choices)
        }

        saveFields(formId, updated)
    }

    /** Reads a form's current fields, pulling from the server first if it
     *  isn't cached yet — avoids a dead end when a screen is opened
     *  straight from a deep link or after a process death wiped the cache. */
    private suspend fun currentFields(formId: Long): List<PsrmField> {
        formDao.getById(formId)?.let {
            return fieldListAdapter.fromJson(it.fieldsJson).orEmpty().map { dto -> dto.toDomain() }
        }

        val response = api.getForm(formId)
        if (!response.isSuccessful) error("Form not found (HTTP ${response.code()})")
        val dto = response.body()!!
        formDao.upsert(dto.toEntity())
        return dto.fields.map { it.toDomain() }
    }

    private suspend fun saveFields(formId: Long, fields: List<PsrmField>): PsrmForm {
        val response = api.updateFormFields(formId, FormsFieldsPayload(fields.map { it.toDto() }))
        if (!response.isSuccessful) error("Save failed (HTTP ${response.code()})")
        return cache(response.body()!!)
    }

    private suspend fun cache(dto: FormDto): PsrmForm {
        val entity = dto.toEntity()
        formDao.upsert(entity)
        return entity.toDomain()
    }

    private fun newFieldId(): String = "f_" + System.currentTimeMillis().toString(36)

    /** Mirrors the plugin's own label_to_handle(): lowercase, spaces to
     *  underscores, strip anything sanitize_key() would also strip. */
    private fun deriveHandle(label: String): String =
        label.trim().lowercase().replace(' ', '_').replace(Regex("[^a-z0-9_-]"), "")

    private fun FormDto.toEntity(): FormEntity = FormEntity(
        id = id,
        title = title,
        status = status,
        shortcode = shortcode,
        shareUrl = shareUrl,
        responseCount = responseCount,
        fieldsJson = fieldListAdapter.toJson(fields),
        modifiedAt = modifiedAt
    )

    private fun FormEntity.toDomain(): PsrmForm = PsrmForm(
        id = id,
        title = title,
        status = status,
        shortcode = shortcode,
        shareUrl = shareUrl,
        responseCount = responseCount,
        fields = fieldListAdapter.fromJson(fieldsJson).orEmpty().map { it.toDomain() },
        modifiedAt = modifiedAt
    )

    private fun FieldDto.toDomain(): PsrmField = PsrmField(id, type, label, required, handle, order, choices)
    private fun PsrmField.toDto(): FieldDto = FieldDto(id, type, label, required, handle, order, choices)
}
