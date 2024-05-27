
package myio;

import java.io.*;


public class FileIO extends java.lang.Object {

    /** Creates new OpenInputFile */
	public FileIO() {
	}


	public static BufferedWriter openOutputFile(String filename){
		BufferedWriter stream = null;

		try{
			stream = new BufferedWriter(new FileWriter(filename));

			System.out.println();
		}
		catch (FileNotFoundException e){System.out.println(e);}
		catch (StreamCorruptedException  e){System.out.println(e);}
		catch (IOException e){System.out.println(e);}

		return stream;
	}

	public static BufferedWriter openOutputFile(File file){
		BufferedWriter stream = null;

		try{
			stream = new BufferedWriter(new FileWriter(file));

			System.out.println();
		}
		catch (FileNotFoundException e){System.out.println(e);}
		catch (StreamCorruptedException  e){System.out.println(e);}
		catch (IOException e){System.out.println(e);}

		return stream;
	}

	
	public static BufferedReader openInputFile(String filename){
		BufferedReader stream = null;

		try{
			stream = new BufferedReader(new FileReader(filename));

		}
		catch (FileNotFoundException e){System.out.println(e);}
		catch (IOException e){System.out.println(e);}

		return stream;
	}
}
