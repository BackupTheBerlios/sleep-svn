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

         if (size == 0)
         {
            current = current.addBefore(o);
         }
         else
         {
            /* add the new element after the current element */
            current = current.addAfter(o);
 
            /* increment the list so that the next element returned is
               unaffected by this call */
            index++;
         }
        
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
   private ListEntry    boundaryLeft;
   private ListEntry    boundaryRight;
   private MyLinkedList parentList;

   public int size()
   {
      return size;
   }

   private MyLinkedList(MyLinkedList plist, ListEntry begin, ListEntry end, int _size)
   {
      parentList = plist;
      modCount = parentList.modCount;

      /* setup the header */
      header = new SublistHeaderEntry();

      boundaryLeft = begin;
      boundaryRight = end;
      
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

      ListEntry begin = getAt(beginAt).next();  /* included */
      ListEntry end = getAt(endAt); /* not included */

      /* we want each sublist to consist of a direct view into the parent... operations on other
         sublists will fail if the parent is changed through some other sublist, this makes things
         efficient and safe */
        
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

/*   public boolean add(Object o)
   {
      ListEntry entry = header;
      header.previous().addAfter(o);
      return true;
   } */

   /** get an object from the linked list */
   public Object get(int index)
   {
      if (size == 0)
         throw new IndexOutOfBoundsException("list is empty");

      return getAt(index).next().element();
   }

   /** returns the entry at the specified index */
   private ListEntry getAt(int index)
   {
      if (index < 0 || index > size)
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);

      ListEntry entry = header;

      if (index == size)
      {
         return header.previous();
      }
      else if (index < (size / 2))
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

      return entry;
   }

   public ListIterator listIterator(int index)
   {
      return new MyListIterator(getAt(index), index);
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

   private class SublistHeaderEntry implements ListEntry
   {
      public SublistHeaderEntry() { }

      public ListEntry remove() 
      {
         throw new UnsupportedOperationException("remove");
      }

      public ListEntry previous() 
      {
         return new ListEntryWrapper(boundaryRight);
      }

      public ListEntry next() 
      {
         return new ListEntryWrapper(boundaryLeft);
      }

      public void setNext(ListEntry e)
      {
         throw new UnsupportedOperationException("setNext");
      }

      public void setPrevious(ListEntry e)
      {
         throw new UnsupportedOperationException("setPrevious");
      }

      public ListEntry addBefore(Object o)
      {
         return previous().addAfter(o);
      }

      public ListEntry addAfter(Object o)
      {
         return next().addBefore(o);
      }

      public Object element()
      {
         return SleepUtils.getScalar("[:header:]");
      }

      public void setElement(Object o)
      {
         throw new UnsupportedOperationException("setElement");
      }
   }

   private class ListEntryWrapper implements ListEntry
   {
      public ListEntry parent;

      public ListEntryWrapper(ListEntry _parent)
      {
         parent = _parent;
      }

      public ListEntry remove()
      {
         checkSafety();

         ListEntry temp = parent.remove();

         size--;
         modCount++;

         if (size == 0)
         {
            boundaryLeft = boundaryLeft.previous();
            boundaryRight = boundaryRight.next();

//            System.out.println("Did the remove() magic: " + boundaryLeft.element() + " and " + boundaryRight.element());

            return header;
         }
         else
         {
            if (parent == boundaryLeft)
            {
                boundaryLeft = temp;
            } 

            if (parent == boundaryRight)
            {
                boundaryRight = temp;
            }
         }

         return new ListEntryWrapper(temp);
      }

      public ListEntry addBefore(Object o)
      {
         checkSafety();

         ListEntry temp = parent.addBefore(o);

         size++;
         modCount++;

         if (size == 1)
         {
            boundaryLeft = temp;
            boundaryRight = temp;
         }
         else if (parent == boundaryLeft)
         {
            boundaryLeft = temp;
         }

         return new ListEntryWrapper(temp);
      }

      public ListEntry addAfter(Object o)
      {
         checkSafety();

         ListEntry temp = parent.addAfter(o);

         size++;
         modCount++;

         if (size == 1)
         {
            boundaryLeft = temp;
            boundaryRight = temp;
         }
         else if (parent == boundaryRight)
         {
            boundaryRight = temp;
         }
	
         return new ListEntryWrapper(temp);
      }

      public void setNext(ListEntry entry)
      {
         System.err.println("setNext");
      }

      public void setPrevious(ListEntry entry)
      {
         System.err.println("setPrevious");
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

         if (parent == boundaryRight)
         {
            return new ListEntryWrapper(boundaryLeft);
         }

         ListEntryWrapper r = new ListEntryWrapper(parent.next());
         return r;
      }

      public ListEntry previous()
      {
         checkSafety();

         if (parent == boundaryLeft)
         {
            return new ListEntryWrapper(boundaryRight);
         }

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
