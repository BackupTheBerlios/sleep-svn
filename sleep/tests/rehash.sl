debug(7);

global('%hash');

%hash = ohasha(a => "apple", b => "bat", c => "cat", d => "dog");
setMissPolicy(%hash, { println("Didn't find: $2"); return 33; });
%hash['b'] = 'blah';
println(%hash);

rehash(%hash, 100, 0.2);
println(%hash);
%hash['c'] = 'car';
println(%hash);
println(%hash['ee']);
