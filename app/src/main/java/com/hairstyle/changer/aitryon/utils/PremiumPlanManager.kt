package com.hairstyle.changer.aitryon.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.di.billing.BillingProductIds
import com.hairstyle.changer.aitryon.model.ActivePlanRecord
import com.hairstyle.changer.aitryon.model.PremiumPlanDetailsRoot
import com.hairstyle.changer.aitryon.model.PremiumPlanItem
import com.hairstyle.changer.aitryon.ui.premium.PremiumPlan
import java.lang.reflect.Type
import java.util.Locale

object PremiumPlanManager {

    private const val TAG = "PremiumPlanManager"

    enum class ImageAccessBlockReason {
        NONE,
        DAILY_LIMIT,
        PERIOD_LIMIT
    }

    private val gson = Gson()
    // R8-safe: do NOT use `object : TypeToken<List<...>>() {}` (strips generic signature → crash)
    private val plansListType: Type =
        TypeToken.getParameterized(List::class.java, ActivePlanRecord::class.java).type

    private const val PLAN_YEAR = "year"
    private const val PLAN_MONTH = "month"
    private const val PLAN_WEEK = "week"

    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val YEARLY_MONTH_DAYS = 30

    /** Static plan lengths — not driven by Firebase. */
    const val DAYS_YEARLY = 365
    const val DAYS_MONTHLY = 29
    const val DAYS_WEEKLY = 7

    private data class QuotaPeriod(
        val totalImages: Int,
        val periodDays: Int,
        val periodStartedAt: Long
    )

    fun getDetails(): PremiumPlanDetailsRoot {
        val json = PrefManager.getPremiumPlanDetailsJson()
        if (json.isBlank()) return defaultDetails()
        return runCatching {
            gson.fromJson(json, PremiumPlanDetailsRoot::class.java)
        }.getOrElse {
            // Falling back here means Firebase values are ignored — never fail silently.
//            Log.w(TAG, "premium_plan_detils parse failed, using defaults: $json", it)
            null
        } ?: defaultDetails()
    }

    fun hasRemotePlanDetails(): Boolean {
        return PrefManager.getPremiumPlanDetailsJson().isNotBlank()
    }

    fun planDays(plan: PremiumPlan): Int = when (plan) {
        PremiumPlan.YEARLY -> DAYS_YEARLY
        PremiumPlan.MONTHLY -> DAYS_MONTHLY
        PremiumPlan.WEEKLY -> DAYS_WEEKLY
    }

    fun getPlan(plan: PremiumPlan): PremiumPlanItem {
        val details = getDetails()
        // Prefer new Firebase price-only keys (year_price / month_price / week_price).
        val priceOnly = when (plan) {
            PremiumPlan.YEARLY -> details.yearPrice
            PremiumPlan.MONTHLY -> details.monthPrice
            PremiumPlan.WEEKLY -> details.weekPrice
        }?.trim()?.takeIf { it.isNotEmpty() }

        // OLD nested objects — only used if price-only keys missing (legacy cached JSON).
        val nested = when (plan) {
            PremiumPlan.YEARLY -> details.year
            PremiumPlan.MONTHLY -> details.month
            PremiumPlan.WEEKLY -> details.week
        }

        val base = when {
            priceOnly != null -> (nested ?: defaultPlan(plan)).copy(price = priceOnly)
            nested != null -> nested
            else -> defaultPlan(plan)
        }
        // days + ads are app-owned: premium never shows ads; days are static.
        // image_access / monthly_add no longer enforced on client.
        return base.copy(days = planDays(plan), adsShow = false)
    }

    fun getFreeUserPlan(): PremiumPlanItem {
        val details = getDetails()
        val priceOnly = details.freeUserPrice?.trim()?.takeIf { it.isNotEmpty() }
        val nested = details.freeUser
        return when {
            priceOnly != null -> (nested ?: defaultFreeUserPlan()).copy(price = priceOnly)
            nested != null -> nested
            else -> defaultFreeUserPlan()
        }
    }

