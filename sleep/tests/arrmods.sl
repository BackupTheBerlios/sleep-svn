#
# test of array functions...
#

@a = @("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
@b = @(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
@c = copy(@b);

println(splice(@c, subarray(@a, -4), 3));

@c = copy(@b);
println(splice(@c, subarray(@a, -4), 3, 0));

@c = copy(@b);
println(splice(@c, subarray(@a, -4, -2), 3, 0));

@c = copy(@b);
println(removeAt(@c, -3));
println(@c);

add(@c, "test!!!", 0);
println(@c);

add(@c, "zzzz", -1);
println(@c);
