package sleep.engine;

import java.lang.reflect.*;
import java.util.*;

import sleep.runtime.*;

import sleep.engine.types.*;
import sleep.interfaces.Function;

import sleep.bridges.*;

/** This class is used to mock an instance of a class that implements a specified Java interface 
    using a Sleep function. */
public class ProxyInterface implements InvocationHandler
{
   protected ScriptInstance    script;
   protected Function          func;

   public ProxyInterface(Function _method, ScriptInstance _script)
   {
      func        = _method;
      script      = _script;
   }

   /** Returns the script associated with this proxy interface. */
   public ScriptInstance getOwner()
   {
      return script;
   }

   /** Constructs a new instance of the specified class that uses the passed Sleep function to respond
       to all method calls on this instance. */
   public static Object BuildInterface(Class className, Function subroutine, ScriptInstance script)
   {
      InvocationHandler temp = new ProxyInterface(subroutine, script);
      return Proxy.newProxyInstance(className.getClassLoader(), new Class[] { className }, temp);
   } 

   /** Constructs a new instance of the specified class that uses the passed block to respond
       to all method calls on this instance. */
   public static Object BuildInterface(Class className, Block block, ScriptInstance script)
   {
      return BuildInterface(className, new SleepClosure(script, block), script);
   } 

   /** This function invokes the contained Sleep closure with the specified arguments */
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      Stack temp = new Stack();

      boolean isTrace = (script.getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;
      StringBuffer message = null;

      if (args != null)
      {
         for (int z = args.length - 1; z >= 0; z--)
         { 
            temp.push(ObjectUtilities.BuildScalar(true, args[z]));
         }
      }

      Scalar value;

      script.getScriptEnvironment().installExceptionHandler(null, null, null);

      if (isTrace)
      {
         if (!script.isProfileOnly())
         {
            message = new StringBuffer("[" + func + " " + method.getName());

            if (!temp.isEmpty())
               message.append(": " + SleepUtils.describe(temp));

            message.append("]");
         }

         long stat = System.currentTimeMillis();
         value = func.evaluate(method.getName(), script, temp); 
         stat = System.currentTimeMillis() - stat;

         if (func.getClass() == SleepClosure.class)
         {
            script.collect(((SleepClosure)func).toStringGeneric(), -1, stat);
         }

         if (message != null)
         {
            if (script.getScriptEnvironment().isThrownValue()) 
               message.append(" - FAILED!"); 
            else
               message.append(" = " + SleepUtils.describe(value)); 

            script.fireWarning(message.toString(), -1, true);
         }
      }
      else
      {
         value = func.evaluate(method.getName(), script, temp); 
      }
      script.getScriptEnvironment().popExceptionContext();
      script.getScriptEnvironment().clearReturn();
 
      if (script.getScriptEnvironment().isThrownValue())
      {
         script.recordStackFrame(func + " as " + method.toString(), "<internal>", 0);

         Object exvalue = (script.getScriptEnvironment().getExceptionMessage()).objectValue();
           
         if (exvalue instanceof Throwable)
         {
            throw (Throwable)exvalue;
         }
         else
         {
            throw new RuntimeException(exvalue.toString());
         }
      }        

      if (value != null)
         return ObjectUtilities.buildArgument(method.getReturnType(), value, script);

      return null;
   }
}
	
