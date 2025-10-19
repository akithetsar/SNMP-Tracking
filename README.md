# SNMP Network Monitoring System

A lightweight Java application for monitoring simulated routers in GNS3 using SNMP (v1/2c). Tracks interface status, traffic metrics, routing tables, and BGP sessions with real‑time updates.

Note: This project uses the community string `si2019` (read/write) by default.

## Features

- Variant 1 - Interface monitoring (status, MTU, speed)
- Variant 2 - Traffic flow analysis (packets, bitrate)
- Variant 3 - Routing table visualization
- Variant 4–5 - BGP neighbor discovery and session tracking
- Variant 6–9 - SNMP traps, CPU/memory monitoring, TCP/UDP session tracking

## Tech stack

- Java
- SNMP4J API
- GNS3 (network simulation)
- iReasoning MIB Browser (debugging / MIB inspection)

## Quick start

1. Prepare a GNS3 lab with simulated routers and configure SNMP v1/v2c on devices.
2. Ensure the SNMP community string is set to `si2019` (read/write) for testing, or update your application configuration to match your lab.
3. Build and run the Java application using your preferred Java build tool (Maven/Gradle/IDE) or run the packaged JAR.

Example (generic):
- mvn package
- java -jar target/snmp-monitor.jar

Note: Adjust the command and paths to match your build setup.

## Configuration

- SNMP versions supported: v1 and v2c.
- Default community string: `si2019` (read/write). Change in your configuration before connecting to production devices.
- You may use tools such as iReasoning MIB Browser for testing and troubleshooting SNMP queries.

## Usage notes

- Designed for monitoring GNS3‑simulated routers but can be adapted for real devices that allow the configured community string.
- Real‑time updates depend on SNMP polling intervals and how the app is configured; tuning the polling frequency helps balance latency vs. load.
- When using BGP features, ensure your simulated routers are configured with neighbors and relevant MIBs are available for the app to query.
