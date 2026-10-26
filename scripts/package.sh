#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

version="${1:-dev}"

case "$(uname -s)" in
    Linux*) os=linux ;;
    Darwin*) os=macos ;;
    MINGW* | MSYS* | CYGWIN*) os=windows ;;
    *) echo "unsupported os" >&2; exit 1 ;;
esac

case "$(uname -m)" in
    x86_64 | amd64) arch=x86_64 ;;
    arm64 | aarch64) arch=aarch64 ;;
    *) echo "unsupported arch" >&2; exit 1 ;;
esac

java=runtime/bin/java
[ "$os" = windows ] && java=runtime/bin/java.exe

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

if command -v sha256sum >/dev/null; then
    sum="$(sha256sum "dist/${name}.tar.gz" | cut -d' ' -f1)"
else
    sum="$(shasum -a 256 "dist/${name}.tar.gz" | cut -d' ' -f1)"
fi
size="$(wc -c < "dist/${name}.tar.gz" | tr -d ' ')"

manifest="dist/manifest-${os}-${arch}.json"
cat > "$manifest" <<JSON
{
  "version": "${version}",
  "os": "${os}",
  "arch": "${arch}",
  "file": "${name}.tar.gz",
  "sha256": "${sum}",
  "size": ${size},
  "java": "${java}",
  "classpath": "lib/*",
  "mainClass": "app.fluxa.gateway.MainKt"
}
JSON

rm -rf "$out"
cat "$manifest"
