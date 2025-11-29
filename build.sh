#!/bin/bash
echo "Building Mr. Dinner Service..."
echo

# Clean previous build
if [ -f "*.class" ]; then
    echo "Cleaning previous build..."
    find . -name "*.class" -delete
fi

echo "Compiling Java source files..."

# Compile all Java files with Java 8 compatibility
javac -d . -source 8 -target 8 com/mrdinner/domain/common/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling common domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/customer/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling customer domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/menu/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling menu domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/order/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling order domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/staff/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling staff domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/delivery/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling delivery domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/payment/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling payment domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/domain/inventory/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling inventory domain classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/service/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling service classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/app/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling application classes"
    exit 1
fi

javac -d . -source 8 -target 8 com/mrdinner/gui/*.java
if [ $? -ne 0 ]; then
    echo "Error compiling GUI classes"
    exit 1
fi

echo
echo "Build completed successfully!"
echo
echo "To run the application:"
echo "Console version: java com.mrdinner.app.Main"
echo "GUI version: java com.mrdinner.gui.MainGUI"
echo
