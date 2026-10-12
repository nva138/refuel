CREATE TABLE settings (
    id BIGSERIAL PRIMARY KEY,
    mode TEXT NOT NULL CHECK(mode IN ('CUT', 'MAINTAIN', 'BULK')),
    body_weight_kg NUMERIC(6,1) NOT NULL,
    kcal_reference INTEGER NOT NULL
);

INSERT INTO settings (mode, body_weight_kg, kcal_reference) VALUES
 ('MAINTAIN', 98, 2500);