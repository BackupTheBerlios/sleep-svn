package sleep.bridges.io;

import java.io.*;
import java.net.*;
import sleep.runtime.*;

import java.util.*;

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
         socket = new Socket();

         socket.connect(new InetSocketAddress(server, port), timeout);
         socket.setSoLinger(true, 5);

         openRead(socket.getInputStream());
         openWrite(socket.getOutputStream());
      }
      catch (Exception ex)
      {
         env.flagError(ex.toString());
      }
   }

   /** releases the socket binding for the specified port */
   public static void release(int port)
   {
      String key = port + "";
      
      ServerSocket temp = null;
      if (servers != null && servers.containsKey(key))
      {
         temp = (ServerSocket)servers.get(key);
         servers.remove(key);
 
         try
         {
            temp.close();
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }
   }

   private static Map servers;

   private static ServerSocket getServerSocket(int port) throws Exception
   {
      String key = port + "";

      if (servers == null)
      {
         servers = Collections.synchronizedMap(new HashMap());
      }

      ServerSocket server = null;

      if (servers.containsKey(key))
      {
         server = (ServerSocket)servers.get(key);
      }
      else
      {
         server = new ServerSocket(port);
         servers.put(key, server);
      }

      return server;
   }
 
   public void listen(int port, int timeout, Scalar data, ScriptEnvironment env)
   {
      ServerSocket server = null;

      try
      {
//         server = new ServerSocket(port);
         server = getServerSocket(port);
         server.setSoTimeout(timeout);
        
         socket = server.accept();
         socket.setSoLinger(true, 5);

  //       server.close();
 /* releases the bound and listening port, probably not a good idea for a massive server but for a scripting
                            lang API who cares */

         data.setValue(SleepUtils.getScalar(socket.getInetAddress().getHostAddress()));

         openRead(socket.getInputStream());
         openWrite(socket.getOutputStream());

         return;
      }
      catch (Exception ex)
      {
         env.flagError(ex.toString());
      }

      try
      {
//         if (server != null) { server.close(); }
      }
      catch (Exception ex) { }
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