    fun formatPrice(price: Double): String {
        return if (price % 1.0 == 0.0) {
            "$${price.toInt()}"
        } else {
            String.format(Locale.US, "$%.2f", price)
        }
    }

    fun formatOldPrice(item: PremiumPlanItem): String {
        val discount = item.discountValue()
        val price = item.priceValue()
        if (discount <= 0.0 || price <= 0.0) return ""
        val oldPrice = price / (1.0 - (discount / 100.0))
        return formatPrice(oldPrice)
    }

    fun formatDiscountBadge(discount: Double): String {
        if (discount <= 0.0) return ""
        return if (discount % 1.0 == 0.0) {
            "${discount.toInt()}% OFF"
        } else {
            String.format(Locale.US, "%.1f%% OFF", discount)
        }
    }

    fun formatDays(days: Int): String {
        val safeDays = days.takeIf { it > 0 } ?: 1
        return "$safeDays Days"
    }

    fun formatPlanDays(plan: PremiumPlan): String = formatDays(planDays(plan))

    /**
     * Yearly Play Console price → equivalent weekly amount (price / 52).
     * Returns currency-formatted weekly price only (e.g. "₹34.59").
     */
    fun formatYearlyAsWeeklyPrice(
        yearlyFormattedPrice: String,
        amountMicros: Long? = null,
        currencyCode: String? = null
    ): String {
        val weeklyAmount = when {
            amountMicros != null && amountMicros > 0L -> amountMicros / 1_000_000.0 / WEEKS_PER_YEAR
            else -> {
                val yearly = PremiumPlanItem.parseNumeric(yearlyFormattedPrice)
                if (yearly <= 0.0) return ""
                yearly / WEEKS_PER_YEAR
            }
        }
        return formatMoney(
            amount = weeklyAmount,
            currencyCode = currencyCode,
            fallbackFormattedSample = yearlyFormattedPrice
        )
    }

    private fun formatMoney(
        amount: Double,
        currencyCode: String?,
        fallbackFormattedSample: String
    ): String {
        if (!currencyCode.isNullOrBlank()) {
            return runCatching {
                val formatter = java.text.NumberFormat.getCurrencyInstance(Locale.getDefault())
                formatter.currency = java.util.Currency.getInstance(currencyCode)
                formatter.maximumFractionDigits = 2
                formatter.minimumFractionDigits = 2
                formatter.format(amount)
            }.getOrElse {
                formatWithSampleSymbol(amount, fallbackFormattedSample)
            }
        }
        return formatWithSampleSymbol(amount, fallbackFormattedSample)
    }

    private fun formatWithSampleSymbol(amount: Double, sample: String): String {
        val symbol = sample.trim().takeWhile { !it.isDigit() && it != '.' && it != ',' }
            .ifBlank { "$" }
        return String.format(Locale.US, "%s%.2f", symbol, amount)
    }

    private const val WEEKS_PER_YEAR = 52.0

    fun getProductId(plan: PremiumPlan): String {
        val fromConfig = when (plan) {
            PremiumPlan.YEARLY -> getDetails().year?.productId
            PremiumPlan.MONTHLY -> getDetails().month?.productId
            PremiumPlan.WEEKLY -> getDetails().week?.productId
        }?.trim().orEmpty()
        if (fromConfig.isNotBlank()) return fromConfig
        return BillingProductIds.productIdFor(plan)
    }

    /** Resolve Play product id → plan (Firebase productId or default BillingProductIds). */
    fun planForProductId(productId: String): PremiumPlan? {
        if (productId.isBlank()) return null
        BillingProductIds.planFor(productId)?.let { return it }
        // Google Play / RC may return "productId:basePlanId"
        val baseId = productId.substringBefore(':')
        if (baseId != productId) {
            BillingProductIds.planFor(baseId)?.let { return it }
        }
        return PremiumPlan.entries.firstOrNull { plan ->
            val configured = getProductId(plan)
            configured == productId || configured == baseId ||
                productId.startsWith("$configured:")
        }
    }

