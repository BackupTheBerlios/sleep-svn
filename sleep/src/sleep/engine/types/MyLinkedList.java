package sleep.engine.types;

import java.io.Serializable;
import java.util.*;

import sleep.runtime.*;

public class MyLinkedList extends AbstractSequentialList implements Cloneable, Serializable, List
{
   private class MyListIterator implements ListIterator, Serializable
   {
      protected int       index; 
      protected int       start;
      protected ListEntry current;
      protected int       modCountCheck = modCount;

      public void checkSafety()
      {
         if (modCountCheck != modCount)
         {
            throw new ConcurrentModificationException("@array changed during iteration");
         }
      }

      public MyListIterator(ListEntry entry, int index)
      {
         this.index   = index;
         this.start   = index;
         current      = entry;
      }

      public void add(Object o)
      {
         checkSafety();

         /* add the new element after the current element */
         current = current.addAfter(o);

         /* increment the list so that the next element returned is
            unaffected by this call */
         index++;

         modCountCheck++;
      }

      public boolean hasNext()
      {
         return index != size;
      }

      public boolean hasPrevious()
      {
         return index != 0;
      }

      public Object next()
      {
         checkSafety();
         current = current.next();
         index++;
         return current.element();
      }

      public Object previous()
      {
         checkSafety();
         current = current.previous();
         index--;
         return current.element();
      }

      public int nextIndex()
      {
         return index;
      }

      public int previousIndex()
      {
         return index - 1;
      }

      public void remove()
      {
         if (current == header)
         {
            throw new IllegalStateException("list is empty");
         }

         checkSafety();
         current = current.remove().previous();

         index--;

         modCountCheck++;
      }

      public void set(Object o)
      {
         if (current == header)
         {
            throw new IllegalStateException("list is empty");
         }

         checkSafety();
         current.setElement(o);
      }
   }

   int size = 0;
   private ListEntry header;

   /* fields used by sublists */
   private ListEntry    boundaryPrevious;
   private ListEntry    boundaryNext;
   private MyLinkedList parentList;

   public int size()
   {
      return size;
   }

   private MyLinkedList(MyLinkedList plist, ListEntry begin, ListEntry end, int _size)
   {
      parentList = plist;
      modCount = parentList.modCount;
      header = new NormalListEntry(SleepUtils.getScalar("[:header:]"), null, null);

      boundaryPrevious = begin;
      boundaryNext     = end;
      
/*      if (begin instanceof ListEntryWrapper || end instanceof ListEntryWrapper)
      {
         System.out.println("This is a problem!");
         Thread.dumpStack();
         System.exit(0);
      } */


      ListEntryWrapper head = new ListEntryWrapper(begin);
      ListEntryWrapper tail = new ListEntryWrapper(end);

      /* set the entries into the header */
      head.check();
      tail.check();

      size = _size;
   }

   public MyLinkedList()
   {
      header = new NormalListEntry(SleepUtils.getScalar("[:HEADER:]"), null, null);
      header.setNext(header);
      header.setPrevious(header);
   }   

   public List subList(int beginAt, int endAt)
   { 
      checkSafety();

      ListEntry begin;
      ListEntry end;

      begin = header.next();

      int x = 0;
      while (x < beginAt)
      {
         begin = begin.next();
         x++;
      }

      if (endAt == size)
      {
         end = header.previous();
      }
      else
      {
         end = begin;

         while (x < endAt)
         {
            end = end.next();
           x++;
         } 
 
         end = end.previous();
      }

      while (begin instanceof ListEntryWrapper)
      {
         begin = ((ListEntryWrapper)begin).parent;
      }

      while (end instanceof ListEntryWrapper)
      {
         end = ((ListEntryWrapper)end).parent;
      }

      return new MyLinkedList(parentList == null ? this : parentList, begin, end, (endAt - beginAt));
   }

   public ListIterator listIterator(int index)
   {
      ListEntry entry = header;

      if (index < 0 || index > size)
         throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);

      if (index < (size / 2))
      {
         for (int x = 0; x < index; x++)
         {
            entry = entry.next();
         }
      }
      else
      {
         entry = entry.previous();
         for (int x = size; x > index; x--)
         {
            entry = entry.previous();
         }
      }

