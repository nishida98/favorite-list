# AGENTS.md

## General Principles

- Always read the specification documents before implementing anything.
- Never assume behavior that is not explicitly described.
- If requirements are ambiguous, stop and ask for clarification.
- Prefer small, incremental, reviewable changes.
- Keep implementations simple and maintainable.
- Prioritize readability over cleverness.
- Preserve existing architecture and conventions unless explicitly instructed otherwise.
- Never introduce breaking changes without updating the specification.
- Always explain important tradeoffs when making architectural decisions.

---

## Development Workflow

1. Read and understand the relevant spec.
2. Review existing code before proposing changes.
3. Create an implementation plan before coding.
4. Write or update tests before modifying behavior when possible.
5. Implement only the requested scope.
6. Run validations/tests/linting after changes.
7. Summarize all modifications clearly.

---

## Code Quality Rules

- Follow clean code principles.
- Prefer composition over inheritance.
- Avoid duplicated logic.
- Keep functions small and focused.
- Use meaningful and explicit naming.
- Avoid unnecessary abstractions.
- Remove dead code whenever encountered.
- Keep side effects isolated and predictable.
- Avoid premature optimization.

---

## Backend Rules

- Validate all external input.
- Handle errors explicitly and consistently.
- Never leak sensitive information in logs or responses.
- Use DTOs/contracts consistently.
- Maintain backward compatibility whenever possible.
- Prefer asynchronous and event-driven patterns for scalable operations.
- Keep database queries efficient and observable.
- Avoid N+1 query patterns.
- Add monitoring/logging for critical flows.

---

## Frontend Rules

- Prioritize responsiveness and accessibility.
- Keep components modular and reusable.
- Avoid large stateful components.
- Prefer reactive patterns over imperative DOM manipulation.
- Maintain visual consistency with the design system.
- Handle loading, empty, and error states properly.
- Avoid unnecessary re-renders.

---

## Testing Rules

- Always add or update tests for behavior changes.
- Prefer deterministic and isolated tests.
- Mock external dependencies when appropriate.
- Cover edge cases and failure scenarios.
- Do not remove tests without justification.
- Ensure critical business rules are validated.

---

## Git & Pull Request Rules

- Keep commits focused and atomic.
- Avoid unrelated refactors in feature branches.
- Document important architectural decisions.
- Summarize risks and limitations in pull requests.
- Include testing notes in PR descriptions.

---

## Security Rules

- Never hardcode secrets or credentials.
- Validate authorization and authentication flows carefully.
- Sanitize all user input.
- Use least-privilege access principles.
- Be cautious with file operations and external integrations.
- Avoid insecure defaults.

---

## Performance Rules

- Measure before optimizing.
- Prefer efficient algorithms and queries.
- Minimize memory footprint when possible.
- Avoid blocking operations in critical paths.
- Use caching carefully and explicitly document invalidation rules.

---

## Agent Behavior

- Do not modify files outside the requested scope unless necessary.
- Do not invent requirements.
- If confidence is low, explain uncertainties clearly.
- Prefer asking questions over making risky assumptions.
- Clearly explain what was changed and why.
- When possible, propose safer alternatives before major changes.

## Purpose

This file defines engineering standards for Java/Kotlin projects.

Agents and contributors must prioritize correctness, maintainability, readability, performance, and simple design.

The goal is not to write clever code. The goal is to write boring, reliable, well-tested code that can survive production.

---

## 1. Core Engineering Principles

### 1.1 Optimize for Clarity First

Code is read more often than it is written.

Prefer:

- Clear names over comments.
- Small functions over large procedural blocks.
- Explicit behavior over hidden magic.
- Simple abstractions over premature frameworks.
- Immutable state where practical.
- Domain language over technical noise.

Avoid:

- Clever one-liners.
- Deep inheritance trees.
- Hidden side effects.
- Boolean parameters that change behavior.
- Over-generalized abstractions created before real need exists.

Bad:

```kotlin
fun process(x: Boolean) {
    if (x) {
        // special behavior
    }
}
```

