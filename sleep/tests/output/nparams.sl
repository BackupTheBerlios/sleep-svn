Trace: &local('$test') at line 9
test1: success!
Trace: &println('test1: success!') at line 10
Trace: &test1($test => 'success!') at line 13
Trace: &local('$test') at line 9
test1: 
Trace: &println('test1: ') at line 10
Trace: &test1() at line 14
test2: yes, another one
Trace: &println('test2: yes, another one') at line 18
Trace: &test2($test => 'yes, another one') at line 21
Warning: variable '$test' not declared at line 18
test2: 
Trace: &println('test2: ') at line 18
Trace: &test2() at line 22
Trace: &this('$test') at line 26
test3: eh?!?
Trace: &println('test3: eh?!?') at line 27
Trace: &test3($test => 'eh?!?') at line 31
Trace: &this('$test') at line 26
test3: 
Trace: &println('test3: ') at line 27
Trace: &test3() at line 32
Trace: &this('$test') at line 26
test3: :)
Trace: &println('test3: :)') at line 27
Trace: &test3() at line 33
Trace: &local('$count $var') at line 37
0   = a
Trace: &println('0   = a') at line 41
1   = b
Trace: &println('1   = b') at line 41
2   = c
Trace: &println('2   = c') at line 41
3   = d
Trace: &println('3   = d') at line 41
a: apple and b: boy and c: cat
Trace: &println('a: apple and b: boy and c: cat') at line 44
Trace: &test4('a', 'b', $a => 'apple', 'c', $b => 'boy', 'd', $c => 'cat') at line 47
Test 5 has been called, executing action:
Trace: &println('Test 5 has been called, executing action:') at line 51
The passed in closure has been called
Trace: &println('The passed in closure has been called') at line 55
Trace: [&closure6961504] at line 52
Trace: &test5($action => &closure6961504) at line 55
Trace: &test5(action => &closure14470877) - FAILED! at line 56
Warning: unreachable named parameter: action at line 56
