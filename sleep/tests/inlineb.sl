#
# test a broken inline call.
#

inline foo
{
   $x += 3;
   yield $x;
}

sub bar
{
   local('$x');
   $x = 2;
   $z = 10 * foo();
   println($z);
   return 10;
}

println(bar());
println(bar());
