package sleep.bridges.io;

import java.io.*;
import java.net.*;
import sleep.runtime.*;

public class SocketObject extends IOObject
{
   protected Socket socket;

   /** returns the socket used for this connection */
   public Object getSource()
   {
      return socket;
   }

   public void open(String server, int port, int timeout, ScriptEnvironment env)
   {
      try
      {
         socket = new Socket(server, port);
         socket.setSoLinger(true, 5);
         socket.setSoTimeout(timeout);

         openRead(socket.getInputStream());
         openWrite(socket.getOutputStream());
      }
      catch (Exception ex)
      {
         env.flagError(ex.toString());
      }
   }

   public void listen(int port, int timeout, Scalar data, ScriptEnvironment env)
   {
      ServerSocket server = null;

      try
      {
         server = new ServerSocket(port);
         server.setSoTimeout(timeout);
        
         socket = server.accept();
         socket.setSoLinger(true, 5);

         server.close(); /* releases the bound and listening port, probably not a good idea for a massive server but for a scripting
                            lang API who cares */

         data.setValue(SleepUtils.getScalar(socket.getInetAddress().getHostAddress()));

         openRead(socket.getInputStream());
         openWrite(socket.getOutputStream());
      }
      catch (Exception ex)
      {
         env.flagError(ex.toString());
      }
   }

   public void close()
   {
      try
      {
         socket.close();
      }
      catch (Exception ex) { }

      super.close();
   }
}
