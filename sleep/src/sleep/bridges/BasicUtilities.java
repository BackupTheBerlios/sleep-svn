/*
   SLEEP - Simple Language for Environment Extension Purposes
 .------------------------------.
 | sleep.bridges.BasicUtilities |_____________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
      provides some of the basic built in utility functions.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */


package sleep.bridges;
 
import java.util.*;

import sleep.engine.*;
import sleep.interfaces.*;
import sleep.runtime.*;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

import java.lang.reflect.*; // for array casting stuff

import sleep.parser.Parser;
import sleep.error.YourCodeSucksException;

/** implementation of basic utility functions */
public class BasicUtilities implements Function, Loadable, Predicate
{
    public boolean scriptUnloaded (ScriptInstance i)
    {
        return true;
    }

    public boolean scriptLoaded (ScriptInstance i)
    {
        Hashtable temp = i.getScriptEnvironment().getEnvironment();
        //
        // functions
        //

        Function f_array = new array();
        Function f_hash  = new hash();

        temp.put("&array", f_array); 
        temp.put("&hash", f_hash);
        temp.put("&@", f_array);
        temp.put("&%", f_hash);  

        // array & hashtable related
        temp.put("&keys",  this);      // &keys(%hash) = @array
        temp.put("&size",  this);      // &size(@array) = <int>
        temp.put("&push",  this);      // &push(@array, $value) = $scalar
        temp.put("&pop",   this);      // &pop(@array) = $scalar
        temp.put("&add",   this);      // &pop(@array) = $scalar
        temp.put("&flatten",   this);      // &pop(@array) = $scalar
        temp.put("&clear", this);
        temp.put("&subarray", this);
        temp.put("&copy",  new copy());
 
        map map_f = new map();

        temp.put("&map",    map_f);
        temp.put("&filter",    map_f);

        Function f_cast = new f_cast();
        temp.put("&cast",    f_cast);
        temp.put("&casti",   f_cast);

        temp.put("&putAll", this);

        temp.put("&addAll", this);
        temp.put("&removeAll", this);
        temp.put("&retainAll", this);

        temp.put("&search", this);
        temp.put("&reduce", this);
        temp.put("&values", this);
        temp.put("&remove", this);     // not safe within foreach loops (since they use an iterator, and remove throws an exception)
        temp.put("-istrue", this);    // predicate -istrue <Scalar>, determine wether or not the scalar is null or not.
        temp.put("-isarray", this);   
        temp.put("-ishash",  this); 
        temp.put("-isfunction", this);
        temp.put("&setField", this);

        temp.put("&exit", this);
     
        SetScope scopeFunctions = new SetScope();

        temp.put("&local",    scopeFunctions);
        temp.put("&this",     scopeFunctions);
        temp.put("&global",     scopeFunctions);

        temp.put("&debug", this);
        temp.put("&profile", this);
        temp.put("&getStackTrace", this);

        temp.put("&reverse",  new reverse());      // @array2 = &reverse(@array) 
        temp.put("&removeAt", new removeAt());   // not safe within foreach loops yada yada yada...
        temp.put("&shift",    new shift());   // not safe within foreach loops yada yada yada...

        temp.put("&systemProperties",    new systemProperties());
        temp.put("&use",     new f_use());
        temp.put("&include", temp.get("&use"));
        temp.put("&checkError", this);

        // closure / function handle type stuff
        temp.put("&lambda",    new lambda());
        temp.put("&let",    temp.get("&lambda"));

        function funcs = new function();
        temp.put("&function",  funcs);
        temp.put("&setf",      funcs);
        temp.put("&compile_closure",    new compile_closure());
        temp.put("&eval",     new eval());
        temp.put("&expr",     new expr());

        // synchronization primitives...
        SyncPrimitives sync = new SyncPrimitives();
        temp.put("&semaphore", sync);
        temp.put("&acquire",   sync);
        temp.put("&release",   sync);

        temp.put("&invoke",    this);

        temp.put("=>",       new HashKeyValueOp());

        return true;
    }

