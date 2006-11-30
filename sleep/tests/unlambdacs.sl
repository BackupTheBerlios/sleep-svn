#
# show lambda creating shared environments...
#

sub foo
{
   println("Value: $bar");
}

unlambda(&foo, $bar => "example 1");
foo();

[unlambda(&foo, $bar => "example 2")];
foo();
