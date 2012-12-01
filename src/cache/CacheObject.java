package cache;

import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class CacheObject {

	private HttpEntity entity;
	private Date responseDate;
	private long maxAge;
	private Date expires;
	private Date lastModified;
	private long objectSize;
	private long accessNumber;
	private Header[] headers;
	private Date lastAccessed;

	public CacheObject(HttpEntity entity, Date responseDate, long maxAge,
			Date expires, Date lastModified, Header[] headers, Date lastAccessed, long objectSize) {
		super();
		this.entity = entity;
		this.responseDate = responseDate;
		this.maxAge = maxAge;
		this.expires = expires;
		this.lastModified = lastModified;
		this.headers = headers;		
		this.lastAccessed = lastAccessed;
		this.objectSize = objectSize;
	}

	public HttpEntity getEntity() {
		return entity;
	}

	public void setEntity(HttpEntity entity) {
		this.entity = entity;
	}

	public Date getResponseDate() {
		return responseDate;
	}

	public void setResponseDate(Date responseDate) {
		this.responseDate = responseDate;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	public Date getExpires() {
		return expires;
	}

	public void setExpires(Date expires) {
		this.expires = expires;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public long getObjectSize() {
		return objectSize;
	}

	public void setObjectSize(long objectSize) {
		this.objectSize = objectSize;
	}

	public long getAccessNumber() {
		return accessNumber;
	}

	public void setAccessNumber(long accessNumber) {
		this.accessNumber = accessNumber;
	}

	public void increaseAccessNumber() {
		this.accessNumber++;
	}

	public Header[] getHeaders() {
		return headers;
	}

	public void setHeaders(Header[] headers) {
		this.headers = headers;
	}

	public Date getLastAccessed() {
		return lastAccessed;
	}

	public void setLastAccessed(Date lastAccessed) {
		this.lastAccessed = lastAccessed;
	}
	
	

	

}
