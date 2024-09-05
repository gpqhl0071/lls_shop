package org.example.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {
    public static final String BASE_URL = "https://plat-deal-api.lilithgame.com/api/v1/products";
    public static final String DEFAULT_PARAMS = "order_by=4&is_desc=false&game_id=10043&ltp_env_id=official_cn";
    
    public static String getApiUrl(int status, int page, int pageSize) {
        return BASE_URL + "?" + DEFAULT_PARAMS + "&status=" + status + "&page=" + page + "&page_size=" + pageSize;
    }
    
    public static final int STATUS_ALL = 0;
    public static final int STATUS_UNSOLD = 2;
    public static final int STATUS_SOLD = 3;
}