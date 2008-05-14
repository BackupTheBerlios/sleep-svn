The source code for Sleep JSR 223 support is separate to allow the primary codebase to develop
without a dependence on Java 1.6.  To satisfy several requests this support is included in the
main Sleep distribution.

This JSR223 codebase requires Java 1.6 and Apache Ant 1.7.0 (http://ant.apache.org/)

To (re)compile this code use:

[raffi@beardsley ~/sleep]$ cd jsr223
[raffi@beardsley ~/sleep/jsr223]$ ant

That's it.  This will produce a sleep-engine.jar file.  Go ahead and execute the normal compilation
of Sleep in the toplevel directory.  The contents of jsr223/sleep-engine.jar will automatically
be included in the Sleep distribution.

I'm not a consumer of the JSR223 API so there may be unintentional bugs in this factory.  Even
though I am not a consumer I still want to deliver the best product I can to you.  If you find
something that seems broken do not hesitate to contact me and I will work with you to resolve it.

-- Raphael (rsmudge@gmail.com)


Special thanks to A. Sundararajan (sundararajana@dev.java.net) at Sun for the development of the
Sleep 2.0 script engine.  This code is mostly his.  I've modified it a little to support Sleep 2.1.

