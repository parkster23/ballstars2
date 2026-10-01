#!/bin/bash

# BallStars Grid Games - Build and Deploy Script
# Usage: ./deploy.sh

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  BallStars Grid Games - Build & Deploy${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}✗ Error: adb not found${NC}"
    echo "Please install Android SDK Platform Tools"
    exit 1
fi

# Check for connected devices
echo -e "${YELLOW}➤ Checking for connected devices...${NC}"
DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l | xargs)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}⚠ No devices connected${NC}"
    echo -e "${YELLOW}➤ Starting Pixel 7a emulator...${NC}"

    # Check if emulator exists
    if ! $HOME/Library/Android/sdk/emulator/emulator -list-avds | grep -q "Pixel_7a_API_VanillaIceCream"; then
        echo -e "${RED}✗ Pixel 7a emulator not found${NC}"
        echo "Available emulators:"
        $HOME/Library/Android/sdk/emulator/emulator -list-avds
        echo "Please create a Pixel 7a emulator or connect a device"
        exit 1
    fi

    # Start emulator in background
    $HOME/Library/Android/sdk/emulator/emulator -avd Pixel_7a_API_VanillaIceCream -no-snapshot-load > /dev/null 2>&1 &
    EMULATOR_PID=$!
    echo -e "${BLUE}ℹ Emulator starting (PID: $EMULATOR_PID)...${NC}"

    # Wait for emulator to boot (max 2 minutes)
    echo -e "${YELLOW}➤ Waiting for emulator to boot...${NC}"
    BOOT_WAIT=0
    MAX_WAIT=120

    while [ $BOOT_WAIT -lt $MAX_WAIT ]; do
        if adb devices | grep -q "emulator.*device"; then
            # Additional check: wait for boot to complete
            BOOT_COMPLETE=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
            if [ "$BOOT_COMPLETE" = "1" ]; then
                echo -e "${GREEN}✓ Emulator ready${NC}"
                break
            fi
        fi
        sleep 2
        BOOT_WAIT=$((BOOT_WAIT + 2))
        echo -ne "\rWaiting... ${BOOT_WAIT}s / ${MAX_WAIT}s"
    done

    echo ""

    if [ $BOOT_WAIT -ge $MAX_WAIT ]; then
        echo -e "${RED}✗ Emulator failed to start within ${MAX_WAIT} seconds${NC}"
        exit 1
    fi

    # Update device count
    DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l | xargs)
fi

echo -e "${GREEN}✓ Found $DEVICE_COUNT device(s)${NC}"
adb devices | grep -w "device"
echo ""

# Build the APK
echo -e "${YELLOW}➤ Building debug APK...${NC}"
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo ""

# Check APK size
APK_PATH="androidApp/build/outputs/apk/debug/androidApp-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
    echo -e "${BLUE}ℹ APK size: $APK_SIZE${NC}"
fi
echo ""

# Install the APK
echo -e "${YELLOW}➤ Installing APK...${NC}"

# Try install with replace flag first
adb install -r "$APK_PATH" 2>&1 | tee /tmp/install_output.txt

# Check if installation failed due to storage
if grep -q "INSTALL_FAILED_INSUFFICIENT_STORAGE" /tmp/install_output.txt; then
    echo -e "${YELLOW}⚠ Insufficient storage, uninstalling old version...${NC}"
    adb uninstall venturewave.one.gridgames 2>/dev/null || true
    echo -e "${YELLOW}➤ Retrying installation...${NC}"
    adb install "$APK_PATH"
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Installation successful${NC}"
else
    echo -e "${RED}✗ Installation failed${NC}"
    exit 1
fi
echo ""

# Launch the app
echo -e "${YELLOW}➤ Launching app...${NC}"
adb shell am start -n venturewave.one.gridgames/.MainActivity

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ App launched successfully${NC}"
else
    echo -e "${RED}✗ Failed to launch app${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✓ Deploy complete!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Tip: To view logs, run: adb logcat | grep GridGames"
echo ""

# Cleanup
rm -f /tmp/install_output.txt
