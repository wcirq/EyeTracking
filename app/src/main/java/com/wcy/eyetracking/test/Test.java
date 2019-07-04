package com.wcy.eyetracking.test;

public class Test {
    enum _MessageType {
        MTYPE_LOGIN,
        MTYPE_LOGOUT,
        MTYPE_LIST,
        MTYPE_PUNCH,
        MTYPE_PING,
        MTYPE_PONG,
        MTYPE_REPLY,
        MTYPE_TEXT,
        MTYPE_END
    };
    public static void main(String []arge){
        System.out.print(_MessageType.MTYPE_END);
    }
}
