package com.ch.plugin;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootApplication
public class Application {

    private static ConfigurableApplicationContext context;
    private static AppProperties properties;
    private static String dockerFile;
    private static String imageVersion;
    private static String imageName;
    private static String afterScript;
    private static Integer historyCount;

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setBannerMode(Banner.Mode.OFF);
        context = application.run();

        properties = context.getBean(AppProperties.class);
        imageVersion = null;
        historyCount = 2;
        argsParse(args);

        build();
        afterBuild();
    }

    public static void argsParse(String[] args) {
        for (String arg : args) {
            if (arg.contains("--docker-file=")) {
                dockerFile = arg.replace("--docker-file=", "");
            } else if (arg.contains("--after-script=")) {
                afterScript = arg.replace("--after-script=", "");
            } else if (arg.contains("--history-count=")) {
                historyCount = Integer.valueOf(arg.replace("--history-count=", ""));
            }
        }
        if (StrUtil.isEmpty(dockerFile)) {
            throw new IllegalArgumentException("?????? --docker-file=xxx ????????????");
        }
    }

    public static void build() throws Exception {
        String content = FileUtil.readString(dockerFile, StandardCharsets.UTF_8);
        // ??????????????????
        Matcher matcher = Pattern.compile(properties.getDockerImageNameRegex()).matcher(content);
        if (!matcher.find()) {
            throw new IllegalArgumentException("docker file ???????????????????????????[" + properties.getDockerImageNameRegex() + "]?????????");
        }
        imageName = matcher.group();
        // ?????????????????????
        matcher = Pattern.compile(properties.getDockerImageVersionRegex()).matcher(content);
        if (matcher.find()) {
            imageVersion = matcher.group();
        } else {
            imageVersion = randomVersion();
        }
        String newImageName = imageName + ":" + imageVersion;
        // ????????????????????????
        String json = Jsoup.connect(properties.getDockerApiDomain() + "/images/json").ignoreContentType(true).get().text();
        JSONArray images = JSON.parseArray(json);
        Set<String> imageNames = images.stream()
                .map(o -> (JSONObject) o)
                .filter(o -> CollectionUtil.isNotEmpty(o.getJSONArray("RepoTags")))
                .flatMap(o -> o.getJSONArray("RepoTags").stream())
                .map(o -> o.toString())
                .collect(Collectors.toSet());
        // ?????????????????????????????????
        String tempImageName = newImageName;
        if (imageNames.stream().anyMatch(o -> o.equals(tempImageName))) {
            imageVersion = randomVersion();
            newImageName = imageName + ":" + imageVersion;
        }
        // ?????? dockerfile
        String result = CmdUtil.run(String.format("docker build -t %s -f %s .", newImageName, dockerFile));
        System.err.println(result);
        if (!result.contains("Successfully")) {
            System.exit(1);
        }
        // ??????????????????
        List<String> expireImageNames = imageNames.stream().filter(o -> o.split(":")[0].equals(imageName)).sorted(Comparator.reverseOrder()).skip(historyCount).collect(Collectors.toList());
        for (String expireImageName : expireImageNames) {
            try {
                Connection.Response response = Jsoup.connect(properties.getDockerApiDomain() + "/images/" + expireImageName)
                        .ignoreContentType(true)
                        .method(Connection.Method.DELETE)
                        .execute();
                if (response.statusCode() == 200) {
                    System.out.println("[INFO] delete history image:" + expireImageName);
                } else {
                    System.out.println("[ERROR] delete history image:" + expireImageName + ", " + response.body());
                }
            } catch (Exception e) {
                System.err.println("[ERROR] delete history image:" + expireImageName + ", error message:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void afterBuild() throws Exception {
        String newImageName = imageName + ":" + imageVersion;
        System.out.println("[INFO] new docker image:" + newImageName);
        if (StrUtil.isNotEmpty(afterScript)) {
            afterScript = afterScript.replace("{imageName}", newImageName);
            System.out.println("[INFO] after-script :" + afterScript);
            String result = CmdUtil.run(afterScript);
            System.out.println(result);
        }
    }

    private static String randomVersion() {
        return DateUtil.format(new Date(), "yyMMddHHmmss");
    }
}
