package com.psrm.forms.psbdx.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val status: String,
    val shortcode: String,
    val shareUrl: String,
    val responseCount: Int,
    val fieldsJson: String,     // serialized List<PsrmField> — see Converters
    val modifiedAt: String
)

@Entity(tableName = "responses")
data class ResponseEntity(
    @PrimaryKey val id: Long,
    val formId: Long,
    val ticketId: String,
    val status: String,
    val reporterName: String?,
    val reporterEmail: String?,
    val submittedAt: String,
    val answersJson: String,
    val repliesEnabled: Boolean,
    val repliesJson: String
)

@Dao
interface FormDao {
    @Query("SELECT * FROM forms ORDER BY modifiedAt DESC")
    fun observeAll(): Flow<List<FormEntity>>

    @Query("SELECT * FROM forms WHERE id = :id")
    suspend fun getById(id: Long): FormEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(forms: List<FormEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(form: FormEntity)

    @Query("DELETE FROM forms WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ResponseDao {
    @Query("SELECT * FROM responses WHERE formId = :formId ORDER BY submittedAt DESC")
    fun observeForForm(formId: Long): Flow<List<ResponseEntity>>

    @Query("SELECT * FROM responses WHERE id = :id")
    suspend fun getById(id: Long): ResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(responses: List<ResponseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(response: ResponseEntity)
}

/** JSON blobs for the nested lists (fields/answers/replies) — keeps the
 *  offline cache schema simple; these are read-through-server payloads,
 *  never queried by their inner fields, so there's no need to normalize
 *  them into their own tables. */
class Converters {
    private val moshi = Moshi.Builder().build()

    @TypeConverter
    fun listToJson(value: List<*>?): String = moshi.adapter<List<*>>().toJson(value ?: emptyList<Any>())

    @TypeConverter
    fun mapToJson(value: Map<String, String>?): String =
        moshi.adapter<Map<String, String>>().toJson(value ?: emptyMap())
}

@Database(
    entities = [FormEntity::class, ResponseEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PsrmDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun responseDao(): ResponseDao
}
