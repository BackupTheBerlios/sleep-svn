 ------- -------------------- ---------   ---------------  ------- ---------
Sleep - 2.1 beta 1 - README   
-  ------------- -------- -   ---------------- --       -    -     -     - - -

"You got the language, all you need now is the O'Reilly book".  That is
what my friend Luke said to me upon closing out a weekend of possessed
coding.

A weekend of possessed coding that yielded a scripting language.  Sleep is
the Scripting Language for Easily Enhancing Programs.  It is a small 
scripting language with perl inspired syntax implemented in Java.

Sleep is primarily a glue language.  Included with Sleep is everything 
required to embed and extend the language to fit into new applications and
problem domains.  

The core of sleep was produced in one weekend in early April of 2002.  
I just wanted something I could integrate into an application I was 
writing.  Specifically I wanted something I could integrate the way *I 
wanted* to integrate it.  Since then, Sleep has been revised, expanded, 
and stabilized.  

Sleep Project Homepage: http://sleep.hick.org/

 ------- -------------------- ---------   ---------------  ------- ---------
Documentation
-  ------------- -------- -   ---------------- --       -    -     -     - - -

An open source project just wouldn't be a good open source project without
a documentation deficiency.  Sleep is no different. 

Contained in the docs/ directory:

sleeplang.pdf
   A tutorial on the sleep language from an end-users perspective.  Covers
   the basic language constructs, built in functions, and relevant 
   background information.  Fun for the whole family.  Now in its 6th 
   revision. 

sleepguide.pdf
   This document is a guide to integrating the sleep language into your
   application.  Part 1 of this document is an absolute must read if you plan 
   to integrate sleep into your application.

common.htm
   Common embedding techniques cheat sheet.  A very short example oriented 
   document on common techniques for embedding sleep in your application.
   Includes information such as how to catch and process a syntax error,
   load a script, call a function etc.  To truly get in depth though you
   need to read sleepguide.pdf.  

console.txt
   A quick reference on the sleep console.  The sleep console is a simple
   console for interacting with the sleep library.

parser.htm
   A little overview on how the sleep parser works for the curious.

You also have the option of generating the JavaDoc API's for the sleep 
language.  I recommend either generating these or downloading them from 
the sleep website.  Javadoc is your friend when working with this project.
     
 ------- -------------------- ---------   ---------------  ------- ---------
Build Instructions
-  ------------- -------- -   ---------------- --       -    -     -     - - -

You will need Apache Ant to compile this source code. I use version 1.6.0. 
Ant is easy to install and is available at http://ant.apache.org 

To compile sleep:

[raffi@beardsley ~/sleep]$ rm -rf bin
[raffi@beardsley ~/sleep]$ ant all

If you made any changes or just want to make sure nothing got broken you can
run a series of regression tests on sleep.

[raffi@beardsley ~/sleep/tests]$ perl test.pl

To Build JavaDoc for Sleep (dumped to the docs/api/ directory):

[raffi@beardsley ~/sleep]$ ant docs

To build full JavaDoc for Sleep (all classes):

[raffi@beardsley ~/sleep]$ ant docs-full

To launch the sleep console (see docs/console.txt for more information):

[raffi@beardsley ~/sleep/bin]$ java -jar sleep.jar

To launch a sleep script from the command line:

[raffi@beardsley ~/sleep/bin]$ java -jar sleep.jar filename.sl

When sleep scripts are run directly on the command line, arguments are
placed into the @ARGV variable.

 ------- -------------------- ---------   ---------------  ------- ---------
Feedback
-  ------------- -------- -   ---------------- --       -    -     -     - - -

Feedback is always welcome.  Suggestions/comments/questions can directed to
me via email: raffi@hick.org

I do respond to most feedback.  For example, this message posted to a public
forum:

   Subject: re: better name would be STOP

   Dear User,
   Thank you for your suggestion on a new name for sleep.  I feel many 
   users recognize the name sleep and changing names at this point might
   do more harm than good.  Thank you for your suggestion and glad to hear
   that you are enjoying the language.

                    -- Raffi

   Subject: better name would be STOP
   From:    NothingPersonal (62.132.1.121) 
   Date:    September 27, 2004 at 10:08:34

   Please. Just STOP. stop stop stop. don't inflict yet another half-baked, 
   il-concieved abortion of a scripting language onto unsuspecting 
   developers. Yes, you had fun writing it, but the only niche it fills is 
   the one in your head that renders you incapable of mastering any of the 
   other numerous scripting options already available to you.

   I know I'm supposed to be nice to you because hey who are you really 
   bothering, and selection of the fittest will surely see sleep sleep it's 
   way to a quiet and peaceful death. But while on it's way to it's 
   inevitable demise, sleep is bound to take with it some hapless developers, 
   who will in turn inflict it on numerous doomed projects, and all that 
   spells misery for all concerned.

   While I'm at it, I also have to point out that the very last thing I want 
   to read when browsing a language reference is pathetic, self-important 
   humour.

   I'm urging you to do the honourable thing. stop sleeping, and wake the 
   f*** up. Take down your cargo-cult website (it even has a wiki! it's 
   _bound_ to be a success!) and spend (alot) more time researching your 
   foundations before embarking on such follies again.


 ------- -------------------- ---------   ---------------  ------- ---------
Legal Garbage
-  ------------- -------- -   ---------------- --       -    -     -     - - -

Sleep is (c) 2002, 2003, 2004, 2005, 2006 Raphael Mudge (raffi@hick.org).  All 
of the source and somehow the documentation are released under the GNU Lesser
Public License.  

The scripts and library files supplied as input to or produced as output 
from the Sleep library do not automatically fall under the copyright of the
Sleep project, but belong to whomever generated them, and may be sold 
commercially, and may be aggregated with this library.

Java or sleep subroutines supplied by you and linked into this library 
shall not be considered part of this library.

See license.txt for more information.

