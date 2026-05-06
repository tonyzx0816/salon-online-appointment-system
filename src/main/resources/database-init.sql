DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS availability_slots;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS providers;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(30) NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('CUSTOMER','PROVIDER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE providers
(
    provider_id  INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NULL,
    display_name VARCHAR(200) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_providers_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    -- At most one provider profile per user account (if linked)
    UNIQUE KEY uq_providers_user_id (user_id),

    KEY          idx_providers_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE services
(
    service_id       INT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    duration_minutes INT          NOT NULL,
    price            INT          NOT NULL, -- store as integer cents (recommended)
    active           BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_service_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_service_price CHECK (price >= 0),

    KEY              idx_services_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE availability_slots
(
    slot_id     INT AUTO_INCREMENT PRIMARY KEY,
    version     INT       NOT NULL DEFAULT 0,
    provider_id INT       NOT NULL,
    service_id  INT NULL,
    start_time  DATETIME  NOT NULL,
    end_time    DATETIME  NOT NULL,
    status      ENUM('OPEN','BLOCKED') NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_slots_provider
        FOREIGN KEY (provider_id) REFERENCES providers (provider_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_slots_service
        FOREIGN KEY (service_id) REFERENCES services (service_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    CONSTRAINT chk_slots_time
        CHECK (end_time > start_time),

    KEY         idx_slots_provider_time (provider_id, start_time, end_time),
    KEY         idx_slots_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE appointments
(
    appointment_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id    INT       NOT NULL,
    provider_id    INT       NOT NULL,
    service_id     INT       NOT NULL,
    start_time     DATETIME  NOT NULL,
    end_time       DATETIME  NOT NULL,
    status         ENUM('BOOKED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'BOOKED',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appt_customer
        FOREIGN KEY (customer_id) REFERENCES users (user_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_appt_provider
        FOREIGN KEY (provider_id) REFERENCES providers (provider_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_appt_service
        FOREIGN KEY (service_id) REFERENCES services (service_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT chk_appt_time
        CHECK (end_time > start_time),

    KEY            idx_appt_provider_time (provider_id, start_time, end_time),
    KEY            idx_appt_customer_time (customer_id, start_time, end_time),
    KEY            idx_appt_status (status)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE conversations
(
    conversation_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT       NOT NULL,
    provider_id     INT       NOT NULL,
    appointment_id  INT NULL,
    status          ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_conv_customer
        FOREIGN KEY (customer_id) REFERENCES users (user_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_conv_provider
        FOREIGN KEY (provider_id) REFERENCES providers (provider_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_conv_appt
        FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    UNIQUE KEY uq_conv_appointment (appointment_id),

    KEY             idx_conv_customer (customer_id),
    KEY             idx_conv_provider (provider_id),
    KEY             idx_conv_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE messages
(
    message_id      INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT       NOT NULL,
    sender_id       INT       NOT NULL,
    content         TEXT      NOT NULL,
    sent_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         TIMESTAMP NULL DEFAULT NULL,

    CONSTRAINT fk_msg_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_msg_sender
        FOREIGN KEY (sender_id) REFERENCES users (user_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    KEY             idx_msg_conversation_time (conversation_id, sent_at),
    KEY             idx_msg_sender_time (sender_id, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Demo password for all seeded accounts: "password" (bcrypt)
-- $2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K

INSERT INTO users (name, email, phone, password_hash, role)
VALUES ('Jane Doe', 'jane@salon.com', '555-0101', '$2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K', 'PROVIDER'),
       ('John Smith', 'john@gmail.com', '555-0202', '$2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K', 'CUSTOMER'),
       ('Maria Garcia', 'maria@salon.com', '555-0303', '$2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K', 'PROVIDER'),
       ('Alex Kim', 'alex@salon.com', '555-0404', '$2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K', 'PROVIDER'),
       ('Sofia Lee', 'sofia@gmail.com', '555-0505', '$2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K', 'CUSTOMER'),
       ('Riley Chen', 'riley@gmail.com', '555-0606', '$2a$10$7EqJtq98hPqEX7fNZaFWoO7E3QnP8Kf7xN3fX2V6tPEjmvYzu3d8K', 'CUSTOMER');

-- user_id 1=Jane, 2=John, 3=Maria, 4=Alex, 5=Sofia, 6=Riley
INSERT INTO providers (user_id, display_name, active)
VALUES (1, 'Jane''s Hair Styling', TRUE),
       (3, 'Maria''s Color Studio', TRUE),
       (4, 'Alex Kim Grooming', TRUE);

INSERT INTO services (name, duration_minutes, price, active)
VALUES ('Haircut', 60, 5000, TRUE),
       ('Hair Color & Highlights', 120, 12000, TRUE),
       ('Manicure', 45, 3500, TRUE),
       ('Beard Trim', 30, 2500, TRUE),
       ('Blowout & Style', 45, 4500, TRUE);

-- service_id: 1=Haircut 60m, 2=Color 120m, 3=Manicure 45m, 4=Beard 30m, 5=Blowout 45m
-- provider_id: 1=Jane, 2=Maria, 3=Alex — slot lengths match service duration
INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
VALUES
    -- Jane — mixed cuts & styling
    (1, 1, '2026-05-25 10:00:00', '2026-05-25 11:00:00', 'OPEN'),
    (1, 5, '2026-05-25 11:15:00', '2026-05-25 12:00:00', 'OPEN'),
    (1, 3, '2026-05-25 14:00:00', '2026-05-25 14:45:00', 'OPEN'),
    (1, 1, '2026-05-26 09:00:00', '2026-05-26 10:00:00', 'OPEN'),
    (1, 5, '2026-05-26 10:30:00', '2026-05-26 11:15:00', 'OPEN'),
    (1, 3, '2026-05-26 13:00:00', '2026-05-26 13:45:00', 'OPEN'),
    (1, 1, '2026-05-27 15:00:00', '2026-05-27 16:00:00', 'OPEN'),
    (1, 1, '2026-06-02 10:00:00', '2026-06-02 11:00:00', 'OPEN'),
    (1, 5, '2026-06-02 11:30:00', '2026-06-02 12:15:00', 'OPEN'),
    (1, 3, '2026-06-03 14:00:00', '2026-06-03 14:45:00', 'OPEN'),
    (1, 1, '2026-06-04 09:00:00', '2026-06-04 10:00:00', 'OPEN'),
    -- Maria — color & highlights (2-hour blocks)
    (2, 2, '2026-05-25 10:00:00', '2026-05-25 12:00:00', 'OPEN'),
    (2, 2, '2026-05-26 13:00:00', '2026-05-26 15:00:00', 'OPEN'),
    (2, 2, '2026-05-27 11:00:00', '2026-05-27 13:00:00', 'OPEN'),
    (2, 2, '2026-05-28 09:00:00', '2026-05-28 11:00:00', 'OPEN'),
    (2, 2, '2026-06-01 10:00:00', '2026-06-01 12:00:00', 'OPEN'),
    (2, 2, '2026-06-03 09:00:00', '2026-06-03 11:00:00', 'OPEN'),
    (2, 2, '2026-06-05 14:00:00', '2026-06-05 16:00:00', 'OPEN'),
    -- Alex — beard trims & haircuts
    (3, 4, '2026-05-25 09:00:00', '2026-05-25 09:30:00', 'OPEN'),
    (3, 1, '2026-05-25 10:00:00', '2026-05-25 11:00:00', 'OPEN'),
    (3, 4, '2026-05-25 14:00:00', '2026-05-25 14:30:00', 'OPEN'),
    (3, 1, '2026-05-26 16:00:00', '2026-05-26 17:00:00', 'OPEN'),
    (3, 4, '2026-05-27 12:00:00', '2026-05-27 12:30:00', 'OPEN'),
    (3, 1, '2026-05-28 10:00:00', '2026-05-28 11:00:00', 'OPEN'),
    (3, 4, '2026-05-29 15:00:00', '2026-05-29 15:30:00', 'OPEN'),
    (3, 1, '2026-06-02 08:00:00', '2026-06-02 09:00:00', 'OPEN'),
    (3, 4, '2026-06-04 11:00:00', '2026-06-04 11:30:00', 'OPEN');
