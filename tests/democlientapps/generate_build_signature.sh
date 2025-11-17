#!/bin/bash
#
# Copyright (C) 2025 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Script to generate build signature with date and nonce

# Ensure we are in the root of the repo to run this

OUTPUT_DIR="packages/apps/ContactsPicker/tests/democlientapps/lib/res/raw"

# Create directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Generate Date and Nonce
DATE=$(date '+%Y-%m-%d')
# Generate a random 6-character hex string for nonce
NONCE=$(openssl rand -hex 3)

BUILD_STRING="Build: $DATE - $NONCE"

# Write to file
echo "$BUILD_STRING" > "$OUTPUT_DIR/build_info.txt"

echo "Generated build signature: $BUILD_STRING"
echo "Saved to $OUTPUT_DIR/build_info.txt"
