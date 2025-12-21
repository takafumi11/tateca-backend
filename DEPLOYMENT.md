# Deployment Guide

## GitHub Container Registry (GHCR) Setup

This project uses GitHub Container Registry to store Docker images and Railway for deployment.

### How it works

1. **CI Pipeline**: When code is pushed to `main` branch
   - Run tests
   - Build Docker image
   - Push to GHCR at `ghcr.io/takafumi11/tateca-backend`

2. **Railway Deployment**: Railway pulls the image from GHCR

---

## Railway Configuration

### Step 1: Configure Railway to use GHCR

1. Go to your Railway project dashboard
2. Click on your service
3. Go to **Settings** → **Source**
4. Change from "GitHub Repo" to "Docker Image"
5. Enter the image URL:
   ```
   ghcr.io/takafumi11/tateca-backend:latest
   ```

### Step 2: Configure Authentication (if repository is private)

If your GitHub repository is private, Railway needs authentication to pull the image:

1. Create a GitHub Personal Access Token (PAT):
   - Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Click "Generate new token (classic)"
   - Select scopes:
     - ✅ `read:packages`
   - Copy the token

2. In Railway, go to your service → **Variables**
3. Add the following variables:
   ```
   DOCKER_REGISTRY_USERNAME=your-github-username
   DOCKER_REGISTRY_PASSWORD=your-github-pat
   ```

### Step 3: Configure Environment Variables

Ensure these environment variables are set in Railway:

```env
# Database
MYSQLHOST=your-mysql-host
MYSQLPORT=3306
MYSQLDATABASE=your-database-name
MYSQLUSER=your-username
MYSQLPASSWORD=your-password

# Firebase
FIREBASE_SERVICE_ACCOUNT_KEY=your-firebase-service-account-json

# Lambda API Key
LAMBDA_API_KEY=your-lambda-api-key

# Exchange Rate API
EXCHANGE_RATE_API_KEY=your-exchange-rate-api-key

# Spring Profile (automatically set)
SPRING_PROFILES_ACTIVE=prod
```

### Step 4: Deploy

1. Railway will automatically detect the image changes
2. Or manually trigger a deployment from Railway dashboard
3. Check the deployment logs to ensure success

---

## Image Tags

The CI pipeline creates multiple tags for flexibility:

- `latest` - Always points to the latest main branch build (recommended for Railway)
- `main-{sha}` - Specific commit from main branch
- `main` - Latest main branch build

### Using a specific version

If you want to pin to a specific version:

```
ghcr.io/takafumi11/tateca-backend:main-abc1234
```

---

## Local Development with GHCR

### Pull the image locally

```bash
# Login to GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Pull the image
docker pull ghcr.io/takafumi11/tateca-backend:latest

# Run locally
docker run -p 8080:8080 \
  -e MYSQLHOST=localhost \
  -e MYSQLPORT=3306 \
  -e MYSQLDATABASE=tateca \
  -e MYSQLUSER=root \
  -e MYSQLPASSWORD=password \
  -e SPRING_PROFILES_ACTIVE=dev \
  ghcr.io/takafumi11/tateca-backend:latest
```

---

## Troubleshooting

### Image not found

1. Verify the CI pipeline completed successfully
2. Check that the image exists: https://github.com/takafumi11/tateca-backend/pkgs/container/tateca-backend
3. Ensure the repository visibility settings allow Railway to access the image

### Authentication errors

1. Verify your GitHub PAT has `read:packages` permission
2. Check that `DOCKER_REGISTRY_USERNAME` and `DOCKER_REGISTRY_PASSWORD` are correctly set in Railway
3. Try regenerating the PAT

### Deployment issues

1. Check Railway deployment logs
2. Verify all environment variables are set
3. Ensure the Spring profile is set to `prod`

---

## CI/CD Pipeline Details

### Workflow: `.github/workflows/ci.yml`

**Triggers:**
- Pull request to `main` → Run tests only
- Push to `main` → Run tests + Build and push Docker image
- Manual trigger → Run tests + Build and push Docker image

**Jobs:**
1. `build-and-test`: Run Gradle build and tests
2. `docker-build-and-push`: Build Docker image and push to GHCR (only on main branch)

**Optimizations:**
- Docker layer caching with GitHub Actions cache
- Multi-stage build in Dockerfile
- Tests are skipped in Docker build (already run in CI)

---

## Security Notes

1. ✅ No secrets in Dockerfile or code
2. ✅ All sensitive data in Railway environment variables
3. ✅ Docker image uses non-root user (if configured in Dockerfile)
4. ✅ GitHub token is automatically provided by GitHub Actions
5. ✅ Railway variables are encrypted at rest

---

## Cost

- **GHCR**: Free for public repositories, 500MB free for private repositories
- **Railway**: Pay-as-you-go based on usage
- **GitHub Actions**: 2000 minutes/month free

This project's Docker image (~200MB) fits well within GHCR's free tier.
