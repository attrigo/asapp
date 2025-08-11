#!/bin/bash

# Create data directory with correct permissions
TOKEN_DIR="/etc/prometheus/auth"
mkdir -p "${TOKEN_DIR}"
ACCESS_TOKEN_FILE="${TOKEN_DIR}/bearer_token"

# Variable to store refresh token
REFRESH_TOKEN=

# UAA service URLs
AUTH_HEALTH_ENDPOINT="${UAA_SERVICE_BASE_URL}/actuator/health"
AUTH_ENDPOINT="${UAA_SERVICE_BASE_URL}/api/auth/token"
REFRESH_ENDPOINT="${UAA_SERVICE_BASE_URL}/api/auth/refresh-token"

# Time to sleep between token refreshing
SLEEP_TIME=$(( ACCESS_TOKEN_EXPIRATION_TIME * 8 / 10000 ))  # Convert from milliseconds and take 80%

validate_parameters() {
    if [ -z "$UAA_SERVICE_BASE_URL" ] || [ -z "$PROMETHEUS_USER" ] || [ -z "$PROMETHEUS_PASSWORD" ] || [ -z "$ACCESS_TOKEN_EXPIRATION_TIME" ]; then
        echo "Error: Required environment variables are not set"
        echo "Please ensure UAA_SERVICE_BASE_URL, PROMETHEUS_USER, PROMETHEUS_PASSWORD, and ACCESS_TOKEN_EXPIRATION_TIME are set"
        exit 1
    fi
}

install_dependencies() {
    if ! command -v jq &> /dev/null; then
        echo "Installing dependencies..."
        apk add --no-cache jq
    fi
}

wait_uaa_service_health() {
    echo "Waiting for asapp-uaa-service to be healthy..."
    until curl -s ${AUTH_HEALTH_ENDPOINT} | grep '"status":"UP"' > /dev/null; do
      echo "Still waiting..."
      sleep 10
    done
}

parse_response() {
    local response="$1"

    echo "Response: $response"

    # Check if response contains tokens
    if echo "$response" | grep -q "access_token"; then
        ACCESS_TOKEN=$(echo "$response" | sed 's/.*access_token:\([^,]*\).*/\1/')
        REFRESH_TOKEN=$(echo "$response" | sed 's/.*refresh_token:\([^}]*\).*/\1/')

        ACCESS_TOKEN=$(echo "$response" | jq -r '.access_token')
        REFRESH_TOKEN=$(echo "$response" | jq -r '.refresh_token')

        echo "Access Token: $ACCESS_TOKEN"
        echo "Refresh Token: $REFRESH_TOKEN"

        echo "$ACCESS_TOKEN" > "${ACCESS_TOKEN_FILE}"
        echo "Token saved to: ${ACCESS_TOKEN_FILE}"
        return 0
    else
        echo "Something goes wrong while authenticating"
        echo "Authentication response: $response"
        return 1
    fi
}

authenticate() {
    echo "Performing initial authentication..."
    RESPONSE=$(curl -s -X POST "${AUTH_ENDPOINT}" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"${PROMETHEUS_USER}\", \"password\": \"${PROMETHEUS_PASSWORD}\"}")

    parse_response "$RESPONSE"
    return $?
}

refresh_token() {
    echo "Refreshing token..."
    RESPONSE=$(curl -s -X POST "${REFRESH_ENDPOINT}" \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\": \"${REFRESH_TOKEN}\"}")

    parse_response "$RESPONSE"
    return $?
}

# Validate required env variables
validate_parameters || exit 1

# Install dependencies if not exists
install_dependencies

# Wait until UAA service is up and ready
wait_uaa_service_health

# Initial authentication
authenticate || exit 1

# Refresh token periodically
while true; do
    sleep $((SLEEP_TIME))
    refresh_token || authenticate || exit 1
done

