package com.wcy.eyetracking.util;

import java.nio.charset.Charset;

public class P2pMessage {
    public final int MTYPE_LOGIN = 0;
    public final int MTYPE_LOGOUT = 1;
    public final int MTYPE_LIST = 2;
    public final int MTYPE_PUNCH = 3;
    public final int MTYPE_PING = 4;
    public final int MTYPE_PONG = 5;
    public final int MTYPE_REPLY = 6;
    public final int MTYPE_TEXT = 7;
    public final int MTYPE_END = 8;
    private final int TARGET_MAGIC = 35172;

    public int magic;
    public short type;
    public int length;
    public String body;

    public P2pMessage(){

    }

    public P2pMessage dealMessage(byte[] bytes){
        this.magic = (int)((bytes[0] & 0xff)<<8)|(bytes[1] & 0xff);
        this.type = (short) (((bytes[2] & 0xff)<<8)|(bytes[3] & 0xff));
        this.length = (int)((bytes[4] & 0xff)<<24)|((bytes[5] & 0xff)<<16)|((bytes[6] & 0xff)<<8)|(bytes[7] & 0xff);
        this.body = new String(bytes, 8, this.length).trim(); // 去掉后面 \0
        return this;
    }

    /**
     * 验证magic是否正确
     * @return boolean
     */
    public boolean verifyMagic(){
        return this.magic==this.TARGET_MAGIC;
    }

    @Override
    public String toString() {
        return "P2pMessage{" +
                "magic=" + magic +
                ", type=" + type +
                ", length=" + length +
                ", body='" + body + '\'' +
                '}';
    }
}