    /**
     * Keep only plans that Google Play still reports as purchased.
     * Empty [keep] = Play has no active subscription → clear local premium.
     */
    fun retainOnlyPlayActivePlans(keep: Set<PremiumPlan>) {
        val keepKeys = keep.map { planKey(it) }.toSet()
        val loaded = loadPlans()
        val filtered = loaded.filter { it.planKey in keepKeys && it.planKey.isNotBlank() }
        if (filtered.isEmpty()) {
            if (loaded.isNotEmpty() || PrefManager.getActivePremiumPlan().isNotBlank()) {
                clearActivePlan()
            } else {
                syncAdsFreeFlag()
            }
            return
        }
        if (filtered.size != loaded.size || filtered.map { it.planKey } != loaded.map { it.planKey }) {
            savePlans(filtered)
            syncLegacyFields(filtered)
            refreshCachedAvailability(filtered)
        }
        syncAdsFreeFlag()
    }

    fun syncAdsFreeFlag() {
        ADSMainClass.setAds_Free(isPremiumActive())
    }

    /** Prune locally expired plans on cold start (before Google Play responds). */
    fun refreshLocalExpiryOnLaunch() {
        getActivePlans()
        syncAdsFreeFlag()
    }

    /**
     * Updates the highest-tier active plan's purchase / expiry from Google Play when available.
     * Used so generate API can send accurate `purchase_date` / `expiry_date`.
     *
     * Never writes a past [expiryMs] — that would make [getActivePlans] prune the plan while
     * Google Play still reports the subscription as active (billing-retry).
     */
    fun updatePrimaryPlanDates(purchaseMs: Long?, expiryMs: Long?) {
        if (purchaseMs == null && expiryMs == null) return
        val now = System.currentTimeMillis()
        val plans = loadPlansMutable()
        if (plans.isEmpty()) return
        val primaryKey = getActivePlans().firstOrNull()?.planKey ?: return
        val idx = plans.indexOfFirst { it.planKey == primaryKey }
        if (idx < 0) return
        val current = plans[idx]
        val nextStarted = purchaseMs?.takeIf { it > 0L } ?: current.startedAt
        // Ignore past RC expiry while sub is still considered active by the caller.
        val safeRcExpiry = expiryMs?.takeIf { it > now }
        val nextExpires = when {
            safeRcExpiry != null -> safeRcExpiry
            current.expiresAt > now -> current.expiresAt
            else -> now + planDays(planFromKey(current.planKey) ?: PremiumPlan.WEEKLY) * DAY_MS
        }
        if (nextStarted == current.startedAt && nextExpires == current.expiresAt) return
        plans[idx] = current.copy(
            startedAt = nextStarted,
            expiresAt = nextExpires,
            periodStartedAt = current.periodStartedAt.takeIf { it > 0L } ?: nextStarted
        )
        savePlans(plans)
        syncLegacyFields(plans)
    }

    /** Adds [plan] while keeping other active plans. Same plan type is replaced. */
    fun activatePlan(
        plan: PremiumPlan,
        purchaseMs: Long? = null,
        expiryMs: Long? = null
    ) {
        val now = System.currentTimeMillis()
        val startedAt = purchaseMs?.takeIf { it > 0L } ?: now
        // RC may send a past expirationDate while entitlement.isActive is still true —
        // never store a past expiry or premium disappears on the next prune.
        val expiresAt = expiryMs?.takeIf { it > now }
            ?: (now + planDays(plan) * DAY_MS)
        val key = planKey(plan)
        val plans = loadPlansMutable()
        plans.removeAll { it.planKey == key }
        plans.add(
            ActivePlanRecord(
                planKey = key,
                startedAt = startedAt,
                expiresAt = expiresAt,
                periodStartedAt = startedAt,
                periodImagesUsed = 0,
                currentDayIndex = 0
            )
        )
        savePlans(plans)
        syncLegacyFields(plans)
        refreshCachedAvailability(plans)
        syncAdsFreeFlag()
    }

