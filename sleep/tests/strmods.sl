#
# test of string functions...
#

println(right("this is a test", 4));  # the right most 4 chars
println(left("this is a test", 4));   # the left most 4 chars

println(right("this is a test", -5)); # all right chars except the left 5
println(left("this is a test", -5));  # all left chars except the right 5

println(replaceAt("this is a test", "uNF", -6, 1));

println(charAt("this is a test", -6));

for ($x = 0; $x < 80; $x++)
{
   print(charAt("this is a test", $x));
}

println();

for ($x = 80; $x >= 0; $x--)
{
   print(charAt("this is a test", $x));
}

println();

# reverse a string quickly with the negative indice bits...
$str = "++this is a reversible string :)--";
for ($x = strlen($str) - 1; $x >= 0; $x--)
{
   print(charAt($str, $x));
}

println();

println(substr($str, -11, -5));

println(mid($str, -11, 6));

[{ println(substr($str, -11, -15)); }];

for ($x = 0; $x < 25; $x++)
{
   println(indexOf("this is a test", "i", $x));
}
