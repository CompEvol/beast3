# Specification of BEAST 3 (Version 0.1.0)

This specification defines a strong typing system that enforces compile-time type checking in order to:

1. Reduce the risk of introducing bugs when models or operators involve multiple logics across different input domains.
2. Ensure that developers classify classes by types, thereby reducing complexity and improving code clarity and maintainability.

## Current scope

- core

## Developer ecosystem

- BEAST 3 is based on JDK 25 (LTS).

https://www.azul.com/downloads/?version=java-25-ea&package=jdk#zulu

<a href="./JDK25.png"><img src="./JDK25.png" width="500" ></a>

## 1. Domain

The supported domain types in this version are:

<a href="./Domain0.1.0.png"><img src="./Domain0.1.0.png" width="800" ></a>


## 2. Shape

Constrain the value’s dimension.

### Scalar

<a href="./Scalar0.1.0.png"><img src="./Scalar0.1.0.png" width="500" ></a>

### Vector

<a href="./Vector0.1.0.png"><img src="./Vector0.1.0.png" width="500" ></a>