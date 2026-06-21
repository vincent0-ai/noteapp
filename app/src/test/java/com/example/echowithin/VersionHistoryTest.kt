package com.example.echowithin

import com.example.echowithin.data.model.VersionDto
import com.example.echowithin.presentation.screens.shouldShowStatusBadge
import com.example.echowithin.presentation.screens.shouldShowApproveRejectButtons
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionHistoryTest {

    @Test
    fun testShouldShowStatusBadge_whenIsProposalIsTrue_returnsTrue() {
        val version = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = true,
            status = "pending"
        )
        assertTrue(version.shouldShowStatusBadge())
    }

    @Test
    fun testShouldShowStatusBadge_whenIsProposalIsFalse_returnsFalse() {
        val version = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = false,
            status = "approved"
        )
        assertFalse(version.shouldShowStatusBadge())
    }

    @Test
    fun testShouldShowApproveRejectButtons_whenIsProposalIsTrueAndStatusIsPending_returnsTrue() {
        val version = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = true,
            status = "pending"
        )
        assertTrue(version.shouldShowApproveRejectButtons())
    }

    @Test
    fun testShouldShowApproveRejectButtons_whenStatusIsPendingDifferentCasing_returnsTrue() {
        val version1 = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = true,
            status = "PENDING"
        )
        val version2 = VersionDto(
            version_id = "v2",
            content = "Hello",
            is_proposal = true,
            status = "Pending"
        )
        assertTrue(version1.shouldShowApproveRejectButtons())
        assertTrue(version2.shouldShowApproveRejectButtons())
    }

    @Test
    fun testShouldShowApproveRejectButtons_whenIsProposalIsTrueAndStatusIsApproved_returnsFalse() {
        val version = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = true,
            status = "approved"
        )
        assertFalse(version.shouldShowApproveRejectButtons())
    }

    @Test
    fun testShouldShowApproveRejectButtons_whenIsProposalIsTrueAndStatusIsRejected_returnsFalse() {
        val version = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = true,
            status = "rejected"
        )
        assertFalse(version.shouldShowApproveRejectButtons())
    }

    @Test
    fun testShouldShowApproveRejectButtons_whenIsProposalIsFalseAndStatusIsPending_returnsFalse() {
        val version = VersionDto(
            version_id = "v1",
            content = "Hello",
            is_proposal = false,
            status = "pending"
        )
        assertFalse(version.shouldShowApproveRejectButtons())
    }
}
