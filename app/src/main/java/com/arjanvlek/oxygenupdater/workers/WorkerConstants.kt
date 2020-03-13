package com.arjanvlek.oxygenupdater.workers

/**
 * This file contains constants that are common to all workers defined in this package
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

const val WORK_UNIQUE_DOWNLOAD_NAME = "WORK_UNIQUE_DOWNLOAD"
const val WORK_UNIQUE_MD5_VERIFICATION_NAME = "WORK_UNIQUE_MD5_VERIFICATION"
const val WORK_UNIQUE_LOG_UPLOAD_NAME = "WORK_UNIQUE_LOG_UPLOAD"
const val WORK_UNIQUE_UPDATE_FILE_CHECK_NAME = "WORK_UNIQUE_UPDATE_FILE_CHECK"

const val WORK_DATA_DOWNLOAD_BYTES_DONE = "DOWNLOAD_BYTES_DONE"
const val WORK_DATA_DOWNLOAD_TOTAL_BYTES = "DOWNLOAD_TOTAL_BYTES"
const val WORK_DATA_DOWNLOAD_PROGRESS = "DOWNLOAD_PROGRESS"
const val WORK_DATA_DOWNLOAD_ETA = "DOWNLOAD_ETA"
const val WORK_DATA_DOWNLOAD_FAILURE_TYPE = "DOWNLOAD_FAILURE_TYPE"

const val WORK_DATA_MD5_VERIFICATION_FAILURE_TYPE = "MD5_VERIFICATION_FAILURE_TYPE"

const val WORK_DATA_LOG_UPLOAD_STATUS = "LOG_UPLOAD_STATUS"
const val WORK_DATA_LOG_UPLOAD_INSTALL_ID = "LOG_UPLOAD_INSTALLATION_ID"
const val WORK_DATA_LOG_UPLOAD_START_OS = "LOG_UPLOAD_START_OS"
const val WORK_DATA_LOG_UPLOAD_DESTINATION_OS = "LOG_UPLOAD_DEST_OS"
const val WORK_DATA_LOG_UPLOAD_CURR_OS = "LOG_UPLOAD_CURR_OS"
const val WORK_DATA_LOG_UPLOAD_FAILURE_REASON = "LOG_UPLOAD_FAILURE_REASON"

const val DOWNLOAD_FOREGROUND_NOTIFICATION_ID = 2
const val MISC_FOREGROUND_NOTIFICATION_ID = 3

/**
 * Used in calls to [android.os.Environment.getExternalStoragePublicDirectory],
 * to get the root directory of internal storage
 */
const val DIRECTORY_ROOT = ""

/**
 * Minimum milliseconds that should have passed since the previous publish progress event.
 * Currently: `2s`
 */
const val THRESHOLD_PUBLISH_PROGRESS_TIME_PASSED = 2000L
