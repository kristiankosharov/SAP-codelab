package com.sap.codelab.background

import androidx.work.ListenableWorker
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult

/** Database failures are worth a retry; missing permissions or location are not (the next periodic run tries again). */
internal fun AppResult<*>.toWorkResult(): ListenableWorker.Result = when {
    this is AppResult.Failure && error is AppError.Local -> ListenableWorker.Result.retry()
    else -> ListenableWorker.Result.success()
}
