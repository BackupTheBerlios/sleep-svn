/*
   SLEEP - Simple Language for Environment Extension Purposes
 .------------------------------.
 | sleep.runtime.ScriptLoader   |_____________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.hick.org/~raffi/

   Description:
     A class for the purpose of representing an instance of a loaded script
     ready for functions to be called against, ready to be run itself, and
     ready to have values from its environment extracted.

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */


package sleep.runtime;

import sleep.bridges.*;
import sleep.engine.Block;
import sleep.error.YourCodeSucksException;
import sleep.interfaces.Loadable;
import sleep.parser.Parser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.*;

/**
 * <p>The ScriptLoader is a convienence container for instantiating and managing ScriptInstances.</p>
 *
 * <p>To load a script from a file and run it:</P
 *
 * <pre>
 * ScriptLoader   loader = new ScriptLoader();
 * ScriptInstance script = loader.loadScript("script.sl");
 *
 * script.runScript();
 * </pre>
 *
 * <p>The above will load the file script.sl and then execute it immediately.</p>
 *
 * <p>Installation of loadable bridges you create can also be managed by the ScriptLoader.</p>
 *
 * <p>A loadable bridge is installed into the language by adding it to a script loader class.  There are two types of
 * bridges.  The two types are specific and global bridges.</p>
 *
 * <p>The load and unload methods for a <b>specific bridge</b> are executed for every script load and unload, no matter
 * what.</p>
 *
 * <p>A <b>global bridge</b> is installed once for each script environment.  If scripts are sharing an environment there is
 * no sense in loading stuff into the environment more than once.  This is why global bridges exist.</p>
 *
 * <p>An example of adding a loadable bridge to a script loader:</p>
 *
 * <pre>
 * ScriptLoader loader = new ScriptLoader()
 * loader.addSpecificBridge(new MyLoadableBridge());
 * </pre>
 *
 * @see sleep.interfaces.Loadable
 * @see ScriptInstance
 */
public class ScriptLoader
{
    /**
     * cache for parsed scripts mantained (optionally) by the script loader.
     */
    protected static HashMap BLOCK_CACHE = null;

    /**
     * loaded scripts
     */
    protected LinkedList loadedScripts;

    /**
     * loaded scripts except referable by key
     */
    protected HashMap scripts;

    /**
     * global bridges
     */
    protected LinkedList bridgesg;

    /**
     * specific bridges
     */
    protected LinkedList bridgess;

    /**
     * initializes the script loader
     */
    public ScriptLoader()
    {
        loadedScripts = new LinkedList();
        scripts = new HashMap();
        bridgesg = new LinkedList();
        bridgess = new LinkedList();

        initDefaultBridges();
    }

    /**
     * The Sleep script loader can optionally cache parsed script files once they are loaded.  This is useful if you will have
     * several script loader instances loading the same script files in isolated objects.
     */
    public HashMap setGlobalCache(boolean setting)
    {
        if (setting && BLOCK_CACHE == null)
            BLOCK_CACHE = new HashMap();

        if (!setting)
            BLOCK_CACHE = null;

        return BLOCK_CACHE;
    }

    /**
     * method call to initialize the default bridges, if you want to change the default bridges subclass this class and
     * override this method
     */
    protected void initDefaultBridges()
    {
        addGlobalBridge(new BasicNumbers());
        addGlobalBridge(new BasicStrings());
        addGlobalBridge(new BasicUtilities());
        addGlobalBridge(new BasicIO());
        addGlobalBridge(new FileSystemBridge());
        addGlobalBridge(new DefaultEnvironment());
        addGlobalBridge(new DefaultVariable());
        addGlobalBridge(new RegexBridge());
        addGlobalBridge(new TimeDateBridge());
    }

    /**
     * A global bridge is loaded into an environment once and only once.  This way if the environment is shared among multiple
     * script instances this will save on both memory and script load time
     */
    public void addGlobalBridge(Loadable l)
    {
        bridgesg.add(l);
    }

    /**
     * A specific bridge is loaded into *every* script regardless of wether or not the environment is shared.  Useful for
     * modifying the script instance while it is being in processed. Specific bridges are the first thing that happens after
     * the script code is parsed
     */
    public void addSpecificBridge(Loadable l)
    {
        bridgess.add(l);
    }

    /**
     * Returns a HashMap with all loaded scripts, the key is a string which is just the filename, the value is a ScriptInstance
     * object
     */
    public HashMap getScriptsByKey()
    {
        return scripts;
    }

    /**
     * Determines wether or not the script is loaded by checking if the specified key exists in the script db.
     */
    public boolean isLoaded(String name)
    {
        return scripts.containsKey(name);
    }

    /**
     * Convienence method to return the script environment of the first script tht was loaded, returns null if no scripts are loaded
     */
    public ScriptEnvironment getFirstScriptEnvironment()
    {
        if (loadedScripts.size() > 0) {
            ScriptInstance si = (ScriptInstance) loadedScripts.getFirst();
            return si.getScriptEnvironment();
        }

        return null;
    }

