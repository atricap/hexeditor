package com.github.hexeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import javax.swing.Timer;

class slaveT extends Thread implements ActionListener {

    binEdit hexV;
    Timer timer = new Timer(300, this);
    private final BufferedReader bR;

    slaveT() {
        this.bR = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        this.timer.addActionListener(this);
        this.timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() != this.timer) {
            return;
        }

        boolean isInString = true;
        int j = 0;
        int i = 0;
        String line = null;
        StringBuilder sb = new StringBuilder();
        Vector<String> strings = new Vector<>();

        this.timer.stop();

        try {
            if (!this.bR.ready()) {
                this.timer.setDelay(300);
            } else {
                line = this.bR.readLine().replaceAll(" {2}", " ");
                this.timer.setDelay(50);
            }
        } catch (IOException ignored) {
        }

        if (line == null || line.isEmpty()) {
            this.timer.restart();
            return;
        }

        for (; i < line.length(); ++i) {
            isInString = true;
            if (line.charAt(i) == '\"') {
                isInString = !isInString;
            } else if (isInString && line.charAt(i) == ' ') {
                strings.add(line.substring(j, i));
                j = i + 1;
            }
        }

        strings.add(line.substring(j, i));
        if (strings.isEmpty()) {
            this.timer.restart();
            return;
        }

        line = strings.firstElement();
        if (line.equals("-goto") && strings.size() == 2) {
            this.hexV.goTo(strings.elementAt(1));
        } else if ((line.equals("-Mark")
                        || line.equals("-mark")
                        || line.equals("-delmark")
                        || line.equals("-delMark"))
                && 1 < strings.size()) {
            for (j = 1; j < strings.size(); ++j) {
                try {
                    Long elem = Long.valueOf(strings.elementAt(j));
                    if (line.equals("-Mark") && !this.hexV.MarkV.contains(elem)) {
                        this.hexV.MarkV.add(elem);
                    } else if (line.equals("-mark") && !this.hexV.markV.contains(elem)) {
                        this.hexV.markV.add(elem);
                    } else if (line.equals("-delMark")) {
                        this.hexV.MarkV.remove(elem);
                    } else if (line.equals("-delmark")) {
                        this.hexV.markV.remove(elem);
                    }
                } catch (Exception ignored) {
                }
            }
        } else if (line.equals("-file") && strings.size() == 2) {
            line = strings.elementAt(1);
            isInString = (line.length() & 3) == 0;
            if (isInString) {
                j = 0;

                while (j < line.length() - 3 && isInString) {
                    long x = 0L;

                    for (i = 0; i < 4 && isInString; ++i) {
                        char c = line.charAt(j);
                        isInString = ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z');
                        x = (x << 4)
                            + (long) c
                            - (long) ('0' <= c && c <= '9' ? '0' : '7');
                        ++j;
                    }

                    sb.append((char) ((int) x));
                }
            }

            this.hexV.loadFile(new File(sb.toString()));
        } else if (line.equals("-close")) {
            this.hexV.closeFile();
        }

        this.timer.restart();
    }
}
