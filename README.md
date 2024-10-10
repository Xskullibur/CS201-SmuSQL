# smuSQL Database System

This repository contains the implementation of the smuSQL database system. Below, you'll find an overview of the supported SQL-like commands, team members, evaluation metrics, and the core logic of our implementation.

## Table of Contents
1. [Compilation and Execution](#compilation-and-execution)
2. [Supported smuSQL Commands](#supported-smusql-commands)
   - [CREATE TABLE](#1-create-table)
   - [INSERT](#2-insert)
   - [SELECT *](#3-select-)
   - [UPDATE](#4-update)
   - [DELETE](#5-delete)
3. [Team Members](#team-members)
4. [Evaluation Metrics](#evaluation-metrics)
5. [Implementation Logic](#implementation-logic)

---
## Compilation and Execution

- Please ensure that Apache Maven and Java JDK 17 is installed on your machine. 
- You can compile the code by navigating to the root directory of the project (i.e. the directory in which pom.xml resides) and
running `mvn compile`.
- Run the code using `mvn exec:java`.

---
## Supported smuSQL Commands

The following is an exhaustive list of commands supported by the smuSQL database system. For examples of correct behavior for each command, please refer to the sample implementation.

### 1. CREATE TABLE
Creates a new table with specific columns.

- **Syntax**:  
  `CREATE TABLE table_name (column1, column2, ...)`
  
- **Example**:  
  `CREATE TABLE student (id, name, age, gpa, deans_list)`

> **Note**: Columns do not have explicit typing in smuSQL. However, all data inserted into a column is expected to be of the same type.

---

### 2. INSERT
Inserts a new row into the specified table.

- **Syntax**:  
  `INSERT INTO table_name VALUES (value1, value2, ...)`
  
- **Example**:  
  `INSERT INTO student VALUES (1, John, 30, 2.4, False)`

---

### 3. SELECT *
Retrieves rows from the specified table.

- **Syntax**:  
  `SELECT * FROM table_name`
  
- **Example**:  
  `SELECT * FROM student`

The `SELECT *` command can accept a `WHERE` clause, such that only rows fulfilling the condition(s) following the `WHERE` clause are returned. A second condition may be used with the `AND` or `OR` clauses. Supported conditional operators include: `=`, `<`, `>`, `<=`, and `>=`.

- **Syntax**:  
  `SELECT * FROM table_name WHERE condition1 AND/OR condition2`
  
- **Examples**:
  - `SELECT * FROM student WHERE gpa > 3.8`
  - `SELECT * FROM student WHERE gpa > 3.8 AND age < 20`
  - `SELECT * FROM student WHERE gpa > 3.8 OR age < 20`

---

### 4. UPDATE
Updates rows fulfilling a specified condition from the specified table. Prints the number of rows updated.

- **Syntax**:  
  `UPDATE table_name SET update WHERE condition1 AND/OR condition2`
  
- **Examples**:
  - `UPDATE student SET age = 25 WHERE id = 1`
  - `UPDATE student SET deans_list = True WHERE gpa > 3.8 OR age = 20`

---

### 5. DELETE
Deletes rows fulfilling a specified condition from the specified table. Prints the number of rows deleted.

- **Syntax**:  
  `DELETE FROM table_name WHERE condition1 AND/OR condition2`
  
- **Examples**:
  - `DELETE FROM student WHERE gpa < 2.0`
  - `DELETE FROM student WHERE gpa < 2.0 OR name = little_bobby_tables`

---

## Team Members

- Alson
- Hsien Ray
- Kenneth
- Nibras
- Nicholas
- Samuel

Each team member contributed to different aspects of the smuSQL database system. You can find their specific responsibilities and roles in the individual module descriptions within the codebase.

---

## Evaluation Metrics

Our evaluation metrics are designed to measure the performance and correctness of the smuSQL system. The following metrics were used:

1. **Execution Time**: Time taken to execute each command (in milliseconds).
   - Example: Time taken to execute `SELECT *` queries.
   
2. **Memory Usage**: The amount of memory consumed by the system during the execution of commands.
   - Example: Memory usage when creating large tables.

3. **Correctness**: Accuracy in returning the correct rows for queries and handling edge cases.
   - Example: Evaluating the correctness of `WHERE` clauses in `SELECT *` queries.

4. **Concurrency**: The system's ability to handle multiple queries at the same time without conflicts.
   - Example: Testing updates and inserts in parallel.

5. **Scalability**: How well the system scales when increasing the number of rows or tables.
   - Example: Creating and querying tables with millions of rows.

---

## Implementation Logic

The implementation of the smuSQL database system is based on a modular design to ensure flexibility, scalability, and maintainability. Below are the key modules and their core functionality:

### 1. **Parser Module**
   - Responsible for parsing SQL-like queries and converting them into internal commands.
   - Implements lexical analysis to identify commands, keywords, and conditions.

### 2. **Table Storage Module**
   - Manages table creation, row insertion, and data storage.
   - Tables are stored using an in-memory structure optimized for fast retrieval and modification.

### 3. **Indexing Module**
   - Implements indexing using a B-tree data structure to optimize search and retrieval times.
   - Indexes are automatically created for primary keys to enable fast lookups.

### 4. **Query Processor Module**
   - Processes `SELECT`, `UPDATE`, and `DELETE` queries.
   - Optimizes query execution by using indexes and caching frequently accessed data.