    /**
     * Returns a linked list of all loaded ScriptInstance objects
     */
    public LinkedList getScripts()
    {
        return loadedScripts;
    }

    /**
     * Process the newly loaded script.  Setup its name, add it to the data structures, and load the bridges into the environment
     * assuming this hasn't been done before.
     */
    private void inProcessScript(String name, ScriptInstance si)
    {
        si.setName(name);

        Iterator i = bridgess.iterator();
        while (i.hasNext()) {
            ((Loadable) i.next()).scriptLoaded(si);
        }

        // load the "global" bridges iff they need to be loaded again...
        if (si.getScriptEnvironment().getEnvironment().get("(isloaded)") != this) {
            i = bridgesg.iterator();
            while (i.hasNext()) {
                ((Loadable) i.next()).scriptLoaded(si);
            }
            si.getScriptEnvironment().getEnvironment().put("(isloaded)", this);
        }

        // add script to our loaded scripts data structure

        if (! name.equals("<interact mode>")) {
            loadedScripts.add(si);
            scripts.put(name, si);
        }
    }

    /**
     * Load a serialized version of the script iff a serialized version exists, and its modification time is greater than the
     * modification time of the script.  Also handles the muss and fuss of reserializing the script if it has to reload the
     * script.  Personally I didn't find much of a startup time decrease when loading the scripts serialized versus parsing them
     * each time.  Theres a command 'bload' in the console to benchmark loading a script normally versus serialized.  Try it.
     *
     * @param script a file object pointing to the script file...
     */
    public ScriptInstance loadSerialized(File script, Hashtable env) throws IOException, ClassNotFoundException
    {
        File bin = new File(script.getAbsolutePath() + ".bin");

        if (bin.exists() && (!script.exists() || script.lastModified() < bin.lastModified())) {
            return loadSerialized(script.getName(), new FileInputStream(bin), env);
        }

        ScriptInstance si = loadScript(script, env);
        saveSerialized(si);
        return si;
    }

    /**
     * Loads a serialized script from the specified input stream with the specified name
     */
    public ScriptInstance loadSerialized(String name, InputStream stream, Hashtable env) throws IOException, ClassNotFoundException
    {
        ObjectInputStream p = new ObjectInputStream(stream);
        Block block = (Block) p.readObject();
        return loadScript(name, block, env);
    }

    /**
     * Saves a serialized version of the compiled script to scriptname.bin.
     */
    public static void saveSerialized(ScriptInstance si) throws IOException
    {
        saveSerialized(si, new FileOutputStream(si.getName() + ".bin"));
    }

    /**
     * Saves a serialized version of the ScriptInstance si to the specified output stream
     */
    public static void saveSerialized(ScriptInstance si, OutputStream stream) throws IOException
    {
        ObjectOutputStream o = new ObjectOutputStream(stream);
        o.writeObject(si.getRunnableBlock());
    }

    public ScriptInstance loadScript(String name, Block code, Hashtable env)
    {
        ScriptInstance si = new ScriptInstance(env);
        si.script = code;
        inProcessScript(name, si);
        return si;
    }

    private static boolean isCacheHit(String name)
    {
        return BLOCK_CACHE != null && BLOCK_CACHE.containsKey(name);
    }

    public ScriptInstance loadScript(String name, String code, Hashtable env) throws YourCodeSucksException
    {
        if (isCacheHit(name)) {
            //System.out.println("BLOCK CACHE HIT FOR: " + name);
            return loadScript(name, (Block) BLOCK_CACHE.get(name), env);
        } else {
            Parser temp = new Parser(code);
            temp.parse();

            if (BLOCK_CACHE != null)
                BLOCK_CACHE.put(name, temp.getRunnableBlock());

            return loadScript(name, temp.getRunnableBlock(), env);
        }
    }

    public ScriptInstance loadScript(String name, InputStream stream) throws YourCodeSucksException, IOException
    {
        return loadScript(name, stream, null);
    }

    public ScriptInstance loadScript(String name, InputStream stream, Hashtable env) throws YourCodeSucksException, IOException
    {
        if (isCacheHit(name)) {
            stream.close();
            return loadScript(name, "", env);
        }

        StringBuffer code = new StringBuffer("");

        BufferedReader in = new BufferedReader(getInputStreamReader(stream));
        String s = in.readLine();
        while (s != null) {
            code.append("\n");
            code.append(s);
            s = in.readLine();
        }

        in.close();
        stream.close();

        return loadScript(name, code.toString(), env);
    }

    /**
     * Loads the specified script file
     */
    public ScriptInstance loadScript(String fileName) throws IOException, YourCodeSucksException
    {
        return loadScript(new File(fileName), null);
    }

    /**
     * Loads the specified script file, uses the specified hashtable for the environment
     */
    public ScriptInstance loadScript(String fileName, Hashtable env) throws IOException, YourCodeSucksException
    {
        return loadScript(new File(fileName), env);
    }

