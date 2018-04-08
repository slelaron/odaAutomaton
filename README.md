# odaAutomaton

Program helps to find minimal ODA automaton consistent with input/output examples.

To run the program add "BumbleBEE" and "pl-satsolver.so" to the project directory.

### Example
```
3
aaaabbbbaaaabbbaaaaababab
0122210001222100122222222
abab
0000
aabbaaabbbaaaabbbb
011001221001222100
```

You should get automaton. First line contains number of states and number of edges. Next number of edges lines contains information about each edge (start node, end node, transaction symbol). Last 2 lines contains number of final states and all final states in non-decreasing order.

### Output
```
4 8
1 4 (a 0)
1 1 (b 0)
2 3 (a 2)
2 1 (b 1)
3 3 (a 2)
3 2 (b 2)
4 2 (a 1)
4 1 (b 0)
4
1 2 3 4
```