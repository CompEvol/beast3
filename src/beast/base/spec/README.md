# Specification of BEAST 3 (Version 0.2.0)

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

<a href="./Domain0.2.png"><img src="./Domain0.2.png" width="800" ></a>


## 2. Type 

Constrain the value’s dimension.

<a href="./Type0.2.png"><img src="./Type0.2.png" width="800" ></a>

## 3. Distribution 

<a href="./Distribution2.0.png"><img src="./Distribution2.0.png" width="800" ></a>

