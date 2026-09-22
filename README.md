# Fair Trip Distribution Across Cab Vendors

## Project Overview
This is a backend system to fairly distribute cab trips among multiple vendors according to their contracted shares. It supports zones, trip types, capacity, rejection, carry-forward, concurrency, and deterministic allocation.

## Technology Stack
* Java 21
* Spring Boot
* Maven
* MySQL
* Redis
* Spring Data JPA
* Spring Security
* Spring Web
* Validation
* Actuator

## Architecture
The project follows a standard MVC/layered architecture:
`Controller -> Service -> Repository -> MySQL`

## Current Setup Status
* Initialized Maven Spring Boot project.
* Created standard package structure.
* Added documentation for algorithm and database design.
