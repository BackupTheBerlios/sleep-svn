package sleep.error;

import sleep.runtime.ScriptInstance;
import java.io.File;

/** A package for all information related to a runtime script warning.  A runtime script warning occurs whenever something bad 
  * happens while executing a script.  Something bad could include an exception being thrown by a bridge, a script trying to 
  * execute a non-existant function, a script trying to make a comparison with a non-existant predicate etc. 
  *
  * @see sleep.error.RuntimeWarningWatcher
  */
public class ScriptWarning
{
   protected ScriptInstance script;
   protected String         message; 
   protected int            line;

   public ScriptWarning(ScriptInstance _script, String _message, int _line)
   {
      script  = _script;
      message = _message;
      line    = _line;
   }

   /** returns the ScriptInstance object that was the source of this runtime error */
   public ScriptInstance getSource()
   {
      return script;
   }

   /** returns a short synopsis of what the warnng is */
   public String getMessage()
   {
      return message;
   }

   /** returns the line number in the source script where the runtime error/warning occured */
   public int getLineNumber()
   {
      return line;
   }

   /** returns the full path for the source script */
   public String getScriptName()
   {
      return getSource().getName();
   }

   /** returns just the filename of the source script */
   public String getNameShort()
   {
      return new File(getSource().getName()).getName();
   }
}

