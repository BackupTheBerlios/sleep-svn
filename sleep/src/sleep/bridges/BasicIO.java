/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-----------------------.
 | sleep.bridges.BasicIO |____________________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
      provides some of the basic IO facilities.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;
 
import java.util.*;
import java.util.regex.*;

import sleep.engine.*;
import sleep.engine.types.*;

import sleep.interfaces.*;
import sleep.runtime.*;

import java.io.*;
import java.nio.*;
import sleep.bridges.io.*;

/** provides IO functions for the sleep language */
public class BasicIO implements Loadable
{
    public boolean scriptUnloaded(ScriptInstance aScript)
    {
        return true;
    }

    public boolean scriptLoaded (ScriptInstance aScript)
    {
        Hashtable temp = aScript.getScriptEnvironment().getEnvironment();

        // predicates
        temp.put("-eof",     new iseof());

        // functions
        temp.put("&openf",      new openf());

        SocketFuncs f = new SocketFuncs();

        temp.put("&connect",    f);
        temp.put("&listen",     f);
        temp.put("&exec",       new exec());
        temp.put("&fork",       new fork());
        temp.put("&sleep",      new sleep());

        temp.put("&closef",     new closef());

        // ascii'sh read functions
        temp.put("&read",       new read());
        temp.put("&readln",     new readln());
        temp.put("&readAll",    new readAll());

        // binary i/o functions :)
        temp.put("&readb",      new readb());
        temp.put("&writeb",     new writeb());

        temp.put("&bread",      new bread());
        temp.put("&bwrite",     new bwrite());

        temp.put("&pack",       new pack());
        temp.put("&unpack",     new unpack());

        temp.put("&available",  new available());
        temp.put("&mark",       new mark());
        temp.put("&skip",       new skip());
        temp.put("&reset",      new reset());
        temp.put("&wait",       new wait());

        // typical ASCII'sh output functions
        temp.put("&print",      new print());

        println f_println = new println();
        temp.put("&println",    f_println);
        temp.put("&printf",    f_println); // I need to fix my unit tests to get rid of the printf function... grr
        temp.put("&printAll",   new printArray());
        temp.put("&printEOF",   new printEOF());

        temp.put("&getConsole", new getConsoleObject());

        return true;
    }

    private static class openf implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String a = ((Scalar)l.pop()).toString();

          FileObject temp = new FileObject();
          temp.open(a, i.getScriptEnvironment());

