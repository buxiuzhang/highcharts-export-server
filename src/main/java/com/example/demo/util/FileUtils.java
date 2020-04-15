package com.example.demo.util;

import com.example.demo.controller.HighchartsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * @author ：zhangzhonghui
 * @version ：v0.1
 * @description ：用于管理文件的增删改查
 * @datetime ：2020-04-15 18:40
 */
public class FileUtils {
    private static final Logger logger= LoggerFactory.getLogger(HighchartsController.class);

    /**
     * 静态内部类 用于构造单例模式
     */
    static class FileUtilsStatic{
        private static FileUtils fileUtils=new FileUtils();
        private static FileUtils getInstance(){
            return fileUtils;
        }
    }
    private FileUtils(){}
    public static FileUtils getInstance(){
        return FileUtilsStatic.getInstance();
    }


    /**
     * 将文件转换成 ByteArrayOutputStream
     * @param path
     * @return
     * @throws IOException
     */
    public ByteArrayOutputStream readFileState(String path) {
        String str = "";
        BufferedInputStream buff;
        ByteArrayOutputStream output = null;
        try {
            buff = new BufferedInputStream(new FileInputStream(path));
            byte[] bytes=new byte[1024];
            int line;
            output = new ByteArrayOutputStream();
            while( (line = buff.read(bytes)) != -1) {
                output.write(bytes,0,line);
            }
            buff.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    /**
     * niod的方式写入文件
     * @param fileName
     * @param text
     * @throws IOException
     */
    public void writeFileState(String fileName, String text) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(fileName));
            FileChannel fileChannel = fileOutputStream.getChannel();
            ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(text);
            int length = 0;
            try {
                while ((length = fileChannel.write(byteBuffer)) != 0) {
                    logger.info("file length have written: " + length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            logger.error("写入文件异常： ",e);
        }finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

    }

}
