package sleep.bridges.io;

import java.io.*;
import java.net.*;
import sleep.runtime.*;

public class SocketObject extends IOObject
{
   protected Socket socket;

   public void open(String server, int port, ScriptEnvironment env)
   {
      try
      {
         socket = new Socket(server, port);
         socket.setSoLinger(true, 5);

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
      try
      {
         ServerSocket server = new ServerSocket(port);
         server.setSoTimeout(timeout);
        
         socket = server.accept();
         socket.setSoLinger(true, 5);

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
