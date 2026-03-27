# Error Handling Reference

## GlobalExceptionHandler Structure

One per service in `infrastructure/error/`. Extends `ResponseEntityExceptionHandler`.

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================================
    // 400 BAD REQUEST - Validation Errors
    // ============================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        logger.warn("Validation failed: {}", ex.getBindingResult()
                                               .getFieldErrors()
                                               .stream()
                                               .map(FieldError::getDefaultMessage)
                                               .collect(Collectors.joining(", ")));

        var invalidParameters = buildInvalidParameters(ex.getBindingResult().getFieldErrors());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("errors", invalidParameters);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Argument");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ============================================================================
    // 500 INTERNAL SERVER ERROR - Server/Infrastructure Failures
    // ============================================================================

    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ProblemDetail> handleDataAccessException(DataAccessException ex) {
        logger.error("Database operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", "server_error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    // ============================================================================
    // 503 SERVICE UNAVAILABLE - External Service Failures
    // ============================================================================

    @ExceptionHandler(RedisConnectionFailureException.class)
    protected ResponseEntity<ProblemDetail> handleRedisException(RedisConnectionFailureException ex) {
        logger.error("Redis operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setProperty("error", "temporarily_unavailable");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private List<InvalidRequestParameter> buildInvalidParameters(List<FieldError> fieldErrors) {
        Function<FieldError, InvalidRequestParameter> fieldErrorMapper =
                fieldError -> new InvalidRequestParameter(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        fieldError.getDefaultMessage());

        return fieldErrors.stream().map(fieldErrorMapper).toList();
    }

}
```

## InvalidRequestParameter Record

One per service in `infrastructure/error/`:

```java
public record InvalidRequestParameter(
        String entity,
        String field,
        String message
) {}
```

## Error Response Strategy

| HTTP Status | Handler approach | Detail message |
|---|---|---|
| 400 BAD_REQUEST | Validation: override `handleMethodArgumentNotValid`; domain: `IllegalArgumentException` | Include field-level errors (safe for clients) |
| 401 UNAUTHORIZED | Generic message only -- prevent user enumeration | `"Invalid credentials"` or `"Invalid token"` |
| 500 INTERNAL_SERVER_ERROR | Generic message -- hide infrastructure details | `"An internal error occurred"` |
| 503 SERVICE_UNAVAILABLE | Generic message -- hide infrastructure details | `"Service temporarily unavailable"` |

## ProblemDetail Conventions

- Always set `title` and `detail` via `ProblemDetail.forStatusAndDetail(...)`
- Set `title` to a human-readable HTTP status label (e.g., `"Bad Request"`, `"Internal Server Error"`)
- Add `error` property for machine-readable error codes (e.g., `"invalid_grant"`, `"server_error"`, `"temporarily_unavailable"`)
- Validation errors add `errors` property with `List<InvalidRequestParameter>`
- Critical failures add `"critical": true` property for monitoring alerts

## Logging Rules in Error Handlers

- `logger.warn(...)` for 400/401 errors (client mistakes)
- `logger.error(..., ex)` for 500/503 errors (include stack trace)
- Never log passwords, tokens, or PII
