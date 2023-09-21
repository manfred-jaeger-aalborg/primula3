/*
* SystemIO.java 
* 
* Copyright (C) 2009 Aalborg University
*
* contact:
* jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package RBNutilities;

import java.io.*;


public class SystemIO extends java.lang.Object {

    /* Use: 
     * BufferedReader theStream = new BufferedReader(new InputStreamReader(System.in));
     */

    public static boolean ynQuestion(String msg, BufferedReader theStream) throws IOException{
        boolean again = true;
        boolean res = false;
        String answer;
        while(again){
            System.out.print(msg+"[y/n]");
            answer = theStream.readLine();
            if(answer.compareToIgnoreCase("y") == 0){
                res = true;
                again = false;
            } else if(answer.compareToIgnoreCase("n") == 0){
                res = false;
                again = false;
            } else {
                System.out.println("You typed '"+answer+"', please choose y or n.");
            }
        }
        return res;
    }


    public static int readInt(String msg, BufferedReader theStream, int min, int max) throws IOException{
        String tmp = "";
        boolean again = true;
        int y = 0;
        while(again){
            System.out.print(msg);
            try{
                tmp = theStream.readLine();
                y = Integer.parseInt(tmp);
                if(y > max || y < min)
                    System.out.println(y+" is out of range. Plaese select a number whithin ["+min+" ,"+max+"].");
                else
                    again = false;
            } catch (NumberFormatException e){
                System.out.println("You typed '"+tmp+"'! That doesn't seem to be a valid number, try again");
                again = true;
            }
        }
        return y;
    }


    public static double readDouble(String msg, BufferedReader theStream, double min, double max) throws IOException{
        boolean again = true;
        double y = 0.0;
        String tmp = "";
        while(again){
            System.out.print(msg);
            try{
                tmp = theStream.readLine();
                y = Double.parseDouble(tmp);
                if(y > max || y < min)
                    System.out.println(y+" is out of range. Plaese select a double whithin ["+min+" ,"+max+"].");
                else
                    again = false;
            } catch (NumberFormatException e){
                System.out.println("You typed '"+tmp+"'! That doesn't seem to be a valid double, try again");
                again = true;
            }
        }
        return y;
    }
}
