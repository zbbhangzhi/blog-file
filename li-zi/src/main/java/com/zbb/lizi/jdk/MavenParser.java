package com.zbb.lizi.jdk;

import org.apache.maven.model.Dependency;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 已知maven信息 拉取jar包解析
 * @Author: tiancha
 * @Date: 2020/9/24 4:11 下午
 */
public class MavenParser {
    public static void main(String[] args) {
        String pomXml = "<dependency><groupId>com.yunhu.thirdparty</groupId><artifactId>thirdparty-client</artifactId><version>1.0.11</version>";
        parser(pomXml);
    }

    private static void parser(String pomXml){
        // 1. 已知jar坐标，解析出编译jar的下载地址
        String classJarFileUrl = getClassJarFileUrl(pomXml);
        String classFileName = classJarFileUrl.substring(0);

        // 2. 下载编译jar，存到本地
//        File classFile = downloadJar();

        // 3. 解析文件内容
    }

    private static File downloadJar(String remoteUrl, String savePath, String fileName)throws IOException {
        File file = new File(savePath + fileName);
        URL url = new URL(remoteUrl);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows XP; DigExt)");

        InputStream inputStream = connection.getInputStream();
        // copy input stream to file

        return file;
    }

    private static String getClassJarFileUrl(String pomXml) {
        Dependency dependency = parseDependencyInfo(pomXml);
        return getJarFileUrlFromDependency(dependency);
    }

    private static String getJarFileUrlFromDependency(Dependency dependency) {
        return null;
    }

    private static Dependency parseDependencyInfo(String pomXml) {
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(pomXml);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        Element element = doc.getRootElement();

        Dependency dependency = new Dependency();
        dependency.setArtifactId(element.element("").getStringValue());
        dependency.setVersion(element.element("").getStringValue());
        dependency.setGroupId(element.element("").getStringValue());
        return dependency;
    }
}
