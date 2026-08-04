package com.mbn.kculturenews.naver;

public class NaverConfigurationException extends RuntimeException {

    public NaverConfigurationException() {
        super("NAVER_CLIENT_ID와 NAVER_CLIENT_SECRET 환경변수를 설정해 주세요.");
    }
}
