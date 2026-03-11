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
# Script to generate a copy of existing client app for testing multi client env.
#
# Usage: ./generate_clients.sh <path_to_app_dir> [num_copies]

# Check for passed arguments.
if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    echo "Usage: $0 <path_to_app_dir> [num_copies]"
    exit 1
fi

SRC_DIR="${1%/}"
# Set NUM_COPIES to the second argument if it exists, otherwise default to 1
NUM_COPIES="${2:-1}"

MANIFEST="$SRC_DIR/AndroidManifest.xml"
BP_FILE="$SRC_DIR/Android.bp"

# Extract base package name and Soong target
ORIG_PKG=$(grep -oP 'package="\K[^"]+' "$MANIFEST" | head -1)
ORIG_TARGET=$(grep -oP 'name:\s*"\K[^"]+' "$BP_FILE" | head -1)
SAFE_ORIG_PKG="${ORIG_PKG//./\.}"

for ((i=1; i<=NUM_COPIES; i++)); do
    NEW_DIR="${SRC_DIR}_$i"
    NEW_PKG="${ORIG_PKG}.client${i}"
    NEW_TARGET="${ORIG_TARGET}_${i}"

    echo "-> Generating copy $i in $NEW_DIR..."
    rm -rf "$NEW_DIR"
    cp -r "$SRC_DIR" "$NEW_DIR"

    # Update Soong Target Name
    sed -i "s/${ORIG_TARGET}/${NEW_TARGET}/g" "$NEW_DIR/Android.bp"

    # Update package name in manifest, kotlin files, and Android.bp
    sed -i "s/package=\"$ORIG_PKG\"/package=\"$NEW_PKG\"/g" "$NEW_DIR/AndroidManifest.xml"
    find "$NEW_DIR" -type f \( -name "*.kt" -o -name "Android.bp" \) -exec sed -i "s/${SAFE_ORIG_PKG}/${NEW_PKG}/g" {} +

    # Visual Changes

    # Change the app label directly in the Manifest
    sed -i -E "s/android:label=\"[^\"]+\"/android:label=\"Picker Client ${i}\"/g" "$NEW_DIR/AndroidManifest.xml"

    # Generate distinct hex colors
    # Icon background
    ICON_R=$(printf '%02X' $(( (i * 75) % 200 + 55 )))
    ICON_G=$(printf '%02X' $(( (i * 115) % 200 + 55 )))
    ICON_B=$(printf '%02X' $(( (i * 155) % 200 + 55 )))
    ICON_HEX="${ICON_R}${ICON_G}${ICON_B}"

    # App background
    BG_R=$(printf '%02X' $(( 150 + (i * 35) % 105 )))
    BG_G=$(printf '%02X' $(( 150 + (i * 65) % 105 )))
    BG_B=$(printf '%02X' $(( 150 + (i * 95) % 105 )))
    BG_HEX="${BG_R}${BG_G}${BG_B}"

    # Change the icon background color in colors.xml
    COLORS_XML="$NEW_DIR/res/values/colors.xml"
    if [ -f "$COLORS_XML" ]; then
        sed -i -E "s/<color name=\"icon_background\">#[0-9A-Fa-f]+<\/color>/<color name=\"icon_background\">#FF${ICON_HEX}<\/color>/g" "$COLORS_XML"
    fi

    # Apply a distinct background to the Main Activity
    # Inject the required import statement below the existing Modifier import
    find "$NEW_DIR" -type f -name "MainActivity*.kt" -exec sed -i 's/^package .*/&\n\nimport androidx.compose.foundation.background/' {} +

    # Attach the background modifier to the top-level Column/Box
    find "$NEW_DIR" -type f -name "MainActivity*.kt" -exec sed -i "s/Modifier\.fillMaxSize()/Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF${BG_HEX}))/g" {} +

    # Replace the on-screen app title
    find "$NEW_DIR" -type f -name "MainActivity*.kt" -exec sed -i "s/ScreenTitle(targetSdk = targetSdk)/androidx.compose.material3.Text(text = \"Picker Client Copy ${i}\", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)/g" {} +

    echo "   Created target: $NEW_TARGET"
done

echo "Done! Generated $NUM_COPIES client apps."
