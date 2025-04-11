package com.github.hexeditor;

import java.io.File;
import javax.swing.filechooser.FileFilter;

class filterRW extends FileFilter {

    @Override
    public boolean accept(File f) {
        return f.canWrite();
    }

    @Override
    public String getDescription() {
        return "";
    }
}
