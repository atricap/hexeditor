package com.github.hexeditor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

class SaveThread extends Thread {

    File file1;
    File file2;
    Vector<EdObj> edV;
    BinEdit hexV;
    JProgressBar progressBar;
    private long time;
    private long virtualSize;
    private long pos;

    @Override
    public void run() {
        final int twoMiB = 2 * 1024 * 1024;
        final long twoMiBL = 2L * 1024 * 1024;
        final int oneGiB = 1 * 1024 * 1024 * 1024;

        int n = 0;
        byte[] var8 = new byte[twoMiB];
        if (this.edV == null || this.edV.isEmpty()) {
            return;
        }

        long var14 = 0L;
        this.pos = 0L;
        this.time = System.currentTimeMillis();
        this.virtualSize = this.edV.lastElement().p2;
        this.progressBar.setMaximum(oneGiB);

        EdObj eObj;
        while (n < this.edV.size()) {
            eObj = this.edV.get(n);
            if (this.pos < eObj.p2) {
                break;
            }
            ++n;
        }

        try {
            File outFile = new File(this.file2.getPath() + ".TMP");

            try (FileInputStream in = this.file1 != null ? new FileInputStream(this.file1) : null;
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile), twoMiB)) {
                for (; n < this.edV.size() && this.next(); ++n) {
                    eObj = this.edV.get(n);
                    long var10 = eObj.p1 - eObj.offset;
                    this.pos = eObj.p1;
                    if (eObj.o.a1 != 4
                        && eObj.o.a1 != 2
                        && (eObj.o.a1 != 6 || 1 >= eObj.o.bytes.size())) {
                        int var2;
                        long var12;
                        if (eObj.o.a1 == 6) {
                            Arrays.fill(var8, eObj.o.bytes.get(0));

                            while (this.pos < eObj.p2 && this.next()) {
                                var12 = eObj.p2 - this.pos;
                                var2 = var12 < twoMiBL ? (int) var12 : twoMiB;
                                out.write(var8, 0, var2);
                                this.pos += var2;
                                this.setJPBar();
                            }
                        } else {
                            for (var12 = this.pos - var10;
                                 var14 < var12;
                                 var14 += in.skip(var12 - var14))
                                ;

                            while (this.pos < eObj.p2 && this.next()) {
                                var12 = eObj.p2 - this.pos;
                                var2 = in.read(var8, 0, var12 < twoMiBL ? (int) var12 : twoMiB);
                                if (var2 <= 0) {
                                    throw new IOException(
                                        var2 == 0 ? "Unable to access file" : "EOF");
                                }

                                var14 += var2;
                                out.write(var8, 0, var2);
                                this.pos += var2;
                                this.setJPBar();
                            }
                        }
                    } else {
                        while (this.pos < eObj.p2 && this.next()) {
                            out.write(
                                eObj.o.bytes.get((int) (this.pos - var10)));
                            ++this.pos;
                        }

                        this.setJPBar();
                    }
                }
            }

            if (this.hexV.randomAccessFile != null) {
                this.hexV.randomAccessFile.close();
            }

            if (this.file1 != null && this.file1.equals(this.file2)) {
                File bakFile = new File(this.file1.getParent(), this.file1.getName() + ".bak");
                if (bakFile.exists()) {
                    bakFile.delete();
                }
                this.file1.renameTo(bakFile);
            }

            outFile.renameTo(this.file2);
            this.hexV.save2(this.file2);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this.hexV, e);
            this.hexV.save2(null);
        }
    }

    protected void setJPBar() {
        this.progressBar.setValue((int) (1.07374182E9F * ((float) this.pos / (float) this.virtualSize)));
        this.progressBar.setString(
                this.toTime(this.pos, this.virtualSize, System.currentTimeMillis() - this.time));
    }

    private boolean next() {
        return !Thread.currentThread().isInterrupted();
    }

    private String toTime(long a, long b, long c) {
        StringBuilder sb = new StringBuilder(
            Float.toString((float) ((int) ((float) a / ((float) b / 1000.0F))) / 10.0F));
        sb.append("% saved");
        if (a != 0L) {
            c = c / 1000L * (b / a);
        }
        if (c == Long.MAX_VALUE) {
            return "";
        }

        long[] conversions = new long[] {86400L, 3600L, 60L, 1L};
        String[] units = new String[] {"D ", "H ", "mn ", "s "};
        int n = 0;
        sb.append(", time remaining ");

        for (int i = 0; i < conversions.length && n < 2; ++i) {
            long k;
            if ((k = c / conversions[i]) != 0L || n == 1) {
                if (k < 10L && 0 < n) {
                    sb.append("0");
                }

                sb.append(k).append(units[i]);
                c %= conversions[i];
                ++n;
            }
        }

        return sb.toString();
    }
}
