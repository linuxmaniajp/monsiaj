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

import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.montsuqi.util.GtkStockIcon;
import org.montsuqi.util.Logger;
import org.montsuqi.util.OptionParser;
import org.montsuqi.util.SystemEnvironment;
import org.montsuqi.widgets.Button;
import org.montsuqi.widgets.ConsolePane;
import org.montsuqi.widgets.ExceptionDialog;

public class Launcher {

    protected static final Logger logger = Logger.getLogger(Launcher.class);
    protected String title;
    protected Configuration conf;
    protected ConfigurationPanel configPanel;
    protected JComboBox configCombo;
    private Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    static {
        if (System.getProperty("monsia.logger.factory") == null) { //$NON-NLS-1$
            System.setProperty("monsia.logger.factory", "org.montsuqi.util.StdErrLogger");
        }
    }

    public static void main(String[] args) {
        Launcher launcher = new Launcher(Messages.getString("application.title")); //$NON-NLS-1$
        launcher.launch(args);
    }

    public Launcher(String title) {
        this.title = title;
        SystemEnvironment.setMacMenuTitle(title);
        conf = new Configuration(this.getClass());
        installLookAndFeels();
    }

    private void installLookAndFeels() {
        try {
            UIManager.installLookAndFeel("Nimrod", "com.nilo.plaf.nimrod.NimRODLookAndFeel");
            UIManager.installLookAndFeel("InfoNode", "net.infonode.gui.laf.InfoNodeLookAndFeel");
        } catch (Exception e) {
            logger.warn(e);
        }
    }

