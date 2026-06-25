package com.proautokimium.api.Application.DTOs.push;

/** Espelha o JSON do PushSubscription do navegador: { endpoint, keys: { p256dh, auth } }. */
public record PushSubscriptionRequest(String endpoint, Keys keys) {
    public record Keys(String p256dh, String auth) {}
}
