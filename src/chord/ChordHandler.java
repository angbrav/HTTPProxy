package chord;
import java.net.MalformedURLException;

import config.Configuration;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

import cache.*;

public class ChordHandler{

	private static Chord chord;
	private static ChordHandler chordHandler;

	public ChordHandler(){
		
	}
	public static synchronized ChordHandler getInstance() {
		if (chordHandler == null) {
			chordHandler = new ChordHandler();
		}
		return chordHandler;
	}

	public synchronized void createRing(String me) {
		de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		URL localURL=null;
		try{
			localURL=new URL(protocol + "://"+me+"/");
		}
		catch(MalformedURLException e){
			throw new RuntimeException(e);
		}
		chord=new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		try{
			chord.create(localURL);
		}
		catch (ServiceException e){
			throw new RuntimeException("Could not create Chord DHT!!", e);
		}
		System.out.print("Soy el nodo "+ chord.toString());
	}

	public synchronized void joinRing(String me,String boot) {
		de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		URL localURL = null;
		try {
			localURL=new URL(protocol + "://"+me+"/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		URL bootstrapURL = null;
		try {
			bootstrapURL = new URL(protocol + "://" + boot + "/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		chord = new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		try {
			chord.join(localURL, bootstrapURL);
		} catch (ServiceException e) {
			throw new RuntimeException("Could not join DHT!", e);
		}
		System.out.print("i am connected to the ring: " + chord.toString()
				+ "with ID:  ");

//		InsertKey("HOLA");
//		System.out.print("Inserted key!");
//
//		InsertKey("HOLA");
//		System.out.print("Inserted key!");
//
//		System.out
//				.print("String= " + RetrieveKey("HOLA") + RetrieveKey("CHAU"));
//
//		RemoveKey("HOLA");
//		System.out.print("Removed key!");

		//ChordImpl chord1 = new ChordImpl();
		// chord1.create(localURL);
	}

	public synchronized void InsertKey(String url) {

		try {
			chord.insert(new StringKey(url), url);
		} catch (ServiceException e) {
			System.out.print("Could not insert element");
		}
	}

	
	public synchronized String getChordID(){
		return chord.getID().toString();
	}
	
	public synchronized CacheObject RetrieveKey(String url) {
		
		CacheObject result=null;
		StringKey myKey = new StringKey(url);
		try {
			result= (CacheObject)chord.retrieve(myKey);

		} catch (ServiceException e) {
			System.out.print("Could not retrieve element");
		}
		return result;
	}
//	public synchronized void removeObject(String url, CacheObject object){
//		try {
//			chord.remove(new StringKey(url), object);
//		} catch (ServiceException e) {
//			System.out.print("Could not remove element");
//		}
//	}
	
//	public synchronized CacheObject getIfExistAndValid(String url){
//		Set<Serializable> set = null;
//		CacheObject[] array = null;
//		try {
//			set =chord.retrieve(new StringKey(url));
//			if ((set!=null)&&!(set.isEmpty())){
//				array = (CacheObject[])set.toArray();
//				if (array[0].isValid()){
//					removeObject(url, array[0]);
//					array[0].setHits(array[0].getHits()+1);
//					InsertKey(url, array[0]);
//					return array[0];
//				}else{
//					removeObject(url, array[0]);
//				}
//			}	
//		} catch (ServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
//	}
	
	public synchronized String[] getFingerTable(){
		ChordImpl impl = (ChordImpl) chord;
		String nodes[] = null;
		String table = impl.printFingerTable();
		nodes = table.split("\n");
		for (int i=0; i< nodes.length;i++){
			System.out.println("planetlab de mierda: "+nodes[i]);
			String tmpNode=nodes[i].split("://")[1];
			nodes[i]=tmpNode.substring(0, tmpNode.length()-2);
			nodes[i]=nodes[i].split(":")[0]+":"+Integer.toString(Configuration.proxyPort);
		}
		return nodes;
	}
	
	public synchronized String RetrieveRespURL(String KeyString){
		ChordImpl impl = (ChordImpl) chord;
		StringKey myKey = new StringKey(KeyString);
		return impl.retrieveResponsibleURL(myKey).split("://")[1].split(":")[0];
	}
}