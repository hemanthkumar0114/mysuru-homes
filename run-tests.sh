#!/bin/bash
# Runs the whole automated test suite - backend and frontend - with one command.
# Backend tests run against an in-memory database (see backend/src/test/resources),
# so nothing here needs MySQL running or any environment variable set.
set -e

echo "==> Backend tests (JUnit + Spring Boot)"
(cd "$(dirname "$0")/backend" && ./mvnw test)

echo
echo "==> Frontend tests (Vitest)"
(cd "$(dirname "$0")/frontend" && npm test)

echo
echo "All tests passed."
