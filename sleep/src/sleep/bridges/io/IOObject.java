package sleep.bridges.io;

import java.io.*;

public class IOObject
{
   private static IOObject console = null;

   protected InputStream     in       = null;
   protected OutputStream    out      = null;

   protected BufferedReader  reader   = null;
   protected PrintWriter     writer   = null;

   protected DataInputStream  readerb = null;
   protected DataOutputStream writerb = null;

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

   public void openRead(InputStream _in)
   {
      in = _in;
      
      if (in != null)
      {
         readerb = new DataInputStream(in);
         reader  = new BufferedReader(new InputStreamReader(readerb));
      }
   }

   public void openWrite(OutputStream _out)
   {
      out = _out;

      if (out != null)
      {
         writerb = new DataOutputStream(out);
         writer  = new PrintWriter(writerb, true);
      }
   }

   public void close()
   {
      try
      {
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

   public boolean isEOF()
   {
      return (reader == null);
   }

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

   public DataInputStream getReader()
   {
       return readerb;
   }

   public DataOutputStream getWriter()
   {
       return writerb;
   }

   public void printLine(String text)
   {
      if (writer != null)
      {
         writer.println(text);
      }
   }

   public void print(String text)
   {
      if (writer != null)
      {
         writer.print(text);
         writer.flush();
      }
   }
}

