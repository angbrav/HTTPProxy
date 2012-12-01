package processes;

import config.Configuration;
import cache.CacheHandler;
import cache.ICacheHandler;

public class CacheRefresher extends Thread {

	public void run() {
		ICacheHandler cacheHandler = CacheHandler.getInstance();
		while (true) {
			try {
				sleep(Configuration.refreshCacheInterval * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cacheHandler.refreshCache();

		}
	}
}
