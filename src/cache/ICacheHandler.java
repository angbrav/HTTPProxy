package cache;

import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface ICacheHandler {

	/*
	 * Check if the HttpResponse object is cachable based on Http specification
	 * rules. Should be called before add method.
	 */
	boolean isCachable(HttpResponse response);

	/*
	 * Add new HttpResponse object into the cache url parameter required as a
	 * cache object key
	 */
	HttpResponse add(String url, HttpResponse response);

	/*
	 * Return HttpResponse from cache based on url parameter
	 */
	HttpResponse get(String url);

	/*
	 * Return HttpResponse from cache based on HttpRequest parameter
	 */
	HttpResponse get(HttpRequest request);

	/*
	 * Check if the object with url key exists
	 */
	boolean exists(String url);

	/*
	 * Check if the object with url key exists and is valid based on server
	 * response validation. All times are in seconds
	 */
	boolean existsAndValid(String url);

	/*
	 * Check if the object with HttpRequest url key exists and is valid based on
	 * server response validation and request is not PUT, POST and DELETE method
	 */
	boolean existsAndValid(HttpRequest request);

	/*
	 * Delete object with url key from cache
	 */
	void invalidate(String url);

	/*
	 * Delete all objects from cache
	 */
	void invalidateAll();

	/*
	 * Return number of successful access to the object. Used for management
	 * control.
	 */
	long returnAccessNumber(String url);

	/*
	 * Return size of the stored entity in bytes. Used for management control.
	 */
	long returnObjectSize(String url);

	/*
	 * Return age of the stored object in seconds. Used for management control.
	 */
	long returnObjectAge(String url);

	/*
	 * Return the number of objects stored in the cache. Used for management
	 * control.
	 */
	int returnObjectsNumber();

	/*
	 * Return all objects keys stored in the cache. Used for management control.
	 */
	List<String> returnObjectsKeys();

	/*
	 * Delete objects that are not valid or not accessed last Configuration.cacheObjectRefreshTime seconds
	 */
	boolean refreshCache();
	
	/*
	 * return status about all objects in the cache: String that contains url, age, size and hits number for every object
	 */
	String returnObjectsStatus();

}
