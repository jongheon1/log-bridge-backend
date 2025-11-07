#!/bin/bash

# .env 파일을 읽어서 환경변수로 export
# 사용법: source load-env.sh

if [ -f .env ]; then
    echo "🔧 Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
    echo "✅ Environment variables loaded successfully!"
else
    echo "❌ .env file not found. Please create one based on .env.example"
    exit 1
fi
