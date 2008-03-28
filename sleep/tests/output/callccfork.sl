Trace: &global('$handle $value') at callccfork.sl:18
Begin!
Trace: &println('Begin!') at callccfork.sl:9
Inside of callcc function
Trace: &println('Inside of callcc function') at callccfork.sl:12
Trace: [&closure[callccfork.sl:12-13]#3 CALLCC: &closure[callccfork.sl:9-15]#2] = 'pHEAR' at callccfork.sl:10
Trace: &fork(&closure[callccfork.sl:9-15]#1) = sleep.bridges.io.IOObject@d88db7 at callccfork.sl:20
Trace: &wait(sleep.bridges.io.IOObject@d88db7) = 'pHEAR' at callccfork.sl:21
pHEAR
Trace: &println('pHEAR') at callccfork.sl:22
