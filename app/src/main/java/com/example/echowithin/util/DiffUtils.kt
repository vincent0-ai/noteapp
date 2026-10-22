package com.example.echowithin.util

/**
 * Represents a single line in a computed diff.
 */
sealed class DiffLine {
    abstract val text: String

    data class Added(override val text: String) : DiffLine()
    data class Deleted(override val text: String) : DiffLine()
    data class Unchanged(override val text: String) : DiffLine()
}

/**
 * Result of computing a line-by-line diff between two strings.
 */
data class DiffResult(
    val lines: List<DiffLine>,
    val additions: Int,
    val deletions: Int
) {
    val hasChanges: Boolean get() = additions > 0 || deletions > 0
}

/**
 * Utility for computing line-by-line unified diffs between note versions.
 * Uses the classic Longest Common Subsequence (LCS) algorithm to accurately
 * track added, deleted, and unchanged lines.
 */
object DiffUtils {

    fun computeDiff(oldText: String, newText: String): DiffResult {
        if (oldText == newText) {
            val lines = if (newText.isEmpty()) emptyList() else newText.lines().map { DiffLine.Unchanged(it) }
            return DiffResult(lines = lines, additions = 0, deletions = 0)
        }

        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()

        val m = oldLines.size
        val n = newLines.size

        // Build LCS length table
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0 until m) {
            for (j in 0 until n) {
                if (oldLines[i] == newLines[j]) {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        // Backtrack to assemble diff lines
        val result = mutableListOf<DiffLine>()
        var i = m
        var j = n
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
                result.add(DiffLine.Unchanged(oldLines[i - 1]))
                i--
                j--
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                result.add(DiffLine.Added(newLines[j - 1]))
                j--
            } else if (i > 0) {
                result.add(DiffLine.Deleted(oldLines[i - 1]))
                i--
            }
        }
        result.reverse()

        val additions = result.count { it is DiffLine.Added }
        val deletions = result.count { it is DiffLine.Deleted }
        return DiffResult(result, additions, deletions)
    }
}