    /**
     * Updates a specific plan's purchase / expiry from server (check-free-limit subscription).
     * Ignores past expiry timestamps (same rule as [updatePrimaryPlanDates]).
     */
    fun syncPlanDatesFromServer(serverPlanKey: String, purchaseMs: Long?, expiryMs: Long?) {
        if (purchaseMs == null && expiryMs == null) return
        val plan = planFromKey(serverPlanKey) ?: return
        val now = System.currentTimeMillis()
        val safeExpiry = expiryMs?.takeIf { it > now }
        if (!isPlanActive(plan)) {
            if (safeExpiry != null || purchaseMs != null) {
                activatePlan(plan, purchaseMs = purchaseMs, expiryMs = safeExpiry)
            }
            return
        }
        val plans = loadPlansMutable()
        val key = planKey(plan)
        val idx = plans.indexOfFirst { it.planKey == key }
        if (idx < 0) return
        val current = plans[idx]
        val nextStarted = purchaseMs?.takeIf { it > 0L } ?: current.startedAt
        val nextExpires = when {
            safeExpiry != null -> safeExpiry
            current.expiresAt > now -> current.expiresAt
            else -> now + planDays(plan) * DAY_MS
        }
        if (nextStarted == current.startedAt && nextExpires == current.expiresAt) return
        plans[idx] = current.copy(
            startedAt = nextStarted,
            expiresAt = nextExpires,
            periodStartedAt = current.periodStartedAt.takeIf { it > 0L } ?: nextStarted
        )
        savePlans(plans)
        syncLegacyFields(plans)
    }


    fun isPremiumActive(): Boolean = getActivePlans().isNotEmpty()

    fun isPlanActive(plan: PremiumPlan): Boolean {
        val key = planKey(plan)
        return getActivePlans().any { it.planKey == key }
    }

    /** All non-expired plans, highest tier first (Yearly → Monthly → Weekly). */
    fun getActivePlans(): List<ActivePlanRecord> {
        val loaded = loadPlans()
//        Log.d(TAG, "DDDDDDD   getActivePlans: " + loaded)
        val pruned = pruneExpired(loaded)
        saveIfChanged(pruned, loaded)
        return pruned.sortedWith(compareByDescending { planRank(it.planKey) })
    }

    /** Highest-tier active plan (for primary UI / upgrade ladder). */
    fun getActivePlan(): PremiumPlan? {
        val record = getActivePlans().firstOrNull() ?: return null
        return planFromKey(record.planKey)
    }

    fun getActivePlanConfig(): PremiumPlanItem? {
        val plan = getActivePlan() ?: return null
        return getPlan(plan)
    }

    /** Premium users never see ads; everyone else always does. */
    fun shouldShowAds(): Boolean = !isPremiumActive()

    fun getImageAccessLimit(): Int = getRemainingImageAccess()

    fun getRemainingImageAccess(): Int {
        // TEMP: client image_access disabled — backend free-limit API will manage this.
        return Int.MAX_VALUE
        /*
        refreshPremiumQuotaIfNeeded()
        if (!isPremiumActive()) {
            // Firebase `free_user.image_access` is exact — 0 means no generations.
            val limit = freeImageLimit()
            return (limit - PrefManager.getFreeImagesUsed()).coerceAtLeast(0)
        }
        return getActivePlans().sumOf { availableForRecord(it) }
        */
    }

    @Suppress("unused") // Kept for when client image_access is re-enabled
    private fun freeImageLimit(): Int = getFreeUserPlan().imageAccess.coerceAtLeast(0)

    fun getImageAccessBlockReason(): ImageAccessBlockReason {
        // TEMP: client image_access disabled — always allow generate.
        return ImageAccessBlockReason.NONE
        /*
        refreshPremiumQuotaIfNeeded()

        if (!isPremiumActive()) {
            if (PrefManager.getFreeImagesUsed() >= freeImageLimit()) {
                return ImageAccessBlockReason.PERIOD_LIMIT
            }
            return ImageAccessBlockReason.NONE
        }

        var sawDaily = false
        var sawPeriod = false
        for (record in getActivePlans()) {
            when (blockReasonForRecord(record)) {
                ImageAccessBlockReason.NONE -> return ImageAccessBlockReason.NONE
                ImageAccessBlockReason.DAILY_LIMIT -> sawDaily = true
                ImageAccessBlockReason.PERIOD_LIMIT -> sawPeriod = true
            }
        }
        return when {
            sawDaily -> ImageAccessBlockReason.DAILY_LIMIT
            sawPeriod -> ImageAccessBlockReason.PERIOD_LIMIT
            else -> ImageAccessBlockReason.PERIOD_LIMIT
        }
        */
    }

