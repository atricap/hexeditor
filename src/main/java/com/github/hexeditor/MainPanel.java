package com.github.hexeditor;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class MainPanel extends JPanel
        implements ActionListener, ItemListener, CaretListener, MouseListener {

    JTextField fileField = new JTextField();
    JProgressBar savePBar = new JProgressBar(0, 0, 0);
    JProgressBar findPBar = new JProgressBar(0, 0, 0);
    BinEdit binEdit;
    JComponent help = this.createHelp();
    boolean helpFlag = false;
    boolean cp437Available = false;
    boolean useFindChar = false;
    JComboBox<String>[] viewComboBox = new JComboBox[2];
    JTextField viewField = new JTextField();
    JLabel bytesSelectedField = new JLabel("");
    byte[] selection;
    JLabel findLabel = new JLabel(" ");
    JButton[] findButtons = new JButton[2];
    JComboBox[] findComboBoxes = new JComboBox[4];
    JTextField[] findFields = new JTextField[2];
    JCheckBox findCheckBoxIgnoreCase = new JCheckBox("Ignore case:");
    JPanel progressBarPanel = new JPanel(new BorderLayout());
    JPanel findPanel0 = new JPanel(new BorderLayout());
    JPanel findPanel1 = this.findPanel();
    JPanel stat = this.createStatusBar();
    JPanel frameFile;
    byte[] finByte;
    byte[] finByteU;
    byte[][][] findChar;
    boolean isApplet = false;
    boolean isHexOffset = true;
    SlaveThread slaveThread;
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    DecimalFormat floatFormat = new DecimalFormat("#.##########E0");
    DecimalFormat doubleFormat = new DecimalFormat("#.###################E0");

    public static MainPanel createInstance(boolean isApplet, boolean isSlave, String fileName, Consumer<JMenuBar> onMenuCreated) {
        return new MainPanel(isApplet, isSlave, fileName, onMenuCreated);
    }

    public MainPanel(boolean isApplet, boolean isSlave, String fileName, Consumer<JMenuBar> onMenuCreated) {
        this.isApplet = isApplet;

        onMenuCreated.accept(createMenuBar());
        this.setLayout(new BorderLayout());
        this.frameFile = this.createFramePanel(isSlave, fileName);
        this.add(this.frameFile);
        this.decimalFormatSymbols.setDecimalSeparator('.');
        this.floatFormat.setDecimalFormatSymbols(this.decimalFormatSymbols);
        this.doubleFormat.setDecimalFormatSymbols(this.decimalFormatSymbols);
    }

    private JMenuBar createMenuBar() {
        String[][] titles = new String[][]{
                {
                    "File",
                    "Open",
                    "Save as ",
                    "Close file (Q)",
                    "Screen to Png"},
                {
                    "Edit",
                    "Select All",
                    "Undo (Z)",
                    "Redo (Y)",
                    "Cut (X)",
                    "Copy",
                    "Paste (V)",
                    "Find",
                    "Insert (before)",
                    "Delete"
                },
                {
                    "View",
                    "Goto",
                    "Toggle position Mark",
                    "Down to next mark",
                    "Up to previous mark ",
                    "Toggle caret ",
                    "Higher fontSize",
                    "Lower fontSize",
                    "Black/White background"
                },
                {
                    "hidden",
                    "Font +",
                    "Font -"
                },
                {
                    "Help",
                    "Toggle help"
                }
            };
        int[][] keyCodes =
                new int[][] {
                    {KeyEvent.VK_F, KeyEvent.VK_O, KeyEvent.VK_S, KeyEvent.VK_Q, KeyEvent.VK_P},
                    {KeyEvent.VK_E, KeyEvent.VK_A, KeyEvent.VK_Z, KeyEvent.VK_Y, KeyEvent.VK_X, KeyEvent.VK_C,
                        KeyEvent.VK_V, KeyEvent.VK_F, KeyEvent.VK_INSERT, KeyEvent.VK_DELETE},
                    {KeyEvent.VK_V, KeyEvent.VK_G, KeyEvent.VK_M, KeyEvent.VK_D, KeyEvent.VK_U, KeyEvent.VK_T,
                        KeyEvent.VK_ADD, KeyEvent.VK_SUBTRACT, KeyEvent.VK_W},
                    {0xb2, KeyEvent.VK_PLUS, KeyEvent.VK_MINUS},
                    {KeyEvent.VK_H, KeyEvent.VK_H}
                };
        int[][] mnemonics =
                new int[][] {
                    {KeyEvent.VK_F, KeyEvent.VK_O, KeyEvent.VK_S, KeyEvent.VK_Q, KeyEvent.VK_P},
                    {KeyEvent.VK_E, KeyEvent.VK_A, KeyEvent.VK_Z, KeyEvent.VK_Y, KeyEvent.VK_X, KeyEvent.VK_C,
                        KeyEvent.VK_V, KeyEvent.VK_F, KeyEvent.VK_I, KeyEvent.VK_D},
                    {KeyEvent.VK_V, KeyEvent.VK_G, KeyEvent.VK_M, KeyEvent.VK_D, KeyEvent.VK_U, KeyEvent.VK_T,
                        KeyEvent.VK_H, KeyEvent.VK_L, KeyEvent.VK_W},
                    {KeyEvent.VK_H, '+', KeyEvent.VK_MINUS},
                    {KeyEvent.VK_H, KeyEvent.VK_H}
                };
        int[][] modifiers =
                new int[][] {
                    {0, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK},
                    {0, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK,
                        InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, 0, 0},
                    {0, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK,
                        InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK},
                    {0, InputEvent.CTRL_MASK, InputEvent.CTRL_MASK},
                    {0, InputEvent.CTRL_MASK}
                };

        JMenuBar menuBar = new JMenuBar();
        for (int i = 0; i < titles.length; ++i) {
            JMenu menu = new JMenu(titles[i][0]);
            menu.setMnemonic(mnemonics[i][0]);

            if (i != 0 || !this.isApplet) { // no file menu for applets
                for (int j = 1; j < titles[i].length; ++j) {
                    JMenuItem menuItem = new JMenuItem(titles[i][j], mnemonics[i][j]);
                    if (i != 4) {
                        menuItem.setAccelerator(
                                KeyStroke.getKeyStroke(keyCodes[i][j], modifiers[i][j]));
                    }

                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    if (i == 1 && (j == 1 || j == 3 || j == 6 || j == 7)
                            || i == 2 && (j == 1 || j == 4 || j == 5 || j == 7)) {
                        menu.addSeparator();
                    }
                }
            }

            if (i != 3) { // no view menu
                menuBar.add(menu);
            }
        }

        return menuBar;
    }

    private JPanel createFramePanel(boolean isSlave, String fileName) {
        this.savePBar.setStringPainted(true);
        this.savePBar.setString("");
        this.viewField.setEditable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0D;
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.fileField.setEditable(false);
        panel.add(this.fileField, gbc);

        ++gbc.gridy;
        panel.add(this.progressBarPanel, gbc);

        ++gbc.gridy;
        panel.add(this.findPanel0, gbc);

        ++gbc.gridy;
        panel.add(Box.createVerticalStrut(3), gbc);

        gbc.fill = 1;
        gbc.weighty = 1.0D;
        ++gbc.gridy;
        this.binEdit = new BinEdit(this, this.isApplet);
        panel.add(this.binEdit, gbc);

        gbc.fill = 2;
        gbc.weighty = 0.0D;
        ++gbc.gridy;
        panel.add(this.stat, gbc);

        this.findComboBoxes[2].setSelectedIndex(6);

        if (isSlave) {
            this.slaveThread = new SlaveThread();
            this.slaveThread.setDaemon(true);
            this.slaveThread.binEdit = this.binEdit;
            this.slaveThread.start();
        } else if (fileName != null) {
            this.binEdit.loadFile(new java.io.File(fileName));
        }

        return panel;
    }

    public JComponent createHelp() {
        HelpEditorPane editor = new HelpEditorPane(null, false);
        editor.setContentType("text/html");

        String lang = Locale.getDefault().getLanguage();
        String langTitleCase = "" + Character.toUpperCase(lang.charAt(0)) + lang.charAt(1);
        String readMe = String.format("ReadMe%s.htm", langTitleCase);
        try {
            String readMe2 = UI.class.getResource(readMe) != null ? readMe : "ReadMeEn.htm";
            URL readMeR = Objects.requireNonNull(UI.class.getResource(readMe2),
                String.format("%s not found!", readMe2));
            editor.editor.getEditorKit().read(
                readMeR.openStream(),
                editor.editor.getDocument(),
                0);

            String shortKey = "shortKey.htm";
            URL shortKeyR = Objects.requireNonNull(UI.class.getResource(shortKey),
                String.format("%s not found!", shortKey));
            editor.editor.getEditorKit().read(
                shortKeyR.openStream(),
                editor.editor.getDocument(),
                editor.editor.getDocument().getLength());
        } catch (Exception ignored) {
        }

        return editor;
    }

    private JPanel findPanel() {
        final String[][] items = {
            {"BE", "LE"},
            {"Signed", "Unsigned"},
            {
                "Short (16)",
                "Int (32)",
                "Long (64)",
                "Float (32)",
                "Double (64)",
                "Hexa",
                "ISO/CEI 8859-1",
                "UTF-8",
                "UTF-16"
            },
            {"8 bits", "16 bits", "32 bits", "64 bits", "128 bits"}
        };
        final String[] toolTips = {
            "<html>Big-indian (natural order) or<br>Little-indian (Intel order).",
            "Only for integer",
            "Data type",
            "<html>Select '64' if you search a machine instruction for a 64 bits processor.<br>If you don't know, left it at '8'."
        };
        final String[] prototypeDisplayValues = {"BE", "Unsigned", "ISO/CEI 8859-1", "128 bits"};
        final String[] buttonTitles = {"Next", "Hide"};

        JPanel outerPanel = new JPanel(new GridBagLayout());
        JPanel line1 = new JPanel(new GridBagLayout());
        JPanel line2 = new JPanel(new GridBagLayout());

        TitledBorder outerBorder = BorderFactory.createTitledBorder("Find");
        outerBorder.setTitleColor(Color.blue);
        outerPanel.setBorder(outerBorder);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        outerPanel.add(line1, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        ++gbc.gridx;
        outerPanel.add(line2, gbc);

        this.findPBar.setStringPainted(true);
        outerPanel.add(this.findPBar, gbc);

        this.findFields[0] = new JTextField();
        this.findFields[0].addCaretListener(this);
        line1.add(this.findFields[0]);
        line1.add(new JLabel("  "));
        line1.add(this.findLabel);

        for (int i = 0; i < this.findComboBoxes.length; ++i) {
            this.findComboBoxes[i] = new JComboBox<>();
            this.findComboBoxes[i].setPrototypeDisplayValue(prototypeDisplayValues[i]);
            this.findComboBoxes[i].setToolTipText(toolTips[i]);

            for (int j = 0; j < items[i].length; ++j) {
                this.findComboBoxes[i].addItem(items[i][j]);
            }

            this.findComboBoxes[i].addItemListener(this);
            line2.add(this.findComboBoxes[i]);
        }

        this.findCheckBoxIgnoreCase.setHorizontalTextPosition(SwingConstants.LEFT);
        this.findCheckBoxIgnoreCase.setMargin(new Insets(0, 1, 0, 1));
        this.findCheckBoxIgnoreCase.addActionListener(this);
        line2.add(this.findCheckBoxIgnoreCase);
        this.findFields[0].setPreferredSize(
            new Dimension(
                this.findComboBoxes[0].getPreferredSize().width
                    + this.findComboBoxes[1].getPreferredSize().width
                    + this.findComboBoxes[2].getPreferredSize().width
                    + this.findComboBoxes[3].getPreferredSize().width,
                this.findFields[0].getPreferredSize().height));
        line2.add(Box.createHorizontalGlue());
        line2.add(new JLabel("   From:"));
        line2.add(this.findFields[1] = new JTextField(15));

        for (int i = 0; i < this.findButtons.length; ++i) {
            this.findButtons[i] = new JButton(buttonTitles[i]);
            this.findButtons[i].setMargin(new Insets(3, 2, 3, 2));
            this.findButtons[i].addActionListener(this);
            line2.add(this.findButtons[i]);
        }

        return outerPanel;
    }

    private JPanel createStatusBar() {
        String[][] defs = new String[][] {
            { // this.viewComboBox[0]
                "BE",
                "LE "
            },
            { // this.viewComboBox[1]
                "Binary",
                "Byte, signed/unsigned    ",
                "Short (16), signed",
                "Short (16), unsigned",
                "Int (32), signed",
                "Int (32), unsigned",
                "Long (64), signed",
                "Long (64), unsigned",
                "Float (32)",
                "Double (64)",
                "DOS-US/OEM-US/cp437",
                "UTF-8",
                "UTF-16"
            }
        };
        String[] toolTips = {
            "<html>Big-Endian (natural order) or little-Endian (Intel order).",
            "<html>Conversion rule for the data following the caret (shown here after)."
        };

        try {
            this.cp437Available = Charset.isSupported("cp437");
            if (!this.cp437Available) {
                defs[1][10] = "ISO/CEI 8859-1";
            }
        } catch (Exception ignored) {
        }

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        for (int cb = 0; cb < this.viewComboBox.length; ++cb) {
            JComboBox<String> box = this.viewComboBox[cb] = new JComboBox<>();
            box.setPrototypeDisplayValue(defs[cb][1]);
            box.setToolTipText(toolTips[cb]);

            for (String item : defs[cb]) {
                box.addItem(item);
            }

            ++gbc.gridx;
            panel.add(box, gbc);
        }

        this.viewComboBox[1].setSelectedIndex(1);

        gbc.weightx = 1.0D;
        ++gbc.gridx;
        this.viewField.setPreferredSize(
                new Dimension(
                        this.viewField.getPreferredSize().width,
                        this.viewComboBox[0].getMinimumSize().height));
        panel.add(this.viewField, gbc);

        gbc.weightx = 0.0D;
        ++gbc.gridx;
        panel.add(Box.createHorizontalStrut(3), gbc);

        ++gbc.gridx;
        panel.add(this.bytesSelectedField, gbc);

        ++gbc.gridx;
        panel.add(Box.createHorizontalStrut(3), gbc);

        this.viewComboBox[0].addItemListener(this);
        this.viewComboBox[1].addItemListener(this);
        this.bytesSelectedField.addMouseListener(this);

        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == this.findCheckBoxIgnoreCase) {
            this.checkFindEntry();

        } else if (event.getSource() == this.findButtons[0] && "Next".equals(this.findButtons[0].getText())) {
            this.binEdit.find1();

        } else if (event.getSource() == this.findButtons[0] && "Stop".equals(this.findButtons[0].getText())) {
            this.binEdit.findThread.interrupt();

        } else if (event.getSource() == this.findButtons[1]) {
            this.findPanel0.removeAll();
            this.validate();
            this.repaint();
            this.binEdit.slideScr(-1L, false);

        } else if (event.getSource().getClass().isInstance(new JMenuItem())) {
            JMenuItem menuItem = (JMenuItem) event.getSource();
            boolean isToggleHelp = menuItem.getText().equals("Toggle help");
            if (isToggleHelp || this.helpFlag) {
                this.removeAll();
                if (this.helpFlag) {
                    this.add(this.frameFile);
                } else {
                    this.add(this.help);
                }

                this.validate();
                this.repaint();
            }

            this.helpFlag = !this.helpFlag && isToggleHelp;
            if (!isToggleHelp) {
                this.binEdit.KeyFromMenu(menuItem.getAccelerator().getKeyCode());
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() == this.viewComboBox[0] || event.getSource() == this.viewComboBox[1]) {
            this.binEdit.rePaint();
            return;
        }
        if (event.getSource() == this.findComboBoxes[2]) {
            int selectedIndex = this.findComboBoxes[2].getSelectedIndex();
            this.findComboBoxes[0].setEnabled(selectedIndex < 5 || selectedIndex == 8);
            this.findComboBoxes[1].setEnabled(selectedIndex < 3);
            this.findCheckBoxIgnoreCase.setEnabled(5 < selectedIndex);
        }

        this.checkFindEntry();
    }

    @Override
    public void caretUpdate(CaretEvent event) {
        this.checkFindEntry();
    }

    protected void saveRunning(boolean isShowSavePBar) {
        if (isShowSavePBar) {
            this.progressBarPanel.add(this.savePBar);
        } else {
            this.progressBarPanel.removeAll();
        }

        this.savePBar.setValue(0);
        this.validate();
        this.repaint();
    }

    protected void find() {
        this.findPBar.setString("");
        this.findPanel0.add(this.findPanel1, "West");
        this.validate();
        this.repaint();
    }

    protected void findRunning(boolean isStop) {
        this.findButtons[0].setText(isStop ? "Stop" : "Next");
        this.findButtons[1].setEnabled(!isStop);
        this.findPBar.setValue(0);
        if (!isStop) {
            this.findPBar.setString("");
        }
    }

    private void checkFindEntry() {
        boolean flag1 = false;
        BigDecimal var3 = null;
        double var11 = 0.0D;
        float var14 = 0.0F;
        long time = System.currentTimeMillis();
        long[] var17 = new long[2];
        StringBuilder sb = new StringBuilder(220);
        sb.append("<html>");

        while (System.currentTimeMillis() < time + 50L)
            ; // Busy wait

        String findText = this.findFields[0].getText();
        String findTextSub = null;
        int jj = findText.length();
        int selectedIndex = this.findComboBoxes[2].getSelectedIndex();
        this.useFindChar = this.findCheckBoxIgnoreCase.isSelected() && 5 < selectedIndex;
        if (jj == 0) {
            this.findLabel.setText(" ");
            return;
        }

        int ii;
        if (selectedIndex < 5
                && !findText.startsWith("0x")
                && !findText.startsWith("Ox")
                && !findText.startsWith("ox")) {
            if (findText.charAt(0) == '-'
                    && this.findComboBoxes[1].getSelectedIndex() == 1 && selectedIndex < 3) {
                this.findLabel.setText(
                        "<html><FONT color=red>Input must be positive for unsigned integer.</FONT>");
                return;
            }

            if (findText.equals("-") || findText.equals("+")) {
                this.findLabel.setText(" ");
                return;
            }

            final BigDecimal minValue =
                selectedIndex == 0 ? BigDecimal.valueOf(Short.MIN_VALUE) :
                selectedIndex == 1 ? BigDecimal.valueOf(Integer.MIN_VALUE) :
                selectedIndex == 2 ? BigDecimal.valueOf(Long.MIN_VALUE) :
                selectedIndex == 3 ? BigDecimal.valueOf(-Float.MAX_VALUE) :
                BigDecimal.valueOf(-Double.MAX_VALUE);

            final BigDecimal maxValue;
            if (2 < selectedIndex) {
                maxValue =
                    selectedIndex == 3 ? BigDecimal.valueOf(Float.MAX_VALUE) :
                    BigDecimal.valueOf(Double.MAX_VALUE);
            } else if (this.findComboBoxes[1].getSelectedIndex() == 0) {
                maxValue =
                    selectedIndex == 0 ? BigDecimal.valueOf(Short.MAX_VALUE) :
                    selectedIndex == 1 ? BigDecimal.valueOf(Integer.MAX_VALUE) :
                    BigDecimal.valueOf(Long.MAX_VALUE);
            } else {
                maxValue =
                    selectedIndex == 0 ? BigDecimal.valueOf(0xffffL) : // (long) uint16 max value
                    selectedIndex == 1 ? BigDecimal.valueOf(0xffff_ffffL) : // (long) uint32 max value
                    new BigDecimal("18446744073709551615"); // uint64 max value
            }

            final int finByteLen =
                selectedIndex < 3 ? 2 << selectedIndex :
                selectedIndex == 3 ? 4 :
                8;
            this.finByte = new byte[finByteLen];

            while (0 < jj) {
                try {
                    var3 = new BigDecimal(findText.substring(0, jj));
                    if (var3.compareTo(minValue) >= 0 && maxValue.compareTo(var3) >= 0) {
                        break;
                    }

                    throw new Exception("");
                } catch (Exception e) {
                    --jj;
                }
            }

            if (jj == 0) {
                this.findLabel.setText("<html><FONT color=red>Input must be a number.</FONT>");

            } else if (selectedIndex < 3) {
                BigInteger var7;
                try {
                    var7 = var3.setScale(0, RoundingMode.UNNECESSARY).unscaledValue();
                } catch (Exception e) {
                    var7 = var3.setScale(0, RoundingMode.HALF_DOWN).unscaledValue();
                    flag1 = true;
                }

                if (var7.signum() < 0) {
                    time = var7.longValue();
                    if (this.findComboBoxes[0].getSelectedIndex() == 0) {
                        for (int i = 0; i < this.finByte.length; ++i) {
                            this.finByte[this.finByte.length - i - 1] =
                                    (byte) ((int) (time & 255L));
                            time >>>= 8;
                        }
                    } else {
                        for (int i = 0; i < this.finByte.length; ++i) {
                            this.finByte[i] = (byte) ((int) (time & 255L));
                            time >>>= 8;
                        }
                    }
                } else {
                    byte[] var2 = var7.toByteArray();
                    ii = Math.min(this.finByte.length, var2.length);
                    Arrays.fill(this.finByte, (byte) 0);
                    if (this.findComboBoxes[0].getSelectedIndex() == 0) {
                        for (int i = 1; i <= ii; ++i) {
                            this.finByte[this.finByte.length - i] =
                                    var2[var2.length - i];
                        }
                    } else {
                        for (int i = 0; i < ii; ++i) {
                            this.finByte[i] = var2[var2.length - 1 - i];
                        }
                    }
                }
            } else {
                float floatValue = var3.floatValue();
                double doubleValue = var3.doubleValue();
                BigDecimal asBigDecimal = new BigDecimal(selectedIndex == 3 ? (double) floatValue : doubleValue);
                ii = var3.compareTo(asBigDecimal);
                this.useFindChar = flag1 = 0 != ii;

                time = var17[0] =
                    selectedIndex == 3 ? (long) Float.floatToRawIntBits(floatValue) :
                    Double.doubleToRawLongBits(doubleValue);

                if (this.findComboBoxes[0].getSelectedIndex() == 0) {
                    for (int i = 0; i < this.finByte.length; ++i) {
                        this.finByte[this.finByte.length - i - 1] =
                                (byte) (int) (time & 0xffL);
                        time >>>= 8;
                    }
                } else {
                    for (int i = 0; i < this.finByte.length; ++i) {
                        this.finByte[i] = (byte) (int) (time & 0xffL);
                        time >>>= 8;
                    }
                }

                if (this.useFindChar) {
                    var17[1] =
                        selectedIndex == 3
                        ? (long) Float.floatToRawIntBits(var14 = 0 < ii ? MathUtils.nextUp(floatValue) : MathUtils.nextDown(floatValue))
                        : Double.doubleToRawLongBits(var11 = 0 < ii ? MathUtils.nextUp(doubleValue) : MathUtils.nextDown(doubleValue));

                    if (selectedIndex == 3) {
                        sb
                            .append(ii < 0 ? "&lt; " : "&gt; ")
                            .append(this.floatFormat.format(new BigDecimal(floatValue)))
                            .append("<br>")
                            .append(ii < 0 ? "&gt; " : "&lt; ")
                            .append(this.floatFormat.format(new BigDecimal(var14)));
                    } else {
                        sb
                            .append(ii < 0 ? "&lt; " : "&gt; ")
                            .append(this.doubleFormat.format(new BigDecimal(doubleValue)))
                            .append("<br>")
                            .append(ii < 0 ? "&gt; " : "&lt; ")
                            .append(this.doubleFormat.format(new BigDecimal(var11)));
                    }

                    this.findChar = new byte[1][2][selectedIndex == 3 ? 4 : 8];

                    for (ii = 0; ii < 2; ++ii) {
                        if (this.findComboBoxes[0].getSelectedIndex() == 0) {
                            for (int i = 0; i < this.findChar[0][ii].length; ++i) {
                                this.findChar[0][ii][this.finByte.length - i - 1] =
                                        (byte) ((int) (var17[ii] & 255L));
                                var17[ii] >>>= 8;
                            }
                        } else {
                            for (int i = 0; i < this.findChar[0][ii].length; ++i) {
                                this.findChar[0][ii][i] =
                                        (byte) ((int) (var17[ii] & 255L));
                                var17[ii] >>>= 8;
                            }
                        }
                    }
                }
            }
        } else if (selectedIndex == 5) {
            findText = findText.trim().replaceAll(" ", "");
            if (findText.startsWith("0x") || findText.startsWith("Ox") || findText.startsWith("ox")) {
                findText = findText.substring(2);
            }

            for (jj = 0; jj < findText.length()
                    && -1 < "0123456789abcdefABCDEF".indexOf(findText.charAt(jj)); ++jj)
                ;

            if (jj < 2) {
                this.findLabel.setText(jj == findText.length()
                    ? " "
                    : "<html><FONT color=red>Input must be a hexa string.</FONT>");
                return;
            }

            this.finByte = new byte[jj >> 1];

            try {
                for (int i = 0; i < this.finByte.length; ++i) {
                    this.finByte[i] = (byte) Integer.parseInt(
                        findText.substring(i << 1, i * 2 + 2), 16);
                }
            } catch (Exception ignored) {
            }

        } else if (selectedIndex == 6) {
            while (0 < jj) {
                try {
                    findTextSub = findText.substring(0, jj);
                    this.finByte = findTextSub.getBytes(StandardCharsets.ISO_8859_1);
                    if (!findTextSub.equals(new String(this.finByte, StandardCharsets.ISO_8859_1))) {
                        throw new Exception("");
                    }
                    break;
                } catch (Exception var28) {
                    --jj;
                }
            }

            if (jj < 1) {
                this.findLabel.setText("<html><FONT color=red>Input must be an ISO-8859-1 string.</FONT>");
                return;
            }
        } else {
            while (0 < jj) {
                try {
                    findTextSub = findText.substring(0, jj);
                    this.finByte = findTextSub.getBytes(
                        selectedIndex == 7 ? StandardCharsets.UTF_8 :
                        this.findComboBoxes[0].getSelectedIndex() == 0 ? StandardCharsets.UTF_16BE :
                        StandardCharsets.UTF_16LE);
                    break;
                } catch (Exception e) {
                    --jj;
                }
            }

            if (jj < 1) {
                this.findLabel.setText("<html><FONT color=red>Input must be an UTF string.</FONT>");
                return;
            }
        }

        if (selectedIndex < 3 || 4 < selectedIndex || !flag1) {
            for (int i = 0; i < this.finByte.length; ++i) {
                ii = this.finByte[i] & 255;
                sb.append(ii < 16 ? "0" : "")
                    .append(Integer.toHexString(ii).toUpperCase());
                if ((i + 1) % 16 == 0) {
                    sb.append("<br>");
                } else if ((i + 1) % (1 << this.findComboBoxes[3].getSelectedIndex()) == 0) {
                    sb.append(" ");
                }
            }
        }

        findText = findText.toUpperCase();
        if (jj == findText.length() - 1
                && (selectedIndex >= 5 || findText.charAt(findText.length() - 1) != 'E')) {
            sb.append("<br><FONT color=red>The last char is invalid.</FONT>");
        } else if (jj < findText.length() - 1
                && (jj != findText.length() - 2
                        || selectedIndex >= 5
                        || findText.charAt(findText.length() - 2) != 'E'
                        || findText.charAt(findText.length() - 1) != '+'
                                && findText.charAt(findText.length() - 1) != '-')) {
            sb
                .append("<br><FONT color=red>The last ")
                .append(findText.length() - jj)
                .append(" characters are invalid.</FONT>");
        }

        if (flag1 && !this.useFindChar) {
            sb.append("<br><FONT color=red>The binary doesn't represent exactly the significand.</FONT>");
        }

        this.findLabel.setText(sb.toString());
        if (findTextSub == null || findTextSub.isEmpty()
            || !this.findCheckBoxIgnoreCase.isSelected()
            || findTextSub.toUpperCase().equals(findTextSub.toLowerCase())) {
            return;
        }

        findTextSub = findTextSub.toUpperCase();
        this.findChar = new byte[findTextSub.length()][][];

        for (jj = 0; jj < findTextSub.length(); ++jj) {
            char ch = findTextSub.charAt(jj);
            int k = ch == Character.toLowerCase(ch) ? 1 : 2;
            if (k == 2) {
                for (selectedIndex = 0; selectedIndex < Accent.s.length; ++selectedIndex) {
                    if (-1 < Accent.s[selectedIndex].indexOf(ch)) {
                        k = Accent.s[selectedIndex].length();
                        break;
                    }
                }
            }

            this.findChar[jj] = new byte[k][];

            for (ii = 0; ii < k; ++ii) {
                if (k < 3) {
                    ch = ii == 0 ? ch : Character.toUpperCase(ch);
                } else if (selectedIndex < Accent.s.length) {
                    ch = Accent.s[selectedIndex].charAt(ii);
                }

                if (selectedIndex == 6) {
                    this.findChar[jj][ii] = new byte[1];
                    this.findChar[jj][ii][0] =
                            ch < 256 ? (byte) ch : this.findChar[jj][0][0];

                } else if (selectedIndex == 8) {
                    this.findChar[jj][ii] = new byte[2];
                    this.findChar[jj][ii][1 - this.findComboBoxes[0].getSelectedIndex()] =
                            (byte) (ch & 255);
                    this.findChar[jj][ii][this.findComboBoxes[0].getSelectedIndex()] =
                            (byte) (ch >> 8);

                } else {
                    this.findChar[jj][ii] = new byte[
                        ch < 128 ? 1 :
                        ch < 2048 ? 2 :
                        ch < 65536 ? 3 :
                        4];
                    if (ch < 128) {
                        this.findChar[jj][ii][0] = (byte) ch;
                    } else {
                        for (int n = this.findChar[jj][ii].length - 1; 0 < n; --n) {
                            this.findChar[jj][ii][n] = (byte) (ch & 0x3f | 0x80);
                            ch = (char) (ch >> 6);
                        }

                        int n = this.findChar[jj][ii].length;
                        int addBits =
                            n == 2 ? 0b11000000 :
                            n == 3 ? 0b11100000 :
                                     0b11110000;
                        this.findChar[jj][ii][0] = (byte) (ch | addBits);
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {}

    @Override
    public void mouseEntered(MouseEvent event) {}

    @Override
    public void mouseExited(MouseEvent event) {}

    @Override
    public void mousePressed(MouseEvent event) {}

    @Override
    public void mouseClicked(MouseEvent event) {
        this.isHexOffset = !this.isHexOffset;
        this.binEdit.setStatus();
    }
}
