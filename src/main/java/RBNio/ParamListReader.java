package RBNio;

import java.util.*;
import java.io.File;

import myio.StringOps;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.io.*;

public class ParamListReader {

/*
 *  Reads files of the form:
 *  
 *  <root>
 *	<paramblock params="(paramname1;paramname2)"/>
 *	<paramblock params="(paramname3)"/>
 *	</root>
 * 
 * 
 * 
 */
	

	public String[][] readPList(File f)
	{
		Vector<String[]> prelimres = new Vector<String[]>();
		try{
			SAXReader reader = new SAXReader();


			Document doc = reader.read(f);
			Element root = doc.getRootElement();
			
			
			for (Iterator i = root.elementIterator("paramblock");i.hasNext();){
				Element parblck = (Element) i.next();
				// use , (44) as separator between parameters in list
				String[] parstring = StringOps.stringToStringArray(parblck.attributeValue("params"),44);
				prelimres.add(parstring);
			}
			
		}
		catch (Exception e) {
			System.err.println(e);
		}
		return StringOps.vectorTo2DArray(prelimres);
	}
}
