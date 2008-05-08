#

chdir("data");
printAll(ls());

chdir("../data2");

if (!-exists "test.pl")
{
   println("So far so good...");
}

printAll(`ls -1`);
