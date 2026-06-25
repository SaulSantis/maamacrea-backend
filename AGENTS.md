# Maama Crea Backend - Agent Rules

## Project context

Maama Crea is a small business focused on personalized cushions and custom textile products.

This backend is the foundation for an internal business management system. The first functional area is product costing:

* Inputs/materials.
* Final products.
* Product recipes.
* Manufacturing cost calculation.
* Suggested selling price.
* Material replacement amount.
* Estimated profit.

Later, the system may include:

* Simple sales records.
* Monthly financial summaries.
* Customers.
* Custom orders.
* Payments.
* Inventory movements.
* Users and roles.
* Multi-company / SaaS support.

Do not implement future modules unless they are explicitly requested.

---

## Non-negotiable rules

* Always read this `AGENTS.md` before planning or implementing changes.
* Do NOT modify anything outside the explicit task in the current request.
* Do NOT rename packages, classes, endpoints, folders, entities, tables, or fields unless explicitly requested.
* Do NOT change existing behavior unless explicitly requested.
* Do NOT add new modules unless explicitly requested.
* Do NOT implement customers, orders, payments, users, authentication, authorization, JWT, email, WebPay, AWS, Docker changes, or multitenancy unless explicitly requested.
* Do NOT refactor unrelated code while working on a specific task.
* Do NOT introduce large architectural changes without first explaining the plan.
* Do NOT delete existing code unless explicitly requested or clearly obsolete within the current task.
* Do NOT change ports, database configuration, environment configuration, or build configuration unless explicitly requested.
* Do NOT add dependencies to `pom.xml` unless the current task explicitly requires it.
* Do NOT modify frontend code in this repository.

---

## Current development focus

The current priority is the product costing module.

The system must allow Maama Crea to:

1. Create inputs/materials.
2. Create final products.
3. Associate materials to a product as a manufacturing recipe.
4. Calculate product material cost.
5. Calculate additional cost.
6. Calculate total manufacturing cost.
7. Calculate suggested selling price.
8. Calculate estimated profit.
9. Show how much money should be reserved to replace materials.
10. Later, record simple sales and calculate monthly accumulated values.

Keep the current scope focused on:

* `Insumo`
* `Producto`
* `ProductoInsumo` as product recipe
* Cost calculation
* Simple financial summaries only when explicitly requested

Do not expand scope beyond the costing domain unless the user asks for it directly.

---

## Backend scope

This repository is backend only.

Expected stack in this project:

* Java 21
* Spring Boot
* PostgreSQL
* Spring Data JPA
* Maven
* Lombok, because it is already present in the project

Frontend will be handled separately with Angular in a different workspace/editor.

Do not add frontend files here.

---

## Package and structure rules

* Keep the current base package: `com.maamacrea.backend`
* Keep the current feature-oriented structure already present in:
  * `com.maamacrea.backend.insumos`
  * `com.maamacrea.backend.productos`
* Prefer small, incremental changes.
* Prefer creating code inside the existing feature/module related to the task.
* Shared utilities, exceptions, or configuration should go into a clear shared/config/common package only when needed.
* Do not move files just to reorganize the project unless explicitly requested.
* Keep `BackendApplication` as the Spring Boot entry point unless explicitly requested otherwise.

---

## Entity and JPA rules

* Avoid using Lombok `@Data` on JPA entities for future changes.
* Prefer controlled Lombok annotations such as:
  * `@Getter`
  * `@Setter`
  * `@NoArgsConstructor`
  * `@AllArgsConstructor`
  * `@Builder`, only when useful and safe
* Be careful with `equals`, `hashCode`, and `toString` on JPA entities.
* Do not expose JPA entities directly through controllers when adding new endpoints.
* Prefer DTOs for request and response objects.
* Do not create bidirectional relationships unless necessary.
* Avoid lazy-loading issues in API responses.
* Do not add cascade rules unless the business behavior is clear.
* Do not remove database relationships without explicit approval.
* Use PostgreSQL-friendly column definitions for money and quantities, typically with `BigDecimal`.
* Keep entity names and table names aligned with the current domain language already used by the project.

---

## DTO and validation rules

When adding or modifying API endpoints:

* Use request DTOs for incoming data.
* Use response DTOs for outgoing data.
* Use validation annotations where appropriate:
  * `@NotNull`
  * `@NotBlank`
  * `@Positive`
  * `@PositiveOrZero`
  * `@Size`
* Use `@Valid` in controller methods for validated request bodies.
* Do not allow negative prices, negative quantities, or empty required names.
* Keep validation messages clear and business-friendly.
* Do not accept raw JPA entities as request bodies in newly created endpoints.

---

## Error handling rules

When adding new behavior:

* Avoid returning raw Java exceptions to the client.
* Prefer clear business errors.
* Add or reuse a global exception handler when appropriate.
* Common errors should include:
  * Resource not found.
  * Invalid input.
  * Business rule violation.
  * Invalid cost calculation.
