#!/bin/bash
echo "Starting Mr. Dinner Service GUI..."
echo

# Check if classes exist
if [ ! -f "com/mrdinner/app/Main.class" ]; then
    echo "Classes not found. Building first..."
    chmod +x build.sh
    ./build.sh
    if [ $? -ne 0 ]; then
        echo "Build failed. Exiting..."
        exit 1
    fi
fi

echo "Compiling GUI classes..."
javac -d . -source 8 -target 8 com/mrdinner/gui/*.java

if [ $? -ne 0 ]; then
    echo "GUI compilation failed. Exiting..."
    exit 1
fi

echo "Starting Mr. Dinner Service GUI..."
echo

java com.mrdinner.gui.MainGUI

echo
echo "GUI application closed."
