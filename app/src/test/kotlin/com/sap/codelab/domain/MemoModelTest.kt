package com.sap.codelab.domain

import com.sap.codelab.domain.model.REMINDER_TEXT_LENGTH
import com.sap.codelab.testing.memo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoModelTest {

    @Test
    fun `reminder text is the first 140 characters of the description`() {
        val long = "a".repeat(REMINDER_TEXT_LENGTH) + "b".repeat(50)
        assertEquals("a".repeat(REMINDER_TEXT_LENGTH), memo(1).copy(description = long).reminderText)
        assertEquals("short", memo(1).copy(description = "short").reminderText)
    }

    @Test
    fun `only open, not yet reminded memos are active`() {
        assertTrue(memo(1).isActive)
        assertFalse(memo(1, isDone = true).isActive)
        assertFalse(memo(1, isReminded = true).isActive)
    }
}
