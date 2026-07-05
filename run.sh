#!/bin/bash
# Run CodegenIDE
# Usage: ./run.sh [codegen-source-file.src]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CODEGEN_LIB="$HOME/Desktop/Resources/pureprogrammer/Codegen/lib/*"

cd "$SCRIPT_DIR"
mvn -q package -DskipTests 2>/dev/null

java -Dcodegen.classpath="$CODEGEN_LIB" \
     -jar target/CodegenIDE-1.0.0.jar "$@"
