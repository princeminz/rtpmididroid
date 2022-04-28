// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild
package com.minz.midi;

import java.io.IOException;
import java.util.Arrays;


/**
 * timestamp
 */
public class Timestamp extends KaitaiStruct.ReadWrite {
    public static Timestamp fromFile(String fileName) throws IOException {
        return new Timestamp(new ByteBufferKaitaiStream(fileName));
    }
    public Timestamp() {
        this(null, null, null);
        this.sig = (new byte[] { -1, -1 });
        this.command = (new byte[] { 67, 75 });
    }

    public Timestamp(KaitaiStream _io) {
        this(_io, null, null);
    }

    public Timestamp(KaitaiStream _io, KaitaiStruct.ReadWrite _parent) {
        this(_io, _parent, null);
    }

    public Timestamp(KaitaiStream _io, KaitaiStruct.ReadWrite _parent, Timestamp _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
    }
    public void _read() {
        this.sig = this._io.readBytes(2);
        if (!(Arrays.equals(sig(), new byte[] { -1, -1 }))) {
            throw new KaitaiStream.ValidationNotEqualError(new byte[] { -1, -1 }, sig(), _io(), "/seq/0");
        }
        this.command = this._io.readBytes(2);
        if (!(Arrays.equals(command(), new byte[] { 67, 75 }))) {
            throw new KaitaiStream.ValidationNotEqualError(new byte[] { 67, 75 }, command(), _io(), "/seq/1");
        }
        this.ssrc = this._io.readU4be();
        this.count = this._io.readU1();
        this.unused = this._io.readBytes(3);
//        if (!(Arrays.equals(unused(), new byte[] { 0, 0, 0 }))) {
//            throw new KaitaiStream.ValidationNotEqualError(new byte[] { 0, 0, 0 }, unused(), _io(), "/seq/4");
//        }
        this.timestamp1 = this._io.readU8be();
        this.timestamp2 = this._io.readU8be();
        this.timestamp3 = this._io.readU8be();
    }

    public void _write() {
        this._io.writeBytes(new byte[] { -1, -1 });
        this._io.writeBytes(new byte[] { 67, 75 });
        this._io.writeU4be(this.ssrc);
        this._io.writeU1(this.count);
        this._io.writeBytes(new byte[] { 0, 0, 0 });
        this._io.writeU8be(this.timestamp1);
        this._io.writeU8be(this.timestamp2);
        this._io.writeU8be(this.timestamp3);
    }

    public void _check() {
    }
    private byte[] sig;
    private byte[] command;
    private long ssrc;
    private int count;
    private byte[] unused;
    private long timestamp1;
    private long timestamp2;
    private long timestamp3;
    private Timestamp _root;
    private KaitaiStruct.ReadWrite _parent;
    public byte[] sig() { return sig; }
    public void setSig(byte[] _v) { sig = _v; }
    public byte[] command() { return command; }
    public void setCommand(byte[] _v) { command = _v; }
    public long ssrc() { return ssrc; }
    public void setSsrc(long _v) { ssrc = _v; }
    public int count() { return count; }
    public void setCount(int _v) { count = _v; }
    public byte[] unused() { return unused; }
    public void setUnused(byte[] _v) { unused = _v; }
    public long timestamp1() { return timestamp1; }
    public void setTimestamp1(long _v) { timestamp1 = _v; }
    public long timestamp2() { return timestamp2; }
    public void setTimestamp2(long _v) { timestamp2 = _v; }
    public long timestamp3() { return timestamp3; }
    public void setTimestamp3(long _v) { timestamp3 = _v; }
    public Timestamp _root() { return _root; }
    public void set_root(Timestamp _v) { _root = _v; }
    public KaitaiStruct.ReadWrite _parent() { return _parent; }
    public void set_parent(KaitaiStruct.ReadWrite _v) { _parent = _v; }
}
