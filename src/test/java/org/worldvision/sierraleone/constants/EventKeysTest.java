package org.worldvision.sierraleone.constants;

import org.junit.Before;
import org.junit.Test;
import org.motechproject.event.MotechEvent;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.motechproject.commons.date.util.DateUtil.newDateTime;

public class EventKeysTest {
    private MotechEvent event;

    @Before
    public void setUp() throws Exception {
        event = new MotechEvent();
        event.getParameters().put("string", "correct_value");
        event.getParameters().put("date", newDateTime(2013, 8, 12));
        event.getParameters().put("collection", asList(1, 2, 3));
    }

    @Test
    public void shouldReturnCorrectValue() {
        assertEquals("correct_value", EventKeys.getStringValue(event, "string"));
        assertEquals(newDateTime(2013, 8, 12), EventKeys.getDateTimeValue(event, "date"));
        assertEquals(asList(1, 2, 3), EventKeys.getListValue(event, "collection", Integer.class));
    }

    @Test
    public void shouldReturnNullIfValueHasNotCorrectType() {
        assertNull(EventKeys.getStringValue(event, "date"));
        assertNull(EventKeys.getDateTimeValue(event, "string"));
        assertTrue(EventKeys.getListValue(event, "string", Integer.class).isEmpty());
    }
}
