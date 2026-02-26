#!/usr/bin/env zsh
# Restart Ktor server + Compose Desktop app
# Usage: .claude/commands/restart.sh [--server-only | --app-only]

PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SERVER_LOG="/tmp/ai-challenge-server.log"
APP_LOG="/tmp/ai-challenge-app.log"

cd "$PROJECT_DIR" || exit 1

# ── Kill existing processes ──────────────────────────────────────────────────
echo "⏹  Stopping existing processes..."
lsof -ti:8080 | xargs kill -9 2>/dev/null && echo "   Killed port 8080" || true
pkill -f "composeApp:run" 2>/dev/null && echo "   Killed composeApp" || true
sleep 1

# ── Server ───────────────────────────────────────────────────────────────────
if [[ "$1" != "--app-only" ]]; then
    echo "🚀 Starting server..."
    ./gradlew :server:run > "$SERVER_LOG" 2>&1 &
    SERVER_PID=$!

    # Wait for startup
    for i in {1..20}; do
        if grep -q "Responding at" "$SERVER_LOG" 2>/dev/null; then
            echo "   Server ready on port 8080 (PID $SERVER_PID)"
            break
        fi
        sleep 1
    done
fi

# ── App ───────────────────────────────────────────────────────────────────────
if [[ "$1" != "--server-only" ]]; then
    echo "🖥  Starting desktop app..."
    ./gradlew :composeApp:run > "$APP_LOG" 2>&1 &
    APP_PID=$!
    echo "   App launching (PID $APP_PID)"
    echo "   Log: $APP_LOG"
fi

echo ""
echo "✅ Done. Logs:"
echo "   Server → $SERVER_LOG"
echo "   App    → $APP_LOG"
