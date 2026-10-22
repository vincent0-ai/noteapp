package com.example.echowithin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffUtilsTest {

    @Test
    fun `identical texts produce zero additions and zero deletions`() {
        val text = "Line 1\nLine 2\nLine 3"
        val diff = DiffUtils.computeDiff(text, text)

        assertEquals(0, diff.additions)
        assertEquals(0, diff.deletions)
        assertFalse(diff.hasChanges)
        assertEquals(3, diff.lines.size)
        assertTrue(diff.lines.all { it is DiffLine.Unchanged })
    }

    @Test
    fun `empty old text marks all lines as added`() {
        val newText = "Hello\nWorld"
        val diff = DiffUtils.computeDiff("", newText)

        assertEquals(2, diff.additions)
        assertEquals(0, diff.deletions)
        assertTrue(diff.hasChanges)
        assertEquals(2, diff.lines.size)
        assertTrue(diff.lines.all { it is DiffLine.Added })
        assertEquals("Hello", diff.lines[0].text)
        assertEquals("World", diff.lines[1].text)
    }

    @Test
    fun `empty new text marks all lines as deleted`() {
        val oldText = "Line A\nLine B"
        val diff = DiffUtils.computeDiff(oldText, "")

        assertEquals(0, diff.additions)
        assertEquals(2, diff.deletions)
        assertTrue(diff.hasChanges)
        assertEquals(2, diff.lines.size)
        assertTrue(diff.lines.all { it is DiffLine.Deleted })
    }

    @Test
    fun `detects single line insertion in the middle`() {
        val oldText = "First\nThird"
        val newText = "First\nSecond\nThird"
        val diff = DiffUtils.computeDiff(oldText, newText)

        assertEquals(1, diff.additions)
        assertEquals(0, diff.deletions)
        assertEquals(3, diff.lines.size)

        assertTrue(diff.lines[0] is DiffLine.Unchanged)
        assertEquals("First", diff.lines[0].text)

        assertTrue(diff.lines[1] is DiffLine.Added)
        assertEquals("Second", diff.lines[1].text)

        assertTrue(diff.lines[2] is DiffLine.Unchanged)
        assertEquals("Third", diff.lines[2].text)
    }

    @Test
    fun `detects line deletion and replacement`() {
        val oldText = "Apple\nBanana\nCherry"
        val newText = "Apple\nBlueberry\nCherry"
        val diff = DiffUtils.computeDiff(oldText, newText)

        assertEquals(1, diff.additions)
        assertEquals(1, diff.deletions)
        assertEquals(4, diff.lines.size)

        assertEquals(DiffLine.Unchanged("Apple"), diff.lines[0])
        assertEquals(DiffLine.Deleted("Banana"), diff.lines[1])
        assertEquals(DiffLine.Added("Blueberry"), diff.lines[2])
        assertEquals(DiffLine.Unchanged("Cherry"), diff.lines[3])
    }
}
