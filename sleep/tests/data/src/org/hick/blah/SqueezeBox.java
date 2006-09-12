package org.hick.blah;

public class SqueezeBox
{
   protected int sq = 33;

   public int squeeze()
   { 
      sq++;
      return sq;
   }

   public void doStuff(double[][] matrix)
   {
      System.out.println("Printing the table:");

      for (int x = 0; x < matrix.length; x++)
      {
         for (int y = 0; y < matrix[x].length; y++)
         {
            System.out.print(matrix[x][y] + "; ");
         } 

         System.out.println("");
      }
   } 
}
