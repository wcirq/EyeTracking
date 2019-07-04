package com.wcy.eyetracking;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class P2pActivity extends AppCompatActivity {
    final int MTYPE_LOGIN = 0;
    final int MTYPE_LOGOUT = 1;
    final int MTYPE_LIST = 2;
    final int MTYPE_PUNCH = 3;
    final int MTYPE_PING = 4;
    final int MTYPE_PONG = 5;
    final int MTYPE_REPLY = 6;
    final int MTYPE_TEXT = 7;
    final int MTYPE_END = 8;

    private BlockingQueue<DatagramPacket> _sendQueue=new LinkedBlockingQueue<>();
    DatagramSocket socket= null;
    InetAddress serverAddress=null;
    int port=47240;
    String localAddr;
    private MyHandler handler = null;
    private TextView textView;

    ByteArrayOutputStream byteOut;
    DataOutputStream dataOut;

    private String GetLocalAddr(){
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff)
                    + "." + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
        }
        return "192.168.2.100";
    }

    public static int getLength(String s) {
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255) {
                length++;
            } else {
                length += 2;
            }
        }
        return length;
    }

    public byte[]IntToByte(int num){
        byte[]bytes=new byte[4];
        bytes[0]=(byte) ((num>>24)&0xff);
        bytes[1]=(byte) ((num>>16)&0xff);
        bytes[2]=(byte) ((num>>8)&0xff);
        bytes[3]=(byte) (num&0xff);
        return bytes;
    }

    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2p);
        byteOut = new ByteArrayOutputStream();
        dataOut = new DataOutputStream(byteOut);

        handler = new MyHandler(this);
        localAddr=GetLocalAddr();
        Button connBtn=(Button)findViewById(R.id.button);
        Button burrowBtn=(Button)findViewById(R.id.button2);
        Button sendBtn=(Button)findViewById(R.id.button3);
        textView=(TextView) findViewById(R.id.textView);
        final EditText nameEditText=(EditText)findViewById(R.id.editText);
        final EditText distEditText=(EditText)findViewById(R.id.editText2);
        final EditText sendEditText=(EditText)findViewById(R.id.editText3);
        connBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    byte []body = (localAddr + " " + nameEditText.getText().toString()).getBytes();
                    dataOut.writeShort(0x8964); // write magic
                    dataOut.writeShort(MTYPE_LOGIN); // write type
                    dataOut.writeInt(body.length*8); // write length
                    dataOut.write(body);
                    byte []data = byteOut.toByteArray();
                    byteOut.reset();
                    DatagramPacket datagramPacket = new DatagramPacket(data,data.length,serverAddress,port);//指定发送数据、远程IP、远程端口
                    boolean is =  _sendQueue.add(datagramPacket);//告诉server自己的name和本地ip，远程ip和端口server端是可以直接获取到的
                    Log.i("", String.valueOf(is));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        burrowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String values = distEditText.getText().toString();
                    byte []body = (values).getBytes();
                    dataOut.writeShort(0x8964); // write magic
                    dataOut.writeShort(MTYPE_PUNCH); // write type
                    dataOut.writeInt(body.length*8); // write length
                    dataOut.write(body);
                    byte []data = byteOut.toByteArray();
                    byteOut.reset();

                    DatagramPacket datagramPacket = new DatagramPacket(data,data.length,InetAddress.getByName(values.split(":")[0]), Integer.parseInt(values.split(":")[1]));//指定发送数据、远程IP、远程端口
                    boolean is =  _sendQueue.add(datagramPacket);//告诉server自己的name和本地ip，远程ip和端口server端是可以直接获取到的
                    DatagramPacket datagramPacket1 = new DatagramPacket(data,data.length,serverAddress,port);//指定发送数据、远程IP、远程端口
                    boolean is1 =  _sendQueue.add(datagramPacket1);//告诉server自己的name和本地ip，远程ip和端口server端是可以直接获取到的
                    Log.i("", String.valueOf(is1));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String host_port = distEditText.getText().toString();
                    String values = sendEditText.getText().toString();
                    byte []body = (values).getBytes();
                    dataOut.writeShort(0x8964); // write magic
                    dataOut.writeShort(MTYPE_TEXT); // write type
                    dataOut.writeInt(body.length*8); // write length
                    dataOut.write(body);
                    byte []data = byteOut.toByteArray();
                    byteOut.reset();

                    DatagramPacket datagramPacket = new DatagramPacket(data,data.length,InetAddress.getByName(host_port.split(":")[0]), Integer.parseInt(host_port.split(":")[1]));//指定发送数据、远程IP、远程端口
                    boolean is =  _sendQueue.add(datagramPacket);//告诉server自己的name和本地ip，远程ip和端口server端是可以直接获取到的
                    Log.i("", String.valueOf(is));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            serverAddress = InetAddress.getByName("106.15.234.102");
            socket = new DatagramSocket(47240);//这个地方是指定本地发送、接收端口
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Thread thread1=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true){
                        DatagramPacket packet = _sendQueue.take(); //如果队列空了，一直阻塞
                        Log.i("send", " " + serverAddress.getHostAddress()+ " " + port);
                        socket.send(packet);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread1.start();


        Thread thread2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        byte[] data = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(data, data.length);
                        socket.receive(packet);
                        String result = new String(packet.getData(), packet.getOffset(), packet.getLength());
                        Log.i("receive", result);
                        if(result.charAt(0)=='0'&&result.charAt(1)=='0'&&result.charAt(2)=='2'){//收到Server发来的对方信息
                            String[] strs=result.split(",");
                            serverAddress = InetAddress.getByName(strs[1]);//不再发送数据到Server，而是到C1或者C2
                            port=Integer.valueOf(strs[2]);//NAT上C1或C2的外网端口
                            Timer timer=new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    _sendQueue.add(null);
                                }
                            },1000,1000);
                        }
                        else if(result.charAt(0)=='0'&&result.charAt(1)=='0'&&result.charAt(2)=='3'){
                            Log.i("ok", "123");// 验证P2P通信
                            handler.sendEmptyMessage(1);
                        }
                        // handler.sendEmptyMessage(2);
                        Log.i("ok", result+packet.getAddress()+packet.getPort());
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread2.start();
    }

    private static class MyHandler extends Handler {
        private final WeakReference<P2pActivity> mTarget;

        MyHandler(P2pActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            P2pActivity activity = mTarget.get();
            if (msg.what == 1) {
                activity.textView.setText("P2P连接成功！");
            } else if (msg.what == 2) {
                activity.textView.setText("非P2P连接");
            }
        }
    }
}
