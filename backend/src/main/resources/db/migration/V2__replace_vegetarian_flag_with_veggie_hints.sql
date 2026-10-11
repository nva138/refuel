CREATE TABLE meal_veggie_hint (
    id BIGSERIAL PRIMARY KEY,
    meal_id BIGINT NOT NULL REFERENCES meal(id) ON DELETE CASCADE,
    hint TEXT NOT NULL
);

ALTER TABLE meal DROP COLUMN is_vegetarian;