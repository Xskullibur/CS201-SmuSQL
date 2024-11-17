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

## SkipHash

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

## HashMap

### AI Prompts (GPT 4o)

1. Chatgpt I want to implement a hashmap implementation for my project. The details for my project are as such: 
    - The rows are mapped by key, (id) then values would be the rows
    - All the columns would be an index that is also map the rows
    For example:
    if there is a row:
    id name age --> there would be 3 keys where the first key would map the id to the row
    Then the columns name and age would also be indexed to map the row so that a select statement based on the rows would just use the index.

    You may also work on the parsing of the user's input so that it may be able to parse in the user's input efficiently for the hashmap implementation.

    I will attach several files for you to refer to to start working on the project.
    <Attach table.java, engine.java, parser.java, and main.java from the sample>

    For the commands that I need, I would want you to do simple SELECT, UPDATE, CREATE, DELETE statements like this:
    6.1.1 CREATE TABLE
    Creates a new table with specific columns.
        • Syntax: CREATE TABLE table_name (column1, column2, ...)
        • Example: CREATE TABLE student (id, name, age, gpa, deans_list)
    Note that in our database system, columns do not have explicit typing; however, all data inserted into a column is expected to be of the same type.
    6.1.2 INSERT
    Inserts a new row into the specified table.
        • Syntax: INSERT INTO table_name VALUES (value1, value2, ...)
        • Example: INSERT INTO student VALUES (1, John, 30, 2.4, False)
    6.1.3 SELECT *
    Retrieves rows from the specified table.
        • Syntax: SELECT * FROM table_name
        • Example: SELECT * FROM student
    The SELECT * command can accept a WHERE clause, such that only rows fulfilling the condition(s) following the WHERE clause are returned. A second condition may be used, with the AND or OR clauses. Supported conditional operators include: =, <, >, <= and >=.
        • Syntax: SELECT * FROM table_name WHERE condition1 AND/OR condition2
        • Example: SELECT * FROM student WHERE gpa > 3.8
        • Example: SELECT * FROM student WHERE gpa > 3.8 AND age < 20
        • Example: SELECT * FROM student WHERE gpa > 3.8 OR age < 20
    6.1.4 UPDATE
    Updates rows fulfilling a specified condition from the specified table. Prints of number of rows updated.
        • Syntax: UPDATE table_name SET update WHERE condition1 AND/OR condition2
        • Example: UPDATE student SET age = 25 WHERE id = 1
        • Example: UPDATE student SET deans_list = True WHERE gpa > 3.8 OR age = 201
    6.1.5 DELETE
    Deletes rows fulfilling a specified condition from the specified table. Prints number of rows deleted.
        • Syntax: DELETE FROM table_name WHERE condition1 AND/OR condition2
        • Example: DELETE FROM student WHERE gpa < 2.0
        • Example: DELETE FROM student WHERE gpa < 2.0 OR name = little_bobby_tables

2. <Copy the wrong section> Hello chat gpt, this part causes a null pointer exception, please help me fix this I also attached the modified table.java and engine.java for you

3. Chatgpt, you forgot to include the operators for comparison, please do add it in the table.java

4. Do you know any ways to make this code more efficient?

5. The complex SELECT statement causes problem: <copy over code section and the error statement>

6. The comparisons still won't work are you able to help? Which files do you think I need to look add to fix the problem?

7. How would an evaluation method look like for each individual SQL operation?

8. Help me format the select statement results into a table with the each column having equal spaces through out its rows

## SkipLinkListIndexed
ChatGPT4o Queries:
1. Below is the implementation of a singly linked list. Help me create the implementation of a skip linked list <insert implementation of java SLL>

2. explain your insert code

3. what does this line do: Node<E>[] update = new Node[levels];

4. help me create an 'add last' method into the skip link list implementation

5. how are tables stored and retrieved in H2 database?

6. format this into a better and more propre markdown file <insert smusql project handout under commands>

7. We are going to include more items in the readme file, such as team members, evaluation metrics, implementation logic etc. Please reformat the markdown such that they can be accounted for.

8. <insert my current Row.java> implement comparable in Row by id.

9. <insert Row.java> Let's allow rows to have other data in the form of a map, mapped by column name. How do i build other ways to index the database using the Objects involved in the columns of the row 

10. insert <
import java.util.*;
public class Table {
    String name;
    List<String> columns;
    SkipList<Row> data;

}  > Here is my db table, which has rows, and is implemented using skipList. Help me create the rest of the table for my database.

11. help me override the .equals method in row so that it is possible to search

12. should i store all the objects and id as string in the case that the primary key is not an integer?

13. Since all the column data are strings, help me create secondary indices based on the columns, which can be used to locate the row using the skip list structure

14. Change the secondary index implementation such that its map no longer stores duplicates, but instead the id of the original data

15. SkipList accepts a node<E>. Hence, help me create another class called 'Indexing' which implements comparable. The indexing contains String columnvalue, and String primaryKey which maps it to the original row

16. now help me build this indexing alongside the table as the values get inserted

17. help me create another method inside skipList where it gives all values below, equal to, or greater than, a specific value. Below is my current implementation of skip list. <insert SkipList.java>

18. what is enum? Using enum, give me the example of 3 values: equals, greater, below, and using it inside a function

19. the skip linked list should be able to handle duplicated values. Also, could you create 3 seperate methods for each comparison type, and do not use enum?

20. Work on just this method below. When building the result list, make sure to down to the lowest level which contains the value smaller than E value, then proceed to append to the result when there are equals. This will prevent earlier, but equal values from being missed, as the list contains duplicate values <insert public List<E> getValuesEqual(E value){...} method>

21. okay, the method is correct. Using these principles, help me create just the methods for get ValuesGreater, getValuesLesser, and getValuesBetween

22. Now help me create 5 more methods following those principles. These methods are:
getValuesGreaterOrEquals, getValuesLesserOrEquals, getValuesBetweenOrEquals, getValuesBetweenOrStartEquals, getValuesBetweenOrEndEquals

23. how do implement iterable in a skip linked list? Just give me the iterator mehtod

24. java maxint

25. help me change the compare function in Indexing such that it will compare the Strings as numbers if compatible with the column types. Otherwise compare as Strings 

26. how to convert String[] to List<String>

27. give me the the return statement for a successful create, select, update and delete operation being performed

28. give me the code for 'union' and 'intersection' in java

29. <insert Engine.java, Table.java> Help me create the java code for the smusql select statement and use a helper method to format it correctl

30. Here is my current code for engine. Since a lot hashing and comparisons are used, there might be bugs if the letters care cased differently. Help me change the code below such that smuSQL will always work regardless of casing. <insert Engine.java>

31. <insert image of error stack trace, and SkipList.java> help me debug.