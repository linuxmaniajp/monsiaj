/*      PANDA -- a simple transaction monitor

Copyright (C) 1998-1999 Ogochan.
              2000-2003 Ogochan & JMA (Japan Medical Association).
              2002-2006 OZAWA Sakuro.

This module is part of PANDA.

		PANDA is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY.  No author or distributor accepts responsibility
to anyone for the consequences of using it or for whether it serves
any particular purpose or works at all, unless he says so in writing.
Refer to the GNU General Public License for full details.

		Everyone is granted permission to copy, modify and redistribute
PANDA, but only under the conditions described in the GNU General
Public License.  A copy of this license is supposed to have been given
to you along with PANDA so you can know your rights and
responsibilities.  It should be in a file named COPYING.  Among other
things, the copyright notice and this notice must be preserved on all
copies.
*/

package org.montsuqi.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import org.montsuqi.util.Logger;
import org.montsuqi.util.SystemEnvironment;

/** <p>A class to manage configuration settings of the application.</p>
 * <p>Configuration is stored using the Preferences API. On Unix based systems,
 * it is stored in XML files somewhere in user's home directory. On Windows,
 * it is stored in the registry.</p>
 */
public class Configuration {

	private String pass;
	private String clientCertificatePass;
	private Preferences prefs;

	private static final String PORT_KEY = "port"; //$NON-NLS-1$
	private static final String HOST_KEY = "host"; //$NON-NLS-1$
	private static final String USER_KEY = "user"; //$NON-NLS-1$
	private static final String CACHE_KEY = "cache"; //$NON-NLS-1$
	private static final String SAVE_PASSWORD_KEY = "save_pass"; //$NON-NLS-1$
	private static final String PASSWORD_KEY = "password"; //$NON-NLS-1$
	private static final String ENCODING_KEY = "encoding"; //$NON-NLS-1$
	private static final String STYLES_KEY = "styles"; //$NON-NLS-1$
	private static final String APPLICATION_KEY = "application"; //$NON-NLS-1$
	private static final String USE_SSL_KEY = "use_ssl"; //$NON-NLS-1$
	private static final String SAVE_CLIENT_CERTIFICATE_PASSWORD_KEY = "save_client_certificate_pass"; //$NON-NLS-1$
	private static final String CLIENT_CERTIFICATE_KEY = "client_certificate"; //$NON-NLS-1$
	private static final String CLIENT_CERTIFICATE_PASSWORD_KEY = "client_certificate_password"; //$NON-NLS-1$
	private static final String PROTOCOL_VERSION_KEY = "protocol_version"; //$NON-NLS-1$
	private static final String LOOK_AND_FEEL_KEY = "look_and_feel"; //$NON-NLS-1$
	private static final String USE_LOG_VIEWER_KEY = "use_log_viewer"; //$NON-NLS-1$
	private static final String PROPERTIES_KEY = "properties"; //$NON-NLS-1$

	private static final String PANDA_SCHEME = "panda:"; //$NON-NLS-1$

	/** <p>Default port: 8000</p> */
	static final int DEFAULT_PORT = 8000;

	/** <p>Default host: localhost</p> */
	static final String DEFAULT_HOST = "localhost"; //$NON-NLS-1$

	/** <p>Default user: value of System property user.name.</p> */
	static final String DEFAULT_USER = System.getProperty("user.name"); //$NON-NLS-1$

	/** <p>Default password: empty string.</p> */
	static final String DEFAULT_PASSWORD = ""; //$NON-NLS-1$

	/** <p>Default client certificate password: empty string.</p> */
	static final String DEFAULT_CLIENT_CERTIFICATE_PASSWORD = ""; //$NON-NLS-1$

	/** <p>Default client certificate path: empty string.</p> */
	static final String DEFAULT_CLIENT_CERTIFICATE = ""; //$NON-NLS-1$

	/** <p>Default value of save password checkbox: false.</p> */
	static final boolean DEFAULT_SAVE_PASSWORD = false;

	/** <p>Default application name: "demo".</p> */
	static final String DEFAULT_APPLICATION = "demo"; //$NON-NLS-1$

	/** <p>Default encoding for the stream: EUC-JP.</p> */
	static final String DEFAULT_ENCODING = "EUC-JP"; //$NON-NLS-1$

	/** <p>Default cache directory: [value of System property user.home]/.monsiaj/cache</p> */
	static final String DEFAULT_CACHE_PATH;
	static {
		String[] pathElements = {
			System.getProperty("user.home"), //$NON-NLS-1$
			".monsiaj", //$NON-NLS-1$
			"cache" //$NON-NLS-1$
		};
		DEFAULT_CACHE_PATH = SystemEnvironment.createFilePath(pathElements).getAbsolutePath();
	}

