package com.github.hexeditor;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.Timer;

class binEdit extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener,
        ActionListener, AdjustmentListener {

    boolean nibArea = true;
    boolean isNibLow = false;
    boolean jSbSource = true;
    boolean isApplet;
    boolean isOled = false;
    int[] xPos;
    int[] xNib = new int[32];
    int[] xTxt = new int[16];
    int[] cShift = new int[256];
    int wChar = -1;
    int wPanel = -1;
    int hMargin = -1;
    int hChar = -1;
    int hPanel = -1;
    int fontSize = 0;
    int caretVisible = 0;
    int maxRow = -1;
    int maxPos;
    int hLimit = -1;
    Font font;
    long virtualSize = 0L;
    long scrPos = 0L;
    long firstPos = 0L;
    long lastPos = 0L;
    long newPos = 0L;
    long clipboardSize;
    long jSBStep = 1L;
    JFileChooser fileChooser;
    JScrollBar scrollBar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, 1);
    Object[] InsDelOptions = new Object[7];
    JRadioButton[] InsDelRadioButtons = new JRadioButton[5];
    JTextField InsDelField = new JTextField();
    Timer timer = new Timer(500, this);
    Clipboard clipboard;
    RandomAccessFile randomAccessFile;
    Stack<edObj> undoStack = new Stack<>();
    Vector<byte[]> srcV = new Vector<>();
    Vector<Long> markV = new Vector<>();
    Vector<Long> MarkV = new Vector<>();
    Vector<edObj> edV = new Vector<>();
    Vector<byte[]> copyPasteV = new Vector<>();
    Byte byteCtrlY = null;
    edObj eObj = null;
    edObj eObjCtrlY = null;
    File file1 = null;
    public binPanel topPanel;
    saveT saveThread;
    findT findThread;
    long longInput = 0L;

    public binEdit(binPanel theBinPanel, boolean isApplet) {
        this.setLayout(new BorderLayout());
        this.add(this.scrollBar, "East");
        this.setGrid(14);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        this.addKeyListener(this);
        this.scrollBar.setEnabled(true);
        this.scrollBar.setFocusable(true);
        this.scrollBar.setUnitIncrement(1);
        this.scrollBar.addMouseWheelListener(this);
        this.scrollBar.addAdjustmentListener(this);
        this.timer.addActionListener(this);
        this.topPanel = theBinPanel;
        this.isApplet = isApplet;

        String[] labels = new String[]{
            "Delete",
            "Insert & fill with 0x00",
            "Insert & fill with 0xFF",
            "Insert & fill with 0x20 (space)",
            "or Insert clipboard"
        };
        ButtonGroup buttonGroup = new ButtonGroup();
        for (int i = 0; i < this.InsDelRadioButtons.length; ++i) {
            JRadioButton button = this.InsDelRadioButtons[i] = new JRadioButton(labels[i]);
            button.addActionListener(this);
            buttonGroup.add(button);
            this.InsDelOptions[i < 4 ? i : 6] = button;
        }
        this.InsDelOptions[4] = "<html>Bytes to delete or to insert<br>(0x.. for hexa entry):";
        this.InsDelField.setColumns(9);
        this.InsDelOptions[5] = this.InsDelField;

        if (!isApplet) {
            this.fileChooser = new JFileChooser(System.getProperty("user.dir"));
            this.fileChooser.setAcceptAllFileFilterUsed(false);
            this.fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            this.fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            this.fileChooser.setMultiSelectionEnabled(false);
            this.fileChooser.setDragEnabled(false);
            this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        } else {
            this.jSbSource = false;
            this.scrollBar.setValue(0);
            this.pushHObj(new edObj(0L, 0L, 2),
                "\tTry   Hexeditor.jar with this   virtual file.\n" +
                "  An applet cannot access a real  file nor the    clipboard.\n" +
                "     The File menu,  Ctrl+X, Ctrl+C, & Ctrl+V are    therefore       inhibited.");
        }

        this.focus();
    }

    public void closeFile() {
        if (this.randomAccessFile != null) {
            try {
                this.randomAccessFile.close();
            } catch (Exception e) {
                System.err.println("Ctrl+OQ " + e);
            }
        }

        this.file1 = null;
        this.undoStack.clear();
        this.doVirtual();
        System.gc();
    }

    public void loadFile(File f) {
        this.file1 = f;
        this.topPanel.fileField.setText(
            this.file1.toString() + (this.file1.canWrite() ? "" : " ( ReadOnly ) "));

        try {
            this.randomAccessFile = new RandomAccessFile(this.file1, this.file1.canWrite() ? "rw" : "r");
            this.jSbSource = false;
            this.scrollBar.setValue(0);
            this.undoStack.push(new edObj(0L, this.file1.length(), 0));
            this.doVirtual();
            this.focus();
        } catch (Exception e) {
            System.err.println("loadFile " + e);
        }

        this.eObjCtrlY = null;
        this.byteCtrlY = null;
    }

    private void focus() {
        if (!this.requestFocusInWindow()) {
            this.requestFocus();
        }
    }

    private void setGrid(int fontSize) {
        final byte minFontSize = 11;
        final byte maxFontSize = 35;
        int x = -3;

        if (fontSize != this.fontSize) {
            this.fontSize = Math.max(minFontSize, Math.min(maxFontSize, fontSize));
            this.font = new Font(
                "Monospaced",
                this.fontSize < 27 ? Font.PLAIN : Font.BOLD,
                this.fontSize);
            FontMetrics metrics = this.getFontMetrics(this.font);
            this.cShift = metrics.getWidths();
            this.wChar = -1;

            for (int i = 0; i < 256; ++i) {
                if (i != 9) {
                    this.wChar = Math.max(this.cShift[i], this.wChar);
                }
            }

            this.wChar = Math.max(metrics.charWidth('∙'), this.wChar);

            for (int i = 0; i < 256; ++i) {
                this.cShift[i] = this.wChar - this.cShift[i] >> 1;
            }

            this.wChar >>= 1;
            this.hChar = metrics.getHeight();
            this.hMargin = metrics.getLeading() + metrics.getAscent();
        }

        this.maxRow = (this.hPanel - this.hMargin) / this.hChar + 1;
        this.maxPos = (this.maxRow << 4) - 1;
        this.xPos = new int[Long.toHexString(this.virtualSize + 32L).length() + 1 >> 1 << 1];

        for (int i = this.xPos.length - 1; -1 < i; --i) {
            this.xPos[i] = x += i % 2 == 0 ? 2 : 3;
        }

        for (int i = 0; i < this.xNib.length; ++i) {
            int ax =
                (i & 1) == 1 ? 2 :
                (i & 7) == 0 ? 5 :
                3;
            x += ax;
            this.xNib[i] = x;
        }

        x += 4;

        for (int i = 0; i < this.xTxt.length; ++i) {
            x += 2;
            this.xTxt[i] = x;
        }

        long v = (this.virtualSize >> 4) + 2L;
        if (v < 0x4000_0000L) {
            this.jSbSource = false;
            this.scrollBar.setMaximum((int) v);
            this.jSbSource = false;
            this.scrollBar.setVisibleAmount(this.maxRow);
            this.jSbSource = false;
            this.scrollBar.setBlockIncrement(this.maxRow - 1);
            this.jSBStep = 16L;
        } else {
            int i = (int) (v >> 30);
            this.jSbSource = false;
            this.scrollBar.setMaximum(0x4000_0000);
            this.jSbSource = false;
            this.scrollBar.setVisibleAmount(1);
            this.jSbSource = false;
            this.scrollBar.setBlockIncrement(0x10_0000);
            this.jSBStep = (long) i << 4;
        }
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent event) {
        if (event.getSource() == this.scrollBar && this.jSbSource) {
            this.slideScr((long) this.scrollBar.getValue() * this.jSBStep, false);
        }

        this.jSbSource = true;
    }

    protected void slideScr(long pos, boolean flag) {
        long var4 =
            this.virtualSize < 0x7fff_ffff_ffff_fff0L
                ? this.virtualSize & 0xffff_ffff_ffff_fff0L
                : 0x7fff_ffff_ffff_fff0L;
        if (flag || pos >= 0L) {
            if (flag && (this.lastPos < pos || pos < this.lastPos - (long) this.maxPos)) {
                this.scrPos = this.lastPos - (long) (this.maxPos >> 1) & 0xffff_ffff_ffff_fff0L;
            } else {
                this.scrPos = pos & 0xffff_ffff_ffff_fff0L;
            }
        }

        if (var4 - (long) this.maxPos + 31L <= this.scrPos) {
            this.scrPos = var4 - (long) this.maxPos + 31L;
        }

        if (this.scrPos < 0L) {
            this.scrPos = 0L;
        }

        if (this.scrPos != (long) this.scrollBar.getValue() * this.jSBStep) {
            this.jSbSource = false;
            this.scrollBar.setValue((int) (this.scrPos / this.jSBStep));
        }

        this.setSrc();
        this.rePaint();
        this.caretVisible = 0;
        this.timer.restart();
    }

    protected void goTo(String s) {
        if (s == null) {
            return;
        }

        this.String2long(s);
        if (this.longInput < 0L) {
            this.longInput = this.lastPos;
        }

        this.firstPos = this.lastPos = Math.min(this.longInput, this.virtualSize);
        this.isNibLow = false;
        this.slideScr(0L, true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        this.paintImg(g, true);
    }

    protected void paintImg(Graphics g, boolean flag) {
        final char[] var3 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        final Color[] colors = {
            Color.WHITE,
            Color.BLACK,
            new Color(50, 50, 50, 40),
            new Color(50, 50, 50, 80),
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            Color.YELLOW,
            Color.MAGENTA,
            Color.CYAN,
            Color.GREEN.darker(),
            new Color(0, 0, 0, 0)
        };
        byte[] src = new byte[2];
        int[] xy1 = new int[2];
        int[] xy2 = new int[2];
        long pos2 = 0L;
        if (this.hPanel != this.getHeight()) {
            this.wPanel = this.getWidth();
            this.hPanel = this.getHeight();
            this.setGrid(this.fontSize);
            this.setSrc();
        }

        g.setColor(colors[this.isOled ? 1 : 0]);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setFont(this.font);
        g.setColor(colors[this.isOled ? 7 : 6]);
        this.hLimit = 0;

        char ch;
        int ii;
        for (ii = 0; ii < this.maxRow; ++ii) {
            pos2 = this.scrPos + (long) (ii << 4);
            if ((this.virtualSize | 0xfL) < pos2 || pos2 < 0L) {
                break;
            }

            for (int x : this.xPos) {
                ch = var3[(int) (pos2 & 15L)];
                this.hLimit = this.hMargin + this.hChar * ii;
                g.drawString(
                    "" + ch,
                    this.cShift[ch] + this.wChar * x,
                    this.hLimit);
                pos2 >>= 4;
            }
        }

        if (pos2 < 0L) {
            g.setColor(colors[5]);
            g.drawString(
                "-- Limit = 0x7FFFFFFFFFFFFFFE = Long.MAX_VALUE-1 = 2^63-2 = 9223372036854775806 --",
                0,
                this.hMargin + this.hChar * ii - 3);
        }

        g.setColor(colors[9]);
        boolean posOrder = this.firstPos < this.lastPos;
        xy1 = this.pos2XY(posOrder ? this.firstPos : this.lastPos);
        xy2 = this.pos2XY(!posOrder ? this.firstPos : this.lastPos);
        if (this.lastPos != this.firstPos && this.lastPos >= this.scrPos) {
            if (xy1[1] == xy2[1]) {
                g.fillRect(
                    this.wChar * this.xNib[xy1[0] * 2] - 2,
                    this.hChar * xy1[1] + 3,
                    this.wChar * (this.xNib[xy2[0] * 2] - this.xNib[xy1[0] * 2]),
                    this.hChar - 4);
                g.fillRect(
                    this.wChar * this.xTxt[xy1[0]],
                    this.hChar * xy1[1] + 3,
                    this.wChar * (this.xTxt[xy2[0]] - this.xTxt[xy1[0]]),
                    this.hChar - 4);
            } else if (xy1[1] + 1 == xy2[1] && xy2[0] == 0) {
                g.fillRect(
                    this.wChar * this.xNib[xy1[0] * 2] - 2,
                    this.hChar * xy1[1] + 3,
                    this.wChar * (this.xNib[31] + 2 - this.xNib[xy1[0] * 2]),
                    this.hChar - 4);
                g.fillRect(
                    this.wChar * this.xTxt[xy1[0]],
                    this.hChar * xy1[1] + 3,
                    this.wChar * (this.xTxt[15] + 2 - this.xTxt[xy1[0]]),
                    this.hChar - 4);
            } else {
                g.fillRect(
                    this.wChar * this.xNib[xy1[0] * 2] - 2,
                    this.hChar * xy1[1] + 3,
                    this.wChar * (this.xNib[31] + 2 - this.xNib[xy1[0] * 2]) + 4,
                    this.hChar - 4);
                g.fillRect(
                    this.wChar * this.xNib[0] - 2,
                    this.hChar * (xy1[1] + 1) - 1,
                    this.wChar * (this.xNib[31] + 2 - this.xNib[0]) + 4,
                    this.hChar * (xy2[1] - xy1[1] - 1) + 4);
                if (this.xNib[xy2[0] * 2] != this.xNib[0]) {
                    g.fillRect(
                        this.wChar * this.xNib[0] - 2,
                        this.hChar * xy2[1] + 3,
                        this.wChar * (this.xNib[xy2[0] * 2] - this.xNib[0]),
                        this.hChar - 4);
                }

                g.fillRect(
                    this.wChar * this.xTxt[xy1[0]],
                    this.hChar * xy1[1] + 3,
                    this.wChar * (this.xTxt[15] + 2 - this.xTxt[xy1[0]]),
                    this.hChar - 4);
                g.fillRect(
                    this.wChar * this.xTxt[0],
                    this.hChar * (xy1[1] + 1) - 1,
                    this.wChar * (this.xTxt[15] + 2 - this.xTxt[0]),
                    this.hChar * (xy2[1] - xy1[1] - 1) + 4);
                g.fillRect(
                    this.wChar * this.xTxt[0],
                    this.hChar * xy2[1] + 3,
                    this.wChar * (this.xTxt[xy2[0]] - this.xTxt[0]),
                    this.hChar - 4);
            }
        }

        if (this.isOled) {
            g.setXORMode(Color.BLACK);
        }

        for (int i = 0; i < this.srcV.size() && i < this.maxRow << 4; ++i) {
            int jj = i % 16;
            ii = i >> 4;
            src = this.srcV.get(i);
            g.setColor(colors[src[1] != 1 ? 11 : (this.isOled ? 2 : 2)]);
            g.fillRect(
                this.wChar * this.xNib[jj * 2] - 2,
                this.hChar * ii + 3,
                this.wChar * 5,
                this.hChar - 4);
            g.fillRect(
                this.wChar * this.xTxt[jj],
                this.hChar * ii + 3,
                this.wChar * 2,
                this.hChar - 4);
            g.setColor(colors[2 < src[1] ? (this.isOled ? 0 : 5) : (this.isOled ? 4 : 1)]);
            ch = var3[(0xff & src[0]) >> 4];
            g.drawString(
                "" + ch,
                this.cShift[ch] + this.wChar * this.xNib[jj * 2],
                this.hMargin + this.hChar * ii);
            ch = var3[(0xff & src[0]) % 16];
            g.drawString(
                "" + ch,
                this.cShift[ch] + this.wChar * this.xNib[jj * 2 + 1],
                this.hMargin + this.hChar * ii);
            ch = (char) (0xff & src[0]);
            if (Character.isISOControl(ch)) {
                g.drawString(
                    "∙", this.wChar * this.xTxt[jj], this.hMargin + this.hChar * ii);
            } else {
                g.drawString(
                    "" + ch,
                    this.cShift[ch] + this.wChar * this.xTxt[jj],
                    this.hMargin + this.hChar * ii);
            }
        }

        g.setPaintMode();
        g.setColor(colors[10]);
        if (this.markV != null && !this.markV.isEmpty()) {
            Iterator<Long> markVIter = this.markV.iterator();

            while (markVIter.hasNext()) {
                long mPos = markVIter.next();
                if (this.virtualSize <= mPos) {
                    markVIter.remove();
                } else if (this.scrPos <= mPos && mPos - (long) this.maxPos <= this.scrPos) {
                    xy1 = this.pos2XY(mPos);
                    g.fillRect(
                        this.wChar * (this.xNib[0] - 1),
                        this.hChar * (xy1[1] + 1) - 2,
                        this.wChar * (this.xNib[xy1[0] << 1] - this.xNib[0] + 1) - 2,
                        1);
                    g.fillRect(
                        this.wChar * this.xNib[xy1[0] << 1] - 2,
                        this.hChar * xy1[1] + 3,
                        1,
                        this.hChar - 4);
                    g.fillRect(
                        this.wChar * this.xNib[xy1[0] << 1] - 2,
                        this.hChar * xy1[1] + 3,
                        this.wChar * (this.xNib[31] - this.xNib[xy1[0] << 1] + 3),
                        1);
                    g.fillRect(
                        this.wChar * (this.xTxt[0] - 1),
                        this.hChar * (xy1[1] + 1) - 2,
                        this.wChar * (this.xTxt[xy1[0]] - this.xTxt[0] + 1),
                        1);
                    g.fillRect(
                        this.wChar * this.xTxt[xy1[0]] - 1,
                        this.hChar * xy1[1] + 3,
                        1,
                        this.hChar - 4);
                    g.fillRect(
                        this.wChar * this.xTxt[xy1[0]],
                        this.hChar * xy1[1] + 3,
                        this.wChar * (this.xTxt[15] - this.xTxt[xy1[0]] + 3),
                        1);
                }
            }
        }

        if (this.MarkV != null && !this.MarkV.isEmpty()) {
            Iterator<Long> MarkVIter = this.MarkV.iterator();

            while (MarkVIter.hasNext()) {
                long MPos = MarkVIter.next();
                if (this.virtualSize <= MPos) {
                    MarkVIter.remove();
                } else if (this.scrPos <= MPos && MPos - (long) this.maxPos <= this.scrPos) {
                    xy1 = this.pos2XY(MPos);
                    g.fillRect(
                        this.wChar * (this.xNib[0] - 1),
                        this.hChar * (xy1[1] + 1) - 2,
                        this.wChar * (this.xNib[xy1[0] << 1] - this.xNib[0] + 1),
                        2);
                    g.fillRect(
                        this.wChar * this.xNib[xy1[0] << 1] - 2,
                        this.hChar * xy1[1] + 3,
                        2,
                        this.hChar - 3);
                    g.fillRect(
                        this.wChar * this.xNib[xy1[0] << 1] - 2,
                        this.hChar * xy1[1] + 3,
                        this.wChar * (this.xNib[31] - this.xNib[xy1[0] << 1] + 3),
                        2);
                    g.fillRect(
                        this.wChar * (this.xTxt[0] - 1),
                        this.hChar * (xy1[1] + 1) - 2,
                        this.wChar * (this.xTxt[xy1[0]] - this.xTxt[0] + 1),
                        2);
                    g.fillRect(
                        this.wChar * this.xTxt[xy1[0]] - 1,
                        this.hChar * xy1[1] + 3,
                        2,
                        this.hChar - 3);
                    g.fillRect(
                        this.wChar * this.xTxt[xy1[0]],
                        this.hChar * xy1[1] + 3,
                        this.wChar * (this.xTxt[15] - this.xTxt[xy1[0]] + 3),
                        2);
                }
            }
        }

        if (this.scrPos <= this.lastPos && this.lastPos - (long) this.maxPos <= this.scrPos) {
            g.setColor(colors[8]);
            xy2 = this.pos2XY(this.lastPos);
            if (this.caretVisible < 2 || !flag) {
                int xx = this.nibArea
                    ? this.xNib[(xy2[0] << 1) + (this.isNibLow ? 1 : 0)]
                    : this.xTxt[xy2[0]];
                g.fillRect(
                    this.wChar * xx - 1,
                    this.hChar * xy2[1] + 3,
                    2,
                    this.hChar - 4);
            }

            int xx = this.nibArea
                ? this.xTxt[xy2[0]]
                : this.xNib[xy2[0] << 1];
            g.fillRect(
                this.wChar * xx,
                this.hChar * (xy2[1] + 1) - 2,
                this.wChar << (this.nibArea ? 1 : 2),
                2);
        }
    }

    private int[] pos2XY(long pos) {
        pos -= this.scrPos;
        int pos2 =
            pos < 0L ? 0 :
            pos < (long) (this.maxPos + 1) ? (int) pos :
            this.maxPos + 1;
        return new int[]{pos2 % 16, pos2 >> 4};
    }

    protected void rePaint() {
        this.repaint();
        this.setStatus();
    }

    protected void setStatus() {
        int selectedIndex1 = this.topPanel.viewComboBox[1].getSelectedIndex();

        if (this.firstPos != this.lastPos) {
            if (this.lastPos - this.firstPos < 0x7fff_ffffL
                && this.firstPos - this.lastPos < 0x7fff_ffffL) {
                this.topPanel.bytesSelectedField.setText(this.lastPos - this.firstPos + " bytes selected.");
            } else {
                this.topPanel.bytesSelectedField.setForeground(Color.red);
                this.topPanel.bytesSelectedField.setText("Don't select more than 2^31-1 bytes!");
            }
        } else {
            String offsetString = (this.isApplet ? "Offset: " : "<html>Offset:&nbsp;<b>") +
                this.coloredLong(this.lastPos) +
                "/-" +
                this.coloredLong(this.virtualSize - this.lastPos);
            this.topPanel.bytesSelectedField.setForeground(Color.black);
            this.topPanel.bytesSelectedField.setText(offsetString);
        }

        this.topPanel.viewField.setText("");
        if (this.lastPos >= this.scrPos && this.scrPos + (long) this.srcV.size() >= this.lastPos) {
            if (this.lastPos <= this.virtualSize) {
                int nBits =
                    selectedIndex1 == 0 || selectedIndex1 == 1 ? 1 :
                    selectedIndex1 == 2 || selectedIndex1 == 3 ? 2 :
                    selectedIndex1 == 4 || selectedIndex1 == 5 || selectedIndex1 == 8 ? 4 :
                    selectedIndex1 == 6 || selectedIndex1 == 7 || selectedIndex1 == 9 ? 8 :
                    selectedIndex1 == 10 || selectedIndex1 == 11 ? 64 :
                    128;
                long posCapped = Math.min(this.virtualSize, this.scrPos + (long) this.srcV.size());
                int posDiff = (int) (posCapped - this.lastPos);
                if (posDiff == 0 || posDiff < nBits && nBits < 9) {
                    return;
                }

                posDiff =
                    nBits < posDiff ? nBits :
                    selectedIndex1 != 12 ? posDiff :
                    posDiff >> 1 << 1;
                byte[] bytes = new byte[posDiff];

                for (posDiff = 0; posDiff < bytes.length; ++posDiff) {
                    int pos = (int) (this.lastPos - this.scrPos + (long) posDiff);
                    bytes[posDiff] = this.srcV.get(pos)[0];
                }

                nBits = bytes[0];

                String viewText = "";
                try {
                    if (selectedIndex1 == 0) {
                        StringBuilder sb = new StringBuilder(viewText);
                        for (posDiff = 0; posDiff < 8; ++posDiff) {
                            sb.append((nBits & 0x80) == 0x80 ? '1' : '0');
                            if (posDiff == 3) {
                                sb.append(' ');
                            }

                            nBits <<= 1;
                        }
                        viewText = sb.toString();

                    } else if (selectedIndex1 == 1) {
                        viewText = String.format("%s / %s", nBits, nBits & 0xff);

                    } else if (selectedIndex1 == 8) {
                        viewText = this.topPanel.floatFormat.format(
                            Float.intBitsToFloat(new BigInteger(bytes).intValue()));

                    } else if (selectedIndex1 == 9) {
                        viewText = this.topPanel.doubleFormat.format(
                            Double.longBitsToDouble(new BigInteger(bytes).longValue()));

                    } else if (selectedIndex1 == 10) {
                        viewText = new String(bytes,
                            this.topPanel.cp437Available
                            ? Charset.forName("cp437")
                            : StandardCharsets.ISO_8859_1);

                    } else if (selectedIndex1 == 11) {
                        viewText = new String(bytes,
                            StandardCharsets.UTF_8);

                    } else if (selectedIndex1 == 12) {
                        viewText = new String(bytes,
                            this.topPanel.viewComboBox[0].getSelectedIndex() < 1
                            ? StandardCharsets.UTF_16BE
                            : StandardCharsets.UTF_16LE);

                    } else {
                        byte[] bytes2 = new byte[
                            selectedIndex1 < 6 ? selectedIndex1 :
                            selectedIndex1 == 6 ? 8 :
                            9];
                        bytes2[0] = 0;

                        if (this.topPanel.viewComboBox[0].getSelectedIndex() < 1) {
                            if ((bytes2.length & 1) == 0) {
                                System.arraycopy(bytes, 0, bytes2, 0, bytes2.length);
                            } else {
                                System.arraycopy(bytes, 0, bytes2, 1, bytes2.length - 1);
                            }
                        } else {
                            for (posDiff = bytes2.length & 1; posDiff < bytes2.length; ++posDiff) {
                                bytes2[posDiff] = bytes[bytes2.length - posDiff - 1];
                            }
                        }

                        viewText = new BigInteger(bytes2).toString();
                    }
                } catch (Exception e) {
                    System.err.println("setStatus " + e);
                }

                this.topPanel.viewField.setText(viewText.replaceAll("[\t\n]", "  "));
                this.topPanel.viewField.setCaretPosition(0);
            }
        }
    }

    private String coloredLong(long pos) {
        boolean isHexOffset = this.topPanel.isHexOffset;
        StringBuilder sb = new StringBuilder(isHexOffset ? "0x" : "");
        String posString = isHexOffset ? Long.toHexString(pos).toUpperCase() : Long.toString(pos);
        int len = posString.length();
        if (isHexOffset && len % 2 == 1) {
            posString = "0" + posString;
        }

        len = isHexOffset ? 0 : len % 3;
        if (this.isApplet) {
            sb.append(posString);
        } else {
            int i;
            for (i = 0; i < posString.length(); ++i) {
                if (i % (isHexOffset ? 4 : 6) == len) {
                    sb.append("<FONT color=blue>");
                }

                sb.append(posString.charAt(i));
                if (i % (isHexOffset ? 4 : 6) == (isHexOffset ? 1 : 2 + len)) {
                    sb.append("</FONT>");
                }
            }

            if (!isHexOffset && i % 6 < 3 + len) {
                sb.append("</FONT>");
            }
        }

        return sb.toString();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == this.timer) {
            int[] xy = this.pos2XY(this.lastPos);
            this.caretVisible = ++this.caretVisible % 4;
            if ((this.caretVisible & 1) == 0) {
                this.paintImmediately(
                    this.wChar * (this.nibArea ? this.xNib[xy[0] << 1] : this.xTxt[xy[0]])
                        - 1,
                    this.hChar * xy[1] + 3,
                    this.wChar + 1 << 1,
                    this.hChar - 3);
            }

        } else if (event.getSource() != this.InsDelRadioButtons[4]) {
            this.InsDelField.setEnabled(true);

        } else {
            this.InsDelField.setEnabled(true);
            boolean isHexOffset = this.topPanel.isHexOffset;
            String offsetString =
                isHexOffset
                ? Long.toHexString(this.clipboardSize).toUpperCase()
                : Long.toString(this.clipboardSize);
            if (isHexOffset && offsetString.length() % 2 == 1) {
                offsetString = "0" + offsetString;
            }

            this.InsDelField.setText((isHexOffset ? "0x" : "") + offsetString);
            this.fromClipboard(true);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        this.setCursor(new Cursor(Cursor.TEXT_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent event) {
        this.setCursor(null);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        this.focus();
        this.lastPos = this.getCaretPos(event);
        this.caretVisible = 0;
        if (event.isShiftDown()) {
            this.isNibLow = false;
            this.rePaint();
        } else {
            this.firstPos = this.lastPos;
            this.rePaint();
            this.timer.restart();
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        this.newPos = this.getCaretPos(event);
        if (this.lastPos != this.newPos) {
            this.lastPos = this.newPos;
            this.isNibLow = false;
            this.rePaint();
        }
    }

    protected long getCaretPos(MouseEvent var1) {
        int n = -2;
        int width =
                (this.getWidth() <= var1.getX() ? this.getWidth() :
                0 < var1.getX() ? var1.getX() - 3 * this.wChar / 2 :
                0)
            / this.wChar;

        for (int i = 0; i < this.xNib.length && n < 0; ++i) {
            if (width < this.xNib[i]) {
                n = i;
            }
        }

        if (n < 0 && width < this.xNib[this.xNib.length - 1] + 3) {
            n = this.xNib.length - 1;
        }

        this.nibArea = -2 < n;
        this.isNibLow = 1 == (n & 1);
        n >>= 1;

        for (int i = 0; i < this.xTxt.length && n < 0; ++i) {
            if (width < this.xTxt[i]) {
                n = i;
            }
        }

        int height =
            this.getHeight() <= var1.getY() ? this.maxRow - 1 :
            0 < var1.getY() ? var1.getY() / this.hChar :
            0;
        this.newPos = this.scrPos
            + (long) (height << 4)
            + (long) (n < 0 ? 15 : n);
        if (this.virtualSize <= this.newPos || this.newPos < 0L) {
            this.newPos = this.virtualSize;
            this.isNibLow = false;
        }

        if ((this.lastPos != this.newPos || this.lastPos + 1L != this.newPos)
                && !this.undoStack.isEmpty()) {
            this.undoStack.lastElement().isEditing = false;
        }

        return this.newPos;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        if (!event.isControlDown()) {
            long scroll = event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL ? 32L :
                (long) this.maxPos + 1L;
            this.slideScr(this.scrPos + (long) event.getWheelRotation() * scroll, false);

        } else {
            this.setGrid(this.fontSize - 3 * event.getWheelRotation());
            this.slideScr(this.scrPos, true);
            this.rePaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
    }

    @Override
    public void keyPressed(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE:
                this.KeyFromMenu(KeyEvent.VK_Z);

            case KeyEvent.VK_PAGE_UP:
                this.newPos = this.lastPos - ((long) this.maxPos - 15L);
                break;
            case KeyEvent.VK_PAGE_DOWN:
                this.newPos = this.lastPos + ((long) this.maxPos - 15L);
                break;
            case KeyEvent.VK_END:
                if (!event.isControlDown() && this.lastPos + 15L <= this.virtualSize) {
                    this.newPos = this.lastPos | 15L;
                } else {
                    this.newPos = this.virtualSize - 1L;
                }
                break;
            case KeyEvent.VK_HOME:
                if (!event.isControlDown()) {
                    this.newPos = this.lastPos & -16L;
                } else {
                    this.newPos = 0L;
                }
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                this.newPos = this.lastPos - (this.isNibLow ? 0L : 1L);
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                this.newPos = this.lastPos - 16L;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                this.newPos = this.lastPos + 1L;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                this.newPos = this.lastPos + 16L;
                break;
            default:
                return;
        }

        this.isNibLow = false;
        long posDiff = this.newPos - this.lastPos;
        if (this.newPos == -1L && this.lastPos < 99_999L) {
            this.lastPos = 0L;
        } else if (this.newPos <= 0L && this.lastPos < 99_999L) {
            this.lastPos = this.newPos & 15L;
        } else if ((this.newPos == this.virtualSize || this.newPos == this.virtualSize + 1L) && posDiff == 1L) {
            this.lastPos = this.virtualSize;
        } else if (this.virtualSize > this.newPos && this.newPos >= 0L) {
            this.lastPos = this.newPos;
        } else {
            this.lastPos = (this.virtualSize - 1L & -16L)
                + (this.newPos & 15L)
                - ((this.newPos & 15L) <= (this.virtualSize - 1L & 15L) ? 0L : 16L);
        }

        if (!event.isShiftDown()) {
            this.firstPos = this.lastPos;
        }

        if (!this.undoStack.empty() && this.undoStack.lastElement().isEditing) {
            this.undoStack.lastElement().isEditing = false;
        }

        if (0x7fff_ffff_ffff_fff0L - (long) this.maxPos + 16L < this.scrPos) {
            this.slideScr(this.scrPos = Long.MAX_VALUE & -16L - (long) this.maxPos + 15L, true);

        } else if (this.newPos >= this.scrPos && posDiff != -((long) this.maxPos - 15L)) {

            if (this.scrPos >= this.newPos - (long) this.maxPos
                    && posDiff != (long) this.maxPos - 15L) {
                this.timer.stop();
                this.rePaint();
                this.caretVisible = 0;
                this.timer.restart();
            } else {
                this.slideScr(this.scrPos += posDiff + 15L & -16L, true);
            }
        } else {
            this.slideScr(this.scrPos = this.scrPos + posDiff & -16L, true);
        }
    }

    public void KeyFromMenu(int keyCode) {
        final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        boolean hasThread = this.saveThread != null || this.findThread != null;
        final Object[] messages = new Object[]{"<html>Valid entry are:<br>decimal, hexa (0x..) or percent (..%)"};
        long first;
        long last;

        switch (keyCode) {
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_ADD:
            case KeyEvent.VK_SUBTRACT:
            case KeyEvent.VK_PLUS:
                final int delta = keyCode == KeyEvent.VK_ADD || keyCode == KeyEvent.VK_PLUS ? 3 : -3;
                this.setGrid(this.fontSize + delta);
                this.slideScr(this.scrPos, true);
                this.rePaint();
                break;
            case KeyEvent.VK_A:
                if (this.virtualSize < 0x8000_0000L) {
                    this.firstPos = 0L;
                    this.lastPos = this.virtualSize;
                    this.rePaint();
                } else {
                    JOptionPane.showMessageDialog(this, "Selection cannot be greater than 2GiB");
                }
                break;
            case KeyEvent.VK_C:
            case KeyEvent.VK_X:
                if (this.firstPos != this.lastPos && !hasThread && !this.isApplet) {
                    boolean isOrder = this.firstPos < this.lastPos;
                    first = isOrder ? this.firstPos : this.lastPos;
                    last = !isOrder ? this.firstPos : this.lastPos;

                    try {
                        this.copyPasteV = this.virtualStack(first, last);
                        char[] chars;
                        if (!this.nibArea) {
                            chars = new char[this.copyPasteV.size()];

                            for (int i = 0; i < this.copyPasteV.size(); ++i) {
                                byte[] bytes = this.copyPasteV.get(i);
                                chars[i] = (char) (255 & bytes[0]);
                                if (Character.isISOControl(chars[i])
                                        && "\t\\u000A\f\\u000D".indexOf(chars[i]) < 0) {
                                    throw new Exception(
                                        "'" + (chars[i] & 255) + "' isIsoControl");
                                }
                            }
                        } else {
                            chars = new char[this.copyPasteV.size() << 1];

                            for (int i = 0; i < this.copyPasteV.size(); ++i) {
                                byte[] bytes = this.copyPasteV.get(i);
                                chars[2 * i] = hexDigits[(255 & bytes[0]) >> 4];
                                chars[2 * i + 1] = hexDigits[(255 & bytes[0]) % 16];
                            }
                        }

                        this.clipboard.setContents(
                            new StringSelection(new String(chars)), null);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            this, "Can't copy text into the clipboard:\n" + e);
                    }

                    if (keyCode == KeyEvent.VK_X) {
                        this.pushHObj(new edObj(first, last - first, 8), null);
                    }
                }
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_U:
                last = 0L;
                long pos2 = this.virtualSize;
                this.isNibLow = false;
                if (this.markV != null && !this.markV.isEmpty()) {
                    for (Iterator<Long> iter = this.markV.iterator();
                            iter.hasNext();
                            pos2 = this.lastPos < first && first < pos2 ? first : pos2) {
                        first = iter.next();
                        if (last < first && first < this.lastPos) {
                            last = first;
                        }
                    }
                }

                if (this.MarkV != null && !this.MarkV.isEmpty()) {
                    for (Iterator<Long> iter = this.MarkV.iterator();
                             iter.hasNext();
                             pos2 = this.lastPos < first && first < pos2 ? first : pos2) {
                        first = iter.next();
                        if (last < first && first < this.lastPos) {
                            last = first;
                        }
                    }
                }

                this.firstPos = this.lastPos =
                    keyCode == KeyEvent.VK_U ? last : pos2;
                this.slideScr(0L, true);
                break;
            case KeyEvent.VK_F:
                if (this.findThread == null) {
                    this.topPanel.find();
                }
                break;
            case KeyEvent.VK_G:
                String res1 = JOptionPane.showInputDialog(
                    this,
                    messages,
                    "Hexeditor.jar: GoTo",
                    JOptionPane.PLAIN_MESSAGE);
                this.goTo(res1);
                break;
            case KeyEvent.VK_M:
                Long pos = this.lastPos;
                if (this.markV.remove(pos)) {
                    this.MarkV.add(pos);
                } else if (!this.MarkV.remove(pos)) {
                    this.markV.add(pos);
                }

                this.rePaint();
                break;
            case KeyEvent.VK_O:
            case KeyEvent.VK_Q:
                if (!hasThread && !this.isApplet) {
                    if (this.randomAccessFile == null && !this.undoStack.empty()
                            || this.randomAccessFile != null && 1 < this.undoStack.size()) {
                        int res2 = JOptionPane.showConfirmDialog(
                            this,
                            "Save the current modified file?");
                        if (res2 == JOptionPane.CANCEL_OPTION) {
                            break;
                        }

                        if (res2 == JOptionPane.OK_OPTION) {
                            this.save1();
                            break;
                        }
                    }

                    this.closeFile();
                    if (keyCode == KeyEvent.VK_O && this.fileChooser.showOpenDialog(this) == 0) {
                        this.loadFile(this.fileChooser.getSelectedFile());
                    }
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Busy, save or find running.");
                }
                break;
            case KeyEvent.VK_P:
                int width = this.wChar * (this.xTxt[15] + 9);
                width = Math.min(width, this.getWidth());
                BufferedImage image = new BufferedImage(width, this.hLimit + 10, BufferedImage.TYPE_USHORT_565_RGB);
                this.paintImg(image.getGraphics(), false);
                String file1Path1 = this.file1.getPath();
                width = 1;

                File file;
                do {
                    file = new File(file1Path1 + width + ".png");
                    ++width;
                } while (file.exists());

                try {
                    ImageIO.write(image, "png", file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_S:
                if (this.saveThread == null && !this.isApplet) {
                    this.save1();
                }
                break;
            case KeyEvent.VK_T:
                this.nibArea = !this.nibArea;
                this.isNibLow = false;
                this.timer.stop();
                this.repaint();
                this.caretVisible = 0;
                this.timer.restart();
                break;
            case KeyEvent.VK_V:
                if (!hasThread && !this.isApplet) {
                    String pasted = this.fromClipboard(true);
                    if (pasted != null) {
                        edObj eObj = new edObj(this.lastPos, pasted.length(), 4);
                        this.pushHObj(eObj, pasted);
                    }
                }
                break;
            case KeyEvent.VK_W:
                this.isOled = !this.isOled;
                this.rePaint();
                break;
            case KeyEvent.VK_Y:
                if (this.eObjCtrlY != null) {
                    this.pushHObj(this.eObjCtrlY, null);
                    this.eObjCtrlY = null;
                } else if (this.byteCtrlY != null) {
                    this.eObj.bytes.push(this.byteCtrlY);
                    this.firstPos = ++this.lastPos;
                    if (this.scrPos < this.lastPos - (long) this.maxPos) {
                        this.scrPos += 16L;
                    }

                    this.doVirtual();
                    this.byteCtrlY = null;
                }
                break;
            case KeyEvent.VK_Z:
                if (!hasThread && !this.undoStack.empty()
                        && (1 < this.undoStack.size() ||
                            1 == this.undoStack.size() && 2 < this.undoStack.lastElement().a1)) {
                    this.eObj = this.eObjCtrlY = this.undoStack.lastElement();
                    if (this.eObj.isEditing && !this.eObj.bytes.empty()) {
                        this.byteCtrlY = this.eObj.bytes.pop();
                        if (!this.isNibLow) {
                            this.firstPos = --this.lastPos;
                        } else {
                            this.isNibLow = false;
                        }

                        this.eObj.size = this.eObj.bytes.size();
                        if (this.eObj.size == 0L) {
                            this.undoStack.pop();
                            this.byteCtrlY = null;
                        } else {
                            this.eObjCtrlY = null;
                        }
                    } else {
                        this.firstPos = this.lastPos = this.eObj.p1;
                        this.undoStack.pop();
                    }

                    this.doVirtual();
                    this.slideScr(this.scrPos, true);
                }
                break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_INSERT:
                if (!hasThread) {
                    this.InsDelRadioButtons[keyCode == KeyEvent.VK_DELETE ? 0 : 1].setSelected(true);
                    this.InsDelField.setEnabled(true);
                    String pasted = this.fromClipboard(false);
                    int res = JOptionPane.showConfirmDialog(
                        this,
                        this.InsDelOptions,
                        "Hexeditor.jar: DEL/INS",
                        JOptionPane.OK_CANCEL_OPTION);
                    if (res == JOptionPane.OK_OPTION) labelHandleConfirmation: {
                        if (this.InsDelRadioButtons[4].isSelected()) {
                            if (!this.isApplet && pasted != null &&
                                    this.virtualSize + (long) pasted.length() < Long.MAX_VALUE) {
                                this.pushHObj(new edObj(this.lastPos, pasted.length(), 6), pasted);
                            }
                        } else {
                            String text = this.InsDelField.getText();
                            if (text == null) {
                                break labelHandleConfirmation;
                            }

                            this.String2long(text);
                            if (this.longInput < 1L) {
                                break labelHandleConfirmation;
                            }

                            if (this.InsDelRadioButtons[0].isSelected()) {
                                if (this.longInput + this.lastPos >= this.virtualSize) {
                                    this.longInput = this.virtualSize - this.lastPos;
                                }
                            } else if (Long.MAX_VALUE < this.virtualSize + this.longInput) {
                                this.longInput = Long.MAX_VALUE - this.virtualSize;
                            }

                            this.pushHObj(
                                new edObj(
                                    this.lastPos,
                                    this.longInput,
                                    this.InsDelRadioButtons[0].isSelected() ? 8 : 6),
                                this.InsDelRadioButtons[0].isSelected() ? null :
                                this.InsDelRadioButtons[1].isSelected() ? " " :
                                this.InsDelRadioButtons[2].isSelected() ? "ÿ"
                                : " ");
                        }

                        this.isNibLow = false;
                        this.eObjCtrlY = null;
                        this.byteCtrlY = null;
                        this.InsDelField.setText("");
                    }
                }
        }

        this.focus();
    }

    private void pushHObj(edObj eObj, String s) {
        if (!this.undoStack.isEmpty()) {
            this.eObj = this.undoStack.lastElement();
            this.eObj.isEditing = false;
        }

        if (s != null) {
            for (int i = 0; i < s.length(); ++i) {
                eObj.bytes.push((byte) s.charAt(i));
            }
        }

        this.undoStack.push(eObj);
        this.firstPos = this.lastPos;
        this.doVirtual();
    }

    private String fromClipboard(boolean isUI) {
        String s = null;
        StringBuilder sb = new StringBuilder();

        try {
            s = (String) this.clipboard.getContents(null)
                .getTransferData(DataFlavor.stringFlavor);
            if (s == null || s.isEmpty()) {
                throw new Exception("nothing to paste");
            }

            if (Long.MAX_VALUE < this.lastPos + (long) s.length()) { // FIXME overflow (=> condition always false)
                throw new Exception("file cannot exceed Long.MAX_VALUE");
            }

            if (this.nibArea) {
                if (s.length() % 2 != 0) {
                    throw new Exception(
                        "Nibble area, String must be an hexa string with odd characters.");
                }

                s = s.toUpperCase();

                for (int i = 0; i < s.length(); i += 2) {
                    if ("0123456789ABCDEFabcdef".indexOf(s.charAt(i)) < 0
                        || "0123456789ABCDEFabcdef".indexOf(s.charAt(i + 1)) < 0) {
                        throw new Exception("Nibble area, String must be an hexa string.");
                    }

                    sb.append((char) (("0123456789ABCDEFabcdef".indexOf(s.charAt(i)) << 4)
                                + "0123456789ABCDEFabcdef".indexOf(s.charAt(i + 1))));
                }

                s = sb.toString();
            }

            this.clipboardSize = s.length();

        } catch (Exception e) {
            s = null;
            this.clipboardSize = 0L;
            if (isUI) {
                JOptionPane.showMessageDialog(
                    this, "Can't paste text from the clipboard:\n" + e);
            }
        }

        return s;
    }

    protected void String2long(String s) {
        this.longInput = -1L;
        int sLen = s.length();
        if (sLen == 0 || s.equals("-") || s.equals("+")) {
            return;
        }

        int nBinPrefix = -1;
        int shift = 0;
        BigDecimal bigDecimal = null;
        BigInteger bigInteger = null;
        boolean flag = false;
        boolean startsWith0x = s.startsWith("0x") || s.startsWith("Ox") || s.startsWith("ox");
        String siPrefixes = "yzafpnµm kMGTPEZY";
        String binPrefixes = "KMGTPE";
//        String[] var10000 = new String[]{"c", "d", "da", "h"};

        s.replaceAll(" ", ""); // FIXME assign returned value
        if (1 < s.length() && !startsWith0x) {
            flag = 'i' == s.charAt(s.length() - 1);
            if (flag) {
                nBinPrefix = binPrefixes.indexOf(s.charAt(s.length() - 2));
            } else {
                int shift1 =
                    s.endsWith("c") ? -2 :
                    s.endsWith("d") ? -1 :
                    s.endsWith("da") ? 1 :
                    s.endsWith("h") ? 2 :
                    s.endsWith("%") ? -2 :
                    0;
                int nSiPrefix = siPrefixes.indexOf(s.charAt(s.length() - 1));
                shift =
                    shift1 != 0 ? shift1 :
                    -1 < nSiPrefix ? nSiPrefix * 3 - 24 :
                    0;
            }
        }

        if (flag && (s.length() < 3 || nBinPrefix < 0)) {
            return;
        }

        if (startsWith0x) {
            if (s.length() < 3) {
                return;
            }

            try {
                bigInteger = new BigInteger(s.substring(2, sLen), 16);
            } catch (Exception e) {
                return;
            }
        } else {
            while (0 < sLen) {
                try {
                    bigDecimal = new BigDecimal(s.substring(0, sLen));
                    break;
                } catch (Exception e) {
                    --sLen;
                }
            }

            if (sLen == 0 || bigDecimal == null) {
                return;
            }

            bigDecimal = bigDecimal.scaleByPowerOfTen(shift)
                .multiply(BigDecimal.valueOf(1L << 10 * (nBinPrefix + 1)));
            bigInteger = bigDecimal.toBigInteger();
        }

        long longValue = bigInteger.longValue();
        if (bigInteger.signum() < 0) {
            this.longInput = -1L;
        } else if (BigInteger.valueOf(Long.MAX_VALUE).compareTo(bigInteger) < 0) {
            this.longInput = Long.MAX_VALUE;
        } else {
            this.longInput = longValue;
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
        boolean hasThread = this.saveThread != null || this.findThread != null;
        char ch = event.getKeyChar();
        int index = "0123456789ABCDEFabcdef".indexOf(Character.toUpperCase(ch));

        if (event.isAltDown()
                || event.isControlDown()
                || Character.isISOControl(ch)
                || ch >= 256
                || event.getSource() != this
                || 0L >= this.virtualSize
                || hasThread
                || (this.nibArea && -1 == index)) {
            return;
        }

        if (!this.undoStack.empty() && this.undoStack.lastElement().isEditing) {
            this.eObj = this.undoStack.lastElement();
        } else {
            this.eObj = new edObj(this.lastPos, 0L, 4);
            this.undoStack.push(this.eObj);
            this.eObj.isEditing = true;
        }

        if (!this.nibArea) {
            this.eObj.bytes.push((byte) ch);
            if (this.lastPos < Long.MAX_VALUE) {
                this.firstPos = ++this.lastPos;
            }
        } else if (0 <= "0123456789ABCDEFabcdef".indexOf(ch)) {
            byte src;
            int pos = (int) (this.lastPos - this.scrPos);
            if (pos < this.srcV.size()) {
                src = this.srcV.get(pos)[0];
            } else {
                src = 0;
            }

            src = (byte) (
                this.isNibLow ? (src & 240) + index :
                (index << 4) + (src & 15));
            if (this.isNibLow
                    && !this.eObj.bytes.empty()
                    && this.eObj.p1 + (long) this.eObj.bytes.size() == this.lastPos + 1L) {
                this.eObj.bytes.pop();
            }

            this.eObj.bytes.push(src);
            this.isNibLow = !this.isNibLow;
            if (!this.isNibLow && this.lastPos < Long.MAX_VALUE) {
                this.firstPos = ++this.lastPos;
            }
        }

        if (this.scrPos < this.lastPos - (long) this.maxPos) {
            this.scrPos += 16L;
        }

        this.eObjCtrlY = null;
        this.byteCtrlY = null;
        this.doVirtual();
    }

    protected void doVirtual() {
        this.edV.clear();
        if (this.undoStack.isEmpty()) {
            this.scrPos = this.firstPos = this.lastPos = this.virtualSize = 0L;
            this.markV.clear();
            this.MarkV.clear();
            this.jSbSource = false;
            this.scrollBar.setValue(0);
            this.topPanel.fileField.setText("");
            this.topPanel.findFields[1].setText("");
            this.setGrid(this.fontSize);

        } else {
            edObj eObj = this.undoStack.lastElement();
            if (eObj.a1 != 6 && eObj.a1 != 8) {
                eObj.size = !eObj.bytes.isEmpty() ? (long) eObj.bytes.size() : eObj.size;
                eObj.p2 = eObj.p1 + eObj.size;
            }

            if (!this.isApplet) {
                String text = this.topPanel.fileField.getText();
                if (text.endsWith(" *")) {
                    text = text.substring(0, text.length() - 2);
                }

                this.topPanel.fileField.setText(
                    text + (1 < this.undoStack.size() ? " *" : ""));
            }

            eObj = this.undoStack.firstElement();
            this.edV.add(new edObj(0L, eObj.p2, eObj.offset, eObj));

            for (int i = 1; i < this.undoStack.size(); ++i) {
                eObj = this.undoStack.get(i);
                edObj eObj1 =
                    eObj.a1 == 8 ? null :
                    new edObj(eObj.p1, eObj.p2, eObj.offset, eObj);
                long size =
                    eObj.a1 == 6 ? eObj.size :
                    eObj.a1 == 8 ? -eObj.size :
                    0L;
                int edSize = this.edV.size() - 1;
                if (eObj != null && eObj.p1 != eObj.p2) {
                    for (; 0 <= edSize; --edSize) {
                        edObj eObj2 = this.edV.get(edSize);
                        if (edSize == this.edV.size() - 1 && eObj2.p2 == eObj.p1) {
                            this.v1AddNoNull(edSize + 1, eObj1);
                            break;
                        }

                        if (eObj.p2 <= eObj2.p1) {
                            eObj2.p1 += size;
                            eObj2.p2 += size;
                        } else {
                            if (eObj.a1 == 6 && eObj2.p1 == eObj.p1) {
                                eObj2.p1 += size;
                                eObj2.p2 += size;
                                this.v1AddNoNull(edSize, eObj1);
                                break;
                            }

                            if (eObj.a1 != 6 && eObj.p1 <= eObj2.p1 && eObj2.p2 <= eObj.p2) {
                                this.edV.remove(edSize);
                                if (eObj2.p2 == eObj.p2) {
                                    this.v1AddNoNull(edSize, eObj1);
                                }
                            } else {
                                if (eObj.a1 != 6 && eObj.p1 < eObj2.p2 && eObj2.p2 <= eObj.p2) {
                                    if (eObj2.p2 == eObj.p2) {
                                        this.v1AddNoNull(edSize + 1, eObj1);
                                    }

                                    eObj2.p2 = eObj.p1;
                                    break;
                                }

                                if (eObj.a1 == 6 || eObj.p1 > eObj2.p1 || eObj2.p1 >= eObj.p2) {
                                    if (eObj2.p1 < eObj.p1 && (eObj.p2 < eObj2.p2 || eObj.a1 == 6)) {
                                        edObj eObj2a = this.v1Clone(eObj2);
                                        eObj2a.p2 = eObj.p1;
                                        eObj2.offset += (eObj.a1 == 6 ? eObj.p1 : eObj.p2) - eObj2.p1;
                                        eObj2.p1 = eObj.a1 == 8 ? eObj.p1 : eObj.p2;
                                        eObj2.p2 += size;
                                        if (eObj2.p1 == eObj2.p2) {
                                            this.edV.remove(edSize);
                                        }

                                        this.v1AddNoNull(edSize, eObj1);
                                        this.v1AddNoNull(edSize, eObj2a);
                                    }
                                    break;
                                }

                                eObj2.offset += eObj.p2 - eObj2.p1;
                                eObj2.p1 = eObj.p2;
                                this.v1AddNoNull(edSize, eObj1);
                            }
                        }
                    }
                }
            }

            long p2 =
                this.edV != null && !this.edV.isEmpty()
                    ? this.edV.lastElement().p2
                    : 0L;
            if (this.virtualSize != p2) {
                this.virtualSize = p2;
                this.setGrid(this.fontSize);
            }
        }

        this.setSrc();
        this.rePaint();
        this.caretVisible = 0;
        this.timer.restart();
    }

    protected void v1AddNoNull(int index, edObj eObj) {
        if (eObj != null
                && 0 <= index
                && index <= this.edV.size()
                && eObj.p1 != eObj.p2) {
            this.edV.add(index, eObj);
        }
    }

    protected edObj v1Clone(edObj eObj) {
        if (eObj != null && eObj.p1 != eObj.p2) {
            return new edObj(eObj.p1, eObj.p2, eObj.offset, eObj.o);
        } else {
            return null;
        }
    }

    protected Vector<byte[]> virtualStack(long first, long last) {
        byte[] bytes = new byte[2];
        int i = 0;
        int var10 = 0;
        long first1 = first;
        edObj eObj = null;

        Vector<byte[]> byteV;
        for (byteV = new Vector<>(); i < this.edV.size(); ++i) {
            eObj = this.edV.get(i);
            if (first < eObj.p2) {
                break;
            }
        }

        while (i < this.edV.size()) {
            eObj = this.edV.get(i);
            long p1 = eObj.p1 - eObj.offset;
            long p2 = Math.min(eObj.p2, last);
            if (eObj.o.a1 != 4
                    && eObj.o.a1 != 2
                    && (eObj.o.a1 != 6 || 1 >= eObj.o.bytes.size())) {
                if (eObj.o.a1 == 6) {
                    bytes[1] = (byte) eObj.o.a1;

                    for (bytes[0] = eObj.o.bytes.get(0); first1 < p2; ++first1) {
                        byteV.add(bytes.clone());
                    }
                } else {
                    try {
                        bytes[1] = (byte) (eObj.p1 != eObj.offset ? 1 : eObj.o.a1);
                        byte[] bytes1 = new byte[(int) (p2 - first1)];
                        this.randomAccessFile.seek(first1 - p1);
                        int off = 0;

                        int nRead;
                        while (off < bytes1.length) {
                            nRead = this.randomAccessFile.read(bytes1, off, bytes1.length - off);
                            if (nRead < 0) {
                                throw new IOException("EOF");
                            }

                            off += nRead;
                            if (nRead == 0) {
                                ++var10;
                                if (var10 == 9) {
                                    first1 = p2;
                                    JOptionPane.showMessageDialog(this, "Unable to access file");
                                }
                            }
                        }

                        for (nRead = 0; first1 < p2 || nRead < var10; ++nRead) {
                            bytes[0] = bytes1[nRead];
                            byteV.add(bytes.clone());
                            ++first1;
                        }
                    } catch (Exception e) {
                        System.err.println("virtualStack " + e);
                    }
                }
            } else {
                for (bytes[1] = (byte) (eObj.a1 == 2 && eObj.p1 != eObj.offset ? 1 : eObj.o.a1);
                        first1 < p2; ++first1) {
                    bytes[0] = eObj.o.bytes.get((int) (first1 - p1));
                    byteV.add(bytes.clone());
                }
            }

            if (last < eObj.p2) {
                break;
            }

            ++i;
        }

        return byteV;
    }

    protected void setSrc() {
        long pos = this.scrPos + (long) this.maxPos + 1L;
        long pos2 = pos >= 0L ? pos : Long.MAX_VALUE;
        this.srcV = this.virtualStack(this.scrPos, pos2);
    }

    protected boolean save1() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.setDialogTitle("Save as...");
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        jfc.setMultiSelectionEnabled(false);
        jfc.setDragEnabled(false);
        jfc.setFileFilter(new filterRW());

        if (this.file1 != null && this.file1.canWrite()) {
            jfc.setSelectedFile(this.file1);
        } else {
            jfc.setCurrentDirectory(
                this.file1 == null
                    ? new File(System.getProperty("user.dir"))
                    : this.file1.getParentFile());
        }

        if (jfc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        this.topPanel.saveRunning(true);
        if (this.saveThread != null) {
            this.saveThread.interrupt();
        }

        this.saveThread = new saveT();
        this.saveThread.setDaemon(true);
        this.saveThread.file1 = this.file1;
        this.saveThread.file2 = jfc.getSelectedFile();
        this.saveThread.edV = this.edV;
        this.saveThread.hexV = this;
        this.saveThread.progressBar = this.topPanel.savePBar;
        this.saveThread.start();

        return true;
    }

    protected void save2(File f) {
        if (f != null) {
            this.file1 = new File(f, "");
            this.topPanel.fileField.setText(
                this.file1 + (this.file1.canWrite() ? "" : " (ReadOnly)"));

            try {
                this.randomAccessFile = new RandomAccessFile(this.file1, "rw");
            } catch (Exception e) {
                System.err.println(e);
            }

            this.undoStack.clear();
            this.undoStack.push(new edObj(0L, this.file1.length(), 0));
            this.doVirtual();
        }

        this.topPanel.saveRunning(false);
        this.saveThread = null;
        this.eObjCtrlY = null;
        this.byteCtrlY = null;
    }

    protected void find1() {
        if (this.virtualSize == 0L
                || (this.topPanel.finByte == null && this.topPanel.findChar == null)) {
            return;
        }

        this.String2long(this.topPanel.findFields[1].getText());
        if (this.findThread != null) {
            this.findThread.interrupt();
        }

        this.findThread = new findT();
        this.findThread.setDaemon(true);
        this.findThread.file1 = this.file1;
        this.findThread.edV = this.edV;
        this.findThread.isApplet = this.isApplet;
        this.findThread.ignoreCase = this.topPanel.useFindChar;
        this.findThread.pos =
            this.longInput < 0L
            ? (this.firstPos == this.lastPos ? this.lastPos : this.lastPos + 1L)
            : this.virtualSize - 1L < this.longInput
            ? this.virtualSize - 1L
            : this.virtualSize - 1L == this.longInput
            ? 0L
            : this.longInput + 1L;
        this.findThread.inBytes = this.topPanel.finByte;
        this.findThread.inChars = this.topPanel.findChar;
        this.findThread.wordSize = 1 << this.topPanel.findComboBoxes[3].getSelectedIndex();
        this.findThread.hexV = this;
        this.findThread.jPBar = this.topPanel.findPBar;
        this.topPanel.findRunning(true);
        this.findThread.start();
    }

    protected void find2(long first, long last) {
        this.slideScr(0L, true);
        StringBuilder sb = new StringBuilder("0x");
        String hex = Long.toHexString(last).toUpperCase();
        if (hex.length() % 2 == 1) {
            sb.append("0");
        }

        this.topPanel.findRunning(false);
        this.topPanel.findFields[1].setText(sb.append(hex).toString());
        this.findThread = null;
        this.lastPos = last;
        this.firstPos = first;
        this.isNibLow = false;
        this.slideScr(0L, true);
    }
}
