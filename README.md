# Pantry Planner

Pantry Planner is a React frontend backed by a Spring Boot API and MySQL.

## GitHub Pages deployment

I deploy the frontend with the same GitHub Pages Actions pattern used by my portfolio.
Every push to `main` builds the Vite app and publishes it at `https://jay0078-gif.github.io/Pantry_Planner/`.

The first deployment can publish the frontend before the backend exists.
Until I set `VITE_API_BASE_URL`, the login screen stays disabled and explains that the API still needs configuration.
After the backend is live, I set the repository variable `VITE_API_BASE_URL` to its public HTTPS URL and rerun the workflow.
The value is the backend origin without `/api`, such as `https://api.example.com`.
Vite embeds this value in the public frontend bundle, so it is configuration rather than a secret.
I create it under **Settings > Secrets and variables > Actions > Variables**.

In GitHub, I open **Settings > Pages** and choose **GitHub Actions** as the build source.
The workflow in `.github/workflows/deploy.yml` handles later deployments automatically.

GitHub Pages only hosts the React frontend.
The Spring Boot application and MySQL database must run on a separate host, and that host must allow requests from `https://jay0078-gif.github.io`.

## Free backend deployment

I use an Aiven Free MySQL database and a Render Free web service for the Spring Boot API.
The root `render.yaml` file defines the Render service, its Docker build, health check, and non-secret production settings.

In Aiven, I create a Free MySQL service and wait for it to reach the Running state.
I use the connection details from Aiven's Java quick-connect screen to set these values when Render asks for them:

- `DATABASE_URL` uses `jdbc:mysql://HOST:PORT/DATABASE?sslmode=require`.
- `DATABASE_USERNAME` is the Aiven service username, normally `avnadmin`.
- `DATABASE_PASSWORD` is the generated Aiven service password.
- `SEED_OWNER_PASSWORD` is a separate strong password I choose for the Pantry Planner owner account.

In Render, I create a Blueprint from this repository and keep the `free` service plan selected.
Render prompts for the four private values above during the first Blueprint deployment.
The Blueprint generates and stores `JWT_SECRET`, so I never put the signing secret in GitHub or the frontend bundle.
After the API is live, I copy its HTTPS `onrender.com` origin into the GitHub repository variable named `VITE_API_BASE_URL` and rerun the Pages workflow.

Render Free services sleep after 15 minutes without traffic, so the first request after an idle period can take about a minute.
The frontend waits up to 90 seconds and explains that first wake instead of failing after a few seconds.
The Render health check uses `/actuator/health`, which reports the API as healthy only when the database is also available.

Login uses a short-lived signed bearer token instead of a cross-site session cookie.
That keeps authentication working after Render sleeps and in browsers that block third-party cookies.
The token is valid for 30 minutes by default, and logging out removes it from the current browser tab.
Public registration is disabled for this portfolio deployment.
The seeded owner can create another user or admin through the protected auth endpoints when needed.

Render's local filesystem is temporary, so I removed the unfinished admin upload endpoint.
I will use object storage before adding uploads back.

The safe local variable list is in the backend `.env.example` file.
`SEED_USER_PASSWORD` is optional and only creates the sample user when it is set.

The repository previously contained live credentials in `application.properties`.
Removing them from the current files does not remove them from Git history, so I rotate the database password and Pexels key before exposing the backend.

## Local development

I copy the frontend `.env.example` to `.env` when I need a non-default API URL.
The local default backend is `http://localhost:8080`.
The local frontend opens at `http://localhost:5173/Pantry_Planner/#/` because Vite uses the same project base as GitHub Pages.

```sh
npm install --prefix Pantry_Planner_FrontEndfile/pantry-web-frontend
npm run dev
```
