ALTER TABLE rental_bookings
    MODIFY COLUMN status ENUM(
        'REQUESTED',
        'PENDING_PAYMENT',
        'DEPOSIT_PAID',
        'ACTIVE',
        'EXPIRING_SOON',
        'RENEWAL_PENDING',
        'EARLY_TERMINATION_PENDING',
        'REJECTED',
        'CANCELLED',
        'PAYMENT_FAILED',
        'COMPLETED'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT';
