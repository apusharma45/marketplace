# marketplace

## Docker setup (App + PostgreSQL)

Before running Docker, set environment variables in your terminal.

PowerShell:

```powershell
$env:POSTGRES_DB="marketplace"
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="your_strong_password"
```

The Docker setup uses these environment variables for both PostgreSQL and Spring Boot.

### 1. Build and start application + database

```powershell
docker compose up --build -d
```

### 2. Check container status

```powershell
docker compose ps
```

### 3. Access the app

- App: `http://localhost:8080`
- PostgreSQL: `localhost:5433`

### 4. Stop containers when done

```powershell
docker compose down
```