	/** <p>Default style definitions: empty string.</p> */
	static final String DEFAULT_STYLES = ""; //$NON-NLS-1$

	/** <p>Default style resource name: /org/montsuqi/client/style.properteis.</p> */
	static final String DEFAULT_STYLE_RESOURCE_NAME = "/org/montsuqi/client/style.properties"; //$NON-NLS-1$

	/** <p>Default value of use SSL checkbox: false.</p> */
	static final boolean DEFAULT_USE_SSL = false;

	/** <p>Default value of save client certificate password checkbox: false.</p> */
	static final boolean DEFAULT_SAVE_CLIENT_CERTIFICATE_PASSWORD = false;

	/** <p>Default protocol version: 1.</p> */
	static final int DEFAULT_PROTOCOL_VERSION = 1;

	/** <p>Default look and feel class name: system look and feel.</p> */
	static final String DEFAULT_LOOK_AND_FEEL_CLASS_NAME = UIManager.getSystemLookAndFeelClassName();

	/** <p>Default value of use log viewer checkbox: false.</p> */
	static final boolean DEFAULT_USE_LOG_VIEWER = false;

	protected static final Logger logger = Logger.getLogger(Configuration.class);

	/** <p>Constructs a configuration object.</p>
	 * 
	 * @param clazz class object used to obtain user preference node.
	 */
	public Configuration(Class clazz) {
		prefs = Preferences.userNodeForPackage(clazz);
	}

