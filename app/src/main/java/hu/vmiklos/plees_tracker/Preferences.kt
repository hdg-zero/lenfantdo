/*
 * Copyright 2023 Miklos Vajna
 *
 * SPDX-License-Identifier: MIT
 */

package hu.vmiklos.plees_tracker

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import kotlinx.coroutines.launch

class Preferences : PreferenceFragmentCompat() {

    private fun padMinute(raw: String): String {
        val fro = ":([0-9])$".toRegex()
        val to = ":0$1"
        return raw.replace(fro, to)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupBackupPreferences()
        val wakeup = findPreference<Preference>("wakeup")
        wakeup?.let {
            val preferences = DataModel.preferences
            val value = preferences.getString("wakeup", "")
            if (value != null) {
                it.summary = padMinute(value)
            }
        }
        val bedtime = findPreference<Preference>("bedtime")
        bedtime?.let {
            val preferences = DataModel.preferences
            val value = preferences.getString("bedtime", "")
            if (value != null) {
                it.summary = padMinute(value)
            }
        }
    }

    private fun setupBackupPreferences() {
        val category = findPreference<PreferenceCategory>("backup_destinations_category") ?: return
        val staticKeys = setOf("automatic_backup", "add_backup_destination", "pretty_backup")

        val toRemove = (0 until category.preferenceCount)
            .map { category.getPreference(it) }
            .filter { it.key !in staticKeys }
        toRemove.forEach { category.removePreference(it) }

        val destinations = DataModel.getDestinations()
        findPreference<SwitchPreference>("automatic_backup")?.apply {
            if (isChecked && destinations.isEmpty()) {
                isChecked = false
            }
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true && DataModel.getDestinations().isEmpty()) {
                    (activity as? PreferencesActivity)?.promptAddDestination()
                }
                true
            }
        }
        for ((index, dest) in destinations.withIndex()) {
            val pref = Preference(requireContext()).apply {
                key = "dest_$index"
                order = index
                when (dest) {
                    is BackupDestination.LocalFolder -> {
                        title = getString(R.string.backup_target_folder)
                        summary = dest.path
                        setOnPreferenceClickListener {
                            showFolderOptions(dest)
                            true
                        }
                    }
                }
            }
            category.addPreference(pref)
            pref.dependency = "automatic_backup"
        }

        findPreference<Preference>("add_backup_destination")?.apply {
            val folderAddable = destinations.none { it is BackupDestination.LocalFolder }
            isVisible = folderAddable
            setTitle(R.string.settings_add_device_backup_destination)
            setOnPreferenceClickListener {
                (activity as? PreferencesActivity)?.promptAddDestination()
                true
            }
        }
    }

    private fun showFolderOptions(dest: BackupDestination.LocalFolder) {
        lifecycleScope.launch {
            val ctx = requireContext().applicationContext
            val hasBackup = DataModel.hasFolderBackup(ctx, dest.path)
            val items = buildList {
                add(getString(R.string.drive_backup_now))
                if (hasBackup) {
                    add(getString(R.string.drive_restore))
                }
                add(getString(R.string.folder_change_path))
                add(getString(R.string.folder_destination_remove))
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_target_folder)
                .setItems(items.toTypedArray()) { _, which ->
                    val activity = activity as? PreferencesActivity ?: return@setItems
                    when (items[which]) {
                        getString(R.string.drive_backup_now) ->
                            activity.triggerManualFolderBackup(dest.path)
                        getString(R.string.drive_restore) ->
                            activity.confirmRestoreFromFolder(dest.path)
                        getString(R.string.folder_change_path) ->
                            activity.changeFolder(dest)
                        getString(R.string.folder_destination_remove) ->
                            confirmRemoveFolder(dest, hasBackup)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun confirmRemoveFolder(dest: BackupDestination.LocalFolder, hasBackup: Boolean) {
        val builder = AlertDialog.Builder(requireContext())
        if (hasBackup) {
            val deleteBackup = booleanArrayOf(false)
            builder
                .setTitle(R.string.destination_remove_confirm)
                .setMultiChoiceItems(
                    arrayOf(getString(R.string.folder_remove_also_delete)),
                    deleteBackup
                ) { _, _, isChecked ->
                    deleteBackup[0] = isChecked
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    (activity as? PreferencesActivity)
                        ?.removeFolder(dest.path, deleteBackup[0])
                }
        } else {
            builder
                .setMessage(R.string.destination_remove_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    (activity as? PreferencesActivity)
                        ?.removeFolder(dest.path, deleteBackup = false)
                }
        }
        builder.setNegativeButton(android.R.string.cancel, null).show()
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