Good:

```kotlin
fun processRefund() {
    // refund-specific behavior
}

fun processPaymentCapture() {
    // capture-specific behavior
}
```

---

### 1.2 Prefer Simple Design

Use this order of preference:

1. Simple function.
2. Small cohesive class.
3. Interface with one clear implementation only when useful for boundaries or tests.
4. Design pattern only when it removes real complexity.
5. Framework abstraction only when the project benefits from it.

Do not introduce a pattern just because it is well known.

---

### 1.3 Follow SOLID Pragmatically

#### Single Responsibility Principle

A class should have one reason to change.

Good:

```kotlin
class InvoiceCalculator {
    fun calculateTotal(invoice: Invoice): Money {
        // calculation only
    }
}

class InvoiceRepository {
    fun save(invoice: Invoice) {
        // persistence only
    }
}
```

Bad:

```kotlin
class InvoiceService {
    fun calculateTotal() {}
    fun saveToDatabase() {}
    fun sendEmail() {}
    fun renderPdf() {}
}
```

#### Open/Closed Principle

Prefer adding new behavior without editing fragile conditional chains.

Bad:

```kotlin
when (payment.type) {
    CARD -> chargeCard(payment)
    PAYPAL -> chargePaypal(payment)
    BANK_TRANSFER -> chargeBankTransfer(payment)
}
```

Good:

```kotlin
interface PaymentProcessor {
    fun supports(type: PaymentType): Boolean
    fun process(payment: Payment)
}
```

#### Dependency Inversion

Business logic should depend on domain contracts and abstractions, not framework or persistence details.

Good:

```kotlin
class CreateOrderService(
    private val orderRepository: OrderRepository,
    private val paymentGateway: PaymentGateway
)
```

Bad:

```kotlin
class CreateOrderService {
    private val jdbcTemplate = JdbcTemplate(...)
    private val stripeClient = StripeClient(...)
}
```

---

## 2. Code Style

### 2.1 Naming

Use precise names.

Prefer:

```kotlin
val paidInvoices: List<Invoice>
fun calculateOutstandingBalance(customerId: CustomerId): Money
class PaymentAuthorizationFailedException
```

Avoid:

```kotlin
val data: List<Any>
fun handleStuff()
class Helper
class Manager
class Processor
```

Names like `Helper`, `Util`, `Manager`, and `Processor` are usually signs of weak modeling.

---

### 2.2 Function Design

Functions should be:

- Small.
- Focused.
- Named after intent.
- Free of surprising side effects.
- Easy to test.

Prefer early returns over deeply nested conditionals.

Bad:

```kotlin
fun approve(order: Order) {
    if (order.isPaid) {
        if (order.hasStock) {
            if (!order.isFraudulent) {
                order.approve()
            }
        }
    }
}
```

Good:

```kotlin
fun approve(order: Order) {
    if (!order.isPaid) return
    if (!order.hasStock) return
    if (order.isFraudulent) return

    order.approve()
}
```

---

### 2.3 Class Design

A class should have high cohesion and a small public API.

Prefer:

```kotlin
class Order(
    val id: OrderId,
    private val lines: List<OrderLine>,
    private var status: OrderStatus
) {
    fun approve() {
        require(status == OrderStatus.PENDING) {
            "Only pending orders can be approved"
        }

        status = OrderStatus.APPROVED
    }

    fun total(): Money =
        lines.fold(Money.zero()) { total, line -> total + line.subtotal() }
}
```

Avoid anemic models where domain behavior is scattered across services.

Bad:

```kotlin
data class Order(
    val id: String,
    var status: String,
    val lines: List<OrderLine>
)

class OrderService {
    fun approve(order: Order) {
        order.status = "APPROVED"
    }
}
```

---

## 3. Java/Kotlin Best Practices

### 3.1 Kotlin Guidelines

Prefer:

- `val` over `var`.
- Non-null types over nullable types.
- Data classes for immutable value objects.
- Sealed classes for closed hierarchies.
- Extension functions only when they improve readability.
- `Result`, sealed outcomes, or domain-specific errors where appropriate.

