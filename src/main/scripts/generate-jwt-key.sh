#!/bin/bash
# This script is committed, but the keys it generates are not

mkdir -p src/main/resources

# Generate RSA key pair
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem

echo "✅ JWT keys generated!"
echo "⚠️  privateKey.pem is git-ignored for security"
