# Test Scripts

This directory contains scripts for testing the Tateca Backend API in local development.

## Files

- `test-api.sh` - Comprehensive API test suite covering all endpoints
- `init-test-data.sql` - Initial test data for development database

## Prerequisites

1. **Application running** on `http://localhost:8080` with **dev profile**
   ```bash
   ./gradlew bootRun
   # or
   SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
   ```

2. **Database running** with Docker Compose
   ```bash
   docker-compose up -d
   ```

3. **Test data loaded** (see instructions below)

4. **jq installed** (optional, for JSON formatting)
   ```bash
   # macOS
   brew install jq

   # Ubuntu/Debian
   sudo apt-get install jq
   ```

## Setup Test Data

### Option 1: Auto-load on Docker Compose startup

Update `docker-compose.yml` to mount the init script:

```yaml
services:
  mysql:
    # ... other config ...
    volumes:
      - mysql_data:/var/lib/mysql
      - ./scripts/init-test-data.sql:/docker-entrypoint-initdb.d/init.sql
```

Then restart:
```bash
docker-compose down -v  # Remove volumes to reset database
docker-compose up -d
```

### Option 2: Manual execution

```bash
# Wait for MySQL to be ready
docker-compose up -d
sleep 10

# Execute SQL script
mysql -h localhost -u a -pa db < scripts/init-test-data.sql
```

### Option 3: Using MySQL client in container

```bash
docker-compose exec db bash -c "mysql -u a -pa db < /scripts/init-test-data.sql"
```

## Running Tests

### Run all tests

```bash
./scripts/test-api.sh
```

### Test output

The script will:
- Display colored output (green for success, red for errors)
- Format JSON responses with jq (if available)
- Show HTTP status codes
- Group tests by controller

Example output:
```
╔════════════════════════════════════════════════════════╗
║        Tateca Backend API Test Suite                  ║
╚════════════════════════════════════════════════════════╝
Base URL: http://localhost:8080
Date: Sat Dec 28 12:00:00 JST 2025

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. DevController Tests (/dev)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

▶ 1.1 Generate Firebase Token
GET /dev/firebase-token/test-user-001
✓ Success
{
  "customToken": "eyJhbGc..."
}
...
```

## Test Data Reference

### Test Users

| UID | Name | Email | Auth | User UUID |
|-----|------|-------|------|-----------|
| test-user-001 | Test User 1 | test1@example.com | ✓ | 550e8400-e29b-41d4-a716-446655440001 |
| test-user-002 | Test User 2 | test2@example.com | ✓ | 550e8400-e29b-41d4-a716-446655440002 |
| test-user-003 | Test User 3 | test3@example.com | ✓ | 550e8400-e29b-41d4-a716-446655440003 |

### Test Groups

| UUID | Name | Join Token |
|------|------|------------|
| 650e8400-e29b-41d4-a716-446655440001 | Team Outing 2024 | 750e8400-e29b-41d4-a716-446655440001 |

### Test Transactions

| UUID | Type | Title | Amount | Currency |
|------|------|-------|--------|----------|
| 850e8400-e29b-41d4-a716-446655440001 | LOAN | Dinner at restaurant | 5000 | JPY |

## Customization

### Disable colors

Edit `test-api.sh`:
```bash
USE_COLOR=false
```

### Disable jq formatting

Edit `test-api.sh`:
```bash
USE_JQ=false
```

### Change base URL

Edit `test-api.sh`:
```bash
BASE_URL="http://your-server:port"
```

### Test specific endpoints

You can copy individual test commands from the script and run them manually:

```bash
# Example: Test get group info only
curl -X GET "http://localhost:8080/groups/650e8400-e29b-41d4-a716-446655440001" | jq .
```

## Troubleshooting

### Application not responding

```bash
# Check if application is running
curl -I http://localhost:8080/actuator/health

# Check logs
./gradlew bootRun
```

### Database connection errors

```bash
# Check MySQL is running
docker-compose ps

# Check MySQL logs
docker-compose logs db

# Restart MySQL
docker-compose restart db
```

### Test data not loaded

```bash
# Verify data in database
docker-compose exec db mysql -u a -pa db -e "SELECT COUNT(*) FROM users"

# Reload test data
mysql -h localhost -u a -pa db < scripts/init-test-data.sql
```

### jq not formatting output

```bash
# Install jq
brew install jq  # macOS
# or
sudo apt-get install jq  # Ubuntu/Debian

# Or disable jq in script
USE_JQ=false
```

## Notes

- Dev profile disables authentication, allowing curl testing without Firebase tokens
- The `@UId` resolver falls back to `x-uid` header in dev profile
- Exchange rate API calls require `EXCHANGE_RATE_API_KEY` environment variable
- Some tests create new resources (groups, transactions) - these persist in the database