Avoid:

- Excessive use of `!!`.
- Platform type leaks from Java APIs.
- Mutable collections in public APIs.
- Overuse of scope functions like `let`, `also`, `apply`, and `run`.

Bad:

```kotlin
val name = user!!.profile!!.name!!
```

Good:

```kotlin
val name = user.profile?.name
    ?: throw MissingUserProfileException(user.id)
```

Prefer immutable public APIs:

```kotlin
class Cart(
    private val items: MutableList<CartItem> = mutableListOf()
) {
    fun items(): List<CartItem> = items.toList()

    fun add(item: CartItem) {
        items.add(item)
    }
}
```

---

### 3.2 Java Guidelines

Prefer:

- Constructor injection.
- `final` for fields.
- `Optional` for return values, not fields or parameters.
- Records for simple immutable data carriers.
- Sealed classes/interfaces when modeling closed hierarchies.
- Meaningful exceptions.
- Streams only when they improve readability.

Good:

```java
public final class CreateOrderService {
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public CreateOrderService(
        OrderRepository orderRepository,
        PaymentGateway paymentGateway
    ) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }
}
```

Avoid field injection:

```java
@Autowired
private OrderRepository orderRepository;
```

Prefer constructor injection:

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

---

## 4. Architecture

### 4.1 Prefer MVC with DDD

Recommended package structure:

```text
com.company.product
├── config
├── controller
├── dto
├── domain
│   ├── model
│   ├── service
│   ├── repository
│   ├── event
│   └── policy
├── repository
├── service
└── exception
```

Dependency direction:

```text
controller -> service -> domain -> repository contracts
repository implementations -> domain repository contracts
domain -> no MVC, HTTP, or persistence framework dependencies
```

Domain code must not depend on:

- Spring.
- Hibernate.
- HTTP frameworks.
- Database clients.
- Message brokers.
- Cloud SDKs.

Controllers are the MVC entry point, services coordinate application flow, and the domain layer contains the business rules and invariants. Apply DDD to model aggregates, value objects, domain services, repositories, and domain events explicitly when they represent real business concepts.

---

### 4.2 Services Should Orchestrate Within MVC

Application services coordinate controllers, domain objects, repositories, and external integrations without moving business rules out of the domain.

Good:

```kotlin
class CreateOrderService(
    private val orderRepository: OrderRepository,
    private val paymentGateway: PaymentGateway,
    private val eventPublisher: EventPublisher
) {
    fun createOrder(command: CreateOrderCommand): OrderId {
        val order = Order.create(command.customerId, command.items)

        paymentGateway.authorize(order.paymentRequest())

        orderRepository.save(order)
        eventPublisher.publish(OrderCreated(order.id))

        return order.id
    }
}
```

Avoid putting business rules directly in controllers.

Bad:

```kotlin
@RestController
class OrderController {
    @PostMapping("/orders")
    fun create(@RequestBody request: CreateOrderRequest): ResponseEntity<Any> {
        // validation, pricing, payment, persistence, events all here
    }
}
```

---

## 5. Design Patterns

Use design patterns only when they simplify the design.

### 5.1 Strategy Pattern

Use when behavior varies by type, provider, or policy.

```kotlin
interface ShippingCostStrategy {
    fun calculate(order: Order): Money
}

class StandardShippingStrategy : ShippingCostStrategy {
    override fun calculate(order: Order): Money = Money.of(5)
}

class ExpressShippingStrategy : ShippingCostStrategy {
    override fun calculate(order: Order): Money = Money.of(15)
}
```

Good for:

- Payment methods.
- Pricing rules.
- Shipping rules.
- Notification providers.
- Authorization policies.

---

### 5.2 Factory Pattern

Use when object creation has business meaning or complex validation.