    private static class SyncPrimitives implements Function 
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          if (n.equals("&semaphore"))
          {
             int initial = BridgeUtilities.getInt(l, 1);
             return SleepUtils.getScalar(new Semaphore(initial));
          }
          else if (n.equals("&acquire"))
          {
             Semaphore sem = (Semaphore)BridgeUtilities.getObject(l);
             sem.P();
          }
          else if (n.equals("&release"))
          {
             Semaphore sem = (Semaphore)BridgeUtilities.getObject(l);
             sem.V();
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class HashKeyValueOp implements Operator
    {
        public Scalar operate(String name, ScriptInstance script, Stack locals)
        {
            Scalar identifier = (Scalar)locals.pop();
            Scalar value      = (Scalar)locals.pop();

            return SleepUtils.getScalar(new KeyValuePair(identifier, value));
        }
    }

    public boolean decide(String predName, ScriptInstance anInstance, Stack terms)
    {
       Scalar value = (Scalar)terms.pop();
 
       // Times when a scalar is considered true:
       // - its value is not equal to 0
       // - its not null (string value is not "")
       //
       // Scalar - String intValue
       //   0       "0"      0         - false
       //  null     ""       0         - false
       //  "blah"   "blah"   0         - true
       //  "3"      "3"      3         - true
       //   
       if (predName.equals("-istrue"))
          return value.getValue().toString().length() != 0 && !("0".equals(value.getValue().toString()));

       if (predName.equals("-isfunction"))
          return SleepUtils.isFunctionScalar(value);

       if (predName.equals("-isarray"))
          return value.getArray() != null;

       if (predName.equals("-ishash"))
          return value.getHash() != null;

       return false;
    }

    private static class f_use implements Function
    {
       private HashMap bridges = new HashMap();

       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          File   parent;
          String className;

          if (l.size() == 2)
          {
             parent    = sleep.parser.ParserConfig.findJarFile(l.pop().toString());
             className = BridgeUtilities.getString(l, "");
          }
          else
          {
             File a    = sleep.parser.ParserConfig.findJarFile(l.pop().toString());

             parent    = a.getParentFile();
             className = a.getName();
          }

          if (parent != null && !parent.exists())
          {
             throw new IllegalArgumentException(n + ": could not locate source '" + parent + "'");
          }

          try
          {
             if (n.equals("&use"))
             {
                Class bridge;

                if (parent != null)
                {
                   URLClassLoader loader = new URLClassLoader(new URL[] { parent.toURL() });
                   bridge = Class.forName(className, true, loader);
                }
                else
                {
                   bridge = Class.forName(className);
                }

                Loadable temp;

                if (bridges.get(bridge) == null)
                {
                   temp = (Loadable)bridge.newInstance();
                   bridges.put(bridge, temp);
                }
                else
                {
                   temp = (Loadable)bridges.get(bridge);
                }

                temp.scriptLoaded(si);
             }
             else
             {
                Block          script;
                ScriptLoader   sloader = (ScriptLoader)si.getScriptEnvironment().getEnvironment().get("(isloaded)");
                InputStream    istream;
         
                if (parent != null)
                {
                   URLClassLoader loader = new URLClassLoader(new URL[] { parent.toURL() });
                   istream = loader.getResourceAsStream(className);
                }
                else
                {
                   istream = new FileInputStream(new File(className));
                }

                if (istream != null)
                {
                   script = sloader.compileScript(className, istream);
                   SleepUtils.runCode(script, si.getScriptEnvironment());
                }
                else
                {
                   throw new IOException("unable to locate " + className + " from: " + parent);
                }
             }
          }
          catch (YourCodeSucksException yex)
          {
             throw new RuntimeException(className + ": " + yex.getMessage());
          }
          catch (Exception ex)
          {
             throw new RuntimeException(ex.toString());
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class array implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = SleepUtils.getArrayScalar();
           
          while (!l.isEmpty())
          {
             value.getArray().push(SleepUtils.getScalar(BridgeUtilities.getScalar(l)));
          }

          return value;
       }
    }

    private static class f_cast implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value      = BridgeUtilities.getScalar(l);
          String type       = BridgeUtilities.getString(l, " ");

          if (type.length() == 0) { type = " "; }

          if (n.equals("&casti"))
          {
             Class  atype = ObjectUtilities.convertDescriptionToClass(type);

             if (atype != null)
             {
                Object tempo = ObjectUtilities.buildArgument(atype, value, si);
                return SleepUtils.getScalar(tempo);
             }
             else
             {
                throw new RuntimeException("&casti: '" + type + "' is an invalid primitive cast identifier");
             }
          }

          if (value.getArray() == null)
          {
             if (type.charAt(0) == 'c')
             {
                return SleepUtils.getScalar((Object)value.toString().toCharArray());
             }             
             else if (type.charAt(0) == 'b')
             {
                // we do a straight conversion here because we don't want byte data to be mucked up by charsets
                // this is because string stores an array of bytes as a string...
                char[] tempc = value.toString().toCharArray();
                byte[] tempb = new byte[tempc.length];

                for (int x = 0; x < tempc.length; x++)
                {
                   tempb[x] = (byte)tempc[x];
                }

                return SleepUtils.getScalar((Object)tempb);
             }             

             return SleepUtils.getEmptyScalar();
          }

          if (l.size() == 0) { l.push(SleepUtils.getScalar(value.getArray().size())); }

          int dimensions[] = new int[l.size()];
          int totaldim     = 1;

          for (int x = 0; !l.isEmpty(); x++)
          {
             dimensions[x] = BridgeUtilities.getInt(l, 0);

             totaldim *= dimensions[x];
          }

          Object rv;

          Class atype = ObjectUtilities.convertDescriptionToClass(type);

          if (atype == null)
              atype = ObjectUtilities.getArrayType(value, Object.class);

          Scalar flat = BridgeUtilities.flattenArray(value, null);

          if (totaldim != flat.getArray().size())
          {
             throw new RuntimeException("&cast: specified dimensions " + totaldim + " is not equal to total array elements " + flat.getArray().size());
          }

          rv = Array.newInstance(atype, dimensions);

          int current[] = new int[dimensions.length]; // defaults at 0, 0, 0

          for (int x = 0; true; x++)
          {
             Object tempa = rv;

             //
             // find our index
             //
             for (int z = 0; z < (current.length - 1); z++)
             {
                tempa = Array.get(tempa, current[z]);
             }

             //
             // set our value
             //
             Object tempo = ObjectUtilities.buildArgument(atype, flat.getArray().getAt(x), si);
             Array.set(tempa, current[current.length - 1], tempo);

             //
             // increment our index step...
             //
             current[current.length - 1] += 1;

             for (int y = current.length - 1; current[y] >= dimensions[y]; y--)
             {
                if (y == 0)
                {
                   return SleepUtils.getScalar(rv); // we're done building the array at this point...
                }

                current[y] = 0;
                current[y-1] += 1;
             }
          }

       }
    }

    private static class function implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          if (n.equals("&function"))
          {
             String temp = BridgeUtilities.getString(l, "");
             return SleepUtils.getScalar(si.getScriptEnvironment().getFunction(temp));
          }
          else if (n.equals("&setf"))
          {
             String   temp = BridgeUtilities.getString(l, "&eh");
             Object   o    = BridgeUtilities.getObject(l);

             if (temp.charAt(0) == '&' && (o == null || o instanceof Function))
             {
                if (o == null)
                {
                   si.getScriptEnvironment().getEnvironment().remove(temp);
                }
                else
                {
                   si.getScriptEnvironment().getEnvironment().put(temp, o);
                }
             }
             else if (temp.charAt(0) != '&')
             {
                throw new IllegalArgumentException("&setf: invalid function name '" + temp + "'");
             }
             else if (o != null)
             {
                throw new IllegalArgumentException("&setf: can not set function " + temp + " to a " + o.getClass());
             }
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class hash implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = SleepUtils.getHashScalar();
           
          while (!l.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
 
             Scalar blah = value.getHash().getAt(kvp.getKey());
             blah.setValue(kvp.getValue());
          }

          return value;
       }
    }

    private static class lambda implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          SleepClosure value;
 
          SleepClosure temp = BridgeUtilities.getFunction(l, si);           

          if (n.equals("&lambda"))
          {
             value = new SleepClosure(si, temp.getRunnableCode());
          }
          else
          {
             value = temp;
          }
           
          Variable vars = value.getVariables();

          while (!l.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);

             if (kvp.getKey().toString().equals("$this"))
             {
                SleepClosure c = (SleepClosure)kvp.getValue().objectValue();
                value.setVariables(c.getVariables());
             }
             else
             {
                vars.putScalar(kvp.getKey().toString(), SleepUtils.getScalar(kvp.getValue()));
             }
          }

          return SleepUtils.getScalar(value);
       }
    }

    private static class map implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          SleepClosure temp  = BridgeUtilities.getFunction(l, si);           
          Iterator     i     = BridgeUtilities.getIterator(l, si);

          Scalar       rv     = SleepUtils.getArrayScalar();
          Stack        locals = new Stack();

          while (i.hasNext())
          {
             locals.push(i.next());

             Scalar val = temp.callClosure("eval", si, locals);

             if (!SleepUtils.isEmptyScalar(val) || n.equals("&map"))
             {
                rv.getArray().push(SleepUtils.getScalar(val));
             }

             locals.clear();
          }

          return rv;
       }
    }

    private static class copy implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar      value = SleepUtils.getArrayScalar();
          Iterator    i     = BridgeUtilities.getIterator(l, si);

          while (i.hasNext())
          {
             value.getArray().push(SleepUtils.getScalar((Scalar)i.next()));
          }

          return value;
       }
    }

    private static class removeAt implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          ScalarArray value = BridgeUtilities.getArray(l);
          return value.remove(BridgeUtilities.getInt(l, 0));
       }
    }

    private static class shift implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          ScalarArray value = BridgeUtilities.getArray(l);
          return value.remove(0);
       }
    }

    private static class reverse implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = SleepUtils.getArrayScalar();
          Iterator  i  = BridgeUtilities.getIterator(l, si);

          while (i.hasNext())
          {
             value.getArray().add(SleepUtils.getScalar((Scalar)i.next()), 0);
          }

          return value;
       }          
    }

    private static class SetScope implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          Variable level = null;

          if (n.equals("&local")) { level = i.getScriptVariables().getLocalVariables(); }
          else if (n.equals("&this")) { level = i.getScriptVariables().getClosureVariables(); }
          else if (n.equals("&global")) { level = i.getScriptVariables().getGlobalVariables(); }

          String temp = l.pop().toString();

          if (level == null)
              return SleepUtils.getEmptyScalar(); 

          String vars[] = temp.split(" "); 
          for (int x = 0; x < vars.length; x++)
          {
             if (level.scalarExists(vars[x]))
             {
                // do nothing...
             }
             else if (vars[x].charAt(0) == '$')
             {
                i.getScriptVariables().setScalarLevel(vars[x], SleepUtils.getEmptyScalar(), level);
             }
             else if (vars[x].charAt(0) == '@')
             {
                i.getScriptVariables().setScalarLevel(vars[x], SleepUtils.getArrayScalar(), level);
             }
             else if (vars[x].charAt(0) == '%')
             {
                i.getScriptVariables().setScalarLevel(vars[x], SleepUtils.getHashScalar(), level);
             }
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class systemProperties implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          return SleepUtils.getHashWrapper(System.getProperties());
       }
    }

    private static class eval implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String code  = l.pop().toString();

          try 
          {
             Scalar temp = SleepUtils.getScalar(i.getScriptEnvironment().evaluateStatement(code));
             return temp;
          }
          catch (YourCodeSucksException ex)
          {
             i.getScriptEnvironment().flagError(ex);
             return SleepUtils.getEmptyScalar();
          }
       }
    }

    private static class compile_closure implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String code  = l.pop().toString();

          try 
          {
             SleepClosure value = new SleepClosure(i, SleepUtils.ParseCode(code));
             Scalar       temp  = SleepUtils.getScalar(value);

             Variable      vars = value.getVariables();

             while (!l.isEmpty())
             {
                KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
                vars.putScalar(kvp.getKey().toString(), kvp.getValue());
             }

             return temp;
          }
          catch (YourCodeSucksException ex)
          {
             i.getScriptEnvironment().flagError(ex);
             return SleepUtils.getEmptyScalar();
          }
       }
    }

    private static class expr implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String code  = l.pop().toString();

          try 
          {
             Scalar temp = SleepUtils.getScalar(i.getScriptEnvironment().evaluateExpression(code));
             return temp;
          }
          catch (YourCodeSucksException ex)
          {
             i.getScriptEnvironment().flagError(ex);
             return SleepUtils.getEmptyScalar();
          }
       }
    }

    public Scalar evaluate(String n, ScriptInstance i, Stack l)
    {
       if (l.isEmpty() && n.equals("&remove"))
       {
          Stack iterators = (Stack)(i.getScriptEnvironment().getContextMetadata("iterators"));

          if (iterators == null || iterators.isEmpty())
          {
             throw new RuntimeException("&remove: no active foreach loop to remove element from");
          }
          else
          {
             sleep.engine.atoms.Iterate.IteratorData d = (sleep.engine.atoms.Iterate.IteratorData)iterators.peek();
             d.iterator.remove();
             d.count = d.count - 1;
          }
         
          return SleepUtils.getEmptyScalar();
       }
       else if (n.equals("&invoke")) 
       {
          Map params = BridgeUtilities.extractNamedParameters(l);

          SleepClosure c    = BridgeUtilities.getFunction(l, i);
          Stack        args = new Stack();
          Iterator iter     = BridgeUtilities.getIterator(l, i);
          while (iter.hasNext()) { args.add(0, iter.next()); }

          String message    = BridgeUtilities.getString(l, null);

          /* parameters option */
          if (params.containsKey("parameters"))
          {
             Scalar   h = (Scalar)params.get("parameters");

             Iterator it = h.getHash().keys().scalarIterator();
             while (it.hasNext())
             {
                Scalar key = (Scalar)it.next();
                KeyValuePair temp = new KeyValuePair(key, h.getHash().getAt(key));
                args.add(0, SleepUtils.getScalar(temp));
             }
          }

          /* message option */
          if (params.containsKey("message"))
          {
             message = params.get("message").toString();
          }
 
          Variable old = c.getVariables();

          /* environment option */
          if (params.containsKey("$this"))
          {
             SleepClosure t = (SleepClosure)((Scalar)params.get("$this")).objectValue();
             c.setVariables(t.getVariables());
          }

          Scalar rv = c.callClosure(message, i, args);
          c.setVariables(old);
          return rv;
       }
       else if (n.equals("&checkError"))
       {
          Scalar value = BridgeUtilities.getScalar(l);
          value.setValue(i.getScriptEnvironment().checkError());           
          return value;
       }
       else if (n.equals("&profile"))
       {
          return SleepUtils.getArrayWrapper(i.getProfilerStatistics());
       }
       else if (n.equals("&getStackTrace"))
       {
          return SleepUtils.getArrayWrapper(i.getStackTrace());
       }
       else if (n.equals("&debug"))
       {
          /* allow the script to programatically set the debug level */
          if (!l.isEmpty())
          {
             int flag = BridgeUtilities.getInt(l, 0);
             i.setDebugFlags(flag);
          }

          return SleepUtils.getScalar(i.getDebugFlags());
       }

       Scalar value = BridgeUtilities.getScalar(l);

       if (n.equals("&push"))
       {
          return value.getArray().push(SleepUtils.getScalar((Scalar)l.pop()));
       }
       else if ((n.equals("&retainAll") || n.equals("&removeAll")) && value.getArray() != null)
       {
          ScalarArray a = value.getArray();
          ScalarArray b = BridgeUtilities.getArray(l);
    
          HashSet s = new HashSet();
          Iterator iter = b.scalarIterator();
          while (iter.hasNext())
          {
             s.add(iter.next().toString());
          }      

          iter = a.scalarIterator();
          while (iter.hasNext())
          {
             Object temp = iter.next();

             if (!s.contains(temp.toString()) && n.equals("&retainAll"))
             {
                iter.remove();
             }
             else if (s.contains(temp.toString()) && n.equals("&removeAll"))
             {
                iter.remove();
             }
          }

          return SleepUtils.getArrayScalar(a);
       }
       else if (n.equals("&addAll") && value.getArray() != null)
       {
          ScalarArray a = value.getArray();
          ScalarArray b = BridgeUtilities.getArray(l);
    
          HashSet s = new HashSet();
          Iterator iter = a.scalarIterator();
          while (iter.hasNext())
          {
             s.add(iter.next().toString());
          }      

          iter = b.scalarIterator();
          while (iter.hasNext())
          {
             Scalar temp = (Scalar)iter.next();

             if (!s.contains(temp.toString()))
             {
                a.push(SleepUtils.getScalar(temp));
             }
          }

          return SleepUtils.getArrayScalar(a);
       }
       else if (n.equals("&add") && value.getArray() != null)
       {
          Scalar item = BridgeUtilities.getScalar(l);
          int index = BridgeUtilities.getInt(l, 0);  
          return value.getArray().add(SleepUtils.getScalar(item), index);
       }
       else if (n.equals("&pop"))
       {
          return value.getArray().pop();
       }
       else if (n.equals("&size") && value.getArray() != null) // &size(@array)
       {
          return SleepUtils.getScalar(value.getArray().size());
       }
       else if (n.equals("&clear"))
       {
          if (value.getArray() != null)
          {
             value.setValue(SleepUtils.getArrayScalar());
          }
          else if (value.getHash() != null)
          {
             value.setValue(SleepUtils.getHashScalar());
          }
          else
          {
             value.setValue(SleepUtils.getEmptyScalar());
          }
       }
       else if (n.equals("&search") && value.getArray() != null)
       {
          SleepClosure f = BridgeUtilities.getFunction(l, i); 
          int start      = BridgeUtilities.getInt(l, 0);
          int count      = 0;
          Stack locals   = new Stack();

          Iterator iter = value.getArray().scalarIterator();
          while (iter.hasNext())
          {
             Scalar temp = (Scalar)iter.next();

             if (start > 0)
             {
                start--;
                count++;
                continue;
             }            

             locals.push(SleepUtils.getScalar(count));
             locals.push(temp);
             Scalar val = f.callClosure("eval", i, locals);

             if (! SleepUtils.isEmptyScalar(val))
             {
                return val;
             }

             locals.clear();
             count++;
          }
       }
       else if (n.equals("&reduce") && SleepUtils.isFunctionScalar(value))
       {
          SleepClosure f    = SleepUtils.getFunctionFromScalar(value, i); 
          Stack locals      = new Stack();

          Iterator iter = BridgeUtilities.getIterator(l, i);

          Scalar a      = iter.hasNext() ? (Scalar)iter.next() : SleepUtils.getEmptyScalar();
          Scalar b      = iter.hasNext() ? (Scalar)iter.next() : SleepUtils.getEmptyScalar();
          Scalar temp   = null;

          locals.push(a);
          locals.push(b);

          a = f.callClosure("eval", i, locals);
 
          locals.clear();

          while (iter.hasNext())
          {
             b = (Scalar)iter.next();

             locals.push(b);
             locals.push(a);
             a = f.callClosure("eval", i, locals);

             locals.clear();
          }

          return a;
       }
       else if (n.equals("&flatten") && value.getArray() != null)
       {
          return BridgeUtilities.flattenArray(value, null);
       }
       else if (n.equals("&subarray"))
       {
          if (value.getArray() != null)
          {
             int begin = BridgeUtilities.getInt(l, 0);
             int end   = BridgeUtilities.getInt(l, value.getArray().size());

             Scalar rv = SleepUtils.getArrayScalar();
             while (begin < end)
             {
                rv.getArray().push(SleepUtils.getScalar(value.getArray().getAt(begin)));
                begin++;
             }

             return rv;
          }
       }
       else if (n.equals("&remove"))
       {
          while (!l.isEmpty())
          {
             Scalar scalar = (Scalar)l.pop();

             if (value.getArray() != null)
             {
                value.getArray().remove(scalar);
             }
             else if (value.getHash() != null)
             {
                value.getHash().remove(scalar);
             }
          }
       }
       else if (n.equals("&keys")) // &keys(%hash)
       {
          if (value.getHash() != null)
          {
             Scalar temp = SleepUtils.getEmptyScalar();
             temp.setValue(value.getHash().keys());
             return temp;
          }
       }
       else if (n.equals("&putAll"))
       {
          if (value.getHash() != null)
          {
             Iterator keys   = BridgeUtilities.getIterator(l, i);
             Iterator values = l.isEmpty() ? keys : BridgeUtilities.getIterator(l, i);

             while (keys.hasNext())
             {
                Scalar blah = value.getHash().getAt((Scalar)keys.next());
                if (values.hasNext())
                {
                   blah.setValue((Scalar)values.next());
                }
                else
                {
                   blah.setValue(SleepUtils.getEmptyScalar());
                }
             }
          }

          return value;
       }
       else if (n.equals("&values")) // &values(%hash)
       {
          if (value.getHash() != null)
          {
             Scalar temp = SleepUtils.getArrayScalar();

             Iterator ih = value.getHash().keys().scalarIterator();

             while (ih.hasNext())
             {
                  temp.getArray().push(SleepUtils.getScalar(value.getHash().getAt((Scalar)ih.next())));
             }

             return temp;
          }
       }
       else if (n.equals("&exit"))
       {
          i.getScriptEnvironment().flagReturn(null, ScriptEnvironment.FLOW_CONTROL_THROW); /* a null throw will exit the interpreter */
       }
       else if (n.equals("&setField"))
       {
          // setField(class/object, "field", "value")

          Field  setMe  = null;
          Class  aClass = null;
          Object inst   = null;

          if ("==CLASS==".equals(value.toString()))
          {
             aClass = (Class)(BridgeUtilities.getScalar(l).objectValue());
          }
          else if (value.objectValue() == null)
          {
             throw new IllegalArgumentException("&setField: can not set field on a null object");
          }
          else
          {
             inst   = value.objectValue();
             aClass = inst.getClass();
          }

          while (!l.isEmpty())
          {
             KeyValuePair pair = BridgeUtilities.getKeyValuePair(l);

             String name = pair.getKey().toString();
             Scalar arg  = pair.getValue();

             try
             {
                setMe = aClass.getDeclaredField(name);

                if (ObjectUtilities.isArgMatch(setMe.getType(), arg) != 0)
                {
                   setMe.setAccessible(true);
                   setMe.set(inst, ObjectUtilities.buildArgument(setMe.getType(), arg, i));
                }
                else
                {
                   throw new RuntimeException("unable to convert " + SleepUtils.describe(arg) + " to a " + setMe.getType());
                }
             }
             catch (NoSuchFieldException fex)
             {
                throw new RuntimeException("no field named " + name + " in " + aClass);
             }
             catch (RuntimeException rex) { throw (rex); }
             catch (Exception ex)
             {
                throw new RuntimeException("cannot set " + name + " in " + aClass + ": " + ex.getMessage());
             }
          }
       }

       return SleepUtils.getEmptyScalar();
    }
}
