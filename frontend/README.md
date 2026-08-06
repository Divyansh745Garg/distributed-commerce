# Distributed Commerce Frontend

A zero-dependency browser dashboard for testing the existing API gateway. It does not alter any backend service.

1. Start the backend from the repository root: `docker compose up --build -d`
2. In another terminal: `cd frontend` then `npm start`
3. Open [http://localhost:3000](http://localhost:3000) and use `admin` / `password`.

The Node server proxies `/api/*` to `http://localhost:8080`, avoiding browser CORS changes. Set `GATEWAY_URL` before starting if the gateway is elsewhere. The dashboard provides login/logout, product browsing and creation, checkout with an editable idempotency key, and a request/response monitor.
