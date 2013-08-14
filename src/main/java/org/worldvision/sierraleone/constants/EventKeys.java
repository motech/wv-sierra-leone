package org.worldvision.sierraleone.constants;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public final class EventKeys {
    private EventKeys() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EventKeys.class);

    public static final String DATE_OF_VISIT = "date_of_visit";
    public static final String REFERRAL_CASE_ID = "referral_case_id";
    public static final String MOTHER_CASE_ID = "mother_case_id";
    public static final String CHILD_CASE_ID = "child_case_id";

    public static final String BASE_SUBJECT = "org.worldvision.sierraleone.";
    public static final String MOTHER_REFERRAL_SUBJECT = BASE_SUBJECT + "mother-referral";

    public static final String CONSECUTIVE_CHILD_VISIT_BASE_SUBJECT = BASE_SUBJECT + "child-visit.";
    public static final String CONSECUTIVE_CHILD_VISIT_WILDCARD_SUBJECT = CONSECUTIVE_CHILD_VISIT_BASE_SUBJECT + "*";

    public static final String CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT = BASE_SUBJECT + "post-partum.";
    public static final String CONSECUTIVE_POST_PARTUM_VISIT_WILDCARD_SUBJECT = CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT + "*";

    public static String getStringValue(MotechEvent event, String key) {
        String ret = null;
        try {
            ret = (String) event.getParameters().get(key);
        } catch (ClassCastException e) {
            LOGGER.warn(String.format("Event: %s Key: %s is not a String", event, key));
        }

        return ret;
    }

    public static DateTime getDateTimeValue(MotechEvent event, String key) {
        DateTime ret = null;
        try {
            ret = (DateTime) event.getParameters().get(key);
        } catch (ClassCastException e) {
            LOGGER.warn(String.format("Event: %s Key: %s is not a DateTime", event, key));
        }

        return ret;
    }

    public static <T> List<T> getListValue(MotechEvent event, String key, Class<T> clazz) {
        List<T> list = new ArrayList<>();

        try {
            Collection collection = (Collection) event.getParameters().get(key);

            if (collection != null) {
                for (Object obj : collection) {
                    list.add(clazz.cast(obj));
                }
            }
        } catch (ClassCastException e) {
            LOGGER.warn(String.format("Event: %s Key: %s is not a Collection of %s", event, key, clazz.getSimpleName()));
            list.clear();
        }

        return list;
    }

}

