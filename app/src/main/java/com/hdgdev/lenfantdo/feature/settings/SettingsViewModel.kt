/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hdgdev.lenfantdo.data.export.LegacyCsvParser
import com.hdgdev.lenfantdo.data.export.SnapshotManager
import com.hdgdev.lenfantdo.data.transfer.EncryptedBackupManager
import com.hdgdev.lenfantdo.domain.model.SleepSession
import com.hdgdev.lenfantdo.domain.usecase.RecordCompletedSleepUseCase
import com.hdgdev.lenfantdo.tracking.TrackingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val totalSessionsCount: Int = 0,
    val message: String? = null,
    val errorMessage: String? = null,
    val isBusy: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager.getInstance(application)
    private val repository = trackingManager.repository
    private val recordUseCase = RecordCompletedSleepUseCase(repository)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllSessions().collect { list ->
                _uiState.value = _uiState.value.copy(totalSessionsCount = list.size)
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, message = null, errorMessage = null)
            val app = getApplication<Application>()
            try {
                val sessions = repository.getAllSessions()
                val snapshotManager = SnapshotManager()

                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { os ->
                        snapshotManager.serializeToCsv(sessions, os)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    message = "Export CSV réussi (${sessions.size} nuits exportées)"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = "Échec de l'export CSV : ${e.message}"
                )
            }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, message = null, errorMessage = null)
            val app = getApplication<Application>()
            try {
                val parsedSessions = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { inputStream ->
                        LegacyCsvParser().parse(inputStream.bufferedReader()).sessions
                    } ?: emptyList()
                }

                if (parsedSessions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        errorMessage = "Le fichier CSV est vide ou illisible."
                    )
                    return@launch
                }

                // Create internal safety snapshot in noBackupFilesDir before modifying records
                withContext(Dispatchers.IO) {
                    val currentSessions = repository.getAllSessions()
                    if (currentSessions.isNotEmpty()) {
                        val snapshotFile = File(app.noBackupFilesDir, "pre_import_snapshot_${System.currentTimeMillis()}.csv")
                        snapshotFile.outputStream().use { os ->
                            SnapshotManager().serializeToCsv(currentSessions, os)
                        }
                    }
                }

                var importedCount = 0
                for (session in parsedSessions) {
                    recordUseCase(
                        startEpochMs = session.startEpochMs,
                        stopEpochMs = session.stopEpochMs,
                        rating = session.rating,
                        note = session.note,
                        wakeups = session.wakeups
                    )
                    importedCount++
                }

                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    message = "Import réussi : $importedCount nuits importées."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = "Échec de l'import CSV : ${e.message}"
                )
            }
        }
    }

    fun exportEncryptedBackup(uri: Uri, passphrase: CharArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, message = null, errorMessage = null)
            val app = getApplication<Application>()
            try {
                val sessions = repository.getAllSessions()
                val encryptedBytes = withContext(Dispatchers.Default) {
                    EncryptedBackupManager.createEncryptedBackup(sessions, passphrase)
                }

                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(encryptedBytes)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    message = "Sauvegarde chiffrée créée avec succès (${sessions.size} nuits)."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = "Erreur lors du chiffrement : ${e.message}"
                )
            }
        }
    }

    fun importEncryptedBackup(uri: Uri, passphrase: CharArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, message = null, errorMessage = null)
            val app = getApplication<Application>()
            try {
                val bytes = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException("Impossible de lire le fichier")

                val restoredSessions = withContext(Dispatchers.Default) {
                    EncryptedBackupManager.restoreEncryptedBackup(bytes, passphrase)
                }

                // Safety snapshot in noBackupFilesDir before restoring
                withContext(Dispatchers.IO) {
                    val currentSessions = repository.getAllSessions()
                    if (currentSessions.isNotEmpty()) {
                        val snapshotFile = File(app.noBackupFilesDir, "pre_restore_snapshot_${System.currentTimeMillis()}.csv")
                        snapshotFile.outputStream().use { os ->
                            SnapshotManager().serializeToCsv(currentSessions, os)
                        }
                    }
                }

                var count = 0
                for (session in restoredSessions) {
                    recordUseCase(
                        startEpochMs = session.startEpochMs,
                        stopEpochMs = session.stopEpochMs,
                        rating = session.rating,
                        note = session.note,
                        wakeups = session.wakeups
                    )
                    count++
                }

                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    message = "Sauvegarde restaurée : $count nuits réintégrées."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = "Échec du déchiffrement (mot de passe incorrect ou fichier altéré) : ${e.message}"
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(message = null, errorMessage = null)
    }
}
