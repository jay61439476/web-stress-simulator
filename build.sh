#!/bin/bash

set -e

BUILD_VERSION=1.0.9

echo "Building web-stress-simulator $BUILD_VERSION..."

docker build -t web-stress-simulator:$BUILD_VERSION .

echo "Finished building web-stress-simulator."
