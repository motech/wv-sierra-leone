package org.worldvision.sierraleone;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.motechproject.commons.date.util.DateUtil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FormCheckerTest {
    private FormChecker checker;

    @Before
    public void setUp() throws Exception {
        checker = new FormChecker();
        checker.addMetadata("one", "1");
        checker.addMetadata("two", "2");
        checker.addMetadata("three", "3");
    }

    @Test
    public void shouldReturnTrueIfAllFieldsAreSet() throws Exception {
        checker.checkFieldExists("string", "I have a value");
        checker.checkFieldExists("date", DateUtil.now());

        assertTrue(checker.check());
    }

    @Test
    public void shouldReturnFalseIfOneOfFieldsAreNotSet() throws Exception {
        checker.checkFieldExists("string", StringUtils.EMPTY);
        assertFalse(checker.check());

        checker.checkFieldExists("date", (String) null);
        assertFalse(checker.check());

        checker.checkFieldExists("date", (DateTime) null);
        assertFalse(checker.check());
    }
}