    fun canGenerateImage(): Boolean {
        // TEMP: client image_access disabled — backend free-limit API will manage this.
        return true
        // return getImageAccessBlockReason() == ImageAccessBlockReason.NONE
    }

    fun consumeImageAccess(): Boolean {
        // TEMP: no client quota decrement until free-limit API is ready.
        return true
        /*
        synchronized(quotaLock) {
            if (!canGenerateImage()) return false
            if (!isPremiumActive()) {
                val limit = freeImageLimit()
                val used = PrefManager.getFreeImagesUsed()
                if (used >= limit) return false
                PrefManager.setFreeImagesUsed((used + 1).coerceAtMost(limit))
                return true
            }

            val plans = loadPlansMutable()
            // Prefer highest tier that still has available quota today.
            val ordered = plans.sortedWith(compareByDescending { planRank(it.planKey) })
            val target = ordered.firstOrNull {
                availableForRecord(it) > 0 &&
                    blockReasonForRecord(it) == ImageAccessBlockReason.NONE
            } ?: return false

            val plan = planFromKey(target.planKey) ?: return false
            val period = buildQuotaPeriod(plan, getPlan(plan), target.periodStartedAt) ?: return false
            val maxAllowed = minOf(
                period.totalImages,
                ImageQuotaDistributor.cumulativeAllowed(
                    period.totalImages,
                    period.periodDays,
                    currentDayIndex(period.periodStartedAt)
                )
            )
            if (target.periodImagesUsed >= maxAllowed) return false

            val idx = plans.indexOfFirst {
                it.planKey == target.planKey && it.startedAt == target.startedAt
            }
            if (idx < 0) return false

            val nextUsed = (target.periodImagesUsed + 1).coerceAtMost(maxAllowed)
            if (nextUsed <= target.periodImagesUsed) return false

            plans[idx] = target.copy(periodImagesUsed = nextUsed)
            savePlans(plans)
            syncLegacyFields(plans)
            refreshCachedAvailability(plans)
            return true
        }
        */
    }

    @Suppress("unused") // Used by commented consumeImageAccess body
    private val quotaLock = Any()

    fun clearActivePlan() {
        PrefManager.setActivePremiumPlansJson("")
        PrefManager.setActivePremiumPlan("")
        PrefManager.setPremiumExpiresAt(0L)
        PrefManager.setPremiumImagesUsed(0)
        PrefManager.setPremiumImageAccessLimit(0)
        PrefManager.setPremiumPurchaseToken("")
        PrefManager.setPremiumProductId("")
        PrefManager.setYearlyLastMonthlyCreditAt(0L)
        PrefManager.setPremiumPlanStartedAt(0L)
        PrefManager.setPremiumQuotaPeriodStartedAt(0L)
        PrefManager.setPremiumPeriodImagesUsed(0)
        PrefManager.setPremiumCurrentDayIndex(-1)
        syncAdsFreeFlag()
    }

    fun refreshPremiumQuotaIfNeeded() {
        val plans = loadPlansMutable()
        if (plans.isEmpty()) return

        val now = System.currentTimeMillis()
        var changed = false
        for (i in plans.indices) {
            var record = plans[i]
            val plan = planFromKey(record.planKey) ?: continue
            val config = getPlan(plan)

            if (plan == PremiumPlan.YEARLY) {
                val refreshed = refreshYearlyMonthlyPeriodIfNeeded(record, config, now)
                if (refreshed != record) {
                    record = refreshed
                    changed = true
                }
            }

            val dayIndex = currentDayIndex(record.periodStartedAt)
            if (dayIndex != record.currentDayIndex) {
                record = record.copy(currentDayIndex = dayIndex)
                changed = true
            }
            plans[i] = record
        }

        if (changed) {
            savePlans(plans)
            syncLegacyFields(plans)
        }
        refreshCachedAvailability(plans)
    }

