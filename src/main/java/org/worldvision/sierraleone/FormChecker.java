package org.worldvision.sierraleone;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormChecker {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, String> metadata = new HashMap<>();
    private List<String> fields = new ArrayList<>();

    public void addMetadata(String key, String name) {
        metadata.put(key, name);
    }

    public void checkFieldExists(String name, String value) {
        if (null == value || value.isEmpty()) {
            fields.add(name);
        }
    }

    public void checkFieldExists(String name, DateTime value) {
        if (null == value) {
            fields.add(name);
        }
    }

    public boolean check() {
        if (fields.isEmpty()) {
            return true;
        } else {
            StringBuffer msg = new StringBuffer("Form Error: ");

            if (!metadata.isEmpty()) {
                msg.append("metadata(");
                for (String key : metadata.keySet()) {
                    msg.append(key + ":" + metadata.get(key));
                }
                msg.append(") ");
            }

            msg.append("fields(");
            for (String field : fields) {
                msg.append(field);
            }
            msg.append(") ");

            logger.error(msg.toString());
        }

        return false;
    }
}
