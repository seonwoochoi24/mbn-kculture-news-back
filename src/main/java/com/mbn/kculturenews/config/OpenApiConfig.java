package com.mbn.kculturenews.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "MBN Knews API",
                version = "v1",
                description = "MBN 연예 RSS 수집 및 한류 뉴스 조회 백엔드 API",
                contact = @Contact(name = "MBN Knews Team")
        ),
        servers = @Server(url = "/", description = "현재 서버")
)
@SecurityScheme(
        name = "AdminKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = AdminApiKeyFilter.HEADER_NAME,
        description = "운영 환경의 관리자 API 키"
)
@Configuration
public class OpenApiConfig {
}
