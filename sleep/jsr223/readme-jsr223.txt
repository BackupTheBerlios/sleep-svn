The source code for Sleep JSR 223 support is separate to allow the primary codebase to develop
without a dependence on Java 1.6.  To satisfy several requests this support is included in the
main Sleep distribution.

This JSR223 codebase requires Java 1.6 and Apache Ant 1.7.0 (http://ant.apache.org/)

To (re)compile this code use:

[raffi@beardsley ~/sleep]$ cd jsr223
[raffi@beardsley ~/sleep/jsr223]$ ant

That's it.  This will produce a sleep-engine.jar file.  Go ahead and execute the normal compilation
of Sleep in the toplevel directory.  The contents of jsr223/sleep-engine.jar will automatically
be included in the sleep.jar file.

Running a script doesn't get any easier than this:

[raffi@beardsley ~/sleep] jrunscript -cp sleep.jar -l sleep -f file.sl

Some notes about the integration:

- The GLOBAL_SCOPE Bindings of the ScriptContext are treated as Sleep globals.  The Binding values
  are dumped into an object scalar with no conversion.

- The getErrorWriter(), getReader(), and getWriter() values of ScriptContext are virtually ignored.
  1) Sleep doesn't speak Reader/Writer language and 2) they seem to act as blackholes when 
  scripts are executed with JRunScript.  

- All scripts share variables and environment.  This was as much a necessity as a convienence.

That's about it.  The integration is minimal but it seems to work.  If you encounter any bugs
don't hesitate to contact me.

-- Raphael (rsmudge@gmail.com)


Special thanks to A. Sundararajan (sundararajana@dev.java.net) at Sun for the development of the
Sleep 2.0 script engine.  
