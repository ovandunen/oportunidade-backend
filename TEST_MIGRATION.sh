#!/bin/bash
# Quick script to test database migrations
# Run this to verify Phase 1 database changes

set -e


echo "======================================"
echo "Phase 1 Migration Verification Script"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if PostgreSQL is running
echo "1. Checking PostgreSQL..."
if psql -h localhost -U postgres  -d odoo_payments -c "SELECT version();" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL is running${NC}"
else
    echo -e "${RED}✗ Cannot connect to PostgreSQL${NC}"
    echo "  Please ensure PostgreSQL is running and database 'odoo_payments' exists"
    exit 1
fi

echo ""

# Check Flyway schema history
echo "2. Checking Flyway migration history..."
MIGRATION_COUNT=$(psql -h localhost -U postgres -d odoo_payments -t -c "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;" 2>/dev/null || echo "0")
echo "   Migrations applied: $MIGRATION_COUNT"

if [ "$MIGRATION_COUNT" -ge "1" ]; then
    echo -e "${GREEN}✓ Flyway migrations found${NC}"
    echo ""
    echo "   Migration details:"
    psql -h localhost -U postgres  -d odoo_payments -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
else
    echo -e "${YELLOW}⚠ No migrations found - will run on first startup${NC}"
fi

echo ""

# Check for V2 migration tables
echo "3. Checking for Phase 1 tables..."

check_table() {
    TABLE_NAME=$1
    if psql -h localhost -U postgres  -d odoo_payments -t -c "SELECT to_regclass('$TABLE_NAME');" | grep -q "$TABLE_NAME"; then
        echo -e "${GREEN}✓ Table exists: $TABLE_NAME${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Table missing: $TABLE_NAME${NC}"
        return 1
    fi
}

check_table "orders"
check_table "employer_references"
check_table "order_candidates"
check_table "order_odoo_documents"

echo ""

# Check for new columns in orders table
echo "4. Checking orders table columns..."

check_column() {
    COLUMN_NAME=$1
    if psql -h localhost -U postgres  -d odoo_payments  -t -c "\d orders" | grep -q "$COLUMN_NAME"; then
        echo -e "${GREEN}✓ Column exists: $COLUMN_NAME${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Column missing: $COLUMN_NAME${NC}"
        return 1
    fi
}

check_column "employer_id"
check_column "employer_email"
check_column "package_type"
check_column "reference_code"


# Check for indexes
echo "5. Checking indexes..."
INDEX_COUNT=$(psql -h localhost -U postgres  -d odoo_payments  -t -c "SELECT COUNT(*) FROM pg_indexes WHERE tablename IN ('employer_references', 'orders', 'order_candidates', 'order_odoo_documents');" 2>/dev/null || echo "0")
echo "   Indexes found: $INDEX_COUNT"

if [ "$INDEX_COUNT" -ge "9" ]; then
    echo -e "${GREEN}✓ Expected indexes found${NC}"
else
    echo -e "${YELLOW}⚠ Some indexes may be missing (expected ~9)${NC}"
fi

echo ""

# Summary
echo "======================================"
echo "Summary"
echo "======================================"

if [ "$MIGRATION_COUNT" -ge "2" ]; then
    echo -e "${GREEN}✓ Phase 1 migrations COMPLETE${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Insert test employer reference (see below)"
    echo "2. Start application: ./mvnw quarkus:dev"
    echo "3. Test payment webhook"
else
    echo -e "${YELLOW}⚠ Migrations PENDING${NC}"
    echo ""
    echo "To apply migrations:"
    echo "1. Start application: ./mvnw quarkus:dev"
    echo "   (Migrations run automatically on startup)"
    echo "2. Re-run this script to verify"
fi

echo ""
echo "======================================"
echo "Test Data Setup (Optional)"
echo "======================================"
echo ""
echo "To insert a test employer reference, run:"
echo 
echo "psql -h localhost -U postgres -d odoo_payments <<EOF"
echo "INSERT INTO employer_references (reference_code, employer_id, employer_email, company_name, created_at, is_active)"
echo "VALUES ('TEST-REF-001', 'EMP-001', 'test@employer.com', 'Test Company', NOW(), true)"
echo "ON CONFLICT (reference_code) DO NOTHING;"
echo "EOF"
echo ""
