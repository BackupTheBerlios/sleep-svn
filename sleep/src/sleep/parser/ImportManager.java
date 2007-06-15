package sleep.parser;

import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import java.io.*;

/** This class mantains a cache of imported package names and resolve classes for a Sleep parser.
    The existence of this class also allows these imports to be shared between parser instances.  Value is allowing
    dynamically parsed code like eval, expr, compile_clousre etc.. to inherit their parents imported class
    information. */
public class ImportManager
{
   protected Map        imports   = new LinkedHashMap();
   protected HashMap    classes   = new HashMap();
   protected HashMap    jars      = new HashMap(); /* resolved jar files, key=jar name value=ClassLoader */  

   /** Used by Sleep to import statement to save an imported package name. */
   public File importPackage(String packagez, String from)
   {
       File returnValue = null;

       String pack, clas;
       clas = packagez.substring(packagez.lastIndexOf(".") + 1, packagez.length());
       pack = packagez.substring(0, packagez.lastIndexOf("."));

       /* resolve and setup our class loader for the specified jar file */

       if (from != null)
       {
          try
          {
             returnValue = ParserConfig.findJarFile(from);
 
             if (!jars.containsKey(from))
             {
                URLClassLoader loader = new URLClassLoader(new URL[] { returnValue.toURL() }, Thread.currentThread().getContextClassLoader());
                jars.put(from, loader);
             }
          }
          catch (Exception ex) { ex.printStackTrace(); }
       }

       /* handle importing our package */

       if (clas.equals("*"))
       {
          imports.put(pack, from);
       }
       else
       {
          imports.put(packagez, from);
         
          Class found = findImportedClass(packagez);
          classes.put(clas, found);
       }

       return returnValue;
   }

   /** This method is used by Sleep to resolve a specific class (or at least try) */
   private Class resolveClass(String pack, String clas, String jar)
   {
       try
       {
          if (jar != null)
          {
             ClassLoader cl = (ClassLoader)jars.get(jar);
             return Class.forName(pack + "." + clas, true, cl);
          }
          else
          {
             return Class.forName(pack + "." + clas);
          }
       }
       catch (Exception ex) { }

       return null;
   }

   /** Attempts to find a class, starts out with the passed in string itself, if that doesn't resolve then the string is 
       appended to each imported package to see where the class might exist */
   public Class findImportedClass(String name)
   {
       if (classes.get(name) == null)
       {
          Class rv = null;
          String clas, pack;

          if (name.indexOf(".") > -1)
          {
             clas = name.substring(name.lastIndexOf(".") + 1, name.length());
             pack = name.substring(0, name.lastIndexOf("."));

	     rv   = resolveClass(pack, clas, (String)imports.get(name));
          }
          else
          {
             Iterator i = imports.entrySet().iterator();
             while (i.hasNext() && rv == null)
             {
                Map.Entry en = (Map.Entry)i.next();
                rv = resolveClass((String)en.getKey(), name, (String)en.getValue());
             }
          }

          // some friendly (really) debugging
/*          if (rv == null)
          {
             System.err.println("Argh: " + name + " is not an imported class");
             Thread.dumpStack();
          } */

          classes.put(name, rv);
       }
     
       return (Class)classes.get(name);
   }
}

