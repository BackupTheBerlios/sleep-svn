package sleep.bridges;
 
import java.util.*;
import java.util.regex.*;

import sleep.engine.*;
import sleep.engine.types.*;

import sleep.interfaces.*;
import sleep.runtime.*;

import java.io.*;

/** provides a bridge for accessing the local file system */
public class FileSystemBridge implements Loadable
{
    public boolean scriptUnloaded(ScriptInstance aScript)
    {
        return true;
    }

    public boolean scriptLoaded (ScriptInstance aScript)
    {
        Hashtable temp = aScript.getScriptEnvironment().getEnvironment();

        // predicates
        temp.put("-exists",   new _exists());
        temp.put("-canread",   new _canread());
        temp.put("-canwrite",   new _canwrite());
        temp.put("-isDir",   new _isDirectory());
        temp.put("-isFile",   new _isFile());
        temp.put("-isHidden",   new _isHidden());

        // functions
        temp.put("&createNewFile",   new createNewFile());
        temp.put("&deleteFile",      new deleteFile());
        temp.put("&getCurrentDirectory",     new getActiveDir());
        temp.put("&getFileName",     new getFileName());
        temp.put("&getFileProper",   new getFileProper());
        temp.put("&getFileParent",   new getFileParent());
        temp.put("&lastModified",    new lastModified());
        temp.put("&lof",             new lof());
        temp.put("&ls",              new listFiles());
        temp.put("&listRoots",       temp.get("&ls"));
        temp.put("&mkdir",           new mkdir());
        temp.put("&rename",          new rename());
        temp.put("&setLastModified", new setLastModified());
        temp.put("&setReadOnly",     new setReadOnly());

        return true;
    }

    private static class createNewFile implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           try
           {
              File a = BridgeUtilities.getFile(l);
              if (a.createNewFile())
              {
                 return SleepUtils.getScalar(1);
              }
           }
           catch (Exception ex) { i.getScriptEnvironment().flagError(ex.getMessage()); }

           return SleepUtils.getEmptyScalar();
       }
    }

    private static class mkdir implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           if (a.mkdirs())
           {
              return SleepUtils.getScalar(1);
           }
           return SleepUtils.getEmptyScalar();
       }
    }

    private static class rename implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           File b = BridgeUtilities.getFile(l);
           if (a.renameTo(b))
           {
              return SleepUtils.getScalar(1);
           }
           return SleepUtils.getEmptyScalar();
       }
    }

    private static class setLastModified implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           long b = BridgeUtilities.getLong(l);

           if (a.setLastModified(b))
           {
              return SleepUtils.getScalar(1);
           }
           return SleepUtils.getEmptyScalar();
       }
    }

    private static class setReadOnly implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);

           if (a.setReadOnly())
           {
              return SleepUtils.getScalar(1);
           }
           return SleepUtils.getEmptyScalar();
       }
    }

    private static class deleteFile implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           if (a.delete())
           {
              return SleepUtils.getScalar(1);
           }
           return SleepUtils.getEmptyScalar();
       }
    }

    private static class getActiveDir implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = new File("");
           return SleepUtils.getScalar(a.getAbsolutePath());
       }
    }

    private static class getFileName implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           return SleepUtils.getScalar(a.getName());
       }
    }

    private static class getFileProper implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File start = BridgeUtilities.getFile(l);

           while (!l.isEmpty())
           {
              start = new File(start, l.pop().toString());
           }

           return SleepUtils.getScalar(start.getAbsolutePath());
       }
    }

    private static class getFileParent implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           return SleepUtils.getScalar(a.getParent());
       }
    }

    private static class lastModified implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           return SleepUtils.getScalar(a.lastModified());
       }
    }

    private static class lof implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File a = BridgeUtilities.getFile(l);
           return SleepUtils.getScalar(a.length());
       }
    }

    private static class listFiles implements Function
    {
       public Scalar evaluate(String n, ScriptInstance i, Stack l)
       {
           File[] files;
 
           if (l.isEmpty())
           {
              files = File.listRoots();
           }
           else
           {
              File a = BridgeUtilities.getFile(l);
              files = a.listFiles();
           }
           LinkedList temp = new LinkedList();

           if (files != null)
           {
              for (int x = 0; x < files.length; x++)
              {
                 temp.add(files[x].getAbsolutePath());
              }
           }

           return SleepUtils.getArrayWrapper(temp);
       }
    }

    private static class _canread implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          File a = BridgeUtilities.getFile(l);
          return a.canRead();
       }
    }

    private static class _isDirectory implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          File a = BridgeUtilities.getFile(l);
          return a.isDirectory();
       }
    }

    private static class _isFile implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          File a = BridgeUtilities.getFile(l);
          return a.isFile();
       }
    }

    private static class _isHidden implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          File a = BridgeUtilities.getFile(l);
          return a.isHidden();
       }
    }

    private static class _exists implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          File a = BridgeUtilities.getFile(l);
          return a.exists();
       }
    }

    private static class _canwrite implements Predicate
    {
       public boolean decide(String n, ScriptInstance i, Stack l)
       {
          File a = BridgeUtilities.getFile(l);
          return a.canWrite();
       }
    }
}
