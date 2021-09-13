package edu.ucla.belief.ui.primula;

//import edu.ucla.belief.io.InflibFileFilter;
//import edu.ucla.belief.io.NetworkIO;

//import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/**
	Represents a bundle of software that exists somewhere on the file system, e.g. c:\\dev\\inflib\\compiled or primula.jar
	@author keith cascio
	@since 20040420
*/
public class SoftwareEntity
{
	public final String displayname;
	public final String packagename;// = "RBNgui";
	public final String mainclassname;// = "Primula";
	public final String mainclassnamefull;// = STR_PACKAGENAME_RBNGUI+"."+STR_CLASSNAME_PRIMULA;
	public final String mainclassfilename;// = STR_CLASSNAME_PRIMULA+".class";
	public final String jarfilenamedefault;// = "primula.jar";

	public final String extension;
	public final String targetfilenamedefault;

	private SimpleFileFilter myFileFilterJar;
	private SimpleFileFilter myFileFilterClass;
	private JFileChooser     myJFileChooser;
	private File             myCodeLocation;

	public SoftwareEntity( String displayname, String packagename, String mainclassname, String jarfilenamedefault )
	{
		this.displayname        = displayname;
		this.packagename        = packagename;
		this.mainclassname      = mainclassname;
		this.mainclassnamefull  = packagename + "." + mainclassname;
		this.mainclassfilename  = mainclassname + ".class";
		this.jarfilenamedefault = jarfilenamedefault;

		this.extension             = null;
		this.targetfilenamedefault = null;
	}

	/** Represent a single target file rather than a Java module

		@since 20060602 */
	public SoftwareEntity( String displayname, String extension, String targetfilenamedefault )
	{
		this.displayname           = displayname;
		this.extension             = extension;
		this.targetfilenamedefault = targetfilenamedefault;

		this.packagename        = null;
		this.mainclassname      = null;
		this.mainclassnamefull  = null;
		this.mainclassfilename  = null;
		this.jarfilenamedefault = null;
	}

	/** @since 20060602 */
	public boolean isResolved(){
		return (myCodeLocation != null) && (myCodeLocation.exists());
	}

	/** @since 20060602 */
	public boolean isJava(){
		if(      this.mainclassname         != null ) return true;
		else if( this.targetfilenamedefault != null ) return false;
		else throw new IllegalStateException( "incorrectly configured SoftwareEntity" );
	}

	public String toString(){
		return "(" + super.toString() + ")" + displayname;
	}

	/** @since 20040421 */
	public void setCodeLocation( File fileLocation ){
		myCodeLocation = fileLocation;
	}

	public File getCodeLocation(){
		return myCodeLocation;
	}

	/** @since 20060602 */
	public File browseForMe( Component parent ) throws Exception
	{
		if(      this.mainclassname         != null ) return browseForMyClass(  parent );
		else if( this.targetfilenamedefault != null ) return browseForMyTarget( parent );
		else throw new IllegalStateException( "incorrectly configured SoftwareEntity" );
	}

	/** @since 20060602 */
	public File browseForMyTarget( Component parent ) throws Exception
	{
		//System.out.println( "(SoftwareEntity)"+this+".browseForMyTarget()" );
		JFileChooser chooser = getJFileChooser();
		int ret = chooser.showOpenDialog( parent );
		File target = chooser.getSelectedFile();

		if( (ret == JFileChooser.APPROVE_OPTION) && (target != null) && target.exists() ){
			return (myCodeLocation = target);
		}

		return null;
	}

	public File browseForMyClass( Component parent ) throws Exception
	{
		//System.out.println( "(SoftwareEntity)"+this+".browseForMyClass()" );
		JFileChooser chooser = getJFileChooser();
		int ret = chooser.showOpenDialog( parent );
		File fileMyClass = chooser.getSelectedFile();

		if( (ret == JFileChooser.APPROVE_OPTION) && (fileMyClass != null) && fileMyClass.exists() ){
			if( myFileFilterJar.accept( fileMyClass ) ) return (myCodeLocation = fileMyClass);
			else if( myFileFilterClass.accept( fileMyClass ) || SimpleFileFilter.extractFileNameFromPath( fileMyClass.getPath() ).equals( this.mainclassfilename ) ){
				File dirRoot = getCodeRoot( fileMyClass.getParentFile() );
				if( dirRoot != null ) return (myCodeLocation = dirRoot);
				else throw new RuntimeException( this.mainclassfilename+" must occur in package directory '"+this.packagename+"'." );
			}
			else throw new RuntimeException( "Incorrect file " + fileMyClass.getPath() + ": "+this.mainclassfilename+" or "+this.jarfilenamedefault+" required. " );
		}

		return null;
	}

	/** @since 20060602 */
	public void seed( SoftwareEntity seed ){
		File location = seed.getJFileChooser().getCurrentDirectory();
		getJFileChooser().setCurrentDirectory( location );
	}

	public JFileChooser getJFileChooser()
	{
		if( myJFileChooser == null ){
			//System.out.println( "(SoftwareEntity)"+this+" new JFileChooser" );
			myJFileChooser = new JFileChooser( "." );
			setFilters( myJFileChooser );
			myJFileChooser.setApproveButtonText( "Load" );
			myJFileChooser.setDialogTitle( "Find "+this.displayname );
			if( (myCodeLocation != null) && myCodeLocation.exists() ) myJFileChooser.setCurrentDirectory( myCodeLocation );
		}
		return myJFileChooser;
	}

	/** @since 20060602 */
	private void setFilters( JFileChooser chooser ){
		if(      this.mainclassfilename     != null ){
			chooser.addChoosableFileFilter( myFileFilterJar   = new SimpleFileFilter( new String[]{ ".jar"                 }, this.displayname+" Jar Archive (*.jar)" ) );
			chooser.addChoosableFileFilter( myFileFilterClass = new SimpleFileFilter( new String[]{ this.mainclassfilename }, this.displayname+" Main Class ("+this.mainclassfilename+")" ) );
			chooser.setFileFilter( myFileFilterJar );
		}
		else if( this.targetfilenamedefault != null ){
			chooser.addChoosableFileFilter( myFileFilterJar   = new SimpleFileFilter( new String[]{ this.extension       }, this.displayname+" (*"+this.extension+")" ) );
			chooser.addChoosableFileFilter( myFileFilterClass = new SimpleFileFilter( new String[]{ this.targetfilenamedefault }, this.displayname+" ("+this.targetfilenamedefault+")" ) );
			chooser.setFileFilter( myFileFilterClass );
		}
		myFileFilterClass.setCaseSensitive( true );
	}

	public File getCodeRoot( File dirPackage )
	{
		//System.out.println( "getCodeRoot( "+dirPackage.getPath()+" )" );
		if( (dirPackage == null) || (!dirPackage.exists()) || (!dirPackage.isDirectory()) ) return null;

		String strPathFragment = this.packagename.replace( '.', File.separatorChar );
		//System.out.println( "\t strPathFragment " + strPathFragment );

		if( dirPackage.getPath().endsWith( strPathFragment ) )
		{
			String[] strSplit = this.packagename.split( "\\." );
			File current = dirPackage;
			for( int i=0; (i<strSplit.length) && (current != null); i++ ) current = current.getParentFile();
			//System.out.println( "\t returning " + current.getPath() );
			return current;
		}
		else return null;
	}
}
