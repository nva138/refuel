CREATE TABLE weight_entry (
    id BIGSERIAL PRIMARY KEY,
    measured_on DATE NOT NULL UNIQUE,
    weight_kg NUMERIC(6,1) NOT NULL
);

INSERT INTO weight_entry(measured_on, weight_kg)
SELECT CURRENT_DATE, body_weight_kg FROM settings;