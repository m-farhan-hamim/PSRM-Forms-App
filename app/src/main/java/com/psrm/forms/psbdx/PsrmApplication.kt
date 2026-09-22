package com.psrm.forms.psbdx

import android.app.Application
import androidx.room.Room
import com.psrm.forms.psbdx.data.local.PsrmDatabase
import com.psrm.forms.psbdx.data.remote.CredentialStore
import com.psrm.forms.psbdx.data.remote.WordPressApiClient
import com.psrm.forms.psbdx.data.repository.AuthRepository
import com.psrm.forms.psbdx.data.repository.FormsRepository
import com.psrm.forms.psbdx.data.repository.ResponsesRepository

/**
 * Hand-rolled service locator — deliberately no Hilt/Dagger. Keeps the
 * dependency graph obvious in a small app and avoids pulling in an
 * annotation-processing framework the F-Droid build has to reproduce
 * bit-for-bit; plain constructor calls are easiest to audit.
 */
class PsrmApplication : Application() {

    lateinit var credentialStore: CredentialStore
        private set
    lateinit var database: PsrmDatabase
        private set
    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        credentialStore = CredentialStore(this)
        database = Room.databaseBuilder(this, PsrmDatabase::class.java, "psrm.db").build()
        authRepository = AuthRepository(credentialStore)
        authRepository.restoreSession()
    }

    /** Built lazily/per-call since it depends on the site URL only known
     *  after login — see WordPressApiClient. */
    fun formsRepository(): FormsRepository =
        FormsRepository(WordPressApiClient.create(credentialStore), database.formDao())

    fun responsesRepository(): ResponsesRepository =
        ResponsesRepository(WordPressApiClient.create(credentialStore), database.responseDao())
}
