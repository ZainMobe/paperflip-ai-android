package com.wapp.paperflipai.core.monetization

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory entitlement store — the Android counterpart of
 * `MockEntitlementStore.swift`. Lets the paywall and the unlocked-Pro state
 * be demoed without a Play Billing sandbox account, and persists the result
 * so "Pro" survives a restart while you're building.
 */
class MockEntitlementStore(
    context: Context? = null,
    initial: Entitlement = Entitlement.Free,
) : EntitlementStore {

    private val prefs = context?.applicationContext
        ?.getSharedPreferences("paperflip_entitlement", Context.MODE_PRIVATE)

    private val _entitlement = MutableStateFlow(
        prefs?.getString(KEY, null)?.let { Entitlement.from(it) } ?: initial
    )
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    private val _availableProducts = MutableStateFlow(defaultProducts)
    override val availableProducts: StateFlow<List<PaywallProduct>> = _availableProducts.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Simulated latency for paywall actions. */
    var latencyMillis: Long = 700

    override suspend fun configure() {
        // No-op for the mock. Real Play Billing / RevenueCat init happens here.
    }

    override suspend fun restorePurchases() {
        delay(latencyMillis)
        // Mock: nothing to restore.
    }

    override suspend fun purchase(product: PaywallProduct) {
        delay(latencyMillis)
        set(
            when (product.cadence) {
                PaywallProduct.Cadence.Monthly -> Entitlement.Pro
                PaywallProduct.Cadence.Annual -> Entitlement.Annual
            }
        )
    }

    /** Dev-only: flip between free and pro from Settings. */
    fun set(value: Entitlement) {
        _entitlement.value = value
        prefs?.edit()?.putString(KEY, value.raw)?.apply()
    }

    private companion object {
        const val KEY = "entitlement.v1"

        // Mirrors the App Store setup: yearly first with a 7-day trial,
        // monthly with no trial. Weekly is intentionally not offered.
        val defaultProducts = listOf(
            PaywallProduct(
                id = "ai.paperflip.yearly",
                cadence = PaywallProduct.Cadence.Annual,
                priceLabel = "$44.99",
                monthlyEquivCents = 375,
                monthlyEquivLabel = "$3.75/mo",
                introLabel = "7-day free trial",
            ),
            PaywallProduct(
                id = "ai.paperflip.monthly",
                cadence = PaywallProduct.Cadence.Monthly,
                priceLabel = "$7.99",
                monthlyEquivCents = 799,
                monthlyEquivLabel = "$7.99/mo",
                introLabel = null,
            ),
        )
    }
}