    private fun refreshYearlyMonthlyPeriodIfNeeded(
        record: ActivePlanRecord,
        config: PremiumPlanItem,
        now: Long
    ): ActivePlanRecord {
        val periodStart = record.periodStartedAt
        if (periodStart <= 0L) {
            return record.copy(
                periodStartedAt = now,
                periodImagesUsed = 0,
                currentDayIndex = 0
            )
        }
        if (currentDayIndex(periodStart) < YEARLY_MONTH_DAYS) return record
        return record.copy(
            periodStartedAt = now,
            periodImagesUsed = 0,
            currentDayIndex = 0
        )
    }

    /** Debug helper: how the period total is split per day and what today unlocks. */
    fun quotaBreakdown(): String {
        val records = getActivePlans()
        if (records.isEmpty()) {
            val limit = freeImageLimit()
            val used = PrefManager.getFreeImagesUsed()
            return "free_user total=$limit used=$used left=${(limit - used).coerceAtLeast(0)}"
        }
        return records.joinToString(" | ") { record ->
            val plan = planFromKey(record.planKey)
            val period = plan?.let { buildQuotaPeriod(it, getPlan(it), record.periodStartedAt) }
                ?: return@joinToString "${record.planKey}: no quota (total=0 or not started)"
            val dayIndex = currentDayIndex(period.periodStartedAt)
            val perDay = ImageQuotaDistributor.distributeDecreasing(
                period.totalImages,
                period.periodDays
            )
            val today = ImageQuotaDistributor.dailyAllocation(
                period.totalImages,
                period.periodDays,
                dayIndex
            )
            val unlocked = ImageQuotaDistributor.cumulativeAllowed(
                period.totalImages,
                period.periodDays,
                dayIndex
            )
            "${record.planKey}: total=${period.totalImages} days=${period.periodDays} " +
                "perDay=[${perDay.joinToString(",")}] " +
                "today=day${dayIndex + 1}(+$today) unlockedSoFar=$unlocked " +
                "used=${record.periodImagesUsed} " +
                "leftToday=${(unlocked - record.periodImagesUsed).coerceAtLeast(0)}"
        }
    }

    private fun availableForRecord(record: ActivePlanRecord): Int {
        val plan = planFromKey(record.planKey) ?: return 0
        val period = buildQuotaPeriod(plan, getPlan(plan), record.periodStartedAt) ?: return 0
        return availableInCurrentPeriod(period, record.periodImagesUsed)
    }

    private fun blockReasonForRecord(record: ActivePlanRecord): ImageAccessBlockReason {
        val plan = planFromKey(record.planKey) ?: return ImageAccessBlockReason.PERIOD_LIMIT
        val period = buildQuotaPeriod(plan, getPlan(plan), record.periodStartedAt)
            ?: return ImageAccessBlockReason.PERIOD_LIMIT
        val periodUsed = record.periodImagesUsed

        if (periodUsed >= period.totalImages) {
            return ImageAccessBlockReason.PERIOD_LIMIT
        }

        val dayIndex = currentDayIndex(period.periodStartedAt)
        if (dayIndex >= period.periodDays) {
            return ImageAccessBlockReason.PERIOD_LIMIT
        }

        val cumulativeAllowed = ImageQuotaDistributor.cumulativeAllowed(
            period.totalImages,
            period.periodDays,
            dayIndex
        )
        if (periodUsed >= cumulativeAllowed) {
            return ImageAccessBlockReason.DAILY_LIMIT
        }
        return ImageAccessBlockReason.NONE
    }

    private fun refreshCachedAvailability(plans: List<ActivePlanRecord>) {
        val remaining = plans.sumOf { availableForRecord(it) }
        PrefManager.setPremiumImageAccessLimit(remaining)
    }

