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

import java.io.File;
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

        // array & hashtable related
        temp.put("&array",   new array());    // &keys(%hash) = @array
        temp.put("&hash",   new hash());      // &keys(%hash) = @array
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
        temp.put("&cast",   new f_cast());

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

        SetScope scopeFunctions = new SetScope();

        temp.put("&local",    scopeFunctions);
        temp.put("&this",     scopeFunctions);
        temp.put("&global",     scopeFunctions);

        temp.put("&reverse",  new reverse());      // @array2 = &reverse(@array) 
        temp.put("&removeAt", new removeAt());   // not safe within foreach loops yada yada yada...
        temp.put("&shift",    new shift());   // not safe within foreach loops yada yada yada...

        temp.put("&systemProperties",    new systemProperties());
        temp.put("&use",    new f_use());
        temp.put("&checkError",    new checkError());

        // closure / function handle type stuff
        temp.put("&lambda",    new lambda());
        temp.put("&function",  new function());
        temp.put("&compile_closure",    new compile_closure());
        temp.put("&eval",     new eval());
        temp.put("&expr",     new expr());


        temp.put("=>",       new HashKeyValueOp());

        return true;
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

       if (predName.equals("-isarray"))
          return value.getArray() != null;

       if (predName.equals("-ishash"))
          return value.getHash() != null;

       return false;
    }

    private static class checkError implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          Scalar value = BridgeUtilities.getScalar(l);

          String temp  = si.getScriptEnvironment().checkError();

          if (temp == null)
              return SleepUtils.getEmptyScalar();

          value.setValue(SleepUtils.getScalar(temp));           
          return value;
       }
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
             parent    = BridgeUtilities.getFile(l);
             className = BridgeUtilities.getString(l, "");
          }
          else
          {
             File a    = BridgeUtilities.getFile(l);
             parent    = a.getParentFile();
             className = a.getName();
          }

          Class bridge;

          try
          {
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
          catch (Exception ex)
          {
             si.getScriptEnvironment().flagError(ex.toString());
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
             value.getArray().push(BridgeUtilities.getScalar(l));
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

          if (value.getArray() == null)
          {
             if (type.charAt(0) == 'c')
             {
                return SleepUtils.getScalar(value.toString().toCharArray());
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

                return SleepUtils.getScalar(tempb);
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

          Class atype = null;

          switch (type.charAt(0))
          {
             case 'z':
                atype = Boolean.TYPE;
                break;
             case 'c':
                atype = Character.TYPE;
                break;
             case 'b':
                atype = Byte.TYPE;
                break;
             case 'h':
                atype = Short.TYPE;
                break;
             case 'i':
                atype = Integer.TYPE;
                break;
             case 'l':
                atype = Long.TYPE;
                break;
             case 'f':
                atype = Float.TYPE;
                break;
             case 'd':
                atype = Double.TYPE;
                break;
             default:
                atype = ObjectUtilities.getArrayType(value, ObjectUtilities.OBJECT_TYPE);
          }

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
          String temp = BridgeUtilities.getString(l, "");
          return SleepUtils.getScalar(si.getScriptEnvironment().getFunction(temp));
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
          value = new SleepClosure(si, temp.getRunnableCode());
           
          Variable vars = value.getVariables();

          while (!l.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
             vars.putScalar(kvp.getKey().toString(), kvp.getValue());
          }

          return SleepUtils.getScalar(value);
       }
    }

    private static class map implements Function
    {
       public Scalar evaluate(String n, ScriptInstance si, Stack l)
       {
          SleepClosure temp  = BridgeUtilities.getFunction(l, si);           
          ScalarArray  value = BridgeUtilities.getArray(l); 

          Scalar       rv    = SleepUtils.getArrayScalar();
          Stack        locals = new Stack();

          Iterator i = value.scalarIterator();
          while (i.hasNext())
          {
             locals.push(i.next());

             Scalar val = temp.callClosure("eval", si, locals);

             if (!SleepUtils.isEmptyScalar(val) || n.equals("&map"))
             {
                rv.getArray().push(val);
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
          ScalarArray temp  = BridgeUtilities.getArray(l);           

          Iterator i = temp.scalarIterator();
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
          ScalarArray temp  = BridgeUtilities.getArray(l);
          Scalar value = SleepUtils.getArrayScalar();

          Iterator i = temp.scalarIterator();
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
             i.getScriptEnvironment().flagError(ex.getMessage());
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
             i.getScriptEnvironment().flagError(ex.getMessage());
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
             i.getScriptEnvironment().flagError(ex.getMessage());
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
                a.push(temp);
             }
          }

          return SleepUtils.getArrayScalar(a);
       }
       else if (n.equals("&add") && value.getArray() != null)
       {
          Scalar item = BridgeUtilities.getScalar(l);
          int index = BridgeUtilities.getInt(l, value.getArray().size());  
          return value.getArray().add(item, index);
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
          ScalarArray array = BridgeUtilities.getArray(l);
          Stack locals      = new Stack();

          Iterator iter = array.scalarIterator();

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
                rv.getArray().push(value.getArray().getAt(begin));
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
       else if (n.equals("&values")) // &values(%hash)
       {
          if (value.getHash() != null)
          {
             Scalar temp = SleepUtils.getArrayScalar();

             Iterator ih = value.getHash().keys().scalarIterator();

             while (ih.hasNext())
             {
                  temp.getArray().push(value.getHash().getAt((Scalar)ih.next()));
             }

             return temp;
          }
       }

       return SleepUtils.getEmptyScalar();
   }
}