    public boolean checkCommandLineOption(String[] args) {
        OptionParser options = new OptionParser();
        options.add("config", Messages.getString("Launcher.config_option_message"), "");
        options.add("config-list", Messages.getString("Launcher.config_list_option_message"), false);
        options.parse(this.getClass().getName(), args);

        String configName = options.getString("config");
        boolean listConfigFlag = options.getBoolean("config-list");
        if (listConfigFlag) {
            conf.listConfiguration();
            return true;
        }
        if (!configName.equals("")) {
            conf.setConfigurationName(configName);
            /*
             * set properties
             */
            conf.applyProperties(configName);

            /*
             * set look and feel
             */
            try {
                String cname = conf.getLookAndFeelClassName(configName);
                if (cname.startsWith("com.nilo.plaf.nimrod")) {
                    System.setProperty("nimrodlf.themeFile", conf.getLAFThemeFileName(configName));
                    UIManager.setLookAndFeel(new NimRODLookAndFeel());
                } else {
                    UIManager.setLookAndFeel(conf.getLookAndFeelClassName(configName));
                }
            } catch (Exception e) {
                logger.warn(e);
                return true;
            }

            /*
             * confirm password when the password not preserved
             */
            if (!conf.getSavePassword(configName)) {
                JPasswordField pwd = new JPasswordField();
                Object[] message = {Messages.getString("Launcher.input_password_message"), pwd};
                int resp = JOptionPane.showConfirmDialog(null, message, Messages.getString("Launcher.input_password_message"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (resp == JOptionPane.OK_OPTION) {
                    conf.setPassword(configName, String.valueOf(pwd.getPassword()));
                } else {
                    return true;
                }
            }
            /*
             * confirm certificate password when the certificate password not
             * preserved
             */
            if (conf.getUseSSL(configName)
                    && !conf.getClientCertificateFileName(configName).equals("")
                    && !conf.getSaveClientCertificatePassword(configName)) {
                JPasswordField pwd = new JPasswordField();
                Object[] message = {Messages.getString("Launcher.input_certificate_password_message"), pwd};
                int resp = JOptionPane.showConfirmDialog(null, message, Messages.getString("Launcher.input_certificate_password_message"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (resp == JOptionPane.OK_OPTION) {
                    conf.setClientCertificatePassword(configName, String.valueOf(pwd.getPassword()));
                } else {
                    return true;
                }
            }
            connect();
            return true;
        }
        return false;
    }

    protected JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints gbc;

        JLabel configLabel = new JLabel(Messages.getString("ConfigurationPanel.config_label"));
        configCombo = new JComboBox();
        updateConfigCombo();
        configCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                configPanel.loadConfiguration((String) configCombo.getSelectedItem(), false);
            }
        });
        configPanel = createConfigurationPanel();
        configPanel.loadConfiguration(conf.getConfigurationName(), false);
        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbed.addTab(Messages.getString("ConfigurationPanel.basic_tab_label"), configPanel.getBasicPanel()); //$NON-NLS-1$
        tabbed.addTab(Messages.getString("ConfigurationPanel.ssl_tab_label"), configPanel.getSSLPanel()); //$NON-NLS-1$
        tabbed.addTab(Messages.getString("ConfigurationPanel.others_tab_label"), configPanel.getOthersPanel()); //$NON-NLS-1$
        tabbed.addTab(Messages.getString("ConfigurationPanel.info_tab_label"), configPanel.getInfoPanel()); //$NON-NLS-1$

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.ipadx = 5;
        gbc.insets = new Insets(2, 2, 2, 2);
        panel.add(configLabel, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(configCombo, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        panel.add(tabbed, gbc);
        return panel;
    }

    private void updateConfigCombo() {
        ActionListener[] listeners = configCombo.getActionListeners();
        for (int i = 0; i < listeners.length; i++) {
            configCombo.removeActionListener(listeners[i]);
        }
        String[] configNames = conf.getConfigurationNames();
        configCombo.removeAllItems();
        for (int i = 0; i < configNames.length; i++) {
            configCombo.addItem(configNames[i]);
        }
        for (int i = 0; i < listeners.length; i++) {
            configCombo.addActionListener(listeners[i]);
        }
        configCombo.setSelectedItem(conf.getConfigurationName());
    }

    private void checkJavaVersion() {
        String ver = System.getProperty("java.version");
        boolean isOld = false;
        if (ver.startsWith("1.7")) {
            if (ver.compareToIgnoreCase("1.7.0_17") < 0) {
                isOld = true;
            }
        } else if (ver.startsWith("1.6")) {
            if (ver.compareToIgnoreCase("1.6.0_43") < 0) {
                isOld = true;
            }
        }
        if (isOld) {
            String contents = "";
            contents += "脆弱性のあるJavaを使用しています\n";
            contents += "\n";
            contents += "使用中のバージョン:" + ver + "\n\n";
            contents += "Javaをアップデートしてください\n";

            Color bgcolor = new Color(240, 240, 30);
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panel.setBackground(bgcolor);

            JPanel textPanel = new JPanel(new BorderLayout(5, 5));
            textPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            textPanel.setBackground(bgcolor);

            JLabel summaryLabel = new JLabel("monsiajセキュリティ警告");
            summaryLabel.setFont(new Font("Suns", Font.BOLD, 20));
            JTextPane bodyText = new JTextPane();
            bodyText.setFont(new Font("Suns", Font.PLAIN, 16));
            bodyText.setText(contents);
            bodyText.setOpaque(false);
            bodyText.setEditable(false);

            textPanel.add(summaryLabel, BorderLayout.NORTH);
            textPanel.add(bodyText, BorderLayout.CENTER);

            boolean checked = prefs.get(Launcher.class.getName()+".security_risk_agreement","no").startsWith("yes");
            final JCheckBox checkBox = new JCheckBox("危険性を理解した上で使用する");
            checkBox.setFont(new Font("Suns", Font.PLAIN, 16));
            checkBox.setBackground(bgcolor);
            checkBox.setSelected(checked);
            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    prefs.put(Launcher.class.getName()+".security_risk_agreement",checkBox.isSelected()?"yes":"no");
                }
            });
            textPanel.add(checkBox, BorderLayout.SOUTH);

            panel.add(new JLabel(GtkStockIcon.get("gtk-dialog-warning")), BorderLayout.WEST);
            panel.add(textPanel, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(null, panel, "monsiajセキュリティ警告", JOptionPane.PLAIN_MESSAGE);
            if (!checkBox.isSelected()) {
                System.exit(0);
            } 
        }
    }

