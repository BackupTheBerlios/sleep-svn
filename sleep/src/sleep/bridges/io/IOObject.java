package sleep.bridges.io;

import java.io.*;
import sleep.runtime.*;

/** <p>The IOObject is the parent class for all IO Source objects that are compatible with Sleep's
 *  I/O API.</p>
 *
 *  <p>When an IOObject is created, calling openRead() with the appropriate input stream will initialize
 *  this IOObject to work with IO functions that read data.  Calling openWrite() has the same effect for
 *  print functions.  It is the responsibility of the IOObject child class to invoke openRead and openWrite.
 *  This is usually done in the constructor.</p>  
 *
 *  <p>The pipeline for reading data looks like this:</p>
 *
 *  <pre>DataInputStream <- BufferedInputStream <- Original Input Stream</pre>
 *
 *  <p>The pipeline for writing data is:</p>
 *
 *  <pre>DataOutputStream -> Original Output Stream</pre>
 */

public class IOObject
{
   /* input pipeline */ 

   protected DataInputStream     readerb = null; /* used to support the binary read/write stuffz */
   protected BufferedInputStream reader  = null; /* used to support mark and reset functionality y0 */
   protected InputStream         in      = null; /* the original stream, love it, hold it... yeah right */

   /* output pipeline */

   protected DataOutputStream writerb = null; /* high level method for writing stuff out, fun fun fun */
   protected OutputStream     out     = null; /* original output stream */

   protected Thread           thread  = null;
   protected Scalar           token   = null;

   protected byte[]           buffer  = null;

   public byte[] getBuffer(int size)
   {
      if (buffer == null || size > buffer.length)
      {
         buffer = new byte[size];
      }

      return buffer;
   }

   /** return the actual source of this IO for scripters to query using HOES */
   public Object getSource()
   {
      return null;
   }

   /** set the thread used for this IOObject (currently used to allow a script to wait() on the threads completion) */
   public void setThread(Thread t)
   {
      thread = t;
   }

   /** returns the thread associated with this IOObject */
   public Thread getThread()
   {
      return thread;
   }

   public Scalar wait(ScriptEnvironment env, long timeout)
   {
      if (getThread() != null && getThread().isAlive())
      {
         try
         {
            getThread().join(timeout);

            if (getThread().isAlive())
            {
               env.flagError("wait on object timed out");
               return SleepUtils.getEmptyScalar();
            }
         }
         catch (Exception ex)
         {
            env.flagError("wait on object failed: " + ex.getMessage());
            return SleepUtils.getEmptyScalar();
         }
      }

      return getToken();
   }

   /** returns a scalar token associated with this IOObject.  Will return the empty scalar if the token is null.  The token is essentially the stored return value of an executing thread.  */
   public Scalar getToken()
   {
      if (token == null) return SleepUtils.getEmptyScalar();

      return token;
   }

   /** sets the scalar token associated with this IOObject.  Any ScriptInstance object calls setToken on it's parent IOObject.  This method is called when the script is finished running and has a return value waiting.  This value can be retrieved in Sleep with the <code>&amp;wait</code> function. */
   public void setToken(Scalar t)
   {
      token = t;
   }

   /** returns an IOObject that represents stdin/stdout to Sleep's I/O API.  To set a script's console
       object install an IOObject into a script environment under the variable name %console% */
   public static IOObject getConsole(ScriptEnvironment environment)
   {
      IOObject console = (IOObject)environment.getEnvironment().get("%console%");

      if (console == null)
      {
         console = new IOObject();
         console.openRead(System.in);
         console.openWrite(System.out);
         environment.getEnvironment().put("%console%", console);
      }

      return console;
   }

   /** Returns the latest hooking point into the input stream */
   public InputStream getInputStream()
   {
      return in;
   }

   /** Returns the latest hooking point into the output stream */
   public OutputStream getOutputStream()
   {
      return out;
   }

   /** Initializes a binary reader (a DataInputStream) and a text reader (a BufferedReader) against this input stream.  Calling this effectively makes this IOObject useable with Sleep's IO read* functions. */
   public void openRead(InputStream _in)
   {
      in = _in;
      
      if (in != null)
      {
         reader  = new BufferedInputStream(in);
         readerb = new DataInputStream(reader);
      }
   }

   /** Initializes a binary writer (a DataOutputStream) and a text writer (a PrintWriter) against this input stream.  Calling this effectively makes this IOObject useable with Sleep's IO print* functions. */
   public void openWrite(OutputStream _out)
   {
      out = _out;

      if (out != null)
      {
         writerb = new DataOutputStream(out);
      }
   }

   /** Closes all of the reader's / writer's opened by this IOObject.  If the IO Source object opens any streams, this method should be overriden to close those streams when requested.  Calling super.close() is highly recommended as well. */
   public void close()
   {
      try
      {
         if (in != null) { in.notifyAll(); } // done to prevent a deadlock, trust me it works
         if (out != null) { out.notifyAll(); } // done to prevent a deadlock, trust me it works
      }
      catch (Exception ex) { } /* we might get an illegal monitor state type exception if we don't own
                                  the lock from this thread... in that case we move on with our lives */
      try
      {
         if (reader != null)
           reader.close();

         if (readerb != null)
           readerb.close();

         if (writerb != null)
           writerb.close();

         if (in != null)
           in.close();

         if (out != null)
           out.close();

         buffer = null;
      }
      catch (Exception ex)
      {
      }
      finally
      {
         in     = null;
         out    = null;
         reader = null;
         readerb = null;
         writerb = null;
      }
   }

   /** Reads in a line of text */
   public String readLine()
   {
      try
      {
         if (readerb != null)
         {
            String temp = readerb.readLine(); /* deprecated, I know, but it has the behavior I want */

            if (temp == null)
            {
               readerb = null;
               reader  = null;
            }

            return temp;
         }
      }
      catch (Exception ex) 
      { 
         close();
      }

      return null;
   }

   /** Returns true if the reader is closed */
   public boolean isEOF()
   {
      return (reader == null);
   }

   /** Closes down the output streams effectively sending an end of file message to the reading end. */
   public void sendEOF()
   {
      try
      {
         if (writerb != null)
            writerb.close();

         if (out != null)
            out.close();
      }
      catch (Exception ex) { }
   }
 
   /** Returns the ascii data reader */
   public BufferedInputStream getInputBuffer()
   {
       return reader;
   }

   /** Returns the binary data reader */
   public DataInputStream getReader()
   {
       return readerb;
   }
 
   /** Returns the binary data writer */
   public DataOutputStream getWriter()
   {
       return writerb;
   }

   private static final String lineSeparator = System.getProperty("line.separator");

   /** Prints out a line of text with a newline character appended */
   public void printLine(String text)
   {
      print(text + lineSeparator);
   }

   /** Prints out a line of text with no newline character appended */
   public void print(String text)
   {
      if (writerb != null)
      {
         try
         {
            for (int x = 0; x < text.length(); x++)
            {
               writerb.writeByte((byte)text.charAt(x));
            }

            writerb.flush(); /* we don't know if the underlying stream does this or not, so we'll force it */
         }
         catch (Exception ex)
         {
            close();
         }
      }
   }
}