    /**
     * Loads the specified script file, uses the specified hashtable for the environment
     */
    public ScriptInstance loadScript(File file, Hashtable env) throws IOException, YourCodeSucksException
    {
        return loadScript(file.getAbsolutePath(), new FileInputStream(file), env);
    }

    /**
     * Loads the specified script file
     */
    public ScriptInstance loadScript(File file) throws IOException, YourCodeSucksException
    {
        return loadScript(file.getAbsolutePath(), new FileInputStream(file), null);
    }

    /**
     * unload a script
     */
    public void unloadScript(String filename)
    {
        unloadScript((ScriptInstance) scripts.get(filename));
    }

    /**
     * unload a script
     */
    public void unloadScript(ScriptInstance script)
    {
        // clear the block cache of this script...
        if (BLOCK_CACHE != null) {
            //System.out.println("Removing: " + script.getName() + " from BLOCK_CACHE");
            BLOCK_CACHE.remove(script.getName());
        }

        //
        // remove script from our loaded scripts data structure
        //
        loadedScripts.remove(script);
        scripts.remove(script.getName());

        //
        // the script must always be set to unloaded first and foremost!
        //
        script.setUnloaded();

        //
        // tell bridges script is going bye bye
        //
        Iterator i = bridgess.iterator();
        while (i.hasNext()) {
            Loadable temp = (Loadable) i.next();
            temp.scriptUnloaded(script);
        }

        i = bridgesg.iterator();
        while (i.hasNext()) {
            Loadable temp = (Loadable) i.next();
            temp.scriptUnloaded(script);
        }
    }

    /**
     * A convienence method to determine the set of scripts to "unload" based on a passed in set of scripts that are currently
     * configured.  The configured scripts are compared to the loaded scripts.  Scripts that are loaded but not configured are
     * determined to be in need of unloading.  The return Set contains String objects of the script names.  The passed in Set is
     * expected to be the same thing (a bunch of Strings).
     */
    public Set getScriptsToUnload(Set configured)
    {
        Set unload, loaded;
        unload = new LinkedHashSet();

        // scripts that are currently loaded and active...
        loaded = scripts.keySet();

        // scripts that need to be unloaded...
        unload.addAll(loaded);
        unload.removeAll(configured);

        return unload;
    }

    /**
     * A convienence method to determine the set of scripts to "load" based on a passed in set of scripts that are currently
     * configured.  The configured scripts are compared to the loaded scripts.  Scripts that are configured but not loaded
     * are determined to be in need of loading.  The return Set contains String objects of the script names.  The passed in
     * Set is expected to be the same thing (a bunch of Strings).
     */
    public Set getScriptsToLoad(Set configured)
    {
        Set load, loaded;
        load = new LinkedHashSet();

        // scripts that are currently loaded and active...
        loaded = scripts.keySet();

        // scripts that need to be unloaded...
        load.addAll(configured);
        load.removeAll(loaded);

        return load;
    }

    /**
     * Java by default maps characters from an 8bit ascii file to an internal 32bit unicode representation.  How this mapping is done is called a character set encoding.  Sometimes this conversion can frustrate scripters making them say "hey, I didn't put that character in my script".  You can use this option to ensure sleep disables charset conversions for scripts loaded with this script loader
     */
    public void setCharsetConversion(boolean b)
    {
        disableConversions = !b;
    }

    public boolean isCharsetConversions()
    {
        return !disableConversions;
    }

    protected boolean disableConversions = false;
    private static CharsetDecoder decoder = null;
    private String charset = null;

    public String getCharset()
    {
        return charset;
    }

    /**
     * If charset conversion is enabled and charset is set, then the stream will be read using specified charset.
     *
     * @param charset The name of a supported {@link java.nio.charset.Charset </code>charset<code>}
     */
    public void setCharset(String charset)
    {
        this.charset = charset;
    }

    private InputStreamReader getInputStreamReader(InputStream in)
    {
        if (disableConversions) {
            if (decoder == null)
                decoder = new NoConversion();

            return new InputStreamReader(in, decoder);
        }

        if (charset != null) {
            try {
                return new InputStreamReader(in, charset);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return new InputStreamReader(in);
    }

    /**
     * Java likes to convert characters inside of a loaded script into something else.  This prevents that if the app
     * developer chooses to flag that option
     */
    private static class NoConversion extends CharsetDecoder
    {
        public NoConversion()
        {
            super(null, 1.0f, 1.0f);
        }

        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out)
        {
            int mark = in.position();
            try {
                while (in.hasRemaining()) {
                    if (!out.hasRemaining())
                        return CoderResult.OVERFLOW;

                    int index = (int) in.get();
                    if (index >= 0) {
                        out.put((char) index);
                    } else {
                        index = 256 + index;
                        out.put((char) index);
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            }
            finally { in.position(mark); }
        }
    }
}
