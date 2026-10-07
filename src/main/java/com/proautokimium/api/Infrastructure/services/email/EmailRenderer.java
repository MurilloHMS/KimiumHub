package com.proautokimium.api.Infrastructure.services.email;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class EmailRenderer {
    private final TemplateEngine templateEngine;

    public EmailRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String template, Map<String, Object> vars){
        Context ctx = new Context();
        ctx.setVariables(vars);
        return templateEngine.process(template, ctx);
    }
}
