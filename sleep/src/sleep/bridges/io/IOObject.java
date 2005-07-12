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
 *  <pre>BufferedReader <- DataInputStream <- Original Input Stream</pre>
 *
 *  <p>The pipeline for writing data is:</p>
 *
 *  <pre>PrintWriter -> DataOutputStream -> Original Output Stream</pre>
 */

public class IOObject
{
   private static IOObject console = null;

   protected InputStream     in       = null;
   protected OutputStream    out      = null;

   protected BufferedReader  reader   = null;
   protected PrintWriter     writer   = null;

   protected DataInputStream  readerb = null;
   protected DataOutputStream writerb = null;

   protected Thread           thread  = null;
   protected Scalar           token   = null;

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

   /** returns an IOObject that represents stdin/stdout */
   public static IOObject getConsole()
   {
      if (console == null)
      {
         console = new IOObject();
         console.openRead(System.in);
         console.openWrite(System.out);
      }
      return console;
   }

   /** Initializes a binary reader (a DataInputStream) and a text reader (a BufferedReader) against this input stream.  Calling this effectively makes this IOObject useable with Sleep's IO read* functions. */
   public void openRead(InputStream _in)
   {
      in = _in;
      
      if (in != null)
      {
         readerb = new DataInputStream(in);
         reader  = new BufferedReader(new InputStreamReader(readerb));
      }
   }

   /** Initializes a binary writer (a DataOutputStream) and a text writer (a PrintWriter) against this input stream.  Calling this effectively makes this IOObject useable with Sleep's IO print* functions. */
   public void openWrite(OutputStream _out)
   {
      out = _out;

      if (out != null)
      {
         writerb = new DataOutputStream(out);
         writer  = new PrintWriter(writerb, true);
      }
   }

   /** Closes all of the reader's / writer's opened by this IOObject.  If the IO Source object opens any streams, this method should be overriden to close those streams when requested.  Calling super.close() is highly recommended as well. */
   public void close()
   {
      try
      {
         in.notifyAll();  // done to prevent a deadlock, trust me it works
         out.notifyAll(); // done to prevent a deadlock, trust me it works

         if (reader != null)
           reader.close();

         if (readerb != null)
           readerb.close();

         if (writer != null)
           writer.close();

         if (writerb != null)
           writerb.close();

         if (in != null)
           in.close();

         if (out != null)
           out.close();
      }
      catch (Exception ex)
      {

      }
      finally
      {
         in     = null;
         out    = null;
         reader = null;
         writer = null;
         readerb = null;
         writerb = null;
      }
   }

   /** Reads in a line of text */
   public String readLine()
   {
      try
      {
         if (reader != null)
         {
            String temp = reader.readLine();

            if (temp == null)
              reader = null;

            return temp;
         }
      }
      catch (Exception ex) { }

      reader = null;
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
      if (writer != null)
        writer.close();

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
   public BufferedReader getPrimaryReader()
   {
       return reader;
   }

   /** Returns the ascii data writer */
   public PrintWriter getPrimaryWriter()
   {
       return writer;
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

   /** Prints out a line of text with a newline character appended */
   public void printLine(String text)
   {
      if (writer != null)
      {
         writer.println(text);
      }
   }

   /** Prints out a line of text with no newline character appended */
   public void print(String text)
   {
      if (writer != null)
      {
         writer.print(text);
         writer.flush();
      }
   }
}

