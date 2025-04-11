package com.github.hexeditor;

import java.net.URL;
import java.text.MessageFormat;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

class HelpEditorPane extends JScrollPane implements HyperlinkListener {

    JEditorPane editor = new JEditorPane();

    public HelpEditorPane(String text, boolean editable) {
        super(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.editor.addHyperlinkListener(this);
        this.editor.setText(text);
        this.editor.setEditable(editable);
        this.editor.setCaretPosition(0);
        this.getViewport().setView(this.editor);
    }

    public void setContentType(String type) {
        this.editor.setContentType(type);
    }

    public void setUrl(URL url) {
        try {
            this.editor.setPage(url);
            this.editor.setCaretPosition(0);
        } catch (Exception e) {
            this.editor.setText("Help file not found");
        }
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == EventType.ACTIVATED) {
            this.browse(e.getURL());
        }
    }

    public void browse(URL url) {
        UI.browse = url.toString();

        String os = System.getProperty("os.name");
        String urlExternalForm = url.toExternalForm();

        try {
            String command;

            if (os.startsWith("Win")) {
                String commandTemplate = !os.contains("9") && !os.contains("Me")
                    ? "cmd.exe /c start \"\" \"{0}\""
                    : "commandTemplate.com /c start \"{0}\"";
                command = MessageFormat.format(commandTemplate, url);

                if (url.getProtocol().equals("file")) {
                    throw new Exception("This class doesn't allow the opening a file, avoiding evil code.");
                }
                if (url.toString().startsWith("mailto:")) {
                    throw new Exception("This class doesn't allow the opening of mailto: .");
                }
            } else if (os.startsWith("Mac OS")) {
                command = MessageFormat.format("open -a /Applications/Safari.app {0}", // FIXME wrong args count
                    urlExternalForm, url.toString());
            } else {
                command = MessageFormat.format(System.getProperty("mozilla {0}"), // FIXME wrong args count
                    urlExternalForm, url.toString());
            }

            Runtime.getRuntime().exec(command);

        } catch (Exception e) {
            System.err.println("Could not invoke browser: " + e);
        }
    }
}