```kotlin
object OrderFactory {
    fun create(customerId: CustomerId, items: List<OrderItem>): Order {
        require(items.isNotEmpty()) {
            "Order must contain at least one item"
        }

        return Order(
            id = OrderId.new(),
            customerId = customerId,
            items = items,
            status = OrderStatus.PENDING
        )
    }
}
```

Do not use factories for trivial constructors unless they improve readability.

---

### 5.3 Builder Pattern

Use for complex object construction, especially in Java tests or APIs.

```java
Order order = OrderBuilder.anOrder()
    .withCustomerId(customerId)
    .withItem(productId, 2)
    .withStatus(OrderStatus.PENDING)
    .build();
```

In Kotlin, prefer named parameters and default arguments when possible.

```kotlin
val order = Order(
    id = OrderId.new(),
    customerId = customerId,
    status = OrderStatus.PENDING
)
```

---

### 5.4 Adapter Pattern

Use to isolate external systems.

```kotlin
interface PaymentGateway {
    fun authorize(request: PaymentRequest): PaymentAuthorization
}

class StripePaymentGateway(
    private val stripeClient: StripeClient
) : PaymentGateway {
    override fun authorize(request: PaymentRequest): PaymentAuthorization {
        val response = stripeClient.authorize(request.toStripeRequest())
        return response.toDomainAuthorization()
    }
}
```

The domain should never know about Stripe, AWS, Kafka, Redis, or HTTP clients.

---

### 5.5 Template Method Pattern

Use sparingly. Prefer composition over inheritance.

Acceptable when an algorithm has fixed steps with customizable behavior.

```kotlin
abstract class ImportJob {
    fun run() {
        val records = read()
        validate(records)
        persist(records)
    }

    protected abstract fun read(): List<Record>
    protected abstract fun validate(records: List<Record>)
    protected abstract fun persist(records: List<Record>)
}
```

Prefer composition if steps vary significantly.

---

### 5.6 Decorator Pattern

Use to add behavior without modifying core logic.

```kotlin
class LoggingPaymentGateway(
    private val delegate: PaymentGateway,
    private val logger: Logger
) : PaymentGateway {
    override fun authorize(request: PaymentRequest): PaymentAuthorization {
        logger.info("Authorizing payment")
        return delegate.authorize(request)
    }
}
```

Good for:

- Logging.
- Metrics.
- Retries.
- Caching.
- Tracing.
- Authorization checks.

---

### 5.7 Observer / Publish-Subscribe

Use domain events to decouple side effects.

```kotlin
data class OrderCreated(
    val orderId: OrderId,
    val customerId: CustomerId
)

interface EventPublisher {
    fun publish(event: Any)
}
```

Good for:

- Sending emails.
- Updating projections.
- Publishing integration events.
- Triggering asynchronous workflows.

Avoid using events to hide essential business flow.

---

## 6. Error Handling

### 6.1 Use Domain-Specific Exceptions

Bad:

```kotlin
throw RuntimeException("Invalid order")
```

Good:

```kotlin
throw InvalidOrderException("Order must contain at least one item")
```

Prefer clear exception types:

```kotlin
class PaymentAuthorizationFailedException(
    paymentId: PaymentId,
    cause: Throwable? = null
) : RuntimeException("Payment authorization failed for paymentId=$paymentId", cause)
```

---

### 6.2 Do Not Swallow Exceptions

Bad:

```kotlin
try {
    paymentGateway.authorize(request)
} catch (e: Exception) {
    logger.error("Failed")
}
```

Good:

```kotlin
try {
    paymentGateway.authorize(request)
} catch (e: PaymentProviderException) {
    logger.warn("Payment provider failed for orderId={}", orderId, e)
    throw PaymentAuthorizationFailedException(orderId, e)
}
```

---

### 6.3 Validate at Boundaries

Validate input at system boundaries:

- REST controllers.
- Message consumers.
- Public API entry points.
- Configuration loading.
- External integration adapters.

Keep internal domain objects valid by construction.

---

## 7. API Design

### 7.1 REST API Guidelines

Use resource-oriented URLs.

Good:

```text
POST /orders
GET /orders/{orderId}
POST /orders/{orderId}/cancel
GET /customers/{customerId}/orders
```

