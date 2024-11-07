# Implementation Citations

This documentation covers all of the citations for the SmuSQL project. The citations are organized
by the file in which they are referenced.

## BPlusTree

- [Java Program to Implement B+ Tree](https://www.geeksforgeeks.org/java-program-to-implement-b-tree/)
- [Understanding B-Trees: The Data Structure Behind Modern Databases](https://www.youtube.com/watch?v=K1a2Bk8NrYQ&t=454s)
- [B-tree vs B+ tree in Database Systems](https://www.youtube.com/watch?v=UzHl2VzyZS4&t=1393s)

### AI Prompts (Claude 3.5 Sonnet / Claude 3 Opus)

- Context Prompt
```text
BPlusTreeEngine is my implementation of an in memory SQL using BPlusTree where it utilizes two different trees a main tree which stores the primary key as the key and the rowData (Hashmap) as it's values, and multiple indexing trees with non-unique fields as the key and the primary key as the value.
```

- Other Prompts
```text
I am working on a Java project tasked with creating an SQL command line interface which supports the following commands

CREATE TABLE Creates a new table with specific columns.
INSERT Inserts a new row into the specified table.
SELECT * Retrieves rows from the specified table.
UPDATE Updates rows fulfilling a specified condition from the specified table.
DELETE Deletes rows fulfilling a specified condition from the specified table.

Help me create a B+ Trees Skeletion implementation for my SQL CLI here are some starter codes that I worked on
```

```text
how to I approach improving this implementation of B+ tree such that it has a it has Values where data is stored in the leaf nodes and Keys responsible for traversal. The tree should support indexing of non-unique keys
```

```text
this is my current implementation of B+ tree used to implement a SQL query processing engine. Help me create a skeleton code to consume the SQL queries, utilize my B+ Tree and provide the appropriate results.
```

```text
during my testing I identified that the evaluateCondition () function in BPlusTreeEngine is taking very long what are some ways I can optimize it
```

```text
my formatSelectResults() is taking a very long time to compute how can I optimize it further
```

```text
during my testing I found out that the rows.rangeSearch() in retrieveFilteredRows is taking a long time to compute and compute is there a way to optimize it?
```

## AstParser

```text
help me create a Lexical Analyzer and Abstract Syntax Tree for parsing SQL commands in java. My program only needs to support CREATE TABLE, INSERT, SELECT *, UPDATE, DELETE, WHERE, OR, AND
```

## Benchmarking B+ Tree Order

```text
Here is an benchmarking script I created to evaluate the best order for my B+ tree implementation. Instead of only outputting a string help me generate graphs for me to better view the results in java.
```

# SQLBenchmark


```text
This is my existing benchmarking script to evaluate different SQL engines and configurations by running a set of queries with consistent randomness, achieved by seeding the random function. The script should:

- Track performance metrics for each type of SQL query (e.g., INSERT, SELECT, UPDATE, DELETE, complex SELECT, complex UPDATE), recording runtime and memory usage.
- Record these metrics in a CSV file after every user-defined interval (e.g., every X queries).
- Enable consistent benchmarking by grouping and saving metrics by query type.

This CSV file will later be used in a Python script to visualize the relationship between the number of queries run and the runtime/memory usage.
```

## BenchmarkVisualizer
```text
create the companion Python code for visualizing the metrics I am going to use a jupyter notebook
```

## skipHash

### AI Prompts (GPT 4o)

1. Please help me finish the implementation of the Table class, which is supposed to emulate an SQL table. Provided as well is the skipList indexing version created for you to use as a reference.
2. Using these fields, help me create a `returnKeysByRequirementsOnIndex` method for the hashmap rows + skiplist indexing `Table.java`.
3. Help me change the implementation of `Engine.java` to accommodate my new table structure and methods.
4. Help me create the insert logic.
5. Since all the column values are indexed, there is no need for `returnKeysByRequirementsOnId`. Use the methods available in the new `Table.java` to fix the `applyDeleteLogic`.
6. Help me create the method for the apply select logic as well.
7. Now help me make the create table and update table logic under `engine.java`.
8. Extract the `applyUpdateLogic` method (I wanted to refactor the code so I can manually debug something).
9. Help me change this part in the insert statement such that strings are properly inserted when using parenthesis such as 'hello' (so that I can pass the test cases provided by Alson).
10. Help me debug various error messages from testing.
11. Help me handle the case where the operator is '!='.

### AI Prompts (GPT o1-preview)

1. These two tests are failing. Help me figure out why. `ValuesGreaterThan` stops too late and `valuesLesserThanOrEquals` stops too early. Provided is the implementation as well.
2. Help me fix this as well. The problem was that 0 rows were returned. Here’s my equals method in indexing and how it works in the skiplist. I cannot select when the city is 'Los Angeles'. What might be causing this issue?