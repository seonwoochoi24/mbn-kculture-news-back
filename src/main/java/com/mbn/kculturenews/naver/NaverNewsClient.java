package com.mbn.kculturenews.naver;

public interface NaverNewsClient {

    NaverNewsPage search(String keyword, int start, int display);
}
