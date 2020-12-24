package com.pqixing.tools;

import com.pqixing.XHelper;

import java.net.URLEncoder;

public class AEXTest {
    public static void main(String[] args) {
        testEncode();
    }

    public static void testEncode() {
        String str = "hsdfl沙发asdf-___=sadf./sadf--=121**^*&%^";
        String encode = XHelper.INSTANCE.testEncode(str);
        System.out.println(URLEncoder.encode(str));
        System.out.println(Integer.valueOf('沙'));
        System.out.println(encode);
        System.out.println(XHelper.INSTANCE.testDecode(encode));
    }
}
