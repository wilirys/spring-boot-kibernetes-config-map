package com.kibernetes.configmap.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControllerMVC {


    @Value("${my.system.property:defaultValue}")
    protected String fromSystem;

    @RequestMapping("/say_hello")
    public String sayHello() {
        return "Hello my majesty, your minions from " + fromSystem;
    }
}
