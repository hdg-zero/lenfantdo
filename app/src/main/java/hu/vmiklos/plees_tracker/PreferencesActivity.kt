/*
 * Copyright 2023 Miklos Vajna
 *
 * SPDX-License-Identifier: MIT
 */

package hu.vmiklos.plees_tracker

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch

class PreferencesActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PreferencesActivity"
        private const val STATE_CHANGE_PATH_FROM = "changePathFrom"
    }

    private var changePathFrom: BackupDestination.LocalFolder? = null

    private val folderPickerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val changeFrom = changePathFrom
            changePathFrom = null
            try {
                result.data?.data?.let { uri ->
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                    val newDest = BackupDestination.LocalFolder(uri.toString())
                    if (changeFrom != null) {
                        if (changeFrom.path != newDest.path) {
                            DataModel.replaceDestination(changeFrom, newDest)
                            releaseFolderPermission(changeFrom.path)
                        }
                    } else {
                        DataModel.addDestination(newDest)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "folderPickerResult: $e")
            }
            refreshFragment()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { state ->
            changePathFrom = state.getString(STATE_CHANGE_PATH_FROM)
                ?.let { BackupDestination.LocalFolder(it) }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root, Preferences())
            .commit()
        setContentView(R.layout.activity_settings)

        DataModel.handleWindowInsets(this)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        DataModel.preferencesActivity = this
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CHANGE_PATH_FROM, changePathFrom?.path)
    }

    override fun onDestroy() {
        super.onDestroy()
        DataModel.preferencesActivity = null
    }

    fun promptAddDestination() {
        openFolderChooser()
    }

    fun openFolderChooser() {
        folderPickerResult.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
    }

    fun changeFolder(dest: BackupDestination.LocalFolder) {
        changePathFrom = dest
        openFolderChooser()
    }

    fun triggerManualFolderBackup(path: String) {
        lifecycleScope.launch {
            val success = DataModel.backupFolder(applicationContext, path)
            toast(if (success) R.string.backup_success else R.string.backup_failure)
        }
    }

    fun confirmRestoreFromFolder(path: String) {
        lifecycleScope.launch {
            if (DataModel.hasSleeps()) {
                showFolderRestoreModeDialog(path)
            } else {
                restoreFromFolder(path, override = false)
            }
        }
    }

    private fun showFolderRestoreModeDialog(path: String) {
        val options = arrayOf(
            getString(R.string.restore_merge),
            getString(R.string.restore_override)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_target_folder)
            .setItems(options) { _, which ->
                restoreFromFolder(path, override = (which == 1))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun restoreFromFolder(path: String, override: Boolean) {
        lifecycleScope.launch {
            val success = DataModel.restoreFromFolder(applicationContext, path, override)
            toast(if (success) R.string.import_success else R.string.backup_failure)
        }
    }

    fun removeFolder(path: String, deleteBackup: Boolean) {
        val dest = BackupDestination.LocalFolder(path)
        lifecycleScope.launch {
            if (deleteBackup) {
                val success = DataModel.deleteFolderBackup(applicationContext, path)
                toast(
                    if (success) {
                        R.string.folder_delete_backup_success
                    } else {
                        R.string.folder_delete_backup_failure
                    }
                )
            }
            DataModel.removeDestination(dest)
            releaseFolderPermission(dest.path)
            refreshFragment()
        }
    }

    private fun releaseFolderPermission(path: String) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.releasePersistableUriPermission(Uri.parse(path), flags)
        } catch (e: Exception) {
            Log.e(TAG, "releaseFolderPermission: $e")
        }
    }

    fun refreshFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root, Preferences())
            .commitAllowingStateLoss()
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    fun showBedtimeDialog() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val currentBedHour = DataModel.getBedtimeHour(preferences)
        val currentBedMinute = DataModel.getBedtimeMinute(preferences)
        val currentWakeHour = DataModel.getWakeupHour(preferences)
        val currentWakeMinute = DataModel.getWakeupMinute(preferences)

        TimePickerDialog(
            this,
            { _, bedHour, bedMinute ->
                val editor = preferences.edit()
                editor.putString("bedtime", "$bedHour:$bedMinute")
                editor.apply()
                TimePickerDialog(
                    this,
                    { _, wakeHour, wakeMinute ->
                        editor.putString("wakeup", "$wakeHour:$wakeMinute")
                        editor.apply()
                        Toast.makeText(
                            this,
                            getString(R.string.bedtime_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    currentWakeHour,
                    currentWakeMinute,
                    true
                ).show()
            },
            currentBedHour,
            currentBedMinute,
            true
        ).show()
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
