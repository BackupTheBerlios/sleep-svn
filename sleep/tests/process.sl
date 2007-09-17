import sleep.error.YourCodeSucksException;
import sleep.runtime.ScriptLoader; 

sub executeScript
{
   local('$loader $script $buffer $error');
    
   $loader = [new ScriptLoader];
   $script = [$loader loadScript: $1];
   [$script addWarningWatcher: { println("*** $1"); }];

   checkError($error);
   if ($error && $error isa ^YourCodeSucksException)
   {
      return [$error formatErrors];
   }
 
   $buffer = allocate();
   [[[$script getScriptEnvironment] getEnvironment] 
                                    put: "%console%", $buffer];
   [$script runScript];

   closef($buffer);
 
   return readb($buffer, available($buffer));
}

$output = executeScript("for.sl");
print($output);
println("Size of output is: " . strlen($output));
