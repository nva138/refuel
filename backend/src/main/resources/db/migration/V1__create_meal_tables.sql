CREATE TABLE meal (
    id BIGSERIAL PRIMARY KEY,
    eaten_at TIMESTAMPTZ NOT NULL,
    name TEXT NOT NULL,
    is_vegetarian BOOLEAN NOT NULL,
    entered_text TEXT
);

CREATE TABLE meal_part (
    id BIGSERIAL PRIMARY KEY,
    meal_id BIGINT NOT NULL REFERENCES meal(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    kcal_per_100g NUMERIC(6,1) NOT NULL,
    protein_per_100g NUMERIC(6,1) NOT NULL,
    carbs_per_100g NUMERIC(6,1) NOT NULL,
    fat_per_100g NUMERIC(6,1) NOT NULL,
    amount_g NUMERIC(6,1) NOT NULL
);