package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * @author ：zhangzhonghui
 * @version ：v0.1
 * @description ：
 * @datetime ：2020-04-13 15:49
 */
@RestController
@RequestMapping("/highcharts")
public class HighchartsController {
    private static final Logger logger= LoggerFactory.getLogger(HighchartsController.class);
    /**
     * 文件输出路径
     */
    @Value("${highcharts.outFilePath}")
    private String outFilePath;
    /**
     * 文件后缀
     */
    @Value("${highcharts.suffix}")
    private String suffix;


    /**
     * 使用 highcharts-export-server 插件 将json串 转换成统计图。
     * 并以base64字符串的进行返回
     * @param json highcharts josn格式
     * @return base64 字符串
     */
    @RequestMapping("/run")
    public JSONObject run(String json){
        JSONObject result=new JSONObject();

        if (json==null ||"".equals(json)){
            result.put("code",500);
            result.put("msg","json格式为空");
            return result;
        }
        try {
            result.parseObject(json);
        } catch (Exception e) {
            logger.error("json 格式错误",e);
            result.put("code",500);
            result.put("msg","json格式错误");
            return result;
        }
        //生成为id
        String uuid=UUID.randomUUID().toString().replaceAll("-","");
        String inFileName="./"+uuid+".json";
        try {
            writeFileState(inFileName,json);
        } catch (IOException e) {
            logger.error("IO关闭异常",e);
            result.put("code",500);
            result.put("msg","文件读取失败");
            return result;
        }

        synchronized (this){
            String outfileName=outFilePath+uuid+suffix;
            String cmd = "highcharts-export-server --type png --infile "+inFileName+" --outfile "+outfileName;
            ByteArrayOutputStream byteOutStream=null;
            try {
                Runtime run = Runtime.getRuntime();
                Process p = run.exec(cmd);

                BufferedInputStream in = new BufferedInputStream(p.getInputStream());
                BufferedReader inBr = new BufferedReader(new InputStreamReader( in ));
                String lineStr;

                while ((lineStr = inBr.readLine()) != null) {
                    System.out.println(lineStr);
                }
                inBr.close(); in .close();

                //将文件转换 base64 字符串
                byteOutStream = readFileState(outfileName);
                String base64=Base64.getEncoder().encodeToString(byteOutStream.toByteArray());

                result.put("data",base64);
                result.put("code",200);
                result.put("msg","success");
                return result;
            } catch (IOException e) {
                logger.error("文件导出失败",e);
                result.put("code",500);
                result.put("msg","文件导出失败");
                return result;
            }finally {
                //删除文件
                try {
                    Files.delete(Paths.get(outfileName));
                    Files.delete(Paths.get(inFileName));
                    if (byteOutStream != null) {
                        byteOutStream.close();
                    }
                } catch (IOException e) {
                    logger.error("文件删除失败",e);
                    result.put("code",500);
                    result.put("msg","文件删除失败");
                    return result;
                }
            }
        }

    }

    /**
     * 将文件转换成 ByteArrayOutputStream
     * @param path
     * @return
     * @throws IOException
     */
    public static ByteArrayOutputStream readFileState(String path) throws IOException {
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
    public static void writeFileState(String fileName, String text) throws IOException {
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
