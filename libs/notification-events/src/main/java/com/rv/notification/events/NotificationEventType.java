package com.rv.notification.events;

public enum NotificationEventType {
    ACCOUNT_CREATED,
    /** One event → two email templates (outgoing / incoming). */
    PAYMENT_COMPLETED,
    CASHBACK_MONTHLY_PAYOUT
}
