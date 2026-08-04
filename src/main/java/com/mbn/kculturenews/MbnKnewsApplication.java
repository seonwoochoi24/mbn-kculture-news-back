package com.mbn.kculturenews;

import com.mbn.kculturenews.naver.NaverNewsProperties;
import com.mbn.kculturenews.rss.RssProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({RssProperties.class, NaverNewsProperties.class})
@SpringBootApplication
public class MbnKnewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MbnKnewsApplication.class, args);
    }
}