Avoid:

```text
POST /createOrder
GET /getOrder
POST /doCancel
```

Use proper status codes:

```text
200 OK
201 Created
202 Accepted
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error
```

---

### 7.2 DTOs Are Not Domain Models

Do not expose entities directly through APIs.

Good:

```kotlin
data class CreateOrderRequest(
    val customerId: String,
    val items: List<CreateOrderItemRequest>
)

data class OrderResponse(
    val id: String,
    val status: String,
    val total: BigDecimal
)
```

Map DTOs to commands:

```kotlin
fun CreateOrderRequest.toCommand(): CreateOrderCommand =
    CreateOrderCommand(
        customerId = CustomerId(customerId),
        items = items.map { it.toCommandItem() }
    )
```

---

## 8. Persistence

### 8.1 Keep Persistence Out of Domain Logic

Repositories are abstractions.

```kotlin
interface OrderRepository {
    fun findById(id: OrderId): Order?
    fun save(order: Order)
}
```

Infrastructure implements them.

```kotlin
class JpaOrderRepository(
    private val springDataRepository: SpringDataOrderRepository
) : OrderRepository {
    override fun findById(id: OrderId): Order? =
        springDataRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun save(order: Order) {
        springDataRepository.save(order.toEntity())
    }
}
```

---

### 8.2 Be Careful with ORM Entities

Avoid leaking JPA entities into domain or API layers.

Common ORM risks:

- Lazy loading surprises.
- N+1 queries.
- Accidental writes from dirty checking.
- Bidirectional relationship complexity.
- Business logic coupled to persistence annotations.

Prefer explicit queries for performance-critical reads.

---

## 9. Transactions

Transactions belong in service boundaries.

```kotlin
@Transactional
fun createOrder(command: CreateOrderCommand): OrderId {
    val order = Order.create(command.customerId, command.items)
    orderRepository.save(order)
    return order.id
}
```

Avoid long transactions involving:

- Remote HTTP calls.
- Message publishing without outbox pattern.
- Slow file operations.
- User interaction.
- Large batch processing.

For reliable event publishing, prefer the transactional outbox pattern.

---

## 10. Concurrency

### 10.1 Prefer Immutability

Mutable shared state is dangerous.

Good:

```kotlin
data class PricingSnapshot(
    val productId: ProductId,
    val price: Money,
    val capturedAt: Instant
)
```

Avoid shared mutable collections unless properly synchronized.

---

### 10.2 Java Concurrency

Prefer:

- `ExecutorService`
- `CompletableFuture`
- Virtual threads where appropriate
- Structured concurrency where available
- Thread-safe collections only when needed

Avoid manually creating unmanaged threads.

Bad:

```java
new Thread(() -> processOrder(order)).start();
```

Good:

```java
executorService.submit(() -> processOrder(order));
```

---

### 10.3 Kotlin Coroutines

Use structured concurrency.

Good:

```kotlin
suspend fun enrichOrder(order: Order): EnrichedOrder = coroutineScope {
    val customer = async { customerClient.getCustomer(order.customerId) }
    val risk = async { riskClient.evaluate(order.id) }

    EnrichedOrder(
        order = order,
        customer = customer.await(),
        risk = risk.await()
    )
}
```

Avoid `GlobalScope`.

Bad:

```kotlin
GlobalScope.launch {
    processOrder(order)
}
```

---

## 11. Performance

### 11.1 Measure Before Optimizing

Do not guess. Use:

- Profiling.
- Metrics.
- Logs.
- Benchmarks.
- Database query analysis.
- Load testing.

Optimize bottlenecks, not aesthetics.

---

### 11.2 Common JVM Performance Rules

Prefer:

- Efficient algorithms over micro-optimizations.
- Batching over repeated remote/database calls.
- Streaming for large data sets.
- Pagination for large reads.
- Connection pooling.
- Prepared statements.
- Bounded queues.
- Timeouts on all external calls.

Avoid:

