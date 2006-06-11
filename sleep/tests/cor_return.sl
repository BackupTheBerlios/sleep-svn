#
# ensure return properly resets the coroutine... 
#

sub a
{
   yield 1;
   
   yield 2;

   if ($1 ne $null)
   {
      return -1;
   }

   yield 3;
  
   yield 4;
}

println("Test 1: ");

println(a());
println(a());
println(a());
println(a());

println(a());
println(a());
println(a());
println(a());

println("Test 2: ");

println(a());
println(a());
println(a("boogidy boogidy"));
println(a());

println(a());
println(a());
println(a());
println(a());

