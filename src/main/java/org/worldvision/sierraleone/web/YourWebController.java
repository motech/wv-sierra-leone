package org.worldvision.sierraleone.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

// service methods for angular ui
@Controller
public class YourWebController {

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