- Loading unbounded result sets.
- N+1 database queries.
- Excessive object allocation in hot paths.
- Blocking calls inside event-loop threads.
- Unbounded thread pools.
- Unbounded retries.
- Chatty service-to-service communication.

---

### 11.3 Collections

Use the right collection.

```text
ArrayList      - default list choice
LinkedList     - rarely appropriate
HashMap        - default map choice
LinkedHashMap  - preserve insertion order
TreeMap        - sorted keys
HashSet        - unique values
EnumSet        - efficient enum sets
```

Avoid repeated linear scans when a map or set would be clearer and faster.

Bad:

```kotlin
orders.filter { it.customerId == customerId }
```

For repeated lookups:

```kotlin
val ordersByCustomer = orders.groupBy { it.customerId }
val customerOrders = ordersByCustomer[customerId].orEmpty()
```

---

## 12. Testing

### 12.1 Testing Pyramid

Prefer many fast tests and fewer slow tests.

```text
Many    Unit tests
Some    Integration tests
Few     End-to-end tests
```

Tests should be:

- Deterministic.
- Fast.
- Independent.
- Clear.
- Focused on behavior, not implementation details.

---

### 12.2 Unit Tests

Test business rules directly.

```kotlin
@Test
fun `cannot approve unpaid order`() {
    val order = OrderFixture.unpaidOrder()

    assertThrows<OrderApprovalException> {
        order.approve()
    }
}
```

Use readable test names.

Good:

```kotlin
fun `returns discount when customer is premium and order exceeds threshold`()
```

Bad:

```kotlin
fun testDiscount1()
```

---

### 12.3 Integration Tests

Use integration tests for:

- Database mappings.
- Repository queries.
- Message publishing/consuming.
- External adapter contracts.
- Transaction behavior.

Prefer Testcontainers for real infrastructure when practical.

---

### 12.4 Mocking

Mock external dependencies, not the domain model.

Good:

```kotlin
val paymentGateway = mockk<PaymentGateway>()
```

Avoid excessive mocking of internal collaborators. If a test requires many mocks, the design may be too coupled.

---

## 13. Security

Security must be designed in, not added later.

Required practices:

- Validate all external input.
- Escape output where needed.
- Use parameterized SQL.
- Never log secrets, tokens, passwords, or personal sensitive data.
- Use least privilege for credentials.
- Store secrets in a secret manager.
- Use TLS for network communication.
- Add authentication and authorization at boundaries.
- Use dependency scanning.
- Keep dependencies updated.
- Enforce timeouts and request size limits.

Bad:

```kotlin
logger.info("Authorization token: {}", token)
```

Good:

```kotlin
logger.info("Authorization request received for customerId={}", customerId)
```

---

## 14. Observability

Production systems must be debuggable.

Include:

- Structured logs.
- Metrics.
- Distributed tracing.
- Correlation IDs.
- Health checks.
- Readiness checks.
- Meaningful error messages.
- Business-level events where useful.

Good log:

```kotlin
logger.info(
    "Order created: orderId={}, customerId={}, total={}",
    order.id,
    order.customerId,
    order.total()
)
```

Bad log:

```kotlin
logger.info("Done")
```

Do not log sensitive data.

---

## 15. Dependency Management

Prefer stable, well-maintained dependencies.

Before adding a dependency, ask:

1. Is it necessary?
2. Can the standard library solve this?
3. Is it actively maintained?
4. Does it introduce security risk?
5. Does it complicate the build?
6. Is the license acceptable?
7. Will it be used broadly enough to justify inclusion?

Avoid adding large frameworks for small utilities.

---

## 16. Refactoring Rules

Refactor when:

- A function is hard to name.
- A class has too many responsibilities.
- Tests require excessive setup.
- Logic is duplicated across multiple places.
- Conditionals keep growing.
- A dependency points in the wrong direction.
- Performance problems are measured.
- A domain concept is implicit but should be explicit.

Refactor safely:

