# Shared Libraries

Located in `libs/`:

## asapp-commons-url
Centralized API endpoint constants
- `AuthenticationRestAPIURL`, `UserRestAPIURL`, `TaskRestAPIURL`
- Use these constants instead of hardcoding paths

## asapp-rest-clients
Shared REST client infrastructure
- `UriHandler`: Interface for building service URIs
- `DefaultUriHandler`: Implementation using base URL configuration
- `FallbackRestClientConfiguration`: Provides RestClient.Builder beans