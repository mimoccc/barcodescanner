package org.mjdev.libs.barcodescanner.bysquare.base

/**
 * Base bysquare document
 * This document is base for all invoices and payments
 */
@Suppress("SpellCheckingInspection")
interface BysquareDocument : IVerifiable {
    val invoiceId: String?
    val amount: Double
    val currency: String?
}
