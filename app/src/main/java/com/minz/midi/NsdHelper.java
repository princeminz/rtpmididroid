package com.minz.midi;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NsdHelper extends AndroidViewModel {
    NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    public static final String SERVICE_TYPE = "_apple-midi._udp.";
    public static final String TAG = "NsdHelper";
    public static String mServiceName;
    NsdServiceInfo mService;
    List<NsdServiceInfo> services;
    List<String> devices;
    public MutableLiveData<List<String>> devicesLiveData;
    MidiManager mMidiManager;
    List<MidiDeviceInfo> midiDeviceInfos;
    List<String> midiDevices;
    public MutableLiveData<List<String>> midiDevicesLiveData;
    NsdServiceInfo server;
    public MutableLiveData<String> buttonState;
    long initToken;
    long ssrc;
    TimestampSync mTimestampSync;
    int midiDevicePos;
    MIDISessionControlPort mMidiSessionControlPort;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public NsdHelper(@NonNull Application application) {
        super(application);
        mNsdManager = (NsdManager) application.getApplicationContext().getSystemService(Context.NSD_SERVICE);
        services = new ArrayList();
        devices = new ArrayList();
        devicesLiveData = new MutableLiveData<List<String>>();
        devicesLiveData.setValue(Arrays.asList("No Network MIDI Devices Found"));
        discoverServices();
        registerService(mServiceName,5004);

        mMidiManager = (MidiManager) application.getApplicationContext().getSystemService(Context.MIDI_SERVICE);
        midiDeviceInfos = new ArrayList();
        for (MidiDeviceInfo midiDevice : mMidiManager.getDevices()){
            if (midiDevice.getOutputPortCount() > 0)
                midiDeviceInfos.add(midiDevice);
        }
        midiDevices = new ArrayList();
        midiDevices = midiDeviceInfos.stream().map(i -> i.getProperties().getString(i.PROPERTY_NAME)).collect(Collectors.toList());
        midiDevicesLiveData = new MutableLiveData<>();
        midiDevicesLiveData.setValue(midiDevices);
        mMidiManager.registerDeviceCallback(new MidiManager.DeviceCallback(){
            @Override
            public void onDeviceAdded(MidiDeviceInfo device) {
                super.onDeviceAdded(device);
                Log.d(TAG, String.valueOf(device.getOutputPortCount()));
                if(device.getOutputPortCount() > 0) {
                    midiDeviceInfos.add(device);
                    midiDevices.add(device.getProperties().getString(device.PROPERTY_NAME));
                    midiDevicesLiveData.setValue(midiDevices);
                }
            }
            @Override
            public void onDeviceRemoved(MidiDeviceInfo device) {
                super.onDeviceRemoved(device);
                if(device.getOutputPortCount() > 0) {
                    int pos = midiDeviceInfos.indexOf(device);
                    midiDeviceInfos.remove(pos);
                    midiDevices.remove(pos);
                    midiDevicesLiveData.setValue(midiDevices);
                }
            }
        }, new Handler(Looper.myLooper()));
        buttonState = new MutableLiveData<String>();
        buttonState.setValue("Connect");

    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else {
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed" + errorCode);
                        }
                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                            if (serviceInfo.getServiceName().equals(mServiceName)) {
                                Log.d(TAG, "Same IP.");
                                return;
                            }
                            services.add(serviceInfo);
                            mService = serviceInfo;
                        }
                    });
                    devices.add(service.getServiceName());
                    devicesLiveData.postValue(devices);
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                    return;
                }

                int pos = devices.indexOf(service.getServiceName());
                if(pos != -1) {
                    services.remove(pos);
                    devices.remove(pos);
                }
                if(devices.isEmpty()){
                    devicesLiveData.postValue(Arrays.asList("No Network MIDI Devices Found"));
                } else {
                    devicesLiveData.postValue(devices);
                }
            }
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered: " + mServiceName);
            }
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "Service registration failed: " + arg1);
            }
            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "Service unregistered: " + arg0.getServiceName());
            }
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Service unregistration failed: " + errorCode);
            }
        };
    }
    public void registerService(String serviceName, int port) {
        tearDown();  // Cancel any previous registration request
        mServiceName = serviceName;
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }
    public void discoverServices() {
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }
    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
    }
    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }
    public void tearDown() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }
    @Override
    protected void onCleared() {
        Log.d(TAG, "NsdManager cleared");
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
        super.onCleared();
    }

    public void manageService(int pos,String IP) {
        if(pos==-1 && buttonState.getValue().equals("Connect")){
            NsdServiceInfo info = new NsdServiceInfo();
            String[] receiverInfo = IP.split(":",2);
            int port;
            if(receiverInfo.length==2){
                try {
                    port = Integer.parseInt(receiverInfo[1]);
                }catch(NumberFormatException e){
                    Toast.makeText(getApplication().getApplicationContext(),"Wrong IP address or port Format",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(Patterns.IP_ADDRESS.matcher(receiverInfo[0]).matches() && ValueRange.of(0,65536).isValidIntValue(port)){
                    Log.v("Custom info","IP:"+receiverInfo[0]+" Port:"+receiverInfo[1]);
                    try{
                        info.setHost(InetAddress.getByName(receiverInfo[0]));
                    }catch (UnknownHostException e){
                        e.printStackTrace();
                    }
                    info.setPort(port);
                    server=info;
                    buttonState.setValue("Connecting");
                    new MIDISessionInitiator().start();
                }
            }
            else{
                Toast.makeText(getApplication().getApplicationContext(),"Wrong IP address or port Format",Toast.LENGTH_SHORT).show();
            }

        }
        else if(services.size() > 0 && buttonState.getValue().equals("Connect")) {
            Log.d(TAG, "connecting to : " + services.get(pos));
            server = services.get(pos);
            buttonState.setValue("Connecting");
            new MIDISessionInitiator().start();
        }
        else if(buttonState.getValue().equals("Disconnect")){
            buttonState.setValue("Disconnecting");
            mMidiSessionControlPort.interrupt();
            try {
                mMidiSessionControlPort.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new MIDISessionTerminator().start();
        }

    }
    class MidiPacketProcessor extends Thread{
        long ssrc;
        DatagramSocket socket;
        long offset_estimate = 0;
        MidiOutputPort outputPort;
        int seq;

        public MidiPacketProcessor(DatagramSocket socket,long ssrc,long offset_estimate){
            this.socket=socket;
            this.ssrc=ssrc;
            this.offset_estimate=offset_estimate;
            seq = 0;
        }

        @Override
        public void run(){
            Looper.prepare();
            Looper looper = Looper.myLooper();
            mMidiManager.openDevice(midiDeviceInfos.get(midiDevicePos), new MidiManager.OnDeviceOpenedListener() {
                @Override
                public void onDeviceOpened(MidiDevice device) {
                    if (device == null) {
                        Log.e(TAG, "could not open device " + midiDeviceInfos.get(midiDevicePos));
                    } else {
                        class MyReceiver extends MidiReceiver {
                            public void onSend(byte[] data, int offset,
                                               int count, long timestamp) throws IOException {

                                StringBuilder sb = new StringBuilder();
                                sb.append("[ ");
                                for (int i = offset; i < offset + count; ++i) {
                                    sb.append(String.format("0x%02X ", data[i]));
                                }
                                sb.append("]");
                                Log.d(" Midiout", "timestamp: " + timestamp + " offset: " + offset + "count: " + count + "data: " + sb.toString());
                                processMidiData(data, offset, count);

                            }
                            private void processMidiData(byte[] midiData, int offset,
                                                         int count){
                                int i = offset;
                                while ( i < offset + count) {
                                    byte[] rtpBuffer = new byte[16];
                                    int numBytes = 0;
                                    rtpBuffer[numBytes++]=(byte) (0x80);
                                    rtpBuffer[numBytes++]=(byte) (0x61);
                                    seq++;
                                    rtpBuffer[numBytes++]=(byte) ((0xFF00 & seq)>>8);
                                    rtpBuffer[numBytes++]=(byte) ((0x00FF) & seq);

                                    long time1 = java.lang.System.currentTimeMillis() * 10 + 100;
                                    rtpBuffer[numBytes++]=(byte) ((0xFF000000 & time1)>>24);
                                    rtpBuffer[numBytes++]=(byte) ((0x00FF0000 & time1)>>16);
                                    rtpBuffer[numBytes++]=(byte) ((0x0000FF00 & time1)>>8);
                                    rtpBuffer[numBytes++]=(byte)  (0x000000FF & time1);

                                    rtpBuffer[numBytes++]=(byte) ((0xFF000000 & ssrc)>>24);
                                    rtpBuffer[numBytes++]=(byte) ((0x00FF0000 & ssrc)>>16);
                                    rtpBuffer[numBytes++]=(byte) ((0x0000FF00 & ssrc)>>8);
                                    rtpBuffer[numBytes++]=(byte)  (0x000000FF & ssrc);
                                    byte msNibble= (byte) (((byte) (0xF0)) & midiData[i]);
                                    switch(msNibble){
                                        case (byte) (0x80):
                                        case (byte) (0x90):
                                        case (byte) (0xA0):
                                        case (byte) (0xB0):
                                        case (byte) (0xE0):
                                            rtpBuffer[numBytes++]=(byte) (0x03);
                                            rtpBuffer[numBytes++]=midiData[i++];
                                            rtpBuffer[numBytes++]=midiData[i++];
                                            rtpBuffer[numBytes++]=midiData[i++];
                                            break;

                                        case (byte) (0xC0):
                                        case (byte) (0xD0):
                                            rtpBuffer[numBytes++]=(byte) (0x02);
                                            rtpBuffer[numBytes++]=midiData[i++];
                                            rtpBuffer[numBytes++]=midiData[i++];
                                            break;

                                        case (byte) (0xF0):
                                            switch (midiData[i]){
                                                case (byte) (0xF0):
                                                    Log.d("Debug","System Exclusive");
                                                    break;
                                                case (byte) (0xF2):
                                                    Log.d("Debug","SONG POSITION");
                                                    rtpBuffer[numBytes++]=(byte) (0x03);
                                                    rtpBuffer[numBytes++]=midiData[i++];
                                                    rtpBuffer[numBytes++]=midiData[i++];
                                                    rtpBuffer[numBytes++]=midiData[i++];
                                                    break;
                                                case (byte) (0xF3):
                                                case (byte) (0xF5):
                                                    //Log.d("Debug","System Exclusive");
                                                    rtpBuffer[numBytes++]=(byte) (0x02);
                                                    rtpBuffer[numBytes++]=midiData[i++];
                                                    rtpBuffer[numBytes++]=midiData[i++];
                                                    break;
                                                default:
                                                    rtpBuffer[numBytes++]=(byte) (0x01);
                                                    rtpBuffer[numBytes++]=midiData[i++];
                                                    break;
                                            }
                                            break;
                                            default:
                                                rtpBuffer[numBytes++]=(byte) (0x02);
                                                rtpBuffer[numBytes++]=midiData[i++];
                                                rtpBuffer[numBytes++]=midiData[i++];
                                            break;
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("[ ");
                                    for (int j = 0; j < numBytes; ++j) {
                                        sb.append(String.format("0x%02X ", rtpBuffer[j]));
                                    }
                                    sb.append("]");
                                    Log.d(" Midipacket", sb.toString());
                                    try {
                                        DatagramPacket sDatagramPacket = new DatagramPacket(rtpBuffer, numBytes);
                                        sDatagramPacket.setAddress(server.getHost());
                                        sDatagramPacket.setPort(server.getPort()+1);
                                        socket.send(sDatagramPacket);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        outputPort = device.openOutputPort(0);
                        if(outputPort!=null){
                            outputPort.connect(new MyReceiver());
                        }else{
                            Toast.makeText(getApplication().getApplicationContext(),"Unable to connect to MIDI Instrument",Toast.LENGTH_SHORT).show();
                            Log.v("Outport","Error");

                        }


                    }

                }
            }, new Handler(looper));
            Looper.loop();
        }

        @Override
        public void interrupt() {
            super.interrupt();
            try {
                Log.d("MIDI", "midipacketprocessor interrupted");
                outputPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class TimestampSync extends Thread{
        long ssrc;
        DatagramSocket socket;
        DatagramPacket sDatagramPacket;
        DatagramPacket rDatagramPacket;
        Timestamp tsPacket;
        Timestamp trPacket;
        MidiPacketProcessor mMidiPacketProcessor;

        byte [] buffer=new byte[1024];
        public TimestampSync(long ssrc, DatagramPacket sDatagramPacket){
            this.ssrc=ssrc;
            this.sDatagramPacket=sDatagramPacket;
            tsPacket=new Timestamp();
            rDatagramPacket = new DatagramPacket(buffer, buffer.length);
            tsPacket.setSsrc(ssrc);
            try{
                socket=new DatagramSocket(5005);
            }catch (SocketException socketException){
                socketException.printStackTrace();
                Log.d("DEBUG",socketException.toString());
            }
        }
        @Override
        public void run(){
            try {

                sDatagramPacket.setPort(server.getPort()+1);
                boolean Received = true;
                socket.send(sDatagramPacket);
                int count = 1;
                do {
                    try {
                        buttonState.postValue("Connecting");
                        socket.receive(rDatagramPacket);
                    } catch (SocketTimeoutException e) {
                        Received = false;
                        socket.send(sDatagramPacket);
                        count++;
                        Log.d("DEBUG","Initiator attempt: " + count);
                    }
                } while (!Received && count < 12);



                RtpMidiSession rPacket = new RtpMidiSession(new ByteBufferKaitaiStream(buffer));
                rPacket._read();
                if (Arrays.equals(rPacket.command(), new byte[]{'O', 'K'})) {
                    Log.d("DEBUG","\nResponder: OK for MIDI  port Session");
                    buttonState.postValue("Disconnect");
                    long offset_estimate=0;
                    //start processing  MIDI Packets from USB
                    mMidiPacketProcessor = new MidiPacketProcessor(socket, ssrc, offset_estimate);
                    mMidiPacketProcessor.start();

                    //start Timestamp Synchronization

                    while (true) {

                            tsPacket.setCount(0);
                            tsPacket.setTimestamp1(java.lang.System.currentTimeMillis() * 10);

                            byte sPacketByteArray[] = tsPacket._toByteArray();
                            sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);

                            sDatagramPacket.setAddress(server.getHost());
                            long timestamp3 = 0;

                            sDatagramPacket.setPort(server.getPort()+1);
                            socket.setSoTimeout(10000);
                            Received = true;
                            socket.send(sDatagramPacket);
                            count = 1;
                            do {
                                try {
                                    socket.receive(rDatagramPacket);
                                } catch (SocketTimeoutException e) {
                                    Received = false;
                                    socket.send(sDatagramPacket);
                                    count++;
                                    Log.d("DEBUG","Initiator attempt: " + count);
                                }
                            } while (!Received && count < 12);

                            if (Received) {
                                Timestamp trPacket = new Timestamp(new ByteBufferKaitaiStream(buffer));
                                trPacket._read();
                                Log.d("DEBUG","packet received!");
                                //if (trPacket.count() == 1) {
                                tsPacket.setCount(2);
                                tsPacket.setTimestamp1(trPacket.timestamp1());
                                tsPacket.setTimestamp2(trPacket.timestamp2());
                                timestamp3 = java.lang.System.currentTimeMillis() * 10;
                                tsPacket.setTimestamp3(timestamp3);
                                //}
                                sPacketByteArray = tsPacket._toByteArray();
                                sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);
                                //now again send the Timestamp
                                sDatagramPacket.setPort(server.getPort()+1);
                                sDatagramPacket.setAddress(server.getHost());
                                socket.send(sDatagramPacket);
                                offset_estimate = ((timestamp3 + trPacket.timestamp1()) / 2) - trPacket.timestamp2();
                                Log.d("DEBUG","offset_estimate: "+offset_estimate);
                            }

                            //wait for receive CK time count=0 from the other side the send your time at count=1
                            socket.setSoTimeout(10000);
                            Received = false;
                            try {
                                socket.receive(rDatagramPacket);
                                Received = true;
                            } catch (SocketTimeoutException e) {
                                Received = false;
                                Log.d("DEBUG","Didn't receice CK command from responder timedout: ");
                            }
                            if (Received) {
                                Timestamp trPacket = new Timestamp(new ByteBufferKaitaiStream(buffer));
                                trPacket._read();
                                Log.d("DEBUG","packet received!");
                                if (trPacket.count() == 0) {
                                    tsPacket.setCount(1);
                                    tsPacket.setTimestamp1(trPacket.timestamp1());
                                    tsPacket.setTimestamp2(java.lang.System.currentTimeMillis() * 10);
                                    tsPacket.setTimestamp3(0);
                                }
                                sPacketByteArray = tsPacket._toByteArray();
                                sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);
                                //now again send the Timestamp
                                sDatagramPacket.setPort(server.getPort()+1);
                                sDatagramPacket.setAddress(server.getHost());
                                socket.send(sDatagramPacket);
                            }
                            Log.d("DEBUG","Sleeping now..");
                            sleep(10000);
                        }


                }
                if (Arrays.equals(rPacket.command(), new byte[]{'N', 'O'})) {
                    Log.d("DEBUG","\nResponder: NO for MIDI  port Session");
                }

            } catch (InterruptedException ie){
                Log.d("DEBUG","timestampsync thread interrupted");
            } catch (Exception e){
                Log.d("DEBUG","Exception at Timestamp:"+e.toString());
                e.printStackTrace();
            } finally {
                mMidiPacketProcessor.interrupt();
                socket.close();
                Log.d("DEBUG","socket 5005 closed!");
            }
        }

    }
    private class MIDISessionTerminator extends Thread {
        DatagramSocket socket;
        DatagramPacket sDatagramPacket;
        RtpMidiSession sPacket;

        @Override
        public void run() {
            super.run();
            midiSessionTermination();
        }

        public void midiSessionTermination() {
            try {
                socket = new DatagramSocket(5004);
                sPacket = new RtpMidiSession();
                sPacket.setValues(new byte[]{'B', 'Y'}, initToken, ssrc, mServiceName);
                byte sPacketByteArray[] = sPacket._toByteArray();
                sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);
                sDatagramPacket.setAddress(server.getHost());
                sDatagramPacket.setPort(server.getPort());
                socket.send(sDatagramPacket);
                socket.close();
                mTimestampSync.interrupt();
                mTimestampSync.join();
                buttonState.postValue("Connect");
            } catch (Exception e) {
                Log.d("DEBUG", "Exception at midiSessionTermination:" + e.toString());
                e.printStackTrace();
            }
        }
    }
    private class MIDISessionInitiator extends Thread {
        DatagramSocket socket;
        DatagramPacket sDatagramPacket;
        DatagramPacket rDatagramPacket;
        RtpMidiSession sPacket;
        RtpMidiSession rPacket;
        Timestamp timestamp;

        byte byteArray[];

        @Override
        public void run() {
            super.run();
            midiSessionInitiation();
        }

        public void midiSessionInitiation(){
            Random ran = new Random();
            initToken = Math.abs(ran.nextLong());
            ssrc = Math.abs(ran.nextLong());
            try{

                socket = new DatagramSocket(5004);
                byte[] buffer = new byte[1024];

                sPacket = new RtpMidiSession();
                sPacket.setValues( new byte[]{'I', 'N'} , initToken, ssrc, mServiceName);

                byte sPacketByteArray[] = sPacket._toByteArray();
                sDatagramPacket=new DatagramPacket(sPacketByteArray , sPacketByteArray.length);
                rDatagramPacket=new DatagramPacket(buffer , buffer.length);
                sDatagramPacket.setAddress(server.getHost());
                sDatagramPacket.setPort(server.getPort());
                socket.setSoTimeout(2000);
                boolean Received=true;
                socket.send(sDatagramPacket);
                int count=1;
                do{
                    try {
                        socket.receive(rDatagramPacket);
                    }catch (SocketTimeoutException e) {
                        Received = false;
                        socket.send(sDatagramPacket);
                        count++;
                        Log.d("DEBUG","Initiator attempt: "+count);
                    }
                } while (!Received && count < 12);

                if(Received) {
                    rPacket = new RtpMidiSession(new ByteBufferKaitaiStream(buffer));
                    rPacket._read();
                    if (Arrays.equals(rPacket.command(), new byte[]{'O', 'K'})) {
                        Log.d("DEBUG","Responder: OK");
                        socket.close();
                        //now start protocol for MIDI port
                        mMidiSessionControlPort = new MIDISessionControlPort();
                        mMidiSessionControlPort.start();
                        mTimestampSync = new TimestampSync(ssrc,sDatagramPacket);
                        mTimestampSync.start();
                    }

                    if (Arrays.equals(rPacket.command(), new byte[]{'N', 'O'})){
                        Log.d("DEBUG","Responder: NO");
                        socket.close();
                        buttonState.postValue("Connect");
                    }
                    if (Arrays.equals(rPacket.command(), new byte[]{'R', 'L'})){
                        Log.d("DEBUG","Responder: RL");
                        socket.close();
                    }
                    if (Arrays.equals(rPacket.command(), new byte[]{'B', 'Y'})) {
                        Log.d("DEBUG","Responder: BY");
                        socket.close();
                        buttonState.postValue("Connect");
                        Log.d("DEBUG", "socket 5004 closed!");
                    }
                }
                if(!Received){
                    buttonState.postValue("Connect");
                }
            } catch (Exception e){
                Log.d("DEBUG","Exception at MidiSessionInit:"+e.toString());
                e.printStackTrace();
            }

        }
    }
    private class MIDISessionControlPort extends Thread {
        DatagramSocket socket;
        DatagramPacket rDatagramPacket;
        RtpMidiSession rPacket;
        byte[] buffer;

        MIDISessionControlPort(){
            try{
                socket = new DatagramSocket(5004);
                buffer = new byte[1024];
                rDatagramPacket = new DatagramPacket(buffer, buffer.length);
            }catch (SocketException socketException){
                socketException.printStackTrace();
                Log.d("DEBUG",socketException.toString());
            }
        }

        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {

                        socket.setSoTimeout(1000);
                        boolean Received = true;
                        try {
                            socket.receive(rDatagramPacket);
                        } catch (Exception e) {
                            Received = false;
                        }
                        if (Received) {
                            rPacket = new RtpMidiSession(new ByteBufferKaitaiStream(buffer));
                            rPacket._read();

                            if (Arrays.equals(rPacket.command(), new byte[]{'B', 'Y'})) {
                                Log.d("DEBUG", "Responder: BY");
                                socket.close();
                                buttonState.postValue("Disconnecting");
                                new MIDISessionTerminator().start();
                                Log.d("DEBUG", "socket 5004 closed!");
                            }
                        }

                }
            } catch (SocketException e) {
                e.printStackTrace();
            } finally {
                socket.close();
            }
        }
    }
}


