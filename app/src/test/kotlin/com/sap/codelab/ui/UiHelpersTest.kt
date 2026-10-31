package com.sap.codelab.ui

import com.sap.codelab.R
import com.sap.codelab.core.AppError
import com.sap.codelab.domain.model.PermissionStatus
import com.sap.codelab.ui.feature.home.HomeAction
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.mvi.UiEffectQueue
import com.sap.codelab.ui.components.permission.PermissionStep
import com.sap.codelab.ui.components.permission.buttonRes
import com.sap.codelab.ui.components.permission.messageRes
import com.sap.codelab.ui.components.permission.nextMissingStep
import com.sap.codelab.ui.components.text.UiText
import com.sap.codelab.ui.components.text.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class UiHelpersTest {

    @Test
    fun `every error has a message`() {
        val cases = mapOf(
            AppError.Local.NotFound to R.string.error_not_found,
            AppError.Local.Constraint(null) to R.string.error_storage,
            AppError.Local.Database(null) to R.string.error_storage,
            AppError.Local.Unknown(null) to R.string.error_storage,
            AppError.Location.PermissionDenied(null) to R.string.error_location_permission,
            AppError.Location.Unavailable(null) to R.string.error_location_unavailable,
            AppError.Location.Geofence(1000, null) to R.string.error_location_unknown,
            AppError.Location.Unknown(null) to R.string.error_location_unknown,
            AppError.NotificationsBlocked(null) to R.string.error_notifications_blocked,
            AppError.Unknown(null) to R.string.error_unknown,
        )
        cases.forEach { (error, id) -> assertEquals(UiText.Resource(id), error.toUiText()) }
    }

    @Test
    fun `permissions are asked in order`() {
        val none = PermissionStatus(hasPreciseLocation = false, hasAnyLocation = false, hasBackgroundLocation = false, hasNotifications = false)
        assertEquals(PermissionStep.PreciseLocation, none.nextMissingStep())
        assertEquals(PermissionStep.Notifications, none.copy(hasPreciseLocation = true).nextMissingStep())
        assertEquals(PermissionStep.BackgroundLocation, none.copy(hasPreciseLocation = true, hasNotifications = true).nextMissingStep())
        assertNull(none.copy(hasPreciseLocation = true, hasNotifications = true, hasBackgroundLocation = true).nextMissingStep())
    }

    @Test
    fun `every step has its own texts`() {
        assertEquals(
            listOf(R.string.permission_location_message, R.string.permission_notifications_message, R.string.permission_background_message),
            PermissionStep.entries.map { it.messageRes },
        )
        assertEquals(
            listOf(R.string.permission_location_button, R.string.permission_notifications_button, R.string.permission_background_button),
            PermissionStep.entries.map { it.buttonRes },
        )
    }

    @Test
    fun `effects are shown in order and a stale consume is ignored`() = runTest {
        val queue = UiEffectQueue<HomeAction>()
        val first = UiEffect.ShowToast(UiText.Dynamic("1"))
        val second = UiEffect.ShowToast(UiText.Dynamic("2"))
        queue.send(first)
        queue.send(second)

        queue.consume(second) // not the head: no-op
        assertSame(first, queue.current)
        queue.consume(first)
        assertSame(second, queue.current)
        queue.consume(second)
        assertNull(queue.current)
    }
}