* Error responses should be predictable and easy for the frontend to consume.

---

## API endpoint rules

* Keep existing endpoints stable.
* Do not change existing endpoint paths unless explicitly requested.
* If adding endpoints, use clear REST-style paths.
* Do not duplicate endpoints that already exist.
* Use Spanish business names only when they already exist in the project.
* Keep API naming consistent with the current project.

Recommended endpoint areas for the current module:

* `/api/insumos`
* `/api/productos`
* `/api/productos/{id}/insumos`
* `/api/productos/{id}/receta`
* `/api/productos/{id}/costeo`

Only add these if the task explicitly asks for them.

---

## Product costing business rules

The product costing module must follow these rules unless the user explicitly changes them:

### Input/material cost

An input/material has:

* Purchase price.
* Purchased quantity.
* Unit of measure.
* Calculated unit cost.

Formula:

`unitCost = purchasePrice / purchasedQuantity`

### Product recipe

A product recipe defines which materials are required to manufacture one unit of a product.

A recipe entry has:

* Product.
* Input/material.
* Quantity used.
* Unit cost at the time of calculation if needed.
* Total cost for that material.

Formula:

`usedMaterialCost = inputUnitCost * quantityUsed`

### Product material cost

Formula:

`materialCost = sum of all usedMaterialCost values`

### Additional cost

Additional cost is a separate business value and must not be confused with material cost.

If this value is introduced later, it should remain explicit and traceable.

### Total manufacturing cost

Formula:

`totalManufacturingCost = materialCost + additionalCost`

### Suggested selling price

Default formula:

`suggestedSellingPrice = totalManufacturingCost * priceMultiplier`

### Estimated profit

Formula:

`estimatedProfit = currentSellingPrice - totalManufacturingCost`

### Material replacement amount

Formula:

`materialReplacementAmount = materialCost`

This is not profit. It is the amount that should be reserved to buy materials again.

---

## Stock rules

* Do NOT discount stock when creating or editing a product recipe.
* A recipe only describes what the product uses.
* Real stock discounting should happen later when a production, order, or sale workflow exists.
* Do not implement stock movements unless explicitly requested.
* Keep `cantidadDisponible` semantics aligned with real inventory behavior once stock workflows are explicitly requested.

---

## Sales and financial summary rules

Simple sales records may be added later, but only when explicitly requested.

When implemented, a sale should store a financial snapshot of the product at the time of sale, including:

* Sale date.
* Product.
* Quantity.
* Unit selling price.
* Total income.
* Unit material cost.
* Total material replacement amount.
* Unit additional cost.
* Total additional cost.
* Unit total manufacturing cost.
* Total manufacturing cost.
* Unit estimated profit.
* Total estimated profit.

Monthly financial summaries should calculate accumulated values from recorded sales.

Do not call this accounting unless the user explicitly requests a formal accounting module.

Use names such as:

* Financial summary.
* Monthly summary.
* Sales summary.
* Material replacement accumulated.
* Estimated profit accumulated.

---

## Database rules

* Use PostgreSQL-compatible data types and queries.
* Do not change database configuration unless explicitly requested.
* If Flyway or Liquibase is already configured and active, do not edit already-applied migrations.
* If migrations are used, only add new incremental migrations.
* If migrations are not currently used, do not introduce them unless explicitly requested.
* Do not rely on destructive schema changes.
* Be careful with existing data.
* Prefer preserving backward compatibility for current tables:
  * `insumos`
  * `productos`
  * `producto_insumos`

---

## Build and execution rules

Before finishing implementation, provide:

* Files modified.
* Endpoints added or changed.
* Example JSON requests.
* Commands to compile.
* Commands to run the backend.
* Any manual test steps.

For Windows, prefer Maven Wrapper commands such as:

`mvnw.cmd clean install`

and:

`mvnw.cmd spring-boot:run`

if the wrapper exists and works.

---

## Testing rules

When adding business logic:

* Prefer adding tests for calculations.
* Test cost formulas.
* Test invalid values.
* Test not-found cases when practical.
* Do not add excessive tests unrelated to the current task.
* Mention clearly when tests could not be executed in the current environment.

---

## Future SaaS rules

The system may become a SaaS in the future, but do not implement SaaS/multitenancy yet.

Do not add:

* Tenant entity.
* Company entity.
* Tenant filters.
* Tenant interceptors.
* Multi-database logic.
* Subdomain logic.
* Subscription logic.
* Billing logic.

Unless explicitly requested.

However, avoid decisions that would make future SaaS support unnecessarily difficult.

---

## Work style

For every implementation task:

1. Explain the plan briefly.
2. List the files that will be touched.
3. Make the smallest safe change.
4. Keep the current project style.
5. Avoid unrelated refactors.
6. Provide a final summary.
7. Provide test instructions.
8. Mention anything that could not be verified.

The goal is to grow this project step by step without breaking the current backend.
