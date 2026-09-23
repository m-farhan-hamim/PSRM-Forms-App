package com.psrm.forms.psbdx.data.repository

import com.psrm.forms.psbdx.data.local.FormDao
import com.psrm.forms.psbdx.data.local.FormEntity
import com.psrm.forms.psbdx.data.remote.WordPressApi
import com.psrm.forms.psbdx.data.remote.dto.FieldDto
import com.psrm.forms.psbdx.data.remote.dto.FormsFieldsPayload
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
                        "(the app can log in fine — wp/v2/users/me works — but form/response data needs a " +
                        "small REST controller added to the plugin; see this app's README for the exact routes)."
                } else {
                    "Failed to load forms (HTTP ${response.code()})"
                }
            )
        }
        val forms = response.body().orEmpty()
        formDao.upsertAll(forms.map { dto ->
            FormEntity(
                id = dto.id,
                title = dto.title,
                status = dto.status,
                shortcode = dto.shortcode,
                shareUrl = dto.shareUrl,
                responseCount = dto.responseCount,
                fieldsJson = fieldListAdapter.toJson(dto.fields),
                modifiedAt = dto.modifiedAt
            )
        })
    }

    suspend fun deleteForm(id: Long): Result<Unit> = runCatching {
        val response = api.deleteForm(id)
        if (!response.isSuccessful) error("Delete failed (HTTP ${response.code()})")
        formDao.deleteById(id)
    }

    /**
     * Duplicate a field within a form's schema — the mobile counterpart to
     * the PC editor's duplicateField(): same "(Copy)" label + re-derived
     * handle convention, so a form edited from either the site or the app
     * behaves identically. Persists the whole field list back to the
     * server (the plugin's schema is form-scoped, not per-field).
     */
    suspend fun duplicateField(formId: Long, fieldId: String): Result<PsrmForm> = runCatching {
        val current = formDao.getById(formId) ?: error("Form not cached — refresh first")
        val fields = fieldListAdapter.fromJson(current.fieldsJson).orEmpty().map { it.toDomain() }

        val index = fields.indexOfFirst { it.id == fieldId }
        if (index == -1) error("Field not found")

        val newId = "f_" + System.currentTimeMillis().toString(36)
        val copy = fields[index].duplicated(newId)
        val updated = fields.toMutableList().apply { add(index + 1, copy) }

        val response = api.updateFormFields(
            formId,
            FormsFieldsPayload(updated.map { it.toDto() })
        )
        if (!response.isSuccessful) error("Save failed (HTTP ${response.code()})")

        val dto = response.body()!!
        val entity = FormEntity(
            id = dto.id,
            title = dto.title,
            status = dto.status,
            shortcode = dto.shortcode,
            shareUrl = dto.shareUrl,
            responseCount = dto.responseCount,
            fieldsJson = fieldListAdapter.toJson(dto.fields),
            modifiedAt = dto.modifiedAt
        )
        formDao.upsert(entity)
        entity.toDomain()
    }

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

    private fun FieldDto.toDomain(): PsrmField = PsrmField(id, type, label, required, handle, order)
    private fun PsrmField.toDto(): FieldDto = FieldDto(id, type, label, required, handle, order)
}
