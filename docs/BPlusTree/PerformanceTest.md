## Query Performance Test

![img.png](queryPerformanceTest.png)
- Majority of the computation time is taken up by the `formatSelectResults` function responsible for
  formatting the results of the query.

### String Field Queries

| Testing Category  | Average Time | Min Time | Max Time |
|-------------------|--------------|----------|----------|
| Single String =   | 342 ms       | 211 ms   | 7513 ms  |
| Single String !=  | 2370 ms      | 1959 ms  | 26477 ms |
| Double String AND | 89 ms        | 65 ms    | 1046 ms  |
| Double String OR  | 634 ms       | 518 ms   | 3120 ms  |

### Numeric Field Queries

| Testing Category         | Average Time | Min Time | Max Time |
|--------------------------|--------------|----------|----------|
| Single Number = (Age)    | 97 ms        | 78 ms    | 670 ms   |
| Single Number = (Salary) | 6 ms         | 3 ms     | 371 ms   |
| Single Number !=         | 3332 ms      | 2228 ms  | 58310 ms |
| Number Range >           | 1636 ms      | 8 ms     | 6190 ms  |
| Number Range >=          | 1830 ms      | 34 ms    | 35924 ms |
| Number Range <           | 1947 ms      | 5 ms     | 30780 ms |
| Number Range <=          | 1970 ms      | 21 ms    | 29399 ms |
| Double Number AND        | 98 ms        | 8 ms     | 806 ms   |
| Double Number OR         | 101 ms       | 80 ms    | 813 ms   |

### Mixed Type Queries

| Testing Category    | Average Time | Min Time | Max Time |
|---------------------|--------------|----------|----------|
| String-Number AND   | 57 ms        | 40 ms    | 652 ms   |
| String-Number OR    | 283 ms       | 225 ms   | 3223 ms  |
| Complex Mixed Query | 702 ms       | 34 ms    | 15942 ms |

## Main Test

| Operation                 | Time Taken (seconds) |
|---------------------------|----------------------|
| INSERT operations         | 4.2729843            |
| SELECT operations         | 1.2421325            |
| Complex SELECT operations | 582.6044442          |
| UPDATE operations         | 4.7006637            |
| UPDATE operations         | 4.6071057            |
| DELETE operations         | 2.3610118            |

- 1,000,000 Operations