package config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConfigurationLoader {

	public static void main(String[] args) {
		loadConfiguration();
	}

	public static void loadConfiguration() {

		try {
			InputStream is = ConfigurationLoader.class
					.getResourceAsStream("/conf.xml");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();

			NodeList cacheMemorySizes = doc
					.getElementsByTagName("cacheMemorySize");
			Node cacheMemorySize = (Node) cacheMemorySizes.item(0);
			Configuration.cacheMemorySize = Long.parseLong(cacheMemorySize
					.getTextContent());

			NodeList cacheObjectRefreshTimes = doc
					.getElementsByTagName("cacheObjectRefreshTime");
			Node cacheObjectRefreshTime = (Node) cacheObjectRefreshTimes
					.item(0);
			Configuration.cacheObjectRefreshTime = Long
					.parseLong(cacheObjectRefreshTime.getTextContent());

			NodeList refreshCacheIntervals = doc
					.getElementsByTagName("refreshCacheInterval");
			Node refreshCacheInterval = (Node) refreshCacheIntervals.item(0);
			Configuration.refreshCacheInterval = Long
					.parseLong(refreshCacheInterval.getTextContent());

			NodeList proxyListenerPorts = doc
					.getElementsByTagName("proxyListenerPort");
			Node proxyListenerPort = (Node) proxyListenerPorts.item(0);
			Configuration.proxyListenerPort = Integer
					.parseInt(proxyListenerPort.getTextContent());

			NodeList proxyPorts = doc.getElementsByTagName("proxyPort");
			Node proxyPort = (Node) proxyPorts.item(0);
			Configuration.proxyPort = Integer.parseInt(proxyPort
					.getTextContent());

			NodeList chordPorts = doc.getElementsByTagName("chordPort");
			Node chordPort = (Node) chordPorts.item(0);
			Configuration.chordPort = Integer.parseInt(chordPort
					.getTextContent());
			
			NodeList managementAddresss = doc
					.getElementsByTagName("managementAddress");
			Node managementAddress = (Node) managementAddresss.item(0);
			Configuration.managementAddress = managementAddress
					.getTextContent();

			NodeList aliveMessageIntervals = doc
					.getElementsByTagName("aliveMessageInterval");
			Node aliveMessageInterval = (Node) aliveMessageIntervals.item(0);
			Configuration.aliveMessageInterval = Integer
					.parseInt(aliveMessageInterval.getTextContent());

			NodeList dbfiles = doc.getElementsByTagName("dbfile");
			Node dbfile = (Node) dbfiles.item(0);			
			URL url = ConfigurationLoader.class.getResource("/"+dbfile.getTextContent());	
			Configuration.dbfile = url.getPath();

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
