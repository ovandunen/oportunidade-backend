# OpenAPI Specification for Third-Party REST Consumers

The Recruiting Agency API exposes an OpenAPI 3.0 specification for integration with external systems.

## Endpoints

| Resource | URL | Description |
|----------|-----|-------------|
| **OpenAPI JSON** | `GET /api-docs/openapi` | Machine-readable spec (for code generators, Postman) |
| **OpenAPI YAML** | `GET /api-docs/openapi?format=yaml` | YAML format |
| **Swagger UI** | `GET /q/swagger-ui` | Interactive API documentation |

## Usage

### Download the spec

```bash
# JSON (default)
curl -o openapi.json http://localhost:8080/api-docs/openapi

# YAML
curl -o openapi.yaml "http://localhost:8080/api-docs/openapi?format=yaml"
```

### Import into tools

- **Postman**: Import → Link → paste `http://localhost:8080/api-docs/openapi`
- **OpenAPI Generator**: `openapi-generator-cli generate -i http://localhost:8080/api-docs/openapi -g java -o ./client`
- **Swagger Codegen**: Use the JSON/YAML URL as input

## API Overview

| Tag | Endpoints | Use Case |
|-----|-----------|----------|
| Payment References | `POST /api/v1/payment-references/text`, `/qr` | Issue MultiCaixa payment references |
| Orders | `GET /api/v1/orders/merchant/{merchantTxId}` | Lookup order status after payment |
| Payment Transactions | `GET /api/v1/payment-transactions/appypay/{id}` | Query payment by AppyPay transaction ID |
| Document Access | `GET /api/documents/candidates`, `/download/...` | Secure document download (token required) |
| Webhooks | `POST /webhooks/appypay` | Incoming from AppyPay (configure in dashboard) |

## Base URL

When integrating, replace the base URL with your deployment:

- Development: `http://localhost:8080`
- Production: `https://your-api.example.com`
