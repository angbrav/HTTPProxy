package cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import config.Configuration;

public class CacheHandler implements ICacheHandler {

	/*
	 * check in the documentation: 1. how to construct response from cache,
	 * which headers must be included and which must not be 2. when to
	 * invalidate cache (put, post and delete methods) 3. should we take into
	 * account cache-request directives and conditional requests (e.g. eTag)
	 */

	private static CacheHandler cacheHandler;
	private static ConcurrentMap<String, CacheObject> cache = new ConcurrentHashMap<String, CacheObject>();
	private static long cacheSize;

	private CacheHandler() {
	}

	public static synchronized CacheHandler getInstance() {
		if (cacheHandler == null) {
			cacheHandler = new CacheHandler();
		}
		return cacheHandler;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public HttpResponse add(String url, HttpResponse response) {
		if (cacheSize >= Configuration.cacheMemorySize) {
			if (!refreshCache()) {
				return response;
			}
		}

		long maxAge = -1;
		Date responseDate = null;
		Date expires = null;
		Date lastModified = null;

		if (response.containsHeader("Cache-Control")) {
			for (HeaderElement element : response.getFirstHeader(
					"Cache-Control").getElements()) {
				if (element.getName().equals("max-age")) {
					maxAge = Long.parseLong(element.getValue());
				}
			}
		}
		if (response.containsHeader("Date")) {
			try {
				responseDate = DateUtils.parseDate(response.getFirstHeader(
						"Date").getValue());

			} catch (DateParseException e) {
				e.printStackTrace();
			}
		} else {
			responseDate = new Date();
		}
		if (response.containsHeader("Expires")) {
			try {
				expires = DateUtils.parseDate(response
						.getFirstHeader("Expires").getValue());
			} catch (DateParseException e) {
				e.printStackTrace();
			}
		}
		if (response.containsHeader("Last-Modified")) {
			try {
				lastModified = DateUtils.parseDate(response.getFirstHeader(
						"Last-Modified").getValue());
			} catch (DateParseException e) {
				e.printStackTrace();
			}
		}

		Header[] headers = response.getAllHeaders();

		HttpEntity entity = null;
		long contentLength = 0;
		try {
			if (response.getEntity() != null) {
				entity = new BufferedHttpEntity(response.getEntity());
				contentLength = entity.getContentLength();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CacheObject cacheObject = new CacheObject(entity, responseDate, maxAge,
				expires, lastModified, headers, new Date(), contentLength);

		cache.put(url, cacheObject);
		increaseCacheSize(cacheObject.getObjectSize());

		response.setEntity(entity);

		return response;
	}

	public HttpResponse get(String url) {
		StatusLine line = new BasicStatusLine(new HttpVersion(1, 1), 200, "OK");
		HttpResponse response = new BasicHttpResponse(line);
		CacheObject object = cache.get(url);
		response.setEntity(object.getEntity());
		response.setHeaders(object.getHeaders());
		// should we set manually some other headers, like Date, Expires and
		// Last-modified, max-age?

		object.increaseAccessNumber();
		object.setLastAccessed(new Date());

		return response;
	}

	public HttpResponse get(HttpRequest request) {
		StatusLine line = new BasicStatusLine(new HttpVersion(1, 1), 200, "OK");
		HttpResponse response = new BasicHttpResponse(line);
		String url = request.getRequestLine().getUri();
		CacheObject object = cache.get(url);
		response.setEntity(object.getEntity());
		response.setHeaders(object.getHeaders());
		// should we set manually some other headers, like Date, Expires and
		// Last-modified, max-age?

		object.increaseAccessNumber();
		object.setLastAccessed(new Date());

		return response;
	}

	public boolean exists(String url) {
		return cache.containsKey(url);
	}

	public boolean valid(String url) {
		CacheObject object = cache.get(url);
		long maxAge = object.getMaxAge();
		Date responseDate = object.getResponseDate();
		Date expires = object.getExpires();
		Date lastModified = object.getLastModified();

		long currentAge = (new Date().getTime() - responseDate.getTime()) / 1000;
		if (maxAge != -1) {
			if (maxAge <= currentAge) {
				invalidate(url);
				return false;
			}
		} else if (expires != null) {
			long freshnes = (expires.getTime() - responseDate.getTime()) / 1000;
			if (freshnes <= currentAge) {
				invalidate(url);
				return false;
			}
		} else if (lastModified != null) {
			long pastTime = responseDate.getTime() - lastModified.getTime();
			long currentTime = new Date().getTime();
			if (currentTime > pastTime + pastTime * 0.1) {
				invalidate(url);
				return false;
			}
		}

		return true;
	}

	public boolean existsAndValid(String url) {
		if (!exists(url) || !valid(url)) {
			return false;
		}

		return true;
	}

	public boolean existsAndValid(HttpRequest request) {
		String url = request.getRequestLine().getUri();
		String method = request.getRequestLine().getMethod();
		if (method.equals("POST") || method.equals("PUT")
				|| method.equals("DELETE")) {
			invalidate(url);
			return false;
		}

		if (request.containsHeader("Cache-Control")) { // 06.12 added client cache control validation
			for (HeaderElement element : request
					.getFirstHeader("Cache-Control").getElements()) {
				if (element.getName().equals("no-cache")
						|| element.getName().equals("no-store")
						|| (element.getName().equals("max-age") && element
								.getValue().equals("0"))) {
					invalidate(url);
					return false;
				}
			}
		}

		if (!existsAndValid(url)) {
			invalidate(url);
			return false;
		}

		return true;
	}

	public boolean visited(String url) {
		Date lastAccessed = cache.get(url).getLastAccessed();
		long notAccessedTime = new Date().getTime() / 1000
				- lastAccessed.getTime() / 1000;
		if (notAccessedTime >= Configuration.cacheObjectRefreshTime) {
			return false;
		}

		return true;
	}

	public boolean isCachable(HttpResponse response) {
		// partial content
		if (response.getStatusLine().getStatusCode() == 206) {
			return false;
		}
		if (response.containsHeader("Cache-Control")) {
			for (HeaderElement element : response.getFirstHeader(
					"Cache-Control").getElements()) {
				if (element.getName().equals("no-cache")
						|| element.getName().equals("no-store")
						|| element.getName().equals("private")
						|| (element.getName().equals("max-age") && element
								.getValue().equals("0"))) {
					return false;
				}
			}
		}

		return true;
	}

	public synchronized void invalidate(String url) {
		CacheObject cacheObject = cache.get(url);
		if (cacheObject == null) {
			return;
		}
		long contentLenght = cacheObject.getObjectSize();
		cache.remove(url);
		decreaseCacheSize(contentLenght);
	}

	public synchronized void invalidateAll() {
		cache.clear();
		cacheSize = 0;
	}

	public long returnAccessNumber(String url) {
		return cache.get(url).getAccessNumber();
	}

	public long returnObjectSize(String url) {
		return cache.get(url).getObjectSize();
	}

	public long returnObjectAge(String url) {
		Date responseDate = cache.get(url).getResponseDate();
		return returnObjectAge(responseDate);
	}

	public long returnObjectAge(Date responseDate) {
		return (new Date().getTime() - responseDate.getTime()) / 1000;
	}

	public int returnObjectsNumber() {
		return cache.size();
	}

	@SuppressWarnings("unchecked")
	public List<String> returnObjectsKeys() {
		return (ArrayList<String>) cache.keySet();
	}

	private synchronized void increaseCacheSize(long contentLenght) {
		cacheSize = cacheSize + contentLenght;
	}

	private synchronized void decreaseCacheSize(long contentLenght) {
		cacheSize = cacheSize - contentLenght;
	}

	public synchronized boolean refreshCache() {
		int numberDeleted = 0;
		for (String url : cache.keySet()) {
			if (!valid(url) || !visited(url)) {
				long contentLength = cache.get(url).getObjectSize();
				cache.remove(url);
				numberDeleted++;
				decreaseCacheSize(contentLength);
			}
		}
		if (numberDeleted > 0) {
			return true;
		} else {
			return false;
		}
	}

	public String returnObjectsStatus() {
		String content = "";
		for (String key : cache.keySet()) {
			CacheObject object = cache.get(key);
			content = content + key + " "
					+ returnObjectAge(object.getResponseDate()) + " "
					+ object.getObjectSize() + " " + object.getAccessNumber()
					+ "\n";
		}
		return content;
	}

}
