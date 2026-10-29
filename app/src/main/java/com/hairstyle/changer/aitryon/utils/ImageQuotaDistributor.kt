package com.hairstyle.changer.aitryon.utils

/**
 * Splits [totalImages] across [days] with a soft-decreasing daily plan.
 * Early days get more, later days get less, but the last day keeps a fair minimum
 * (not just 1 image). Unused days roll into later days via [cumulativeAllowed].
 */
object ImageQuotaDistributor {

    fun distributeDecreasing(totalImages: Int, days: Int): IntArray {
        if (days <= 0 || totalImages <= 0) return IntArray(0)
        if (days == 1) return intArrayOf(totalImages)

        val average = totalImages.toDouble() / days
        val floor = maxOf(3, (average * 0.65).toInt()).coerceAtMost(totalImages / days)
        val floorTotal = floor * days

        val allocation = if (floorTotal >= totalImages) {
            distributeEvenly(totalImages, days)
        } else {
            val bonus = totalImages - floorTotal
            val bonusAllocation = distributeByDecreasingWeights(bonus, days)
            IntArray(days) { floor + bonusAllocation[it] }
        }
        // Hard guarantee: never allocate more than the plan limit.
        return enforceExactTotal(allocation, totalImages)
    }

    /** Caps / pads so sum(allocation) == totalImages exactly. */
    private fun enforceExactTotal(allocation: IntArray, totalImages: Int): IntArray {
        if (allocation.isEmpty()) return allocation
        val sum = allocation.sum()
        if (sum == totalImages) return allocation
        val result = allocation.copyOf()
        if (sum > totalImages) {
            var overflow = sum - totalImages
            for (i in result.indices.reversed()) {
                if (overflow <= 0) break
                val cut = minOf(result[i], overflow)
                result[i] -= cut
                overflow -= cut
            }
        } else {
            result[result.lastIndex] += totalImages - sum
        }
        return result
    }

    /** Total images unlocked through [dayIndex] (0-based), including skipped days. Never exceeds [totalImages]. */
    fun cumulativeAllowed(totalImages: Int, periodDays: Int, dayIndex: Int): Int {
        if (totalImages <= 0 || periodDays <= 0 || dayIndex < 0) return 0
        val distribution = distributeDecreasing(totalImages, periodDays)
        if (distribution.isEmpty()) return 0
        val lastIndex = dayIndex.coerceAtMost(distribution.lastIndex)
        return distribution.take(lastIndex + 1).sum().coerceAtMost(totalImages)
    }

    fun dailyAllocation(totalImages: Int, periodDays: Int, dayIndex: Int): Int {
        if (totalImages <= 0 || periodDays <= 0 || dayIndex < 0) return 0
        val distribution = distributeDecreasing(totalImages, periodDays)
        if (dayIndex >= distribution.size) return 0
        return distribution[dayIndex]
    }

    private fun distributeEvenly(totalImages: Int, days: Int): IntArray {
        val base = totalImages / days
        val remainder = totalImages % days
        return IntArray(days) { index -> base + if (index < remainder) 1 else 0 }
    }

    private fun distributeByDecreasingWeights(totalImages: Int, days: Int): IntArray {
        if (days <= 0 || totalImages <= 0) return IntArray(0)
        if (days == 1) return intArrayOf(totalImages)

        val weights = IntArray(days) { days - it }
        val weightSum = days * (days + 1) / 2

        val allocation = IntArray(days)
        var assigned = 0
        val fractions = DoubleArray(days)

        for (index in 0 until days) {
            val exact = totalImages * weights[index].toDouble() / weightSum
            allocation[index] = exact.toInt()
            fractions[index] = exact - allocation[index]
            assigned += allocation[index]
        }

        var remainder = totalImages - assigned
        val order = fractions.indices.sortedByDescending { fractions[it] }
        var orderIndex = 0
        while (remainder > 0) {
            allocation[order[orderIndex % days]]++
            remainder--
            orderIndex++
        }

        return allocation
    }
}
