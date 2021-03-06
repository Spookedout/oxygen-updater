package com.arjanvlek.oxygenupdater.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_STATUS
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.PUSH_NOTIFICATION_CHANNEL_ID
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager.Companion.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT
import com.arjanvlek.oxygenupdater.models.InstallationStatus
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.workers.UploadRootInstallLogWorker
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_CURR_OS
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_DESTINATION_OS
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_FAILURE_REASON
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_INSTALL_ID
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_START_OS
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_STATUS
import com.arjanvlek.oxygenupdater.workers.WORK_UNIQUE_UPLOAD_ROOT_INSTALL_LOG
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

class VerifyInstallationReceiver : BroadcastReceiver() {

    private val systemVersionProperties by inject(SystemVersionProperties::class.java)
    private val notificationManager by inject(NotificationManager::class.java)
    private val workManager by inject(WorkManager::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (settingsManager.getPreference(PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false)
                && intent.action == Intent.ACTION_BOOT_COMPLETED
            ) {
                settingsManager.savePreference(PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false)

                // Don't check on unsupported devices.
                if (systemVersionProperties.oxygenOSVersion == NO_OXYGEN_OS
                    || systemVersionProperties.oxygenOSOTAVersion == NO_OXYGEN_OS
                ) {
                    return
                }

                val oldOxygenOSVersion = settingsManager.getPreference(SettingsManager.PROPERTY_OLD_SYSTEM_VERSION, "")
                val targetOxygenOSVersion = settingsManager.getPreference(SettingsManager.PROPERTY_TARGET_SYSTEM_VERSION, "")
                val currentOxygenOSVersion = systemVersionProperties.oxygenOSOTAVersion

                if (oldOxygenOSVersion.isEmpty() || targetOxygenOSVersion.isEmpty() || currentOxygenOSVersion.isEmpty()) {
                    displayFailureNotification(context, context.getString(R.string.install_verify_error_unable_to_verify))
                    logFailure(oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion, "ERR_CHECK_FAILED")
                } else if (currentOxygenOSVersion == oldOxygenOSVersion) {
                    displayFailureNotification(context, context.getString(R.string.install_verify_error_nothing_installed))
                    logFailure(oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion, "ERR_INSTALL_FAILED")
                } else if (currentOxygenOSVersion != targetOxygenOSVersion) {
                    displayFailureNotification(context, context.getString(R.string.install_verify_error_wrong_version_installed))
                    logFailure(oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion, "ERR_WRONG_OS_INSTALLED")
                } else {
                    displaySuccessNotification(context, systemVersionProperties.oxygenOSVersion)
                    logSuccess(oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion)
                }
            }
        } catch (e: Throwable) {
            logError(TAG, "Failed to check if update was successfully installed", e)
        }
    }

    private fun displaySuccessNotification(context: Context, oxygenOSVersion: String) {
        val contentIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0)

        val builder = NotificationCompat.Builder(context, PUSH_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.done_circle)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setContentTitle(context.getString(R.string.install_verify_success_title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.install_verify_success_message, oxygenOSVersion)))
            .setContentText(context.getString(R.string.install_verify_success_message, oxygenOSVersion))
            .setCategory(CATEGORY_STATUS)
            .setPriority(PRIORITY_HIGH)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun displayFailureNotification(context: Context, errorMessage: String) {
        val contentIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0)

        val builder = NotificationCompat.Builder(context, PUSH_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.error_outline)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setContentTitle(context.getString(R.string.install_verify_error_title))
            .setContentText(errorMessage)
            .setCategory(CATEGORY_STATUS)
            .setPriority(PRIORITY_HIGH)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun logSuccess(
        startOs: String,
        destinationOs: String,
        currentOs: String
    ) = buildLogData(startOs, destinationOs, currentOs).apply {
        add(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_STATUS to InstallationStatus.FINISHED.toString())

        scheduleLogUploadTask(workDataOf(*toTypedArray()))
    }

    private fun logFailure(
        startOs: String,
        destinationOs: String,
        currentOs: String,
        reason: String
    ) = buildLogData(startOs, destinationOs, currentOs).apply {
        add(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_STATUS to InstallationStatus.FAILED.toString())
        add(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_FAILURE_REASON to reason)

        scheduleLogUploadTask(workDataOf(*toTypedArray()))
    }

    private fun buildLogData(startOs: String, destinationOs: String, currentOs: String) = arrayListOf(
        WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_INSTALL_ID to settingsManager.getPreference(SettingsManager.PROPERTY_INSTALLATION_ID, "<INVALID>"),
        WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_START_OS to startOs,
        WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_DESTINATION_OS to destinationOs,
        WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_CURR_OS to currentOs
    )

    private fun scheduleLogUploadTask(inputData: Data) {
        val logUploadWorkRequest = OneTimeWorkRequestBuilder<UploadRootInstallLogWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniqueWork(
            WORK_UNIQUE_UPLOAD_ROOT_INSTALL_LOG,
            ExistingWorkPolicy.REPLACE,
            logUploadWorkRequest
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 79243095
        private const val TAG = "VerifyInstallationReceiver"
    }
}
