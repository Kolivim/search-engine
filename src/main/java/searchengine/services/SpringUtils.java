package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringUtils
{
    public static ApplicationContext ctx;
    @Autowired
    private void setApplicationContext(ApplicationContext applicationContext) {ctx = applicationContext;}
}
