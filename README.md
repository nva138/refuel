# veggietracker-v2

Take a photo of your meal, get a calorie estimate and hints on whether it might not be vegetarian.

## How it works

1. Take a photo of your meal
2. An AI model suggests the dish, portion size and calories
3. You check the suggestion, correct it if needed and save it

The AI only suggests. Portion sizes are hard to judge from a photo, so the final value is always yours.

## Veggie hints

The app never says "this is vegetarian". It only shows warnings about things that might not be, for example chicken stock in a risotto or gelatin in a dessert. If nothing is found, it says "no hints found", not "vegetarian". A photo can't show hidden ingredients, so when in doubt, ask or check the label.

## MVP

- [ ] Upload a photo and get a suggestion
- [x] Confirm or correct and save the meal
- [x] Daily overview with total calories

## Roadmap

- [ ] Describe a meal in text and get a suggestion
- [ ] Delete and edit meals
- [ ] Daily goals with progress
- [ ] History and weekly overview
- [ ] WHOOP integration to factor in calories burned
- [ ] Deployment

## Stack

- Backend: Java 21, Spring Boot, PostgreSQL, Flyway
- Frontend: Vue 3, TypeScript, Pinia, Vite
