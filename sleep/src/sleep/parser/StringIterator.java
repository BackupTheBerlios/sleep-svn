package sleep.parser;

import java.util.*;

public class StringIterator
{
   protected int    position = 0;
   protected int    lineNo;
   protected char[] text;
   protected String texts;
   protected int    begin    = 0;

   public StringIterator(String text)
   {
      this(text, 0);
   }

   public String toString()
   {
      return texts;
   }

   public StringIterator(String _text, int _lineNo)
   {
      texts  = _text;
      text   = _text.toCharArray();
      lineNo = _lineNo;
   }

   public boolean hasNext()
   {
      return position < text.length;
   }

   public int getLineNumber()
   {
      return lineNo;
   }
  
   public String getEntireLine()
   {
      int temp = position;
      while (temp < text.length && text[temp] != '\n')
      {
         temp++;
      }

      return texts.substring(begin, temp);
   }

   public int getLineMarker()
   {
      return position - begin;
   }

   public boolean isNextChar(char n)
   {
      return hasNext() && text[position] == n;
   }

   public char next()
   {
      char current = text[position];

      if (position > 0 && text[position - 1] == '\n')
      {
         lineNo++;
         begin = position;
      }

      position++;

      return current;
   }

   public void mark()
   {
      mark1.add(0, new Integer(position));
      mark2.add(0, new Integer(lineNo));
   }

   public String reset()
   {
      Integer temp1 = (Integer)mark1.removeFirst();
      Integer temp2 = (Integer)mark2.removeFirst();
//      position = temp1.intValue();
//      lineNo   = temp2.intValue();

      return texts.substring(temp1.intValue(), position);
   }

   protected LinkedList mark1 = new LinkedList();
   protected LinkedList mark2 = new LinkedList();
 

   public static void main(String args[])
   {
      StringIterator temp = new StringIterator(args[0]);
      
      StringBuffer blah = new StringBuffer();
      while (temp.hasNext())
      {
         char t = temp.next();
         blah.append(t);
         if (t == '\n')
         {
            System.out.print(temp.getLineNumber() + ": " + blah.toString());
            blah = new StringBuffer();
         }
      }
   }
}