    private fun availableInCurrentPeriod(period: QuotaPeriod, periodUsed: Int): Int {
        if (period.totalImages <= 0) return 0
        if (periodUsed >= period.totalImages) return 0

        val dayIndex = currentDayIndex(period.periodStartedAt)
        if (dayIndex >= period.periodDays) return 0

        val cumulativeAllowed = ImageQuotaDistributor.cumulativeAllowed(
            period.totalImages,
            period.periodDays,
            dayIndex
        ).coerceAtMost(period.totalImages)

        if (periodUsed >= cumulativeAllowed) return 0

        val unlockedRemaining = (cumulativeAllowed - periodUsed).coerceAtLeast(0)
        val periodRemaining = (period.totalImages - periodUsed).coerceAtLeast(0)
        return minOf(unlockedRemaining, periodRemaining)
    }

    private fun buildQuotaPeriod(
        plan: PremiumPlan,
        config: PremiumPlanItem,
        periodStartedAt: Long
    ): QuotaPeriod? {
        if (periodStartedAt <= 0L) return null
        return when (plan) {
            PremiumPlan.WEEKLY -> QuotaPeriod(
                totalImages = config.imageAccess.coerceAtLeast(0),
                periodDays = planDays(plan),
                periodStartedAt = periodStartedAt
            )
            PremiumPlan.MONTHLY -> QuotaPeriod(
                totalImages = config.imageAccess.coerceAtLeast(0),
                periodDays = planDays(plan),
                periodStartedAt = periodStartedAt
            )
            PremiumPlan.YEARLY -> {
                val monthlyPool = config.monthlyAdd.coerceAtLeast(0)
                if (monthlyPool <= 0) return null
                QuotaPeriod(
                    totalImages = monthlyPool,
                    periodDays = YEARLY_MONTH_DAYS,
                    periodStartedAt = periodStartedAt
                )
            }
        }
    }

    private fun currentDayIndex(periodStartedAt: Long): Int {
        if (periodStartedAt <= 0L) return 0
        return ((System.currentTimeMillis() - periodStartedAt) / DAY_MS).toInt().coerceAtLeast(0)
    }

    private fun loadPlans(): List<ActivePlanRecord> {
        val json = PrefManager.getActivePremiumPlansJson()
//        Log.d(TAG, "DDDDDDD   json: " + json)
        if (json.isNotBlank()) {
            return runCatching {
                gson.fromJson<List<ActivePlanRecord>>(json, plansListType).orEmpty()
            }.getOrDefault(emptyList())
        }
        return migrateLegacySinglePlan()
    }

    private fun loadPlansMutable(): MutableList<ActivePlanRecord> {
        val loaded = loadPlans()
        val pruned = pruneExpired(loaded)
        saveIfChanged(pruned, loaded)
        return pruned.toMutableList()
    }

    private fun migrateLegacySinglePlan(): List<ActivePlanRecord> {
        val key = PrefManager.getActivePremiumPlan()
        if (key.isBlank()) return emptyList()
        val expiresAt = PrefManager.getPremiumExpiresAt()
        if (expiresAt <= 0L || System.currentTimeMillis() > expiresAt) {
            clearActivePlan()
            return emptyList()
        }
        val startedAt = PrefManager.getPremiumPlanStartedAt().takeIf { it > 0L }
            ?: (expiresAt - planDays(planFromKey(key) ?: PremiumPlan.WEEKLY) * DAY_MS)
        val periodStarted = PrefManager.getPremiumQuotaPeriodStartedAt().takeIf { it > 0L }
            ?: startedAt
        val record = ActivePlanRecord(
            planKey = key,
            startedAt = startedAt,
            expiresAt = expiresAt,
            periodStartedAt = periodStarted,
            periodImagesUsed = PrefManager.getPremiumPeriodImagesUsed().coerceAtLeast(0),
            currentDayIndex = PrefManager.getPremiumCurrentDayIndex().coerceAtLeast(0)
        )
        savePlans(listOf(record))
        return listOf(record)
    }

    private fun pruneExpired(plans: List<ActivePlanRecord>): List<ActivePlanRecord> {
        val now = System.currentTimeMillis()
        return plans.filter { it.expiresAt > now && it.planKey.isNotBlank() }
    }

