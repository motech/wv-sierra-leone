package org.worldvision.sierraleone;

import org.apache.commons.lang.StringUtils;
import org.motechproject.event.MotechEvent;

import java.util.HashMap;
import java.util.Map;

public class MotechEventBuilder {
    private String subject = StringUtils.EMPTY;
    private Map<String, Object> parameters = new HashMap<>();

    public MotechEventBuilder withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public MotechEventBuilder withParameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    public MotechEvent build() {
        return new MotechEvent(subject, parameters);
    }

}
