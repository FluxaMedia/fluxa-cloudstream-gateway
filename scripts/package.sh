#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

version="${1:-dev}"
os="$(uname -s | tr '[:upper:]' '[:lower:]')"
arch="$(uname -m)"
name="fluxa-cloudstream-gateway-${version}-${os}-${arch}"
out="dist/${name}"

rm -rf "$out"
mkdir -p "$out"

gradle installDist -q --no-daemon
cp -r build/install/fluxa-cloudstream-gateway/lib "$out/lib"

jlink \
    --add-modules java.base,java.logging,java.naming,java.sql,java.net.http,java.xml,java.management,java.desktop,jdk.unsupported,jdk.crypto.ec,jdk.httpserver,java.security.sasl,java.scripting \
    --strip-debug --no-header-files --no-man-pages --compress=zip-9 \
    --output "$out/runtime"

tar czf "dist/${name}.tar.gz" -C dist "$name"
sum="$(sha256sum "dist/${name}.tar.gz" | cut -d' ' -f1)"
size="$(stat -c %s "dist/${name}.tar.gz")"

cat > "dist/${name}.json" <<JSON
{
  "version": "${version}",
  "os": "${os}",
  "arch": "${arch}",
  "file": "${name}.tar.gz",
  "sha256": "${sum}",
  "size": ${size},
  "java": "runtime/bin/java",
  "classpath": "lib/*",
  "mainClass": "app.fluxa.gateway.MainKt"
}
JSON

rm -rf "$out"
cat "dist/${name}.json"
