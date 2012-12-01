package config;

public class Configuration {
	// memory size of the cache in bytes
	public static long cacheMemorySize = 100000000;	
	//time that cache object can spend without hit in seconds
	public static long cacheObjectRefreshTime = 3600;
	//interval for executing cache refreshment in seconds
	public static long refreshCacheInterval = 3600;
	//proxy server port
	public static int proxyPort = 8081; 
	//management proxy listener port
	public static int proxyListenerPort = 8087;
	//management address
	public static String managementAddress = "localhost";
	//alive message sending inetrval in seconds
	public static int aliveMessageInterval = 120;
	//geolite database file location
	public static String dbfile = "GeoIP.dat";
	public static int chordPort = 8079;
}
