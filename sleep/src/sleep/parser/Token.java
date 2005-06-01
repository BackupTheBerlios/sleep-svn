/* 
   SLEEP - Simple Language for Environment Extension Purposes 
 .-------------------------.
 | sleep.parser.util.Token |__________________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/
 
   Description: 

   Documentation: To see the entire concrete syntax of the SLEEP language
     handled by this parser view the file docs/bnf.txt.

   Changes:

   * This software is distributed under the artistic license, see license.txt
     for more information. *
   
 |____________________________________________________________________________|
 */

package sleep.parser;

/** as much as possible this is a String with a line number associate with it (aka hint) */
public class Token
{
   protected String term;
   protected int    hint;
   protected int    marker;
 
   public Token(String term, int hint)
   {
      this(term, hint, -1);
   }

   public Token(String _term, int _hint, int _marker)
   { 
      term   = _term;
      hint   = _hint;
      marker = _marker;
   }

   public String toString()
   {
      return term;
   }

   public int getMarkerIndex()
   {
      return marker;
   }

   public Token copy(String text)
   {
      return new Token(text, getHint());
   }

   public String getMarker()
   {
      if (marker > -1)
      {
         StringBuffer temp = new StringBuffer();
         for (int x = 0; x < (marker - 1); x++)
         {
            temp.append(" ");
         }
         temp.append("^");

         return temp.toString();
      }

      return null;
   }

   public int getHint()
   {
      return hint;
   }
}