    private fun saveIfChanged(pruned: List<ActivePlanRecord>, original: List<ActivePlanRecord>) {
        if (pruned.size == original.size) return
        if (pruned.isEmpty()) {
            clearActivePlan()
            return
        }
        savePlans(pruned)
        syncLegacyFields(pruned)
        syncAdsFreeFlag()
    }

    private fun savePlans(plans: List<ActivePlanRecord>) {
//        Log.d(TAG, "DDDDDDD   plans: " + plans)
        PrefManager.setActivePremiumPlansJson(gson.toJson(plans))
    }

    /** Keep single-plan prefs in sync for any leftover readers. */
    private fun syncLegacyFields(plans: List<ActivePlanRecord>) {
        val ordered = plans.sortedWith(compareByDescending { planRank(it.planKey) })
        val primary = ordered.firstOrNull()
        if (primary == null) {
            PrefManager.setActivePremiumPlan("")
            PrefManager.setPremiumExpiresAt(0L)
            PrefManager.setPremiumPlanStartedAt(0L)
            PrefManager.setPremiumQuotaPeriodStartedAt(0L)
            PrefManager.setPremiumPeriodImagesUsed(0)
            PrefManager.setPremiumCurrentDayIndex(-1)
            PrefManager.setYearlyLastMonthlyCreditAt(0L)
            return
        }
        PrefManager.setActivePremiumPlan(primary.planKey)
        PrefManager.setPremiumExpiresAt(ordered.maxOf { it.expiresAt })
        PrefManager.setPremiumPlanStartedAt(primary.startedAt)
        PrefManager.setPremiumQuotaPeriodStartedAt(primary.periodStartedAt)
        PrefManager.setPremiumPeriodImagesUsed(primary.periodImagesUsed)
        PrefManager.setPremiumCurrentDayIndex(primary.currentDayIndex)
        PrefManager.setYearlyLastMonthlyCreditAt(
            if (primary.planKey == PLAN_YEAR) primary.periodStartedAt else 0L
        )
    }

    private fun planKey(plan: PremiumPlan): String = when (plan) {
        PremiumPlan.YEARLY -> PLAN_YEAR
        PremiumPlan.MONTHLY -> PLAN_MONTH
        PremiumPlan.WEEKLY -> PLAN_WEEK
    }

    fun planFromKey(key: String): PremiumPlan? = when (key) {
        PLAN_YEAR -> PremiumPlan.YEARLY
        PLAN_MONTH -> PremiumPlan.MONTHLY
        PLAN_WEEK -> PremiumPlan.WEEKLY
        else -> null
    }

    private fun planRank(key: String): Int = when (key) {
        PLAN_YEAR -> 3
        PLAN_MONTH -> 2
        PLAN_WEEK -> 1
        else -> 0
    }

    private fun defaultDetails(): PremiumPlanDetailsRoot = PremiumPlanDetailsRoot(
        yearPrice = "$49.99",
        monthPrice = "$9.99",
        weekPrice = "$4.99",
        freeUserPrice = "$0",
        // OLD nested defaults (kept for getPlan fallback when building PremiumPlanItem)
        year = PremiumPlanItem(
            price = "$49.99",
            days = DAYS_YEARLY,
            // imageAccess = 900,
            // monthlyAdd = 75,
            adsShow = false
        ),
        month = PremiumPlanItem(
            price = "$9.99",
            days = DAYS_MONTHLY,
            // imageAccess = 150,
            adsShow = false
        ),
        week = PremiumPlanItem(
            price = "$4.99",
            days = DAYS_WEEKLY,
            // imageAccess = 50,
            adsShow = false
        ),
        freeUser = defaultFreeUserPlan()
    )

    private fun defaultFreeUserPlan(): PremiumPlanItem = PremiumPlanItem(
        price = "$0",
        discount = "0",
        days = 0,
        // imageAccess = 3, // OLD client free quota — disabled
        adsShow = true
    )

    private fun defaultPlan(plan: PremiumPlan): PremiumPlanItem = when (plan) {
        PremiumPlan.YEARLY -> defaultDetails().year!!
        PremiumPlan.MONTHLY -> defaultDetails().month!!
        PremiumPlan.WEEKLY -> defaultDetails().week!!
    }
}
