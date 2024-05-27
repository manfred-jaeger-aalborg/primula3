package edu.ucla.belief.ui.primula;

import edu.ucla.belief.ui.primula.Preferences.Key;
import edu.ucla.belief.ace.Settings;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.EnumMap;

/** @author keith cascio
	@since 20030506 */
public class PreferencesHandler extends DefaultHandler
{
	public PreferencesHandler()
	{
	}

	public void setPreferences( Preferences prefs )
	{
		myPreferences = prefs;
		if( myPaths     != null ) myPaths.clear();
		if( myAceValues != null ) myAceValues.clear();
	}

	public void startDocument() throws SAXException
	{
		mySubHandler        = theRootCheckHandler;
		myCharactersHandler = theCharactersNoop;
	}

	public void startElement( String uri,String localName,String qName,Attributes attributes ) throws SAXException
	{
		mySubHandler.startElement( uri, localName, qName, attributes );
	}

	public void endElement( String uri,String localName,String qName ) throws SAXException
	{
		mySubHandler.endElement( uri, localName, qName );
	}

	public void characters(	char[] ch,int start,int length) throws SAXException
	{
		myCharactersHandler.characters( ch, start, length );
	}

	public void endDocument() throws SAXException
	{
		mySubHandler        = null;
		myCharactersHandler = null;
	}






	public final ElementHandler theRootCheckHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( qName.equals( Key.PrimulaPreferences.name() ) ) mySubHandler = theValidRootHandler;
			else throw new SAXException( "invalid root" );
		}

		public void endElement(String uri,String localName,String qName) throws SAXException
		{}
	};

	public final ElementHandler theValidRootHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException {
			//System.out.println( "theValidRootHandler.startElement( "+qName+" )" );
			for( Key key : Key.values() ){
				if( qName.equals( key.name() ) ){
					initAccumulator( key );
					return;
				}
			}
			if( qName.equals( Settings.Key.aceSettings.name() ) ){
				//System.out.println( "    mySubHandler = theAceHandler;" );
				mySubHandler = theAceHandler;
			}
		}

		public void endElement(String uri,String localName,String qName) throws SAXException{
			disableAccumulator();
		}
	};

	public final ElementHandler theAceHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException {
			for( Settings.Key key : Settings.Key.values() ){
				if( qName.equals( key.name() ) ){
					initAccumulator( key );
					return;
				}
			}
		}

		public void endElement(String uri,String localName,String qName) throws SAXException{
			if( qName.equals( Settings.Key.aceSettings ) ) mySubHandler = theValidRootHandler;
			else disableAccumulator();
		}
	};

	public final CharactersHandler theCharactersNoop = new CharactersHandler(){
		public void characters(	char[] ch,int start,int length) throws SAXException
		{}
	};

	private StringBuilder newAccumulator( int size ){
		myCharactersHandler  = theCharactersAccumulator;
		return myAccumulator = new StringBuilder( size );
	}

	private StringBuilder setAccumulator( StringBuilder accumulator ){
		if( (myAccumulator = accumulator) == null ) throw new IllegalArgumentException( "illegal null accumulator" );
		else{
			accumulator.setLength(0);
			myCharactersHandler = theCharactersAccumulator;
		}
		return accumulator;
	}

	private StringBuilder initAccumulator( StringBuilder accumulator, int size ){
		return ( accumulator == null ) ? newAccumulator( size ) : setAccumulator( accumulator );
	}

	private StringBuilder initAccumulator( Key key ){
		StringBuilder acc = myPaths.get( key );
		if( acc == null ) myPaths.put( key, acc = newAccumulator( 128 ) );
		else setAccumulator( acc );
		return acc;
	}

	private StringBuilder initAccumulator( Settings.Key key ){
		StringBuilder acc = myAceValues.get( key );
		if( acc == null ) myAceValues.put( key, acc = newAccumulator( 128 ) );
		else setAccumulator( acc );
		return acc;
	}

	private void disableAccumulator(){
		myCharactersHandler = theCharactersNoop;
		myAccumulator       = null;
	}

	private CharactersHandler theCharactersAccumulator = new CharactersHandler()
	{
		public void characters(	char[] ch, int start, int length ) throws SAXException{
			myAccumulator.append( ch, start, length );
		}
	};

	private StringBuilder myAccumulator;

	public void parse( File infile ) throws IOException
	{
		if( mySAXParser == null )
		{
			try{
				mySAXParser = SAXParserFactory.newInstance().newSAXParser();
			}catch( ParserConfigurationException e ){
				System.err.println( "Warning: PreferencesHandler.parse() caused " + e );
				return;
			}catch( SAXException e ){
				System.err.println( "Warning: PreferencesHandler.parse() caused " + e );
				return;
			}
		}

		try{
			mySAXParser.parse( infile, this );
		}catch( SAXException e ){
			System.err.println( "Warning: PreferencesHandler.parse() caused " + e );
			return;
		}

		for( Key key : myPaths.keySet() ){
			myPreferences.setFile( key, new File( myPaths.get( key ).toString() ) );
		}

		if( !myAceValues.isEmpty() ){
			Settings settings = myPreferences.getACESettings();
			for( Settings.Key key : myAceValues.keySet() ){
				key.set( settings, myAceValues.get( key ) );
			}
		}
	}

	protected Preferences       myPreferences;
	protected SAXParser         mySAXParser;
	protected ElementHandler    mySubHandler;
	protected CharactersHandler myCharactersHandler;

	private Map<Key,         StringBuilder> myPaths     = new EnumMap<Key,         StringBuilder>( Key.class );
	private Map<Settings.Key,StringBuilder> myAceValues = new EnumMap<Settings.Key,StringBuilder>( Settings.Key.class );
}
