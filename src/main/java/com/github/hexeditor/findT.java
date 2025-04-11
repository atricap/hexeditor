package com.github.hexeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JProgressBar;

class findT extends Thread {

    File file1;
    Vector<edObj> edV;
    boolean isApplet;
    boolean ignoreCase;
    long pos;
    byte[] inBytes = null;
    byte[][][] inChars = null;
    binEdit hexV;
    JProgressBar jPBar;
    private boolean isFound = false;
    private int realLength = 0;
    private int inCharsLength = 0;
    protected int wordSize;
    private long virtualSize;
    private final Vector pile = new Vector();

    @Override
    public void run() {
        final int twoMiB = 2 * 1024 * 1024;
        final long twoMiBL = 2L * 1024L * 1024L;
        final int oneGiB = 1 * 1024 * 1024 * 1024;
        byte[] bytes = new byte[twoMiB];
        edObj eObj = null;
        if (this.edV == null || this.edV.isEmpty()) {
            return;
        }

        this.virtualSize = this.edV.lastElement().p2;
        this.jPBar.setMaximum(oneGiB);
        int m;
        int k;
        if (!this.ignoreCase) {
            this.inCharsLength = this.inBytes.length;
        } else {
            for (byte[][] inChar : this.inChars) {
                m = 0;
                for (k = 0; k < inChar.length; ++k) {
                    m = Math.min(m, inChar[k].length); // TODO is always 0?!
                }
                this.inCharsLength += m; // TODO ... so this does nothing?
            }
        }

        for (k = 0; k < this.edV.size(); ++k) {
            eObj = this.edV.get(k);
            if (this.pos < eObj.p2) {
                break;
            }
        }

        try (FileInputStream in = this.file1 != null ? new FileInputStream(this.file1) : null) {
            for (long ii = 0L; k < this.edV.size() && this.next(); ++k) {
                eObj = this.edV.get(k);
                long p1a = eObj.p1 - eObj.offset;
                if (eObj.o.a1 == 4 || eObj.o.a1 == 2 || (eObj.o.a1 == 6 && 1 < eObj.o.bytes.size())) {
                    while (this.pos < eObj.p2 && this.next()) {
                        this.findB(eObj.o.bytes.get((int) (this.pos - p1a)));
                    }

                } else if (eObj.o.a1 == 6) {
                    byte var17 = eObj.o.bytes.get(0);

                    while (this.pos < eObj.p2 && this.next()) {
                        boolean isPileEmpty = this.findB(var17);
                        if (isPileEmpty && this.pos < eObj.p2 - (long) this.inCharsLength) {
                            this.pos = eObj.p2 - (long) this.inCharsLength;
                        }
                    }
                } else {
                    for (long i = this.pos - p1a;
                         ii < i;
                         ii += in.skip(i - ii))
                        ;

                    while (this.pos < eObj.p2 && this.next()) {
                        long i = eObj.p2 - this.pos;
                        int n = in.read(bytes, 0, i < twoMiBL ? (int) i : twoMiB);
                        if (n <= 0) {
                            throw new IOException(n == 0 ? "Unable to access file" : "EOF");
                        }

                        ii += n;

                        for (m = 0; m < n && this.next(); ++m) {
                            this.findB(bytes[m]);
                        }

                        this.setJPBar();
                    }
                }

                this.setJPBar();
            }
        } catch (Exception var16) {
            System.err.printf("findT %s\n\t%s\n\t%d\t%d%n", var16, eObj, k, this.pos);
        }

        int length = this.ignoreCase ? this.realLength : this.inBytes.length;
        int length2 = this.isFound ? length : 0;
        this.hexV.find2(this.pos, this.pos - (long) length2);
    }

    protected void setJPBar() {
        final float oneGiBF = 1.07374182E9F;
        float ratio = (float) this.pos / (float) this.virtualSize;
        this.jPBar.setValue((int) (oneGiBF * ratio));

        float ratio2 = (float) this.pos / ((float) this.virtualSize / 1000.0F);
        this.jPBar.setString(((float) ((int) ratio2) / 10.0F) + "%");
    }

