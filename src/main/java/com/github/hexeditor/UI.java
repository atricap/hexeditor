package com.github.hexeditor;

import java.awt.*;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.*;

public class UI extends JApplet {

    private static final String link = "https://github.com/javadev/hexeditor";
    private static final String appName = "hexeditor.jar";
    private static final String version = "2025-xx-xx";
    private static boolean isApplet = false;
    private static final JPanel mainPanel = new JPanel(new BorderLayout());
    private static final Runtime rT = Runtime.getRuntime();
    static final byte[] logo = Base64.getDecoder().decode(
            "R0lGODlhEAAQAIAAAAAAAP///yH5BAEAAAEALAAAAAAQABAAAAIojI+pmwDmGHwhSmsZpppGDk3Ox22j92TbMSKr6KJTVcqqTFeNxPdHAQA7");
    public static JRootPane theRootPane = null;
    public static String browse = null;
    public static final String htmlBase = null;
    public static final String htmlReport = null;
    public static final String htmlEnd = null;

    @Override
    public void init() {
        isApplet = true;
        theRootPane = this.getRootPane();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setUpMainPanel(isApplet, false, null);

        String javaMinVersion = this.getParameter("JAVAMINVERSION");
        if (System.getProperty("java.specification.version").compareToIgnoreCase(javaMinVersion) < 0) {
            JLabel errorLabel = new JLabel("<html><h1>Java version found: "
                            + System.getProperty("java.version")
                            + ", needed: "
                            + javaMinVersion);
            this.getContentPane().add(errorLabel, "Center");
            return;
        }

        this.getContentPane().add(mainPanel, "Center");
    }

    public static void main(String[] args) {
        if (!isApplet && Arrays.stream(args)
                .anyMatch(arg -> arg.equalsIgnoreCase("-bug"))) {
            bugReport();
        }

        SwingUtilities.invokeLater(() -> createAndShowMainFrame(args));
    }

    private static void createAndShowMainFrame(String[] args) {
        boolean isSlave = 0 < args.length && args[0].equals("-slave");
        String linkedTo = 1 < args.length ? args[1] : "";
        String fileName = 1 < args.length ? args[1] : null;

        String title = isSlave
                ? "hexeditor.jar currently linked to  " + linkedTo
                : "hexeditor.jar     https://github.com/javadev/hexeditor     Updated: 2025-xx-xx";
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();

        JFrame f = new JFrame(title, gc);
        f.setIconImage((new ImageIcon(logo)).getImage());
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Rectangle bounds = f.getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(f.getGraphicsConfiguration());
        int innerWidth = bounds.width - insets.left - insets.right;
        int innerHeight = bounds.height - insets.top - insets.bottom;
        innerWidth = Math.min(700, innerWidth);
        if (isSlave) {
            f.setBounds(
                    bounds.x + bounds.width - innerWidth,
                    (bounds.y + (bounds.height + insets.top - insets.bottom - innerHeight)) / 2,
                    innerWidth,
                    innerHeight);
        } else {
            f.setBounds(
                    (bounds.x + (bounds.width + insets.left - insets.right - innerWidth)) / 2,
                    (bounds.y + (bounds.height + insets.top - insets.bottom - innerHeight)) / 2,
                    innerWidth,
                    innerHeight);
        }

        theRootPane = f.getRootPane();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setUpMainPanel(isApplet, isSlave, fileName);

        JFrame.setDefaultLookAndFeelDecorated(true);
        f.getContentPane().add(mainPanel, "Center");
        f.setVisible(true);
    }

    private static void setUpMainPanel(boolean isApplet, boolean isSlave, String fileName) {
        mainPanel.add(binPanel.createInstance(isApplet, isSlave, fileName, menuBar -> theRootPane.setJMenuBar(menuBar)));
    }

    private static void bugReport() {
        try {
            PrintStream newErr = new PrintStream(Files.newOutputStream(
                    Paths.get(System.getProperty("user.dir"), "Hexeditor.jar_BugReport.txt")));
            System.setErr(newErr);
            StringBuffer sb = new StringBuffer(
                    "If you find errors, feel free to send me a mail with a short explanation " +
                            "and this file at: @T \r\n\r\nHexeditor.jar 2025-xx-xx\r\n");
            Properties props = System.getProperties();
            Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();

            while (propNames.hasMoreElements()) {
                String propName = propNames.nextElement();
                if (!" user.name user.home ".contains(propName)) {
                    try {
                        sb
                                .append(propName)
                                .append(" ")
                                .append(props.getProperty(propName))
                                .append("\n");
                    } catch (Exception e) {
                        sb
                                .append(propName)
                                .append(" SECURITY EXCEPTION!\n");
                    }
                }
            }

            sb
                    .append("\r\nµP\t")
                    .append(rT.availableProcessors())
                    .append("\r\nMem(MiB), free/total/max: ")
                    .append(rT.freeMemory() >> 20)
                    .append("/")
                    .append(rT.totalMemory() >> 20)
                    .append("/")
                    .append(rT.maxMemory() >> 20)
                    .append("\r\n\r\nError messages:");
            System.err.println(sb);
        } catch (Exception ignored) {
        }
    }
}
