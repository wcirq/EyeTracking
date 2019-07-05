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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wcy.eyetracking.util.P2pMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class P2pActivity extends AppCompatActivity {
    private BlockingQueue<DatagramPacket> _sendQueue=new LinkedBlockingQueue<>();
    DatagramSocket socket= null;
    InetAddress serverAddress=null;
    int port=47240;
    String localAddr;
    private MyHandler handler = null;
    private TextView textView;
    P2pMessage message;

    ByteArrayOutputStream byteOut;
    DataOutputStream dataOut;

    ArrayAdapter<String> adapter;
    ListView listView;
    ArrayList<String> list_data;
    String target_host_port=null;

    Button sendBtn;

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
        message = new P2pMessage();
        handler = new MyHandler(this);
        localAddr=GetLocalAddr();
        listView = findViewById(R.id.list_item);
        list_data = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,list_data);//新建并配置ArrayAapeter
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String target_host = ((TextView) view).getText().toString();
                if (!target_host.contains("you")){
                    try {
                        byte []body = (target_host).getBytes();
                        dataOut.writeShort(0x8964); // write magic
                        dataOut.writeShort(message.MTYPE_PUNCH); // write type
                        dataOut.writeInt(body.length); // write length
                        dataOut.write(body);
                        byte []data = byteOut.toByteArray();
                        byteOut.reset();
                        DatagramPacket datagramPacket = new DatagramPacket(data,data.length,InetAddress.getByName(target_host.split(":")[0]), Integer.parseInt(target_host.split(":")[1]));
                        _sendQueue.add(datagramPacket);
                        DatagramPacket datagramPacket1 = new DatagramPacket(data,data.length,serverAddress,port);
                        _sendQueue.add(datagramPacket1);
                        target_host_port=target_host;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(P2pActivity.this, "你点击了 " + target_host + " 按钮", Toast.LENGTH_SHORT).show();
            }
        });

        Button connBtn=(Button)findViewById(R.id.button);
        sendBtn=(Button)findViewById(R.id.button3);
        textView=(TextView) findViewById(R.id.textView);
        final EditText nameEditText=(EditText)findViewById(R.id.editText);
        final EditText sendEditText=(EditText)findViewById(R.id.editText3);
        connBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    byte []body = (localAddr + " " + nameEditText.getText().toString()).getBytes();
                    dataOut.writeShort(0x8964); // write magic
                    dataOut.writeShort(message.MTYPE_LOGIN); // write type
                    dataOut.writeInt(body.length); // write length
                    dataOut.write(body);
                    byte []data = byteOut.toByteArray();
                    byteOut.reset();
                    DatagramPacket datagramPacket = new DatagramPacket(data,data.length,serverAddress,port);//指定发送数据、远程IP、远程端口
                    _sendQueue.add(datagramPacket);//告诉server自己的name和本地ip，远程ip和端口server端是可以直接获取到的
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (target_host_port!=null){
                    try {
                        String host_port = target_host_port;
                        String values = sendEditText.getText().toString();
                        byte []body = (values).getBytes();
                        dataOut.writeShort(0x8964); // write magic
                        dataOut.writeShort(message.MTYPE_TEXT); // write type
                        dataOut.writeInt(body.length); // write length
                        dataOut.write(body);
                        byte []data = byteOut.toByteArray();
                        byteOut.reset();

                        DatagramPacket datagramPacket = new DatagramPacket(data,data.length,InetAddress.getByName(host_port.split(":")[0]), Integer.parseInt(host_port.split(":")[1]));//指定发送数据、远程IP、远程端口
                        _sendQueue.add(datagramPacket);//告诉server自己的name和本地ip，远程ip和端口server端是可以直接获取到的
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                        P2pMessage p2pMessage = message.dealMessage(packet.getData());
                        if (p2pMessage.type!=2) {
                            Log.i("send", " " + packet.getAddress() + ":" + packet.getPort() + "  " + message.dealMessage(packet.getData()).toString());
                        }
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
                        message.dealMessage(packet.getData());
                        if (message.type!=6&&!message.body.contains("you")){
                            Log.i("receive", " " + packet.getAddress() + ":" + packet.getPort() + "  " + message.toString());
                        }
                        if(message.verifyMagic()){//收到Server发来的对方信息
                            if ((message.type==message.MTYPE_REPLY)&(message.body.equals("Login success!")|message.body.contains("Login failed"))) {
                                byte[] body = ("").getBytes();
                                dataOut.writeShort(0x8964); // write magic
                                dataOut.writeShort(message.MTYPE_LIST); // write type
                                dataOut.writeInt(body.length); // write length
                                dataOut.write(body);
                                final byte[] data1 = byteOut.toByteArray();
                                final DatagramPacket datagramPacket = new DatagramPacket(data1, data1.length, serverAddress, port);//指定发送数据、远程IP、远程端口
                                byteOut.reset();
                                Timer timer = new Timer();
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        _sendQueue.add(datagramPacket);
                                    }
                                }, 1000, 10000);
                            }else if ((message.type==message.MTYPE_REPLY)&(message.body.contains("you"))){
                                String hosts[] = message.body.split(";");
                                for (String host:hosts){
                                    if (!list_data.contains(host)){
                                        list_data.add(host);
                                    }
                                }
                                handler.sendEmptyMessage(3);
                            }else if ((message.type==message.MTYPE_TEXT)&(message.body.contains("punch request sent"))) {
                                Message mes = new Message();
                                mes.what=4;
                                mes.obj=target_host_port;
                                handler.sendMessage(mes);
                            }else if (message.type==message.MTYPE_PUNCH){
                                // 判断消息是否来自服务器
                                if (packet.getAddress().toString().equals(serverAddress.toString())&&packet.getPort()==port){
                                    String host_port = message.body;
                                    target_host_port = host_port;
                                    byte []body = ("ok").getBytes();
                                    dataOut.writeShort(0x8964); // write magic
                                    dataOut.writeShort(message.MTYPE_REPLY); // write type
                                    dataOut.writeInt(body.length); // write length
                                    dataOut.write(body);
                                    byte []data1 = byteOut.toByteArray();
                                    byteOut.reset();
                                    DatagramPacket datagramPacket = new DatagramPacket(data1,data1.length,InetAddress.getByName(host_port.split(":")[0]), Integer.parseInt(host_port.split(":")[1]));
                                    _sendQueue.add(datagramPacket);
                                    Message mes = new Message();
                                    mes.what=4;
                                    mes.obj=host_port;
                                    handler.sendMessage(mes);
                                }else {
                                    Log.i("", "oooooooooooooooooookkkkkkkkkkkkkkkkkkkkkk");
                                }
                            }else {

                            }
                        }
                        else {
                            Log.i("验证", "magic 不一致");// 验证P2P通信
                        }
                        // handler.sendEmptyMessage(2);
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
            } else if (msg.what == 3){
                activity.adapter.notifyDataSetChanged();
            }else if (msg.what == 4){
                String name = String.format("发送: %s",(String) msg.obj);
                activity.sendBtn.setText(name);
                activity.sendBtn.setEnabled(true);
            }
        }
    }
}