      return new MyListIterator(entry, index); 
   }

   // code for the ListEntry //

   private interface ListEntry extends Serializable
   {
      public ListEntry remove();
      public ListEntry addBefore(Object o);
      public ListEntry addAfter(Object o);

      public ListEntry next();
      public ListEntry previous();

      public void setNext(ListEntry entry);
      public void setPrevious(ListEntry entry);

      public Object element();
      public void setElement(Object o);
   }

   public void checkSafety()
   {
      if (parentList != null && modCount != parentList.modCount)
      {
         throw new ConcurrentModificationException("parent @array changed after &sublist creation");
      }
   }

   private class ListEntryWrapper implements ListEntry
   {
      public ListEntry parent;

      public ListEntryWrapper check()
      {
         if (parent == boundaryPrevious)
         {
            header.setNext(this);
         }

         if (parent == boundaryNext)
         {
            header.setPrevious(this);
         }

         return this;
      }

      public ListEntryWrapper(ListEntry _parent)
      {
         parent = _parent;

       /*  if (parent instanceof ListEntryWrapper)
         {
             System.out.println("Parent is a wrapper");
             Thread.dumpStack();
             System.exit(0);
         }

         if (parent == header)
         {
             System.out.println("Parent is the header!!!");
             Thread.dumpStack();
             System.exit(0);
         } */
      }

      public ListEntry remove()
      {
         checkSafety();

         ListEntry temp = parent.remove();

         size--;
         modCount++;

         if (parent == boundaryPrevious)
         {
            boundaryPrevious = temp;
         }

         if (parent == boundaryNext)
         {
            boundaryNext = temp;
         }

         ListEntryWrapper r = new ListEntryWrapper(temp);
         return r.check();
      }

      public ListEntry addBefore(Object o)
      {
         checkSafety();

         ListEntry temp = parent.addBefore(o);

         size++;
         modCount++;

         if (parent == boundaryPrevious)
         {
            boundaryPrevious = temp;
         }

         ListEntryWrapper r = new ListEntryWrapper(temp);
         return r.check();
      }

      public ListEntry addAfter(Object o)
      {
         checkSafety();

         ListEntry temp = parent.addAfter(o);

         size++;
         modCount++;

         if (parent == boundaryNext)
         {
            boundaryNext = temp;
         }

         ListEntryWrapper r = new ListEntryWrapper(temp);
         return r.check();
      }

      public void setNext(ListEntry entry)
      {
      }

      public void setPrevious(ListEntry entry)
      {
      }

      public Object element()
      {
         return parent.element();
      }

      public void setElement(Object o)
      {
         parent.setElement(o);
      }

      public ListEntry next()
      {
         checkSafety();

         if (parent == boundaryNext)
         {
            return header;
         }

/*         if (parent instanceof ListEntryWrapper)
         {
             System.err.println("next() ARGH!!!!!!!!!!! " + parent);
         } */

         ListEntryWrapper r = new ListEntryWrapper(parent.next());
         return r;
      }

      public ListEntry previous()
      {
         checkSafety();

         if (parent == boundaryPrevious)
         {
            return header;
         }

/*         if (parent instanceof ListEntryWrapper)
         {
             System.err.println("previous() ARGH!!!!!!!!!!! " + parent);
         }
         
         if (parent.previous() instanceof ListEntryWrapper)
         {
             System.err.println(".. " + header.element() + " and whatever");
         } */

         ListEntryWrapper r = new ListEntryWrapper(parent.previous());
         return r;
      }
   }

   private class NormalListEntry implements ListEntry
   {
      public Object element;
      public ListEntry previous;
      public ListEntry next;

      public NormalListEntry(Object _element, ListEntry _previous, ListEntry _next)
      {
         element  = _element;
         previous = _previous;
         next     = _next;

         if (previous != null)
         {
            previous.setNext(this);
         }

         if (next != null)
         {
            next.setPrevious(this);
         }
      }

      public void setNext(ListEntry entry) 
      {
         next = entry;
      }

      public void setPrevious(ListEntry entry)
      {
         previous = entry;
      }

      public ListEntry next()
      {
         return next;
      }

      public ListEntry previous()
      {
         return previous;
      }

      public ListEntry remove()
      {
         ListEntry prev = previous();
         ListEntry nxt  = next();

         nxt.setPrevious(prev);
         prev.setNext(nxt);

         size--;
         modCount++;
         return nxt;
      }

      public void setElement(Object o)
      {
         element = o;
      }

      public Object element()
      {
         return element;
      }

      public ListEntry addBefore(Object o)
      {
         ListEntry temp = new NormalListEntry(o, this.previous, this);

         size++;
         modCount++;

         return temp;
      }

      public ListEntry addAfter(Object o)
      {
         ListEntry temp = new NormalListEntry(o, this, this.next);

         size++;
         modCount++;

         return temp;
      }

      public String toString()
      {
         StringBuffer buffer = new StringBuffer(":[" + element() + "]:");

         if (this == header)
         {
             buffer = new StringBuffer(":[HEADER]:");
         }

         ListEntry entry = this.previous();
         while (entry != header)
         {
            buffer.insert(0, "[" + entry.element() + "]-> ");
            entry = entry.previous();
         }
         
         entry = this.next();
         while (entry != header)
         {
            buffer.append(" ->[" + entry.element() + "]");
            entry = entry.next();
         }

         return buffer.toString();
      }
   }
}