1. Add or improve tests first.
2. Make one small change.
3. Run tests.
4. Commit coherent changes.
5. Avoid mixing refactoring with behavior changes.

---

## 17. Common Anti-Patterns

Avoid:

- God classes.
- Anemic domain models.
- Transaction scripts for complex domains.
- Static utility dumping grounds.
- Overuse of inheritance.
- Primitive obsession.
- Boolean flag arguments.
- Magic strings.
- Global mutable state.
- Hidden framework magic.
- Catching generic exceptions without action.
- Returning `null` where absence should be explicit.
- Premature abstraction.
- Premature optimization.
- Copy-paste programming.
- Business logic in controllers.
- Database entities exposed through APIs.

---

## 18. Primitive Obsession

Prefer domain types over raw primitives.

Bad:

```kotlin
fun pay(customerId: String, amount: BigDecimal, currency: String)
```

Good:

```kotlin
fun pay(customerId: CustomerId, amount: Money)
```

Example:

```kotlin
@JvmInline
value class CustomerId(val value: String)

data class Money(
    val amount: BigDecimal,
    val currency: Currency
)
```

This makes invalid states harder to represent.

---

## 19. Configuration

Configuration should be:

- Externalized.
- Validated at startup.
- Environment-specific.
- Free of secrets in source control.

Use strongly typed configuration objects.

```kotlin
@ConfigurationProperties(prefix = "payment")
data class PaymentProperties(
    val baseUrl: String,
    val timeout: Duration,
    val retries: Int
)
```

Validate configuration early.

---

## 20. Code Review Checklist

Before approving code, verify:

- The code solves the actual problem.
- The design is simpler than the complexity it handles.
- Names are clear and domain-specific.
- Business logic is tested.
- Edge cases are handled.
- Errors are meaningful.
- No secrets or sensitive data are logged.
- Public APIs are stable and documented.
- Transactions are correctly scoped.
- External calls have timeouts.
- No unnecessary dependencies were added.
- Performance-sensitive paths avoid obvious inefficiencies.
- Tests are meaningful and not brittle.
- The change is observable in production.

---

## 21. Agent-Specific Instructions

When modifying code, agents must:

1. Read existing project conventions before changing style.
2. Preserve public behavior unless explicitly asked to change it.
3. Prefer small, focused edits.
4. Add tests for behavior changes.
5. Avoid broad rewrites without need.
6. Avoid introducing new dependencies unless justified.
7. Keep domain logic out of controllers and repository implementations.
8. Keep framework and persistence details out of the domain layer.
9. Use constructor injection.
10. Prefer immutable data structures and values.
11. Add clear error handling.
12. Run or suggest the smallest relevant test set.
13. Explain trade-offs when introducing a pattern or abstraction.
14. Do not hide failures.
15. Do not remove tests unless they are demonstrably invalid.

---

## 22. Default Project Standards

Unless the project already defines different standards, use the defaults below.

### Java

- Java 17 or newer.
- Constructor injection.
- JUnit 5.
- AssertJ.
- Mockito only for external dependencies.
- Records for simple immutable DTOs.
- Sealed interfaces/classes where useful.
- Gradle or Maven with reproducible builds.

### Kotlin

- Kotlin 1.9 or newer.
- `val` by default.
- Nullable types only where absence is valid.
- Data classes for values.
- Sealed classes for closed hierarchies.
- Coroutines with structured concurrency.
- JUnit 5 or Kotest.
- MockK for mocks.
- ktlint or detekt for static analysis.

### Spring Boot

- Keep controllers thin.
- Use constructor injection.
- Use `@ConfigurationProperties` for configuration.
- Keep transactions at service level.
- Avoid business logic in annotations.
- Avoid exposing JPA entities from controllers.
- Use explicit DTO mapping.
- Add health/readiness checks.
- Configure timeouts for external clients.

---

## 23. Final Rule

Prefer code that a tired engineer can safely modify during a production incident.

Simple beats clever.  
Explicit beats implicit.  
Tested beats assumed.  
Measured beats guessed.  
Domain clarity beats technical abstraction.
