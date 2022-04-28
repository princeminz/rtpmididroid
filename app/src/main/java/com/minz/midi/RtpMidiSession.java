// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild
package com.minz.midi;
import com.minz.midi.ByteBufferKaitaiStream;
import com.minz.midi.KaitaiStruct;
import com.minz.midi.KaitaiStream;
import java.io.IOException;
import java.util.Arrays;
import java.nio.charset.Charset;
import com.minz.midi.ConsistencyError;


/**
 * RtpMidiSession
 */
public class RtpMidiSession extends KaitaiStruct.ReadWrite {
    public static RtpMidiSession fromFile(String fileName) throws IOException {
        return new RtpMidiSession(new ByteBufferKaitaiStream(fileName));
    }
    public RtpMidiSession() {
        this(null, null, null);
    }

    public RtpMidiSession(KaitaiStream _io) {
        this(_io, null, null);
    }

    public RtpMidiSession(KaitaiStream _io, KaitaiStruct.ReadWrite _parent) {
        this(_io, _parent, null);
    }

    public RtpMidiSession(KaitaiStream _io, KaitaiStruct.ReadWrite _parent, RtpMidiSession _root) {
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
        this.version = this._io.readU4be();
        this.initiatortoken = this._io.readU4be();
        this.ssrc = this._io.readU4be();
        this.name = new String(this._io.readBytesTerm(0, false, true, true), Charset.forName("UTF-8"));
    }

    public void _write() {
        this._io.writeBytes(new byte[] { -1, -1 });
        this._io.writeBytes(this.command);
        this._io.writeU4be(this.version);
        this._io.writeU4be(this.initiatortoken);
        this._io.writeU4be(this.ssrc);
        this._io.writeBytes((name()).getBytes(Charset.forName("UTF-8")));
        this._io.writeU1(0);
    }




    public void _check() {
        if (command().length != 2)
            throw new ConsistencyError("command", command().length, 2);
    }
    public void setValues( byte[] command, long initiatortoken,long ssrc, String name){
        this.sig = new byte[] { -1, -1 };
        this.command=command;
        this.version=2;
        this.initiatortoken=initiatortoken;
        this.ssrc=ssrc;
        this.name=name;
    }
    private byte[] sig;
    private byte[] command;
    private long version;
    private long initiatortoken;
    private long ssrc;
    private String name;
    private RtpMidiSession _root;
    private KaitaiStruct.ReadWrite _parent;
    public byte[] sig() { return sig; }
    public void setSig(byte[] _v) { sig = _v; }
    public byte[] command() { return command; }
    public void setCommand(byte[] _v) { command = _v; }
    public long version() { return version; }
    public void setVersion(long _v) { version = _v; }
    public long initiatortoken() { return initiatortoken; }
    public void setInitiatortoken(long _v) { initiatortoken = _v; }
    public long ssrc() { return ssrc; }
    public void setSsrc(long _v) { ssrc = _v; }
    public String name() { return name; }
    public void setName(String _v) { name = _v; }
    public RtpMidiSession _root() { return _root; }
    public void set_root(RtpMidiSession _v) { _root = _v; }
    public KaitaiStruct.ReadWrite _parent() { return _parent; }
    public void set_parent(KaitaiStruct.ReadWrite _v) { _parent = _v; }
}