    public void launch(String[] args) {
        checkJavaVersion();
        if (checkCommandLineOption(args)) {
            return;
        }
        final JFrame f = new JFrame(title);
        Container container = f.getContentPane();
        container.setLayout(new BorderLayout(10, 5));
        final JPanel mainPanel = createMainPanel();
        final ConfigurationViewer viewer = createConfigurationViewer();
        container.add(mainPanel, BorderLayout.CENTER);

        URL iconURL = getClass().getResource("/org/montsuqi/widgets/images/orca.png");
        f.setIconImage(Toolkit.getDefaultToolkit().createImage(iconURL));

        JLabel iconLabel = new JLabel("", createIcon(), JLabel.CENTER);
        container.add(iconLabel, BorderLayout.WEST);

        JPanel bar = new JPanel();
        bar.setLayout(new FlowLayout());
        container.add(bar, BorderLayout.SOUTH);

        Button run = new Button(new AbstractAction(Messages.getString("Launcher.run_label")) { //$NON-NLS-1$

            public void actionPerformed(ActionEvent ev) {
                String configName = (String) configCombo.getSelectedItem();
                configPanel.saveConfiguration(configName);
                conf.setConfigurationName(configName);
                connect();
                f.dispose();
            }
        });
        bar.add(run);

        Button cancel = new Button(new AbstractAction(Messages.getString("Launcher.cancel_label")) { //$NON-NLS-1$

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        bar.add(cancel);

        Button config = new Button(new AbstractAction(Messages.getString("Launcher.config_label")) { //$NON-NLS-1$

            public void actionPerformed(ActionEvent e) {
                viewer.run(f);
                updateConfigCombo();
            }
        });
        bar.add(config);

        f.setSize(760, 480);
        f.setResizable(true);

        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.setVisible(true);
        configPanel.changeLookAndFeel();
        run.requestFocus();
    }

    private void connect() {
        conf.save();
        Client client = new Client(conf);
        JFrame logFrame = conf.getUseLogViewer(conf.getConfigurationName()) ? createLogFrame(client) : null;

        try {
            client.connect();
            client.run();
            //Thread t = new Thread(client);
            //t.start();
            //t.join();
        } catch (Exception e) {
            logger.fatal(e);
            ExceptionDialog.showExceptionDialog(e);
            if (logFrame != null) {
                logFrame.setExtendedState(Frame.NORMAL);
            } else {
                client.exitSystem();
            }
        }
    }

    protected ConfigurationPanel createConfigurationPanel() {
        return new ConfigurationPanel(conf, true, true);
    }

    protected ConfigurationViewer createConfigurationViewer() {
        return new ConfigurationViewer(conf);
    }

    protected Icon createIcon() {
        return null;
    }

    private JFrame createLogFrame(final Client client) {
        final JFrame f = new JFrame(Messages.getString("Launcher.log_title")); //$NON-NLS-1$
        URL iconURL = getClass().getResource("/org/montsuqi/widgets/images/orca.png");
        f.setIconImage(Toolkit.getDefaultToolkit().createImage(iconURL));
        Container container = f.getContentPane();
        container.setLayout(new BorderLayout());
        final ConsolePane console = new ConsolePane();
        System.setOut(console.getOut());
        System.setErr(console.getErr());

        JScrollPane scroll = new JScrollPane(console);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setPreferredSize(new Dimension(640, 480));
        container.add(scroll, BorderLayout.CENTER);

        JPanel bar = new JPanel();
        bar.setLayout(new FlowLayout());
        container.add(bar, BorderLayout.SOUTH);
        Button clear = new Button(new AbstractAction(Messages.getString("Launcher.log_clear")) { //$NON-NLS-1$

            public void actionPerformed(ActionEvent e) {
                console.setText(""); //$NON-NLS-1$
            }
        });
        bar.add(clear);

        Button save = new Button(new AbstractAction(Messages.getString("Launcher.log_save_log_as")) { //$NON-NLS-1$

            public void actionPerformed(ActionEvent ev) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showSaveDialog(f);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        FileWriter fw = new FileWriter(file);
                        fw.write(console.getText());
                        fw.close();
                    } catch (IOException e) {
                        logger.warn(e);
                    }
                }
            }
        });
        bar.add(save);

        Button quit = new Button(new AbstractAction(Messages.getString("Launcher.log_quit")) { //$NON-NLS-1$

            public void actionPerformed(ActionEvent e) {
                client.exitSystem();
            }
        });
        bar.add(quit);

        f.setSize(640, 480);
        int state = f.getExtendedState();
        f.setExtendedState(state | Frame.ICONIFIED);
        f.setVisible(true);

        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                client.exitSystem();
            }
        });
        return f;
    }
}
