package com.wapp.paperflipai.core.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * Subscription / entitlement abstraction — the Android counterpart of
 * `EntitlementStore.swift`. Same pattern as AuthService and RemoteStore:
 * screens talk to this interface so a Google Play Billing (or RevenueCat)
 * implementation drops in later without touching the paywall.
 */
enum class Entitlement(val raw: String) {
    Free("free"),
    Pro("pro"),          // active monthly
    Annual("annual"),    // active annual
    Trialing("trialing");

    val isActive: Boolean get() = this != Free

    /** Cap on cards per generated deck. */
    val cardCap: Int get() = if (this == Free) 20 else 50

    /** Cap on decks created per calendar month; null means unlimited. */
    val monthlyDeckCap: Int? get() = if (this == Free) 5 else null

    companion object {
        fun from(raw: String?): Entitlement = entries.firstOrNull { it.raw == raw } ?: Free
    }
}

/** A subscription product surfaced in the paywall. */
data class PaywallProduct(
    val id: String,
    val cadence: Cadence,
    /** Localised price string, e.g. "$9.99". */
    val priceLabel: String,
    /** Cents per month, for "save X%" maths without parsing [priceLabel]. */
    val monthlyEquivCents: Int,
    /** Localised monthly-equivalent string, e.g. "$6.66/mo". */
    val monthlyEquivLabel: String,
    /** Optional intro / trial copy, shown subtly. */
    val introLabel: String? = null,
) {
    enum class Cadence { Monthly, Annual }
}

interface EntitlementStore {
    val entitlement: StateFlow<Entitlement>
    val availableProducts: StateFlow<List<PaywallProduct>>
    val lastError: StateFlow<String?>

    /** Cold-start. Loads the cached entitlement and subscribes to updates. */
    suspend fun configure()

    suspend fun restorePurchases()
    suspend fun purchase(product: PaywallProduct)
}

/** Whether the user can generate another deck this month. */
fun EntitlementStore.canGenerateDeck(usedThisMonth: Int): Boolean {
    val cap = entitlement.value.monthlyDeckCap ?: return true
    return usedThisMonth < cap
}
