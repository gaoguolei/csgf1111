package org.jeecg;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
* 单体启动类
* 报错提醒: 未集成mongo报错，可以打开启动类上面的注释 exclude={MongoAutoConfiguration.class}
*/
@Slf4j
@SpringBootApplication
//@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class})
@EnableScheduling
public class PcmsSystemApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(PcmsSystemApplication.class);
    }


    public static void main(String[] args) throws UnknownHostException {
        try {
            ConfigurableApplicationContext application = SpringApplication.run(PcmsSystemApplication.class, args);
            Environment env = application.getEnvironment();
            String ip = InetAddress.getLocalHost().getHostAddress();
            String port = env.getProperty("server.port");
            String path = oConvertUtils.getString(env.getProperty("server.servlet.context-path"));
            log.info("\n----------------------------------------------------------\n\t" +
                    "Application PCMS(Photovoltaic construction management system) is running! Access URLs:\n\t" +
                    "Local: \t\thttp://localhost:" + port + path + "/\n\t" +
                    "External: \thttp://" + ip + ":" + port + path + "/\n\t" +
                    "Swagger文档: \thttp://" + ip + ":" + port + path + "/doc.html\n" +
                    "----------------------------------------------------------");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

}