    private boolean next() {
        return !this.isFound && !Thread.currentThread().isInterrupted();
    }

    private boolean findB(byte arg1) {
        boolean isPileEmpty = this.ignoreCase
            ? findBIgnoreCase(arg1)
            : findBNoIgnoreCase(arg1);

        ++this.pos;
        return isPileEmpty;
    }

    private boolean findBNoIgnoreCase(byte arg1) {
        for (int i = this.pile.size() - 1; 0 <= i; --i) {
            int intElem = (Integer) this.pile.get(i);
            if (this.inBytes[intElem] != arg1) {
                this.pile.remove(i);
                continue;
            }
            if (intElem + 1 >= this.inBytes.length) {
                this.isFound = true;
                break;
            }
            this.pile.set(i, intElem + 1);
        }

        if (arg1 == this.inBytes[0] && this.pos % (long) this.wordSize == 0L) {
            this.pile.add(1);
            if (this.inBytes.length == 1) {
                this.isFound = true;
            }
        }

        return this.pile.isEmpty();
    }

    private boolean findBIgnoreCase(byte arg1) {
        int[] ints = new int[4];
        for (int i = this.pile.size() - 1; 0 <= i; --i) {
            ints = (int[]) this.pile.get(i);
            ++ints[2];
            ++ints[3];
            if (this.inChars[ints[0]][ints[1]].length <= ints[2]) {
                ++ints[0];
                ints[1] = ints[2] = 0;
            }

            if (this.inChars.length <= ints[0]) {
                this.pile.remove(i);
            } else if (arg1 == this.inChars[ints[0]][ints[1]][ints[2]]) {
                this.pile.set(i, ints.clone());
                if (ints[2] + 1 == this.inChars[ints[0]][ints[1]].length
                    && ints[0] + 1 == this.inChars.length) {
                    this.isFound = true;
                }
            } else {
                for (int intElem = ints[1] + 1; intElem < this.inChars[ints[0]].length; ++intElem) {
                    int var5;
                    for (var5 = 0;
                         var5 < ints[2]
                             && var5 < this.inChars[ints[0]][intElem].length
                             && this.inChars[ints[0]][intElem][var5]
                             == this.inChars[ints[0]][ints[1]][var5];
                         ++var5)
                        ;

                    if (var5 < ints[2]) {
                        this.pile.remove(i);
                        break;
                    }

                    if (arg1 == this.inChars[ints[0]][intElem][var5]) {
                        ints[1] = intElem;
                        this.pile.set(i, ints.clone());
                        if (ints[2] + 1 == this.inChars[ints[0]][ints[1]].length
                            && ints[0] + 1 == this.inChars.length) {
                            this.isFound = true;
                        }
                        break;
                    }

                    if (intElem + 1 == this.inChars[ints[0]].length) {
                        this.pile.remove(i);
                        break;
                    }
                }

                if (ints[0] + 1 == this.inChars.length
                    && ints[1] + 1 == this.inChars[0].length
                    && ints[2] + 1 == this.inChars[0][1].length) {
                    this.pile.remove(i);
                }
            }

            if (this.isFound) {
                this.realLength = ints[3];
                break;
            }
        }

        if (this.pos % (long) this.wordSize == 0L) {
            for (int i = 0; i < this.inChars[0].length; ++i) {
                if (arg1 == this.inChars[0][i][0]) {
                    ints[0] = ints[2] = 0;
                    ints[1] = i;
                    ints[3] = 1;
                    this.pile.add(ints.clone());
                    if (this.inChars.length + this.inChars[0][i].length == 2) {
                        this.isFound = true;
                        this.realLength = ints[3];
                    }
                    break;
                }
            }
        }

        return this.pile.isEmpty();
    }
}
