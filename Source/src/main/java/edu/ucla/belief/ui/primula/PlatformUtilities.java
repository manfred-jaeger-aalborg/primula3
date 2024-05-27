package edu.ucla.belief.ui.primula;

/** @author keith cascio
	@since 20060602 */
public class PlatformUtilities
{
	public enum Platform{
		windows ( true,  false, ".exe" ),
		linux,
		mac,
		solaris,
		unix,
		unknown ( false, false, ".unknown" );

		public boolean isUnix(){
			return myFlagUnix;
		}

		public boolean isWindows(){
			return myFlagWindows;
		}

		public String getExecutableExtension(){
			return myExecutableExtension;
		}

		private Platform(){
			this( false, true, "" );
		}

		private Platform( boolean win, boolean unix, String executableExtension ){
			Platform.this.myFlagWindows         = win;
			Platform.this.myFlagUnix            = unix;
			Platform.this.myExecutableExtension = executableExtension;
		}

		private boolean myFlagWindows = false;
		private boolean myFlagUnix    = false;
		private String  myExecutableExtension;
	}

	public Platform getPlatform(){
		return myPlatform;
	}

	public static PlatformUtilities getInstance(){
		if( INSTANCE == null ) INSTANCE = new PlatformUtilities();
		return INSTANCE;
	}

	private void init(){
		String osname = System.getProperty( "os.name" ).toLowerCase();
		if( osname == null ){
			myPlatform = Platform.unknown;
			return;
		}

		if( osname.startsWith( "windows" ) ){
			myPlatform = Platform.windows;
			return;
		}

		if( osname.indexOf( "mac" ) >= 0 ){
			myPlatform = Platform.mac;
			return;
		}

		if( osname.indexOf( "linux" ) >= 0 ){
			myPlatform = Platform.linux;
			return;
		}

		if( osname.indexOf( "solaris" ) >= 0 ){
			myPlatform = Platform.solaris;
			return;
		}

		if( osname.replaceAll( "\\s+", "" ).indexOf( "osx" ) >= 0 ){
			myPlatform = Platform.mac;
			return;
		}

		myPlatform = Platform.unix;
	}

	private PlatformUtilities(){
		init();
	}

	private static PlatformUtilities INSTANCE;

	private Platform myPlatform;
}
