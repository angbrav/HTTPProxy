package processes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import config.Configuration;

public class AliveMessanger extends Thread {

	private String inetAddress;

	public AliveMessanger(String inetAddress) {
		this.inetAddress = inetAddress;
	}

	public void run() {
		while (true) {
			try {

				HttpClient httpclient = new DefaultHttpClient();
				Header headerMessage = new BasicHeader("Peer-Message", "ALIVE");
				Header headerAddress = new BasicHeader("Peer-Address",
						inetAddress);
				URI uri = URIUtils.createURI("http",
						Configuration.managementAddress,
						Configuration.proxyListenerPort, null, null, null);
				HttpGet httpget = new HttpGet(uri);
				httpget.addHeader(headerMessage);
				httpget.addHeader(headerAddress);
				httpclient.execute(httpget);

			} catch (HttpHostConnectException e) {
				System.out
						.println("Cannot connect to the management server on address "
								+ Configuration.managementAddress
								+ ":"
								+ Configuration.proxyListenerPort);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					sleep(Configuration.aliveMessageInterval * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
