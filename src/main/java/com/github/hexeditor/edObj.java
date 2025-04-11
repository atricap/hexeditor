package com.github.hexeditor;

import java.util.Stack;

class edObj {

    int a1;
    public long p1 = -1L;
    public long p2 = -1L;
    public long offset = 0L;
    public long size = 0L;
    public boolean isEditing = false;
    edObj o = null;
    public Stack<Byte> bytes = new Stack<>();

    public edObj(long p1, long size, int a1) {
        this.p1 = p1;
        this.a1 = a1;
        this.size = size;
        this.p2 = this.p1 + size;
    }

    public edObj(long p1, long p2, long offset, edObj obj) {
        this.p1 = p1;
        this.p2 = p2;
        this.offset = offset;
        this.o = obj;
    }

    public String toString() {
        return this.o != null
            ? String.format("p//offset: %d/%d//%d   \to.a1: %d   \to.B.size/o.size: %d/%d",
                this.p1, this.p2, this.offset, this.o.a1, this.o.bytes.size(), this.o.size)
            : String.format("p: %d/%d   \ta1: %d   \tB.size/size: %d/%d",
                this.p1, this.p2, this.a1, this.bytes.size(), this.size);
    }
}
