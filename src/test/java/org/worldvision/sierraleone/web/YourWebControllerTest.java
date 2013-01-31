package org.worldvision.sierraleone.web;

import org.junit.Assert;
import org.junit.Test;
import org.worldvision.sierraleone.web.YourWebController;

import static org.junit.Assert.assertNotNull;


/**
 * Unit test for Controller.
 */
public class YourWebControllerTest {
    @Test
    public void testController() {
        Assert.assertNotNull(new YourWebController().getAllObjects());
    }
}
