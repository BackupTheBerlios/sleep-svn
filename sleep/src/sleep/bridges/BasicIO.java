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

        temp.put("&mark",       new mark());
        temp.put("&skip",       new skip());
        temp.put("&reset",      new reset());

        // typical ASCII'sh output functions
        temp.put("&print",      new print());
        temp.put("&println",    new println());
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

             new Thread(child, child.getName()).start();
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
             temp = "EOF";
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
          return SleepUtils.getScalar(IOObject.getConsole());
       }
    }

    private static Scalar ReadFormatted(String format, DataInputStream in, ScriptEnvironment env, IOObject control)
    {
       Scalar temp         = SleepUtils.getArrayScalar();
       DataPattern pattern = DataPattern.Parse(format);

       while (pattern != null)
       {
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
          else if (pattern.value == 'z' || pattern.value == 'Z')
          {
             StringBuffer temps = new StringBuffer();
             int tempval;

             try
             {
                tempval = in.readUnsignedByte();
             
                int z = 0;

                for (; tempval != 0 && z < pattern.count; z++)
                {
                   temps.append((char)tempval);
                   tempval = in.readUnsignedByte();
                } 

                if (pattern.value == 'Z' && z < pattern.count)
                {
                   in.skip((pattern.count - z) - 1);
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
                      case 'c':
                        value = SleepUtils.getScalar(in.readChar() + ""); // turns the char into a string
                        break;
                      case 'C':
                        value = SleepUtils.getScalar(((char)in.readUnsignedByte()) + ""); // turns the char into a string
                        break;
                      case 'b':
                        value = SleepUtils.getScalar((int)in.readByte()); // turns the byte into an int
                        break;
                      case 'B':
                        value = SleepUtils.getScalar((int)in.readUnsignedByte()); // turns the byte into an int
                        break;
                      case 's':
                        value = SleepUtils.getScalar((int)in.readShort()); // turns the byte into an int
                        break;
                      case 'S':
                        value = SleepUtils.getScalar((int)in.readUnsignedShort()); // turns the byte into an int
                        break;
                      case 'i':
                        value = SleepUtils.getScalar(in.readInt()); // turns the byte into an int
                        break;
                      case 'I':
                        int ch1 = in.read();
                        int ch2 = in.read();
                        int ch3 = in.read();
                        int ch4 = in.read();

                        if ((ch1 | ch2 | ch3 | ch4) < 0)
                             throw new EOFException();

                        long templ = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

                        value = SleepUtils.getScalar(templ); // turns the byte into an int
                        break;
                      case 'f':
                        value = SleepUtils.getScalar(in.readFloat()); // turns the byte into an int
                        break;
                      case 'd':
                        value = SleepUtils.getScalar(in.readDouble()); // turns the byte into an int
                        break;
                      case 'l':
                        value = SleepUtils.getScalar(in.readLong()); // turns the byte into an int
                        break;
                      case 'u':
                        value = SleepUtils.getScalar(in.readUTF()); // turns the byte into an int
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

    private static void WriteFormatted(String format, DataOutputStream out, Stack arguments, IOObject control)
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

       while (pattern != null)
       {
          if (pattern.value == 'z' || pattern.value == 'Z')
          {
             try
             {
                char[] tempchars = BridgeUtilities.getString(arguments, "").toCharArray();

                for (int y = 0; y < tempchars.length; y++)
                {
                   out.writeByte((byte)tempchars[y]);
                }

                out.writeByte(0); // output the null terminator
   
                if (pattern.value == 'Z')
                {
                   // the +1 for the start of this loop is to account for the outputted null character
                   for (int z = tempchars.length + 1; z < pattern.count; z++)
                   {
                      out.writeByte(0); // in the case of Z, keep padding the field length with nulls.
                   }
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
                        out.writeByte(0);
                        break;
                      case 'c':
                        out.writeChar(temp.toString().charAt(0));
                        break;
                      case 'C':
                        out.writeByte((byte)temp.toString().charAt(0));
                        break;
                      case 'b':
                      case 'B':
                        out.writeByte(temp.intValue());
                        break;
                      case 's':
                      case 'S':
                        out.writeShort(temp.intValue());
                        break;
                      case 'i':
                      case 'I':
                        out.writeInt(temp.intValue());
                        break;
                      case 'f':
                        out.writeFloat((float)temp.doubleValue());
                        break;
                      case 'd':
                        out.writeDouble(temp.doubleValue());
                        break;
                      case 'l':
                        out.writeLong(temp.longValue());
                        break;
                      case 'u':
                        out.writeUTF(temp.toString());
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
          a.getReader().mark(BridgeUtilities.getInt(l, 1024 * 10 * 10));

          return SleepUtils.getEmptyScalar();
       }
    }

    private static class skip implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
          try {
          IOObject        a = chooseSource(l, 2);
          a.getReader().skip(BridgeUtilities.getLong(l, 0));
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
          a.getReader().reset();
          } catch (Exception ex) { }

          return SleepUtils.getEmptyScalar();
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
             value.append((byte)data[x]);
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
             new Thread(this).start();
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
             socket.open(host, port, script.getScriptEnvironment());
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
