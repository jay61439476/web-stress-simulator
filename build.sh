#!/bin/bash

set -e

BUILD_VERSION=1.1.2

echo "Building web-stress-simulator $BUILD_VERSION..."

docker build -t web-stress-simulator:$BUILD_VERSION .

echo "Finished building web-stress-simulator."