	/** <p>Ensures the configuration is saved.</p>
	 */
	protected void save() {
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			logger.warn(e);
		}
	}

	/** <p>Returns the password.</p>
	 * <p>If save password is set to false, it always returns DEFAULT_PASSWORD("").</p>
	 * @return the password or empty string.
	 */
	public String getPass() {
		if (getBoolean(SAVE_PASSWORD_KEY, false)) {
			return getString(PASSWORD_KEY, DEFAULT_PASSWORD);
		} else {
			return pass;
		}
	}

	/** <p>Sets the password.</p>
	 * <p>It stores the new password into the member field.</p>
	 * <p>If save password is set to true, it also store the password into the configuration,
	 * otherwise it sets the DEFAULT_PASSWORD("") to clear it.</p>
	 * @param pass the new password.
	 */
	public void setPass(String pass) {
		this.pass = pass;
		if (getBoolean(SAVE_PASSWORD_KEY, false)) {
			setString(PASSWORD_KEY, pass);
		} else {
			setString(PASSWORD_KEY, DEFAULT_PASSWORD);
		}
	}

	/** <p>Returns the value of save password.</p>
	 * 
	 * @return value of save password.</p>
	 */
	public boolean getSavePassword() {
		return getBoolean(SAVE_PASSWORD_KEY, DEFAULT_SAVE_PASSWORD);
	}

	/** <p>Sets the value of save password.</p>
	 * 
	 * @param flag new value of save password.
	 */
	public void setSavePassword(boolean flag) {
		setBoolean(SAVE_PASSWORD_KEY, flag);
	}

	/** <p>Returns the client certificate password.</p>
	 * <p>If save password is set to false, it always returns DEFAULT_PASSWORD("").</p>
	 * @return the password or empty string.
	 */
	public String getClientCertificatePassword() {
		if (getBoolean(SAVE_CLIENT_CERTIFICATE_PASSWORD_KEY, false)) {
			return getString(CLIENT_CERTIFICATE_PASSWORD_KEY, DEFAULT_CLIENT_CERTIFICATE_PASSWORD);
		} else {
			return clientCertificatePass;
		}
	}

	/** <p>Sets the client certificate password.</p>
	 * <p>It stores the new password into the member field.</p>
	 * <p>If save client password is set to true, it also store the password into the configuration,
	 * otherwise it sets the DEFAULT_PASSWORD("") to clear it.</p>
	 * @param pass the new password.
	 */
	public void setClientCertificatePassword(String pass) {
		this.clientCertificatePass = pass;
		if (getBoolean(SAVE_CLIENT_CERTIFICATE_PASSWORD_KEY, false)) {
			setString(CLIENT_CERTIFICATE_PASSWORD_KEY, pass);
		} else {
			setString(CLIENT_CERTIFICATE_PASSWORD_KEY, DEFAULT_CLIENT_CERTIFICATE_PASSWORD);
		}
	}

	/** <p>Returns the value of save client certificate password.</p>
	 * 
	 * @return value of save client certificate password.</p>
	 */
	public boolean getSaveClientCertificatePassword() {
		return getBoolean(SAVE_CLIENT_CERTIFICATE_PASSWORD_KEY, DEFAULT_SAVE_CLIENT_CERTIFICATE_PASSWORD);
	}

	/** <p>Sets the value of save client certificate password.</p>
	 * 
	 * @param flag new value of save client certificate password.
	 */
	public void setSaveClientCertificatePassword(boolean flag) {
		setBoolean(SAVE_CLIENT_CERTIFICATE_PASSWORD_KEY, flag);
	}

	/** <p>Returns the port.</p>
	 * 
	 * @return the port number.
	 */
	public int getPort() {
		return getInt(PORT_KEY, DEFAULT_PORT);
	}

	/** <p>Sets the port.</p>
	 * 
	 * @param port new value of the port.
	 */
	public void setPort(int port) {
		setInt(PORT_KEY, port);
	}

	/** <p>Returns the host.</p>
	 * 
	 * @return the host.
	 */
	public String getHost() {
		return getString(HOST_KEY, DEFAULT_HOST);
	}

	/** <p>Sets the host.</p>
	 * 
	 * @param host new value of the host.
	 */
	public void setHost(String host) {
		setString(HOST_KEY, host);
	}

	/** <p>Returns the user.</p>
	 * 
	 * @return the user.
	 */
	public String getUser() {
		return getString(USER_KEY, DEFAULT_USER);
	}

	/** <p>Sets the user.</p>
	 * 
	 * @param user new value of the user.
	 */
	public void setUser(String user) {
		setString(USER_KEY, user);
	}

	/** <p>Returns the cache directory.</p>
	 * 
	 * @return the cache directory.
	 */
	public String getCache() {
		return getString(CACHE_KEY, DEFAULT_CACHE_PATH);
	}

	/** <p>Sets the cache directory.</p>
	 * 
	 * @param cache new value of the cache directory.
	 */
	public void setCache(String cache) {
		setString(CACHE_KEY, cache);
	}

	/** <p>Returns the encoding.</p>
	 * 
	 * @return the encoding.
	 */
	public String getEncoding() {
		return getString(ENCODING_KEY, DEFAULT_ENCODING);
	}

	/** <p>Sets the encoding.</p>
	 * 
	 * @param encoding new value of the encoding.
	 */
	public void setEncoding(String encoding) {
		setString(ENCODING_KEY, encoding);
	}

	/** <p>Returns the style URL.</p>
	 * <p>It first tries converting style file name specified in the configuration to URL.
	 * If it fails it falls back to default style resource.</p>
	 * @return the style URL.
	 */
	public URL getStyleURL() {
		String styleFileName = getStyleFileName();
		if (styleFileName != null && styleFileName.length() > 0) {
			File file = new File(styleFileName);
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				logger.warn(e);
			}
		}
		return Configuration.class.getResource(DEFAULT_STYLE_RESOURCE_NAME);
	}

	/** <p>Returns the style file name.</p>
	 * 
	 * @return the style file name.
	 */
	public String getStyleFileName() {
		return getString(STYLES_KEY, DEFAULT_STYLES);
	}

	/** <p>Sets the style file name.</p>
	 * 
	 * @param styles new value of style file name.
	 */
	public void setStyleFileName(String styles) {
		setString(STYLES_KEY, styles);
	}

	/** <p>Returns the name of the application.</p>
	 * 
	 * @return the name of the application.
	 */
	public String getApplication() {
		return getString(APPLICATION_KEY, DEFAULT_APPLICATION);
	}

	/** <p>Sets the name of the application.</p>
	 * @param app new value of the name of the pplication.
	 */
	public void setApplication(String app) {
		if (app == null || app.length() == 0) {
			app = DEFAULT_APPLICATION;
		} else if ( ! app.startsWith(PANDA_SCHEME)) {
			app = PANDA_SCHEME + app;
		}
		setString(APPLICATION_KEY, app);
	}

	/** <p>Returns the value of use SSL.</p>
	 * 
	 * @return true if using SSL. false otherwise.
	 */
	public boolean getUseSSL() {
		return getBoolean(USE_SSL_KEY, DEFAULT_USE_SSL);
	}

	/** <p>Sets the value of use SSL.</p>
	 * 
	 * @param flag new value of use SSL.
	 */
	public void setUseSSL(boolean flag) {
		setBoolean(USE_SSL_KEY, flag);
	}

	/** <p>Returns the name of client certificate file.</p>
	 * 
	 * @return the name of client certificate.
	 */
	public String getClientCertificateFileName() {
		return getString(CLIENT_CERTIFICATE_KEY, DEFAULT_CLIENT_CERTIFICATE);
	}

	/** <p>Sets the name of client certificate file.</p>
	 * 
	 * @param fileName new name of client certificate file.
	 */
	public void setClientCertificateFileName(String fileName) {
		setString(CLIENT_CERTIFICATE_KEY, fileName);
	}

	/** <p>Returns the protocol version.</p>
	 * 
	 * @return protocol version.
	 */
	public int getProtocolVersion() {
		return getInt(PROTOCOL_VERSION_KEY, DEFAULT_PROTOCOL_VERSION);
	}

	/** <p>Sets the protocol version.</p>
	 * 
	 * @param version version number(1 or 2).
	 */
	public void setProtocolVersion(int version) {
		if (version != 1 && version != 2) {
			throw new IllegalArgumentException("only protocol version 1 and 2 are acceptable."); //$NON-NLS-1$
		}
		setInt(PROTOCOL_VERSION_KEY, version);
	}

	/** <p>Returns the look and feel class name.</p>
	 * 
	 * @return look and feel class name.
	 */
	public String getLookAndFeelClassName() {
		return getString(LOOK_AND_FEEL_KEY, DEFAULT_LOOK_AND_FEEL_CLASS_NAME);
	}

	/** <p>Sets the look and feel class name.</p>
	 * 
	 * @param className look and feel class name.
	 */
	public void setLookAndFeelClassName(String className) {
		setString(LOOK_AND_FEEL_KEY, className);
	}

	/** <p>Returns the value of use log viewer.</p>
	 * 
	 * @return the value of use log viewer.
	 */
	public boolean getUseLogViewer() {
		return getBoolean(USE_LOG_VIEWER_KEY, DEFAULT_USE_LOG_VIEWER);
	}

	/** <p>Sets the value of use log viewer.</p>
	 * 
	 * @param flag new value of use log viewer.
	 */
	public void setUseLogViewer(boolean flag) {
		setBoolean(USE_LOG_VIEWER_KEY, flag);
	}

	/** <p>Returns other properties as string.</p>
	 */
	public String getProperties() {
		final String properties = getString(PROPERTIES_KEY, ""); //$NON-NLS-1$
		updateSystemProperties(properties);
		return properties;
	}

	/** <p>Sets other properties.</p>
	 * 
	 * @param properties new line separated property assignments.
	 */
	public void setProperties(String properties) {
		setString(PROPERTIES_KEY, properties);
		updateSystemProperties(properties);
	}

	private void updateSystemProperties(String properties) {
		StringReader sr = new StringReader(properties);
		BufferedReader br = new BufferedReader(sr);
		String line;
		try {
			while ((line = br.readLine()) != null) {
				String[] pair = line.split("\\s*=\\s*"); //$NON-NLS-1$
				if (pair.length == 2) {
					String key = pair[0].trim();
					String value = pair[1].trim();
					System.setProperty(key, value);
				}
			}
		} catch (IOException e) {
			logger.warn(e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// do nothing
			}
		}
	}

	/** <p>Returns configuration value(String).</p>
	 * 
	 * @param key configuration key.
	 * @param defaultValue value if the configuration for the given key is missing.
	 * @return the configuration value.
	 */
	protected String getString(String key, String defaultValue) {
		return prefs.get(key, defaultValue);
	}

	/** <p>Returns configuration value(int).</p>
	 * 
	 * @param key configuration key.
	 * @param defaultValue value if the configuration for the given key is missing.
	 * @return the configuration value.
	 */
	protected int getInt(String key, int defaultValue) {
		return prefs.getInt(key, defaultValue);
	}

	/** <p>Returns configuration value(boolean).</p>
	 * 
	 * @param key configuration key.
	 * @param defaultValue value if the configuration for the given key is missing.
	 * @return the configuration value.
	 */
	protected boolean getBoolean(String key, boolean defaultValue) {
		return prefs.getBoolean(key, defaultValue);
	}

	/** <p>Sets configuration value(String).</p>
	 * 
	 * @param key configuration key.
	 * @param value the configuration value.
	 */
	protected void setString(String key, String value) {
		prefs.put(key, value);
	}

	/** <p>Sets configuration value(int).</p>
	 * 
	 * @param key configuration key.
	 * @param value the configuration value.
	 */
	protected void setInt(String key, int value) {
		prefs.putInt(key, value);
	}

	/** <p>Sets configuration value(boolean).</p>
	 * 
	 * @param key configuration key.
	 * @param value the configuration value.
	 */
	protected void setBoolean(String key, boolean value) {
		prefs.putBoolean(key, value);
	}
}
