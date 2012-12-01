package processes;

/*
 * Http Proxy Server
 * 
 * Authors:
 * Manuel Bravo Gestoso
 * Alejandro Tomsic
 * Strahinja Lazetic
 */

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Date;
import java.util.Locale;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.util.EntityUtils;

import cache.CacheHandler;
import cache.ICacheHandler;
import chord.ChordHandler;

import com.maxmind.geoip.LookupService;

import config.Configuration;
import config.ConfigurationLoader;

public class HttpProxyServer {

	private final static CacheHandler cache = CacheHandler.getInstance();
	private final static ChordHandler chord = ChordHandler.getInstance();
	private static String me;

	public static String getPublicIP() {

		String IP = "";

		try {

			java.net.URL URL1 = new java.net.URL("http://www.whatismyip.org/");
			URLConnection Conn = URL1.openConnection();
			java.io.InputStream InStream = Conn.getInputStream();
			java.io.InputStreamReader Isr = new java.io.InputStreamReader(
					InStream);
			java.io.BufferedReader Br = new java.io.BufferedReader(Isr);
			IP = Br.readLine();
		} catch (Exception e) {
		}
		return (IP);
	}

	public static void main(String[] args) throws Exception {
		String PublicIP = getPublicIP();
		ConfigurationLoader.loadConfiguration();
		Configuration.managementAddress = args[0];
		// me ="192.168.1.75";
		me = PublicIP;
		String myCountry;
		LookupService cl = new LookupService(Configuration.dbfile,
				LookupService.GEOIP_MEMORY_CACHE);
		myCountry = cl.getCountry(PublicIP).getName();
		// myCountry = "Nigeria";
		if (args.length == 1) {
			chord.createRing(me + ":" + Configuration.chordPort);
		} else {
			chord.joinRing(me + ":" + Configuration.chordPort, args[1] + ":"
					+ Configuration.chordPort);
		}

		Thread t = new RequestListenerThread(Configuration.proxyPort, cache,
				chord, myCountry);
		t.setDaemon(false);
		t.start();
		Thread cacheRefresher = new CacheRefresher();
		cacheRefresher.start();
		Thread aliveMessanger = new AliveMessanger(me);
		aliveMessanger.start();
	}

	static class HttpHandler implements HttpRequestHandler {

		private ChordHandler chord;
		private String myCountry;
		private ICacheHandler cache;

		public HttpHandler(CacheHandler ca, ChordHandler cho, String mc) {
			super();
			cache = ca;
			chord = cho;
			myCountry = mc;
		}

