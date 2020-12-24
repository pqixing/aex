package com.pqixing.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jiangqiming on 2017/10/19.
 */

public class UrlUtils {
    public static final int FLAG_LOWCASE = 2;
    public static final int FLAG_UPCASE = 1;
    public static final int FLAG_NULL = 0;

    public static Map<String, String> getParams(String URL,int flag)
    {
        Map<String, String> mapRequest = new HashMap<String, String>();

        String[] arrSplit=null;

        String strUrlParam=TruncateUrlPage(URL,flag);
        if(strUrlParam==null)
        {
            return mapRequest;
        }
        //每个键值为一组 www.2cto.com
        arrSplit=strUrlParam.split("[&]");
        for(String strSplit:arrSplit)
        {
            String[] arrSplitEqual=null;
            arrSplitEqual= strSplit.split("[=]");

            //解析出键值
            if(arrSplitEqual.length>1)
            {
                //正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);

            }
            else
            {
                if(arrSplitEqual[0]!="")
                {
                    //只有参数没有值，不加入
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }

    public static String toUrl(String scheme,String host,Map<String,String> params){
        StringBuilder url  = new StringBuilder(scheme).append("://").append(host).append("?");
        for (Map.Entry<String,String> m: params.entrySet()) {
            url.append(m.getKey()).append("=").append(m.getValue()).append("&");
        }
        return url.substring(0,url.length()-1);
    }
    /**
     * 解析出url参数中的键值对
     * @param URL  url地址
     * @return  url请求参数部分
     */
    public static Map<String, String> getParams(String URL)
    {
        return getParams(URL,FLAG_NULL);
    }
    /**
     * 去掉url中的路径，留下请求参数部分
     * @param strURL url地址
     * @return url请求参数部分
     */
    private static String TruncateUrlPage(String strURL,int flag)
    {
        String strAllParam=null;
        String[] arrSplit=null;

        strURL=strURL.trim();
        switch (flag){
            case FLAG_LOWCASE:
                strURL = strURL.toLowerCase();
                break;
            case FLAG_UPCASE:
                strURL = strURL.toUpperCase();
                break;
        }

        arrSplit=strURL.split("[?]");
        if(strURL.length()>1)
        {
            if(arrSplit.length>1)
            {
                if(arrSplit[1]!=null)
                {
                    strAllParam=arrSplit[1];
                }
            }
        }

        return strAllParam;
    }
}