          return SleepUtils.getScalar(temp);
       }
    }

    private static class exec implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String a = ((Scalar)l.pop()).toString();

          ProcessObject temp = new ProcessObject();
          temp.open(a, i.getScriptEnvironment());

          return SleepUtils.getScalar(temp);
       }
    }

    private static class sleep implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          try
          {
             Thread.currentThread().sleep(BridgeUtilities.getLong(l, 0));
          }
          catch (Exception ex) { }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class fork implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          SleepClosure   param = BridgeUtilities.getFunction(l, i);        

          // create our fork...
          ScriptInstance child = i.fork();
          child.installBlock(param.getRunnableCode());

          ScriptVariables vars = child.getScriptVariables();

          while (!l.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
             vars.putScalar(kvp.getKey().toString(), kvp.getValue());
          }

          // create a pipe between these two items...
          IOObject parent_io = new IOObject();
          IOObject child_io  = new IOObject();

          try
          {
             PipedInputStream  parent_in  = new PipedInputStream();
             PipedOutputStream parent_out = new PipedOutputStream();
             parent_in.connect(parent_out);

             PipedInputStream  child_in   = new PipedInputStream();
             PipedOutputStream child_out  = new PipedOutputStream();
             child_in.connect(child_out);

             parent_io.openRead(child_in);
             parent_io.openWrite(parent_out);

             child_io.openRead(parent_in);
             child_io.openWrite(child_out);
          
             child.getScriptVariables().putScalar("$source", SleepUtils.getScalar(child_io));

             Thread temp = new Thread(child, child.getName());

             parent_io.setThread(temp);
             child_io.setThread(temp);

             child.setParent(parent_io);

             temp.start();
          }
          catch (Exception ex)
          {
             i.getScriptEnvironment().flagError(ex.toString());
          }

          return SleepUtils.getScalar(parent_io);
       }
    }

    private static class SocketFuncs implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          SocketHandler handler = new SocketHandler();
          handler.socket        = new SocketObject();
          handler.script        = i;

          if (n.equals("&listen"))
          {
             handler.port     = BridgeUtilities.getInt(l, -1);          // port
             handler.timeout  = BridgeUtilities.getInt(l, 60 * 1000);   // timeout
             handler.callback = BridgeUtilities.getScalar(l);           // scalar to put info in to

             handler.type     = LISTEN_FUNCTION;
          }
          else
          {
             handler.host     = BridgeUtilities.getString(l, "127.0.0.1");
             handler.port     = BridgeUtilities.getInt(l, 1);
             handler.timeout  = BridgeUtilities.getInt(l, 60 * 1000);   // timeout

             handler.type     = CONNECT_FUNCTION;
          }
          
          if (!l.isEmpty())
             handler.function = BridgeUtilities.getFunction(l, i);

          handler.start();

          return SleepUtils.getScalar(handler.socket);
       }
    }

    private static class closef implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a = (IOObject)BridgeUtilities.getObject(l);
          a.close();

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class readln implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a = chooseSource(l, 1);
    
          String temp = a.readLine();

          if (temp == null)
          {
             return SleepUtils.getEmptyScalar();
          }

          return SleepUtils.getScalar(temp);
       }
    }

    private static class readAll implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a = chooseSource(l, 1);

          Scalar ar = SleepUtils.getArrayScalar();
          
          String temp;
          while ((temp = a.readLine()) != null)
          {
             ar.getArray().push(SleepUtils.getScalar(temp));
          }

          return ar;
       }
    }

    private static class println implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a = chooseSource(l, 2);

          String temp = BridgeUtilities.getString(l, "");
          a.printLine(temp);

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class printArray implements Function
    {
       public Scalar evaluate(String n, ScriptInstance inst, Stack l)
       {
          IOObject a       = chooseSource(l, 2);
          ScalarArray   ar = BridgeUtilities.getArray(l);

          Iterator i = ar.scalarIterator();
          while (i.hasNext())
          {
             a.printLine(i.next().toString());
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class print implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a = chooseSource(l, 2);

          String temp = BridgeUtilities.getString(l, "");
          a.print(temp);

          return SleepUtils.getEmptyScalar();
       }
    }


    private static class printEOF implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a = chooseSource(l, 1);
          a.sendEOF();

          return SleepUtils.getEmptyScalar();
       }
    }

    private static IOObject chooseSource(Stack l, int args)
    {
       IOObject a;

       if (l.size() >= args)
       {
          a = (IOObject)BridgeUtilities.getObject(l);
       }
       else
       {
          a = IOObject.getConsole();
       }  

       return a;
    }

    private static class getConsoleObject implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          return SleepUtils.getScalar(IOObject.getConsole(i.getScriptEnvironment()));
       }
    }

    private static Scalar ReadFormatted(String format, InputStream in, ScriptEnvironment env, IOObject control)
    {
       Scalar temp         = SleepUtils.getArrayScalar();
       DataPattern pattern = DataPattern.Parse(format);

       byte        bdata[] = new byte[8]; 
       ByteBuffer  buffer  = ByteBuffer.wrap(bdata);
       int         read    = 0;

       while (pattern != null)
       {
          buffer.order(pattern.order);

          if (pattern.value == 'M')
          {
             if (pattern.count == 1)
                pattern.count = 1024 * 10; // 10K worth of data :)

             in.mark(pattern.count);
          }
          else if (pattern.value == 'x')
          {
             try
             {
                in.skip(pattern.count);
             }
             catch (Exception ex) { }
          }
          else if (pattern.value == 'z' || pattern.value == 'Z' || pattern.value == 'U' || pattern.value == 'u')
          {
             StringBuffer temps = new StringBuffer();
             int tempval;

             try
             {
                if (pattern.value == 'u' || pattern.value == 'U')
                {
                   read = in.read(bdata, 0, 2);
                   if (read < 2) throw new EOFException();
                   tempval = (int)buffer.getChar(0);
                }
                else
                {
                   tempval = in.read();
                   if (tempval == -1) throw new EOFException();
                }
             
                int z = 1;

                for (; tempval != 0 && z != pattern.count; z++)
                {
                   temps.append((char)tempval);

                   if (pattern.value == 'u' || pattern.value == 'U')
                   {
                      read = in.read(bdata, 0, 2);
                      if (read < 2) throw new EOFException();
                      tempval = (int)buffer.getChar(0);
                   }
                   else
                   {
                      tempval = in.read();
                      if (tempval == -1) throw new EOFException();
                   }
                } 

                if (tempval != 0)
                {
                   temps.append((char)tempval); 
                }

                if ((pattern.value == 'Z' || pattern.value == 'U') && z < pattern.count)
                {
                   int skipby = (pattern.count - z) * (pattern.value == 'U' ? 2 : 1);
                   in.skip(skipby);
                }
             }
             catch (Exception fex) 
             { 
                if (control != null) control.close();
                temp.getArray().push(SleepUtils.getScalar(temps.toString()));       
                return temp;
             }
 
             temp.getArray().push( SleepUtils.getScalar(temps.toString()) ); // reads in a full on string :)
          }
          else
          {
             for (int z = 0; z != pattern.count; z++) // pattern.count is the integer specified "AFTER" the letter
             {
                Scalar value = null;
 
                try
                {
                   switch (pattern.value)
                   {
                      case 'R':
                        in.reset();
                        break;
                      case 'C':
                        read = in.read(bdata, 0, 1);

                        if (read < 1) throw new EOFException();

                        value = SleepUtils.getScalar((char)bdata[0] + ""); // turns the char into a string
                        break;
                      case 'c':
                        read = in.read(bdata, 0, 2);

                        if (read < 2) throw new EOFException();

                        value = SleepUtils.getScalar(buffer.getChar(0) + ""); // turns the char into a string
                        break;
                      case 'b':
                        bdata[0] = (byte)in.read();

                        if (bdata[0] == -1) throw new EOFException();

                        value = SleepUtils.getScalar((int)bdata[0]); // turns the byte into an int
                        break;
                      case 'B':
                        read = in.read();

                        if (read == -1) throw new EOFException();

                        value = SleepUtils.getScalar(read);
                        break;
                      case 's':
                        read = in.read(bdata, 0, 2);

                        if (read < 2) throw new EOFException();

                        value = SleepUtils.getScalar(buffer.getShort(0));
                        break;
                      case 'S':
                        read = in.read(bdata, 0, 2);

                        if (read < 2) throw new EOFException();

                        value = SleepUtils.getScalar((int)buffer.getShort(0) & 0x0000FFFF);
                        break;
                      case 'i':
                        read = in.read(bdata, 0, 4);

                        if (read < 4) throw new EOFException();

                        value = SleepUtils.getScalar(buffer.getInt(0)); // turns the byte into an int
                        break;
                      case 'I':
                        read = in.read(bdata, 0, 4);

                        if (read < 4) throw new EOFException();

                        value = SleepUtils.getScalar((long)buffer.getInt(0) & 0x00000000FFFFFFFFL); // turns the byte into an int
                        break;
                      case 'f':
                        read = in.read(bdata, 0, 4);

                        if (read < 4) throw new EOFException();

                        value = SleepUtils.getScalar(buffer.getFloat(0)); // turns the byte into an int
                        break;
                      case 'd':
                        read = in.read(bdata, 0, 8);

                        if (read < 8) throw new EOFException();

                        value = SleepUtils.getScalar(buffer.getDouble(0)); // turns the byte into an int
                        break;
                      case 'l':
                        read = in.read(bdata, 0, 8);

                        if (read < 8) throw new EOFException();

                        value = SleepUtils.getScalar(buffer.getLong(0)); // turns the byte into an int
                        break;
                      default:
                        env.getScriptInstance().fireWarning("Erroneous file pattern character: " + pattern.value, -1);
                   }
                }
                catch (Exception ex) 
                { 
                   if (control != null) control.close();
                   if (value != null)   
                      temp.getArray().push(value);       
                   return temp;
                }
 
                if (value != null)   
                   temp.getArray().push(value);       
             }
          }

          pattern = pattern.next;
       }

       return temp;
    }

    private static void WriteFormatted(String format, OutputStream out, Stack arguments, IOObject control)
    {
       DataPattern pattern  = DataPattern.Parse(format);

       if (arguments.size() == 1 && ((Scalar)arguments.peek()).getArray() != null)
       {
          Stack temp = new Stack();
          Iterator i = ((Scalar)arguments.peek()).getArray().scalarIterator();
          while (i.hasNext())
              temp.push(i.next());

          WriteFormatted(format, out, temp, control);
          return;
       }

       byte        bdata[] = new byte[8]; 
       ByteBuffer  buffer  = ByteBuffer.wrap(bdata);

       while (pattern != null)
       {
          buffer.order(pattern.order);

          if (pattern.value == 'z' || pattern.value == 'Z' || pattern.value == 'u' || pattern.value == 'U')
          {
             try
             {
                char[] tempchars = BridgeUtilities.getString(arguments, "").toCharArray();

                for (int y = 0; y < tempchars.length; y++)
                {
                   if (pattern.value == 'u' || pattern.value == 'U')
                   {
                      buffer.putChar(0, tempchars[y]);
                      out.write(bdata, 0, 2);
                   }
                   else
                   {
                      out.write((int)tempchars[y]);
                   }
                }

                // handle padding... 

                for (int z = tempchars.length; z < pattern.count; z++)
                {
                   switch (pattern.value)
                   {
                      case 'U':
                         out.write(0); 
                         out.write(0);
                         break;
                      case 'Z':
                         out.write(0);
                         break;
                   }
                }

                // write out our terminating null byte please...

                if (pattern.value == 'z' || (pattern.value == 'Z' && pattern.count == -1))
                {
                   out.write(0);
                }
                else if (pattern.value == 'u' || (pattern.value == 'U' && pattern.count == -1))
                {
                   out.write(0);
                   out.write(0);
                }
             }
             catch (Exception ex)
             {
                if (control != null) control.close();
                return;
             }
          }
          else
          {
             for (int z = 0; z != pattern.count && !arguments.isEmpty(); z++)
             {
                Scalar temp = null;

                if (pattern.value != 'x')
                {
                   temp = BridgeUtilities.getScalar(arguments);
                }

                try
                {
                   switch (pattern.value)
                   {
                      case 'x':
                        out.write(0);
                        break;
                      case 'c':
                        buffer.putChar(0, temp.toString().charAt(0));
                        out.write(bdata, 0, 2);
                        break;
                      case 'C':
                        out.write((int)temp.toString().charAt(0));
                        break;
                      case 'b':
                      case 'B':
                        out.write(temp.intValue());
                        break;
                      case 's':
                      case 'S':
                        buffer.putShort(0, (short)temp.intValue());
                        out.write(bdata, 0, 2);
                        break;
                      case 'i':
                      case 'I':
                        buffer.putInt(0, temp.intValue());
                        out.write(bdata, 0, 4);
                        break;
                      case 'f':
                        buffer.putFloat(0, (float)temp.doubleValue());
                        out.write(bdata, 0, 4);
                        break;
                      case 'd':
                        buffer.putDouble(0, temp.doubleValue());
                        out.write(bdata, 0, 8);
                        break;
                      case 'l':
                        buffer.putLong(0, temp.longValue());
                        out.write(bdata, 0, 8);
                        break;
                      default:
                   }
                }
                catch (Exception ex) 
                { 
                   if (control != null) control.close();
                   return;
                }
             }
          }

          pattern = pattern.next;
       }

       try
       {
          out.flush();
       }
       catch (Exception ex) { }
    }

    private static class bread implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject        a = chooseSource(l, 2);
          String    pattern = BridgeUtilities.getString(l, "");

          return ReadFormatted(pattern, a.getReader(), i.getScriptEnvironment(), a);
       }
    }

    private static class bwrite implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject        a = chooseSource(l, 3);
          String    pattern = BridgeUtilities.getString(l, "");

          WriteFormatted(pattern, a.getWriter(), l, a);
          return SleepUtils.getEmptyScalar();
       }
    }

    private static class mark implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject        a = chooseSource(l, 2);
          a.getInputBuffer().mark(BridgeUtilities.getInt(l, 1024 * 10 * 10));

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class available implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          try
          {
             IOObject        a = chooseSource(l, 1);

             if (l.isEmpty())
             {
                return SleepUtils.getScalar(a.getInputBuffer().available());
             }
             else
             {
                String delim = BridgeUtilities.getString(l, "\n");

                StringBuffer temp = new StringBuffer();

                int x = 0;
                int y = a.getInputBuffer().available();

                a.getInputBuffer().mark(y);
                
                while (x < y)
                {
                   temp.append((char)a.getReader().readUnsignedByte());
                   x++;
                }

                a.getInputBuffer().reset();
      
                return SleepUtils.getScalar(temp.indexOf(delim) > -1);
             }
          }
          catch (Exception ex) { return SleepUtils.getEmptyScalar(); }
       }
    }

    private static class skip implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          try {
          IOObject        a = chooseSource(l, 2);
          a.getInputBuffer().skip(BridgeUtilities.getLong(l, 0));
          } catch (Exception ex) { }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class reset implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          try {
          IOObject        a = chooseSource(l, 1);
          a.getInputBuffer().reset();
          } catch (Exception ex) { }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class wait implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject a     = chooseSource(l, 1);
          long     times = BridgeUtilities.getLong(l, -1);
          long     stamp = System.currentTimeMillis();

          if (a.getThread() != null)
          {
             while (a.getThread().isAlive())
             {
                 if (times > -1 && (System.currentTimeMillis() - stamp) > times)
                 {
                    i.getScriptEnvironment().flagError("wait on object timed out");
                    return SleepUtils.getEmptyScalar();
                 }

                 Thread.yield();
             }
          }

          return a.getToken();
       }
    }

    private static class unpack implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String    pattern = BridgeUtilities.getString(l, "");
          String    data    = BridgeUtilities.getString(l, "");

          try
          {
             ByteArrayOutputStream out = new ByteArrayOutputStream(data.length());
             DataOutputStream toBytes  = new DataOutputStream(out);
             toBytes.writeBytes(data);     

             return ReadFormatted(pattern, new DataInputStream(new ByteArrayInputStream(out.toByteArray())), i.getScriptEnvironment(), null);
          }
          catch (Exception ex)
          {
             return SleepUtils.getArrayScalar();
          }
       }
    }

    private static class pack implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          String    pattern = BridgeUtilities.getString(l, "");

          ByteArrayOutputStream temp = new ByteArrayOutputStream(DataPattern.EstimateSize(pattern));
         
          WriteFormatted(pattern, new DataOutputStream(temp), l, null);

          StringBuffer value = new StringBuffer();
          byte[] data = temp.toByteArray();

          for (int x = 0; x < data.length; x++)
          {
             value.append((char)data[x]);
          }

          return SleepUtils.getScalar(value.toString());
       }
    }

    private static class writeb implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject     a = chooseSource(l, 2);
          String    data = BridgeUtilities.getString(l, "");

          try
          {
             for (int x = 0; x < data.length(); x++)
             {
                a.getWriter().writeByte((byte)data.charAt(x));
             } 
             a.getWriter().flush();
          }
          catch (Exception ex)
          {
             a.close();
             i.getScriptEnvironment().flagError(ex.toString());
          }

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class readb implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject         a = chooseSource(l, 2);
          int             to = BridgeUtilities.getInt(l, 1);

          StringBuffer temp = new StringBuffer(to);

          try
          {
             for (int x = 0; x < to && a.getReader() != null; x++)
             {
                temp.append((char)a.getReader().readUnsignedByte());
             }
          }
          catch (Exception ex)
          {
             a.close();
             i.getScriptEnvironment().flagError(ex.toString());
          }

          return SleepUtils.getScalar(temp.toString());
       }
    }

    private static class read implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          IOObject     a = chooseSource(l, 2);
          SleepClosure b = BridgeUtilities.getFunction(l, i);

          Thread fred = new Thread(new CallbackReader(a, i, b, BridgeUtilities.getInt(l, 0)));
          a.setThread(fred);
          fred.start();

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class iseof implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          IOObject a = (IOObject)BridgeUtilities.getObject(l);
          return a.isEOF();
       }
    }

    private static class CallbackReader implements Runnable
    {
       protected IOObject       source;
       protected ScriptInstance script;
       protected SleepClosure   function;
       protected int            bytes;
 
       public CallbackReader(IOObject s, ScriptInstance si, SleepClosure func, int byteme)
       {
          source   = s;
          script   = si;
          function = func;
          bytes    = byteme;
       }

       public void run()
       {
          Stack  args = new Stack();
          String temp;

          if (bytes <= 0)
          {
             while (script.isLoaded() && (temp = source.readLine()) != null)
             {
                args.push(SleepUtils.getScalar(temp));
                args.push(SleepUtils.getScalar(source));

                function.callClosure("&read", script, args);
             } 
          }
          else
          {
             StringBuffer tempb = null;

             try
             {
                while (script.isLoaded() && !source.isEOF())
                {
                   tempb = new StringBuffer(bytes);

                   for (int x = 0; x < bytes; x++)
                   {
                      tempb.append((char)source.getReader().readUnsignedByte());
                   }

                   args.push(SleepUtils.getScalar(tempb.toString()));
                   args.push(SleepUtils.getScalar(source));
  
                   function.callClosure("&read", script, args);
                }
             }
             catch (Exception ex)
             {
                if (tempb.length() > 0)
                {
                   args.push(SleepUtils.getScalar(tempb.toString()));
                   args.push(SleepUtils.getScalar(source));
  
                   function.callClosure("&read", script, args);
                }

                source.close();
                script.getScriptEnvironment().flagError(ex.toString());
             }
          }
       }
    }

    private static final int LISTEN_FUNCTION  = 1;
    private static final int CONNECT_FUNCTION = 2;

    private static class SocketHandler implements Runnable
    {
       public ScriptInstance script;
       public SleepClosure   function;
       public SocketObject   socket;

       public int            port;
       public int            timeout;
       public String         host;
       public Scalar         callback;

       public int            type;

       public void start()
       {
          if (function != null)
          {
             socket.setThread(new Thread(this));
             socket.getThread().start();
          }
          else
          {
             run();
          }
       }

       public void run()
       {
          if (type == LISTEN_FUNCTION)
          {
             socket.listen(port, timeout, callback, script.getScriptEnvironment());
          }
          else
          {
             socket.open(host, port, timeout, script.getScriptEnvironment());
          }

          if (function != null)
          {
             Stack  args  = new Stack();
             args.push(SleepUtils.getScalar(socket));
             function.callClosure("&callback", script, args);
          }
       }
    }
}
