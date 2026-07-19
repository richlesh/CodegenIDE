#!/bin/bash
# Run CodegenIDE
# Usage: ./run.sh [codegen-source-file.src]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$SCRIPT_DIR"
mvn -q package -DskipTests 2>/dev/null

java -jar target/CodegenIDE-1.1.0.jar "$@"
