package com.mbn.kculturenews.rss;

import java.net.URI;
import java.util.List;

public interface RssFeedClient {

    List<RssItem> fetch(URI feedUri);
}
