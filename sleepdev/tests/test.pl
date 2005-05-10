#!/usr/bin/perl
# Regression test perl script.  A quick way to make sure my crazy changes aren't
# breaking my precious language.
#
# Runs each sleep script in this directory, compares the results to the file output
# w/i the output directory.
#
# If they match - congratulations the script worked okay.
#
# If they don't match - something is broken.
#
# Executing the regression test:
# (from within the examples directory type)
#
# [user ~/sleep/tests]$ perl test.pl
#

@files = `ls -1 *.sl`;
chomp(@files);

if (!-e "output")
{
   `mkdir output`;
}

chdir("..");

foreach $var (@files)
{
   if (!-e "./tests/output/$var")
   {
      `java -jar sleep.jar ./tests/$var >./tests/output/$var`;
      push @errors, "$var output does not exist, creating it";
   }
   else
   {
      $expected_value = join("", `cat ./tests/output/$var`);
      $script_value   = join("", `java -jar sleep.jar ./tests/$var`);

      if ($expected_value ne $script_value)
      {
         push @errors, "Output of $var does not match expected output.";

         if ($ARGV[0] eq "-dump")
         {
            print "\njava -classpath . sleep.console.TextConsole load ../tests/$var\n";
            print "\n".$script_value."\n";
         }
      }  
      else
      {
         print ".";
      }
   }
}

if ($errors[0] eq "")
{
   print "\n[31337] All tests passed.  Looks like nothing is broken.\n";
}
else
{
   print "\n";
}

foreach $var (@errors)
{
   print "[WARNING] " . $var . "\n";
}

