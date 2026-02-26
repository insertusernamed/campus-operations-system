# A11y Scan Contribution Rules

## Mock coverage
- If a UI change introduces a new API endpoint, add a matching handler in `e2e/a11y/mockApi.ts`.
- Keep scenario behavior explicit (`empty`, `normal`, `dense`, `error`) so coverage differences are intentional.

## Interaction discoverability
- Prefer semantic controls (`button`, `summary`, `details`, `aria-expanded`) for hidden or progressive UI.
- For custom widgets that are otherwise hard to discover, add a `data-a11y-scan` hook.

## Pull request checklist
- [ ] A11y scan reports no strict mock gaps.
- [ ] New dynamic UI is discoverable by scanner interactions or marked with `data-a11y-scan`.
