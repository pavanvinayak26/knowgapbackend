# Render Deployment Guide

## Prerequisites
- GitHub repository pushed with latest code
- Render account at https://render.com

## Automatic Deployment with render.yaml

The `render.yaml` file in the root directory automatically configures your deployment. Render will:
1. Build the Docker image using the `Dockerfile`
2. Create a PostgreSQL database
3. Set environment variables automatically

## Manual Setup (if not using render.yaml)

### Step 1: Create a Web Service
1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **New +** → **Web Service**
3. Connect your GitHub repository
4. Fill in the service details:
   - **Name**: `knowgap-backend`
   - **Runtime**: Docker
   - **Build Command**: Leave empty (uses Dockerfile)
   - **Start Command**: Leave empty (uses Dockerfile ENTRYPOINT)

### Step 2: Create a PostgreSQL Database
1. Click **New +** → **PostgreSQL**
2. Choose a name (e.g., `knowgap-db`)
3. Note the connection details

### Step 3: Set Environment Variables
In your Web Service settings, add these environment variables:
- `PORT`: `8080`
- `SPRING_PROFILES_ACTIVE`: `prod`
- `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<host>:<port>/<database>`
- `SPRING_DATASOURCE_USERNAME`: `<username>`
- `SPRING_DATASOURCE_PASSWORD`: `<password>`
- `JWT_SECRET`: Your custom JWT secret key (optional)
- `HUGGINGFACE_API_TOKEN`: Your HF token (if using AI features)

### Step 4: Deploy
Click **Deploy** and wait for the build to complete.

## Database Configuration

The application uses PostgreSQL with automatic schema updates (`ddl-auto=update`).

On first deployment:
- Tables will be created automatically
- You can run database seeders if available

## Scaling & Monitoring

- **Logs**: View in Render dashboard → Web Service → Logs
- **Monitoring**: Use Render's built-in metrics
- **Restart**: Use dashboard to restart the service if needed

## Common Issues

### Build Fails
- Check logs in Render dashboard
- Ensure Dockerfile is at repository root
- Verify Java 21 is available (used in Dockerfile)

### Database Connection Errors
- Verify `SPRING_DATASOURCE_URL`, `USERNAME`, and `PASSWORD` are correct
- Ensure PostgreSQL database is running
- Check network/firewall settings

### Application Startup Fails
- Check logs for bean creation errors
- Verify all required environment variables are set
- Ensure database is accessible before app starts

## Cleanup

To destroy resources:
1. Go to Render dashboard
2. Delete the Web Service
3. Delete the PostgreSQL database
4. Confirm deletion
