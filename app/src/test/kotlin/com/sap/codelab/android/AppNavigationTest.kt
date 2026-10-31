package com.sap.codelab.android

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.sap.codelab.MainActivity
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.data.memo.di.MemoDatabaseModule
import com.sap.codelab.data.memo.di.MemoRepositoryModule
import com.sap.codelab.data.notification.ReminderNotifier
import com.sap.codelab.testing.memoData
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The real Activity, navigation and ViewModels (Hilt), with the memo repository faked. */
@HiltAndroidTest
@UninstallModules(MemoDatabaseModule::class, MemoRepositoryModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class AppNavigationTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @BindValue
    val memoRepository: MemoRepository = mockk()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hilt.inject()
        every { memoRepository.observeMemos(any()) } returns flowOf(AppResult.Success(listOf(memoData(1, title = "Buy milk"))))
        coEvery { memoRepository.getMemo(any()) } answers { AppResult.Success(memoData(firstArg(), title = "Memo ${firstArg<Long>()}")) }
    }

    private fun openMemoIntent(id: Long) = Intent(context, MainActivity::class.java).putExtra(ReminderNotifier.EXTRA_MEMO_ID, id)

    @Test
    fun home_to_create_and_detail_and_back() {
        Robolectric.buildActivity(MainActivity::class.java).setup()

        compose.onNodeWithText("Buy milk").assertIsDisplayed()
        compose.onNodeWithContentDescription("New memo").performClick()
        compose.onNodeWithText("New memo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Buy milk").performClick()
        compose.onNodeWithText("Text 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Buy milk").assertIsDisplayed()
    }

    @Test
    fun a_reminder_opens_its_memo_on_start_and_while_open() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, openMemoIntent(3)).setup()
        compose.onNodeWithText("Memo 3").assertIsDisplayed()

        controller.newIntent(openMemoIntent(4))
        compose.onNodeWithText("Memo 4").assertIsDisplayed()

        controller.newIntent(Intent(context, MainActivity::class.java)) // no memo: nothing opens
        compose.onNodeWithText("Memo 4").assertIsDisplayed()
    }
}