		public void handle(final HttpRequest request,
				final HttpResponse response, final HttpContext context)
				throws HttpException, IOException {
			System.out.println("cache size: " + cache.returnObjectsNumber());

			String target = request.getRequestLine().getUri();
			if (request.containsHeader("Management-Message")) {
				if (request.getFirstHeader("Management-Message").getValue()
						.equals("FLUSH_CACHE")) {
					System.out.println("FLUSH_CACHE received!");
					cache.invalidateAll();
				} else if (request.getFirstHeader("Management-Message")
						.getValue().equals("STATUS_REQUEST")) {
					System.out.println("STATUS_REQUEST received!");
					String entityContent = cache.returnObjectsStatus();
					HttpEntity entity = new StringEntity(entityContent);
					response.addHeader("Date", DateUtils.formatDate(new Date()));
					response.addHeader("Server", "HttpComponents/1.1");
					response.addHeader("Content-Length",
							String.valueOf(entity.getContentLength()));
					response.addHeader("Content-Type", entity.getContentType()
							.getValue());
					response.setEntity(entity);

				} else if (request.getFirstHeader("Management-Message")
						.getElements()[0].getName().equals("DELETE_ENTRY")) {
					String objectUrl = request.getFirstHeader(
							"Management-Message").getElements()[0].getValue();
					System.out.println("DELETE_ENTRY received for url "
							+ objectUrl);
					cache.invalidate(objectUrl);
				}
				return;
			}
			System.out.println("Target:" + target);
			if ((request.getRequestLine().getUri().equals("/") || request
					.getRequestLine()
					.getUri()
					.startsWith(
							"http://" + me + ":" + Configuration.proxyPort
									+ "/"))
					&& !request.containsHeader("emdc")) {

				String addresses = "";
				String header = "<html><head><title>Available proxy servers</title></head><body>"
						+ "<center><h2>The available proxy servers are listed below:</h2>"
						+ "<table width=\"40%\" border=\"1\"><tr><th>IP Address</th><th>Country</th></tr>";
				String footer = "</table></center>" + "</body></html>";
				addresses = addresses + header;

				String[] nodes = chord.getFingerTable();
				LookupService cl = new LookupService(Configuration.dbfile,
						LookupService.GEOIP_MEMORY_CACHE);
				String myCountry = cl.getCountry(me).getName();
				addresses = addresses + "<tr><td>" + me + "</td><td>"
						+ myCountry + "</td></tr>";
				for (String addressPort : nodes) {
					String address = addressPort.split(":")[0];
					String country = cl.getCountry(address).getName();
					addresses = addresses + "<tr><td>" + address + "</td><td>"
							+ country + "</td></tr>";
				}
				addresses = addresses + footer;
				HttpEntity entity = new StringEntity(addresses, "text/html",
						"UTF-8");

				response.addHeader("Date", DateUtils.formatDate(new Date()));
				response.addHeader("Server", "HttpComponents/1.1");
				response.addHeader("Content-Length",
						String.valueOf(entity.getContentLength()));
				response.addHeader("Content-Type", entity.getContentType()
						.getValue());
				response.setEntity(entity);
				EntityUtils.consume(entity);

				return;
			}

			String method = request.getRequestLine().getMethod()
					.toUpperCase(Locale.ENGLISH);
			if (!method.equals("GET") && !method.equals("HEAD")
					&& !method.equals("POST")) {
				throw new MethodNotSupportedException(method
						+ " method not supported");
			}

			HttpUriRequest httpRequest = null;

			LookupService cl = new LookupService(Configuration.dbfile,
					LookupService.GEOIP_MEMORY_CACHE);

			if (cache.existsAndValid(request)) { // 06.12. changed from target
													// to request!
				// checking local cache
				// response.setEntity(entity);
				HttpResponse resp = cache.get(target); // 06.12. changed to use
														// cache.get() only once
				response.setEntity(resp.getEntity());
				copyHeaders(resp, response);
			} else {
				if (request.containsHeader("emdc")) { // Checking if I am the
					// second proxy
					HttpClient httpclient = new DefaultHttpClient();
					if (method.equals("POST")) {
						httpRequest = new HttpPost(request.getFirstHeader(
								"emdc").getValue());
						HttpEntity entity = null;
						if (request instanceof HttpEntityEnclosingRequest) {
							entity = ((HttpEntityEnclosingRequest) request)
									.getEntity();
						}

						((HttpPost) httpRequest).setEntity(entity);

					} else {
						httpRequest = new HttpGet(request
								.getFirstHeader("emdc").getValue());
					}
					HttpResponse response2;
					HttpEntity entity;
					try {
						response2 = httpclient.execute(httpRequest);
						entity = response2.getEntity();
						if (entity != null) {
							copyHeaders(response2, response);
							response.setEntity(entity);
							String respURL = chord.RetrieveRespURL(request
									.getFirstHeader("emdc").getValue());
							if (respURL.equals(me)) { // I am the
														// responsible
														// so
								// I ll store it in my
								// cache

								if (cache.isCachable(response2)) { // 06.12.
																	// added
																	// conditional
																	// caching
									HttpResponse resp = cache.add(request
											.getFirstHeader("emdc").getValue(),
											response2);
									response.setEntity(resp.getEntity());
								}
							}
						}
					} catch (Exception e) {
						System.out.println(e);
					}
				} else { // I am de client proxy
					String respURL = chord.RetrieveRespURL(target);
					System.out.println(target);
					String country = cl.getCountry(respURL).getName();

					String host;
					if (request.containsHeader("Host")) {
						host = request.getFirstHeader("Host").getValue();
						if (host.contains(":")) {
							host = host.split(":")[0];
						}
					} else {
						host = target.split("://")[1];
						if (host.contains("/")) {
							host = host.split("/")[0];
						}
					}
					String WScountry = cl.getCountry(host).getName();

					// country = "Argentina";
					if (!(country.equals(myCountry))
							&& !(country.equals(WScountry))
							&& !me.equals(respURL)) {
						HttpClient httpclient = new DefaultHttpClient();
						if (method.equals("POST")) {
							httpRequest = new HttpPost("http://" + respURL
									+ ":" + Configuration.proxyPort);
							HttpEntity entity = null;
							if (request instanceof HttpEntityEnclosingRequest) {
								entity = ((HttpEntityEnclosingRequest) request)
										.getEntity();
							}

							((HttpPost) httpRequest).setEntity(entity);

						} else {
							httpRequest = new HttpGet("http://" + respURL + ":"
									+ Configuration.proxyPort);
						}
						httpRequest.addHeader("emdc", target);
						HttpResponse response2;
						HttpEntity entity;
						try {
							response2 = httpclient.execute(httpRequest);
							entity = response2.getEntity();
							if (entity != null) {
								if (cache.isCachable(response2)) { // 06.12.
																	// added
																	// conditioanl
																	// caching
									response.setEntity(cache.add(target,
											response2).getEntity());
								} else {
									response.setEntity(entity);
								}

								copyHeaders(response2, response);
								System.out.println("Stop!");
							}
						} catch (Exception e) {
							System.out.println(e);
						}
						// send httprequest to responsible with header(emdc,get)
					} else { // the resp cannot handle the request so I will
						// send the request to another peer
						int i = 0;
						boolean found = false;
						String Nodes[] = chord.getFingerTable();
						while ((i < Nodes.length) && (!found)) {
							country = cl.getCountry(Nodes[i]).getName();
							// country = "Argentina";
							if (!country.equals(myCountry)
									&& !country.equals(WScountry)) {
								// System.out.println("my country " + myCountry
								// + " WS: "+ WScountry + " resp: " + country);
								HttpClient httpclient = new DefaultHttpClient();
								if (method.equals("POST")) {
									httpRequest = new HttpPost("http://"
											+ Nodes[i]);
									HttpEntity entity = null;
									if (request instanceof HttpEntityEnclosingRequest) {
										entity = ((HttpEntityEnclosingRequest) request)
												.getEntity();
									}

									((HttpPost) httpRequest).setEntity(entity);

								} else {
									httpRequest = new HttpGet("http://"
											+ Nodes[i]);
								}
								httpRequest.addHeader("emdc", target);
								HttpResponse response2;
								HttpEntity entity;
								try {
									response2 = httpclient.execute(httpRequest);
									entity = response2.getEntity();
									if (entity != null) {
										// response.setHeader("emdc",
										// "forward");
										if (cache.isCachable(response2)) { // 06.12.
																			// added
																			// conditioanl
																			// caching
											HttpResponse resp = cache.add(
													target, response2);
											copyHeaders(resp, response);
											response.setEntity(resp.getEntity());
										} else {
											response.setEntity(entity);
											copyHeaders(response2, response);
										}

										// chord.InsertKey(target, new
										// CacheObject(response));
									}
								} catch (Exception e) {
									System.out.println(e);
								}
								found = true;
							} else {
								System.out
										.println("I cannot find another peer");
							}
							i++;
						}
					}
				}
			}
		}

