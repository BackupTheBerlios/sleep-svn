#
# a simple test of the profiler...
#   

sub fact
{
   return iff($1 == 0, 1, $1 * [$this : $1 - 1]); 
}

debug(24);

println("Hello World");
$x = ["test" length];
println("string length is: $x");

println([{ return "this is a closure call!: " . fact(10.0); }]);

@stats = profile();
foreach $var (@stats)
{
   # $var is a ScriptInstance.ProfilerStatistic object, it accepts
   # the following messages:
   # [$var calls] - number of calls for the function
   # [$var ticks] - total number of ticks used by the function
   # [$var functionName] - the function name...

   ($count, $name) = @([$var calls], [$var functionName]);
   println("$[3]count $name");
}
