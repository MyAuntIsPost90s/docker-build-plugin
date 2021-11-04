package com.ch.plugin;

import java.io.InputStream;

/**
 * cmd 命令工具类
 *
 * @author cch on 2020/12/08
 **/
public class CmdUtil {

    public static String run(String cmd) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        Runtime run = Runtime.getRuntime();
        Process process = run.exec(cmd);
        try (InputStream inputStream = process.getInputStream()) {
            int len;
            byte[] buff = new byte[1024 * 1024];
            Thread.sleep(100);
            while ((len = inputStream.read(buff)) != -1) {
                stringBuilder.append(new String(buff, 0, len));
            }
        }
        return stringBuilder.toString();
    }
}
