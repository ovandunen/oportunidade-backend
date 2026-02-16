#!/bin/bash
# Generates RSA key pair for JWT signing
# Private key is git-ignored for security

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESOURCES_DIR="$PROJECT_ROOT/src/main/resources"

echo "🔐 Generating JWT RSA key pair..."

mkdir -p "$RESOURCES_DIR"

# Generate private key (2048-bit RSA)
openssl genrsa -out "$RESOURCES_DIR/privateKey.pem" 2048

# Generate public key from private key
openssl rsa -in "$RESOURCES_DIR/privateKey.pem" -pubout -out "$RESOURCES_DIR/publicKey.pem"

# Set restrictive permissions
chmod 600 "$RESOURCES_DIR/privateKey.pem"
chmod 644 "$RESOURCES_DIR/publicKey.pem"

echo ""
echo "✅ JWT keys generated successfully!"
echo ""
echo "📁 Files created:"
echo "   - $RESOURCES_DIR/privateKey.pem (PRIVATE - git-ignored)"
echo "   - $RESOURCES_DIR/publicKey.pem (PUBLIC)"
echo ""
echo "⚠️  IMPORTANT SECURITY NOTES:"
echo "   1. privateKey.pem is git-ignored and must NEVER be committed"
echo "   2. Keep privateKey.pem secure - it signs all tokens"
echo "   3. Backup privateKey.pem securely (encrypted storage)"
echo "   4. In production, use secrets management (Vault, AWS Secrets Manager, etc.)"
echo ""
echo "📝 Add to your .env file:"
echo "   JWT_PRIVATE_KEY_FILE=$RESOURCES_DIR/privateKey.pem"
echo ""
