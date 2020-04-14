package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.util.Strings;
import org.omg.CORBA.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
        writeFileState(inFileName,json);

        synchronized (this){
            String outfileName=outFilePath+uuid+suffix;
            String cmd = "highcharts-export-server --infile "+inFileName+" --outfile "+outfileName;
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
                byte[] b = Files.readAllBytes(Paths.get(outfileName));

                result.put("data",Base64.getEncoder().encodeToString(b));
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
                } catch (IOException e) {
                    logger.error("文件删除失败",e);
                    result.put("code",500);
                    result.put("msg","文件删除失败");
                    return result;
                }
            }
        }

    }

    public static void writeFileState(String fileName, String text) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(fileName));
            FileChannel fileChannel = fileOutputStream.getChannel();
            ByteBuffer byteBuffer = Charset.forName("GBK").encode(text);
            int length = 0;
            try {
                while ((length = fileChannel.write(byteBuffer)) != 0) {
                    System.out.println("file length have written: " + length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
