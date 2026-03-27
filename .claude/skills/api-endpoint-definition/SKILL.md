---
name: api-endpoint-definition
description: Creates REST API endpoints with OpenAPI-annotated interfaces, controllers, request/response DTOs, MapStruct mappers, URL constants, error handling, and security whitelisting. Trigger on new endpoint, REST API, controller, REST interface, add API, add route, HTTP endpoint, request DTO, response DTO.
---

# API Endpoint Definition

## Quick Reference

| Artifact | Location | Naming |
|---|---|---|
| URL constants | `libs/asapp-commons-url/.../url/<domain>/` | `<Domain>RestAPIURL` |
| REST API interface | `infrastructure/<aggregate>/in/` | `<Domain>RestAPI` |
| REST controller | `infrastructure/<aggregate>/in/` | `<Domain>RestController` |
| Request DTO | `infrastructure/<aggregate>/in/request/` | `<Verb><Domain>Request` |
| Response DTO | `infrastructure/<aggregate>/in/response/` | `<Verb><Domain>Response` |
| MapStruct mapper | `infrastructure/<aggregate>/mapper/` | `<Domain>Mapper` |
| Error handler | `infrastructure/error/` | `GlobalExceptionHandler` |
| OpenAPI config | `infrastructure/config/` | `OpenApiConfiguration` |
| Security config | `infrastructure/config/` | `SecurityConfiguration` |

## Core Workflow

### 1. Define URL Constants in `asapp-commons-url`

Create in `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/<domain>/`:

```java
public class OrderRestAPIURL {

    public static final String ORDERS_ROOT_PATH = "/api/orders";

    public static final String ORDERS_GET_BY_ID_PATH = "/{id}";
    public static final String ORDERS_GET_ALL_PATH = "";
    public static final String ORDERS_CREATE_PATH = "";
    public static final String ORDERS_UPDATE_BY_ID_PATH = "/{id}";
    public static final String ORDERS_DELETE_BY_ID_PATH = "/{id}";

    // Full paths = ROOT + relative (used by rest-clients and tests)
    public static final String ORDERS_GET_BY_ID_FULL_PATH = ORDERS_ROOT_PATH + ORDERS_GET_BY_ID_PATH;
    public static final String ORDERS_GET_ALL_FULL_PATH = ORDERS_ROOT_PATH + ORDERS_GET_ALL_PATH;
    public static final String ORDERS_CREATE_FULL_PATH = ORDERS_ROOT_PATH + ORDERS_CREATE_PATH;
    public static final String ORDERS_UPDATE_BY_ID_FULL_PATH = ORDERS_ROOT_PATH + ORDERS_UPDATE_BY_ID_PATH;
    public static final String ORDERS_DELETE_BY_ID_FULL_PATH = ORDERS_ROOT_PATH + ORDERS_DELETE_BY_ID_PATH;

    private OrderRestAPIURL() {}

}
```

Naming: `<DOMAIN_PLURAL>_<VERB>_<QUALIFIER>_PATH` for relative, append `_FULL_PATH` for absolute.

### 2. Create the REST API Interface

Place in `infrastructure/<aggregate>/in/`. All Spring MVC and OpenAPI annotations go here -- never on the controller.

```java
@RequestMapping(ORDERS_ROOT_PATH)
@Tag(name = "Order Operations", description = "REST API contract for managing orders")
@SecurityRequirement(name = "Bearer Authentication")
public interface OrderRestAPI {

    @GetMapping(value = ORDERS_GET_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Gets an order by its unique identifier",
               description = "Retrieves detailed information about a specific order.")
    @ApiResponse(responseCode = "200", description = "Order found",
                 content = { @Content(schema = @Schema(implementation = GetOrderByIdResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid order identifier format",
                 content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed",
                 content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "Order not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval",
                 content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<GetOrderByIdResponse> getOrderById(
            @PathVariable("id") @Parameter(description = "Identifier of the order to get") UUID id);

    @PostMapping(value = ORDERS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new order", description = "Creates a new order in the system.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order creation request", required = true,
            content = @Content(schema = @Schema(implementation = CreateOrderRequest.class)))
    @ApiResponse(responseCode = "201", description = "Order created successfully",
                 content = { @Content(schema = @Schema(implementation = CreateOrderResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data",
                 content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    CreateOrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request);
}
```

Key rules: annotate each method with `@Operation`, `@ApiResponse` entries for each HTTP status, and `@SecurityRequirement(name = "Bearer Authentication")`.

