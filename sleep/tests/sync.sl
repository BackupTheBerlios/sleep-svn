#
# test of synchronization primitive...
#

$mutex = semaphore();

$handle = fork({

  acquire($mutex);
  for ($x = 0; $x < 10; $x++)
  {
     println("fork: $x");
     [Thread yield];
  } 
  release($mutex);
}, $mutex => $mutex);

acquire($mutex);

for ($x = 0; $x < 10; $x++)
{
   println("main: $x");
   [Thread yield];
}

release($mutex);

