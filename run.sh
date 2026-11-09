#!/bin/sh
# Compile and launch the Student Management System (macOS / Linux).
# Usage: ./run.sh            start the app
#        ./run.sh test       compile and run the tests
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/sms/*.java test/sms/*.java
if [ "$1" = "test" ]; then
    java -cp out sms.StudentRepositoryTest
else
    java -cp out sms.StudentManagementApp
fi
