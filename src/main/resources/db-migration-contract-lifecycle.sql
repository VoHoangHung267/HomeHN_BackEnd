ALTER TABLE rental_bookings
    MODIFY COLUMN status ENUM(
        'REQUESTED',
        'PENDING_PAYMENT',
        'DEPOSIT_PAID',
        'ACTIVE',
        'EXPIRING_SOON',
        'RENEWAL_PENDING',
        'REJECTED',
        'CANCELLED',
        'PAYMENT_FAILED',
        'COMPLETED'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT';

ALTER TABLE rooms
    MODIFY COLUMN status ENUM(
        'ACTIVE',
        'PENDING',
        'REJECTED',
        'HIDDEN',
        'EXPIRED',
        'RENTED',
        'AVAILABLE_SOON',
        'HIDDEN_REVIEW'
    ) NOT NULL DEFAULT 'PENDING';
