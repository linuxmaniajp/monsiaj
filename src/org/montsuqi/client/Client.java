package org.montsuqi.client;

import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.montsuqi.util.Logger;
import org.montsuqi.util.OptionParser;

public class Client implements Runnable {

	static final int PORT_GLTERM = 8000;
	private static final String CLIENT_VERSION = "0.0"; //$NON-NLS-1$

	private int portNumber;
	private String host;
	private String cache;
	private String user;
	private String pass;

	private String currentApplication;

	// if USE_SSL
	//private String key;
	//private String cert;
	private boolean useSSL;
	private boolean verify;
	//private String CApath;
	//private String CAfile;

	private Protocol protocol;
	private Logger logger;

	public String getCacheFileName(String name) {
		String sep = System.getProperty("file.separator"); //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		buf.append(cache);
		buf.append(sep);
		buf.append(host);
		buf.append(sep);
		buf.append(portNumber);
		buf.append(sep);
		buf.append(name);
		return buf.toString();
	}
	
	private String[] parseOptions(String[] args) {
		OptionParser options = new OptionParser();

		options.add("port", Messages.getString("Client.port_number"), PORT_GLTERM); //$NON-NLS-1$ //$NON-NLS-2$
		options.add("host", Messages.getString("Client.host_name"), "localhost"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		options.add("cache", Messages.getString("Client.cache_directory"), "cache"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		options.add("user", Messages.getString("Client.user_name"), System.getProperty("user.name")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		options.add("pass", Messages.getString("Client.password"), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		options.add("v1", Messages.getString("Client.use_protocol_version_1"), true); //$NON-NLS-1$ //$NON-NLS-2$
		options.add("v2", Messages.getString("Client.use_protocol_version_2"), true); //$NON-NLS-1$ //$NON-NLS-2$

		options.add("useSSL", "SSL", false); //$NON-NLS-1$ //$NON-NLS-2$
		//options.add("key", "���ե����?̾(pem)", null);
		//options.add("cert", "������ե����?̾(pem)", null);
		//options.add("ssl", "SSL��Ȥ�", false);
		options.add("verifypeer", Messages.getString("Client.verify_peer"), false); //$NON-NLS-1$ //$NON-NLS-2$
		//options.add("CApath", "CA������ؤΥѥ�", null);
		//options.add("CAfile", "CA������ե����?", null);

		String[] files = options.parse(Client.class.getName(), args);

		portNumber = ((Integer)options.getValue("port")).intValue(); //$NON-NLS-1$
		host = (String)options.getValue("host"); //$NON-NLS-1$
		cache = (String)options.getValue("cache"); //$NON-NLS-1$
		user = (String)options.getValue("user"); //$NON-NLS-1$
		pass = (String)options.getValue("pass"); //$NON-NLS-1$

		useSSL = ((Boolean)options.getValue("useSSL")).booleanValue(); //$NON-NLS-1$

		if (useSSL) {
			//key = (String)options.getValue("key");
			//cert = (String)options.getValue("cert");
			//useSSL = ((Boolean)options.getValue("ssl")).booleanValue();
			verify = ((Boolean)options.getValue("verifypeer")).booleanValue(); //$NON-NLS-1$
			//CApath = options.getValue("CApath");
			//CAfile = options.getValue("CAfile");
		}

		return files;
	}

	public Client(String[] args) {
		logger = Logger.getLogger(Client.class);

		String[] files = parseOptions(args);

		if (files.length > 0) {
			currentApplication = files[0];
		} else {
			currentApplication = "demo"; //$NON-NLS-1$
		}
		
		Socket s = null;
		try {
			s = new Socket(host, portNumber);
			// if USE_SSL
			if (useSSL) {
				SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
				SSLSocket ssl = (SSLSocket)factory.createSocket(s, host, portNumber, true);
				/* key, cert, capath, cafile */
				ssl.setNeedClientAuth(verify);
				s = ssl;
			}
			logger.info("socket: {0}", s);
			
			protocol = new Protocol(this, s);
			logger.info(protocol.toString());
		} catch (IOException e) {
			logger.fatal(e);
			System.exit(0);
		}
		if (protocol == null) {
			logger.fatal(Messages.getString("Client.cannot_connect")); //$NON-NLS-1$
		}
	}
		

	public void run() {
		try {
			logger.info("sendConnect({0}, {1}, {2})...", new Object[] { user, pass, currentApplication });
			protocol.sendConnect(user, pass, currentApplication);
			logger.info("done.");
			while (true) {
				logger.info("checkScreens(true)");
				protocol.checkScreens(true);
				logger.info("getScreenData()");
				protocol.getScreenData();
			}
		} catch (IOException e) {
			logger.fatal(e);
		}
	}
		
	public static void main(String[] args) {
		showBannar();
		Client client = new Client(args);
		client.run();
		client.exitSystem();
	}

	public void finalize() {
		if (protocol != null) {
			exitSystem();
		}
	}
	
	public void exitSystem() {
		try {
			protocol.sendPacketClass(PacketClass.END);
			protocol.close();
			System.exit(0);
		} catch (IOException e) {
			logger.fatal(e);
		}
	}

	protected static void showBannar() {
		System.out.println(Messages.getString("Client.banner_1") + CLIENT_VERSION); //$NON-NLS-1$
		System.out.println(Messages.getString("Client.banner_2")); //$NON-NLS-1$
		System.out.println(Messages.getString("Client.banner_3")); //$NON-NLS-1$
	}
}
