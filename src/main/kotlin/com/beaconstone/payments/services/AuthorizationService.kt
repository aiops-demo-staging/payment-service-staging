package com.beaconstone.payments.services

import com.beaconstone.payments.adapters.adapterFor
import com.beaconstone.payments.config.authProfiles
import com.beaconstone.payments.events.emitPaymentSucceeded
import com.beaconstone.payments.repositories.AuthorizationRecord
import com.beaconstone.payments.repositories.authorizationRepository

/**
 * Authorise a single payment against its card network.
 *
 * The network's authorisation profile is resolved first, then handed to the adapter for
 * that network. Adapters validate the profile they are given, because presenting an
 * uncertified profile to the acquirer fails downstream in a way that is hard to diagnose.
 * The capture mode of the selected profile travels with the stored record.
 */
fun authorizePayment(payment: Payment): AuthorizationRecord {
    val profile = authProfiles.default
    val adapter = adapterFor(payment.network)

    val result = adapter.authorize(payment, profile)

    val record = authorizationRepository.save(
        AuthorizationRecord(
            paymentId = payment.paymentId,
            invoiceId = payment.invoiceId,
            network = payment.network,
            profileId = result.profileId,
            captureMode = result.captureMode,
            installmentPlan = result.installmentPlan,
            authorizationCode = result.authorizationCode,
            amountMinor = payment.amountMinor,
            capturedMinor = payment.capturedMinor ?: payment.amountMinor,
            currency = payment.currency,
            status = "authorized",
            idempotencyKey = payment.idempotencyKey,
        ),
    )

    emitPaymentSucceeded(record)
    return record
}
