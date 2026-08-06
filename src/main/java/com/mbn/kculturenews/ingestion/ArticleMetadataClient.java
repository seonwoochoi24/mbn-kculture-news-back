package com.mbn.kculturenews.ingestion;

import java.net.URI;

public interface ArticleMetadataClient {

    MbnArticleMetadata fetch(URI articleUri);
}
