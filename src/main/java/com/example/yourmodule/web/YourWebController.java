package com.example.yourmodule.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class YourWebController {    // service methods for angular ui

    @RequestMapping("/your-objects")
    @ResponseBody
    public String getAllObjects() {

        // send some dummy json
        return "[" +
            "{ \"foo\":\"bar\"}," +
            "{ \"foo\":\"baz\"}" +
        "]";
    }
}