		public void copyHeaders(HttpResponse responseFrom,
				HttpResponse responseTo) {

			responseTo.setStatusLine(responseFrom.getStatusLine());

			for (Header header : responseFrom.getAllHeaders()) {
				if (header.getName().equals("Transfer-Encoding")
						&& header.getValue().equals("chunked")) {
					continue;
				}
				// System.out.println(header.getName() + ": "
				// + header.getValue());
				responseTo.setHeader(header);
			}
		}
	}

	static class RequestListenerThread extends Thread {

		private final ServerSocket serversocket;
		private final HttpParams params;
		private final HttpService httpService;
		private CacheHandler cache;
		private ChordHandler chord;
		private String myCountry;

		public RequestListenerThread(int port, CacheHandler cache,
				ChordHandler chord, String myCountry) throws IOException {
			this.cache = cache;
			this.chord = chord;
			this.myCountry = myCountry;
			this.serversocket = new ServerSocket(port);
			this.params = new SyncBasicHttpParams();
			this.params
					.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
					.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
							8 * 1024)
					.setBooleanParameter(
							CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
					.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
					.setParameter(CoreProtocolPNames.ORIGIN_SERVER,
							"HttpComponents/1.1");

			// Set up the HTTP protocol processor
			HttpProcessor httpproc = new ImmutableHttpProcessor(
					new HttpResponseInterceptor[] { /*
													 * new ResponseDate(), new
													 * ResponseServer(), new
													 * ResponseContent(), new
													 * ResponseConnControl()
													 */

					});

			// Set up request handlers
			HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
			reqistry.register("*", new HttpHandler(this.cache, this.chord,
					this.myCountry));

			// Set up the HTTP service
			this.httpService = new HttpService(httpproc,
					new DefaultConnectionReuseStrategy(),
					new DefaultHttpResponseFactory(), reqistry, this.params);
		}

		public void run() {
			System.out.println("Listening on port "
					+ this.serversocket.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					// Set up HTTP connection
					Socket socket = this.serversocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					System.out.println("Incoming connection from "
							+ socket.getInetAddress() + "to "
							+ socket.getLocalAddress());
					conn.bind(socket, this.params);

					// Start worker thread
					Thread t = new WorkerThread(this.httpService, conn);
					t.setDaemon(true);
					t.start();
				} catch (InterruptedIOException ex) {
					break;
				} catch (IOException e) {
					System.err
							.println("I/O error initialising connection thread: "
									+ e.getMessage());
					break;
				}
			}
		}
	}

	static class WorkerThread extends Thread {

		private final HttpService httpservice;
		private final HttpServerConnection conn;

		public WorkerThread(final HttpService httpservice,
				final HttpServerConnection conn) {
			super();
			this.httpservice = httpservice;
			this.conn = conn;
		}

		public void run() {
			System.out.println("New connection thread");
			HttpContext context = new BasicHttpContext(null);
			try {
				while (!Thread.interrupted() && this.conn.isOpen()) {
					this.httpservice.handleRequest(this.conn, context);
				}
			} catch (ConnectionClosedException ex) {
				System.err.println("Client closed connection: "
						+ ex.getMessage());
				// ex.printStackTrace();
			} catch (IOException ex) {
				System.err.println("I/O error: " + ex.getMessage());
				// ex.printStackTrace();
			} catch (HttpException ex) {
				System.err.println("Unrecoverable HTTP protocol violation: "
						+ ex.getMessage());
				// ex.printStackTrace();
			} finally {
				try {
					this.conn.shutdown();
				} catch (IOException ignore) {
				}
			}
		}

	}

}