### 3. Create Request/Response DTOs

Place in `infrastructure/<aggregate>/in/request/` and `infrastructure/<aggregate>/in/response/`.

```java
// Request -- validation annotations with explicit messages
public record CreateOrderRequest(
        @JsonProperty("user_id") @NotBlank(message = "The user ID must not be empty") String userId,
        @NotBlank(message = "The title must not be empty") String title,
        String description
) {}

// Response -- read-only, @JsonProperty for multi-word snake_case fields
public record CreateOrderResponse(
        @JsonProperty("order_id") UUID orderId
) {}

// Detail response -- all fields exposed
public record GetOrderByIdResponse(
        @JsonProperty("order_id") UUID orderId,
        @JsonProperty("user_id") UUID userId,
        String title,
        String description,
        @JsonProperty("start_date") Instant startDate
) {}
```

One response record per endpoint (e.g., `GetOrderByIdResponse`, `GetAllOrdersResponse`, `UpdateOrderResponse`).

### 4. Create the REST Controller

Place in `infrastructure/<aggregate>/in/`. The controller implements the REST API interface and contains zero annotations besides `@RestController`.

```java
@RestController
public class OrderRestController implements OrderRestAPI {

    private final ReadOrderUseCase readOrderUseCase;
    private final CreateOrderUseCase createOrderUseCase;
    private final OrderMapper orderMapper;

    public OrderRestController(ReadOrderUseCase readOrderUseCase,
            CreateOrderUseCase createOrderUseCase, OrderMapper orderMapper) {
        this.readOrderUseCase = readOrderUseCase;
        this.createOrderUseCase = createOrderUseCase;
        this.orderMapper = orderMapper;
    }

    @Override
    public ResponseEntity<GetOrderByIdResponse> getOrderById(UUID id) {
        return readOrderUseCase.getOrderById(id)
                               .map(orderMapper::toGetOrderByIdResponse)
                               .map(ResponseEntity::ok)
                               .orElseGet(() -> ResponseEntity.notFound()
                                                              .build());
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        var command = orderMapper.toCreateOrderCommand(request);
        var orderCreated = createOrderUseCase.createOrder(command);
        return orderMapper.toCreateOrderResponse(orderCreated);
    }
}
```

### 5. Add Mapper Methods to `<Domain>Mapper` in `infrastructure/<aggregate>/mapper/`

```java
@Mapper(componentModel = "spring", uses = { OrderObjectFactory.class, OrderIdMapper.class, ... })
public interface OrderMapper {
    // Request -> Command
    CreateOrderCommand toCreateOrderCommand(CreateOrderRequest request);
    UpdateOrderCommand toUpdateOrderCommand(UUID orderId, UpdateOrderRequest request);

    // Domain -> Response (use @Mapping when field names differ)
    @Mapping(target = "orderId", source = "id")
    GetOrderByIdResponse toGetOrderByIdResponse(Order order);

    @Mapping(target = "orderId", source = "id")
    CreateOrderResponse toCreateOrderResponse(Order order);
}
```

### 6. Add Error Handler (if new service) -- see `references/error-handling.md`

### 7. Whitelist Public Endpoints in SecurityConfiguration

Add unauthenticated paths to `SecurityConfiguration`:

```java
public static final String[] API_WHITELIST_URLS = { "/api/auth/**" };
public static final String[] API_WHITELIST_POST_URLS = { "/api/users" };
```

## Common Pitfalls

- **Hardcoding paths**: Never put URL strings in controllers -- always use `<Domain>RestAPIURL` constants from `asapp-commons-url`
- **Annotations on controller**: All `@GetMapping`, `@Operation`, `@ApiResponse`, `@Tag` go on the REST API interface, never on the controller
- **Missing `@Valid`**: Always pair `@RequestBody` with `@Valid` on the interface method parameter to trigger bean validation
- **Wrong `@ResponseStatus`**: Use only for fixed status codes (201, 200). Omit when status varies (use `ResponseEntity` for 200/404)
- **Custom error format**: Always use RFC 7807 `ProblemDetail` -- never invent custom error bodies
- **Missing `@JsonProperty`**: Multi-word fields must use `@JsonProperty("snake_case")` (e.g., `@JsonProperty("user_id")`)
- **One response per endpoint**: Create separate response records per endpoint even if fields are identical
- **`@SecurityRequirement`**: Add at the interface level for protected services (not on the auth service)
