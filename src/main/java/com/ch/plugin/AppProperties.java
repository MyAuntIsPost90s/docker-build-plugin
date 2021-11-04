package com.ch.plugin;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class AppProperties {

    @Value("${app.docker-api-domain}")
    private String  dockerApiDomain;

    @Value("${app.docker-image-version-regex}")
    private String dockerImageVersionRegex;

    @Value("${app.docker-image-name-regex}")
    private String dockerImageNameRegex;
}
