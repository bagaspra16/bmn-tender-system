#!/bin/bash
# Run the BNM Tender System
# Ensure you have run 'mvn clean package' and 'mvn dependency:copy-dependencies' first, 
# or just run this if the environment is already set up.

# Clean the project
mvn clean package

# Copy dependencies to target directory
mvn dependency:copy-dependencies

# Run the application
java -cp target/bnm-tender-system-1.0-SNAPSHOT.jar:target/dependency/* com.bnm.tender.Main
