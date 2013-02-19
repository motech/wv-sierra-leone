package org.worldvision.sierraleone;

import org.motechproject.event.MotechEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class EventKeys {
    private EventKeys() { }

    private static final Logger logger = LoggerFactory.getLogger("org.worldvision.sierraleone.EventKeys");

    public static final String DATE_OF_BIRTH = "date_of_birth";
    public static final String DATE_OF_VISIT = "date_of_visit";
    public static final String DAYS_SINCE_BIRTH = "days_since_birth";
    public static final String MOTHER_ALIVE = "mother_alive";
    public static final String GAVE_BIRTH = "gave_birth";
    public static final String REFERRAL_CASE_ID = "referral_case_id";
    public static final String ATTENDED_POSTNATAL = "attended_postnatal";
    public static final String PLACE_OF_BIRTH = "place_of_birth";
    public static final String MOTHER_CASE_ID = "mother_case_id";

    public static final String BASE_SUBJECT = "org.worldvision.sierraleone.";
    public static final String FORM_BASE_SUBJECT = BASE_SUBJECT + "form.";
    public static final String REGISTER_CHILD_FORM_SUBJECT = FORM_BASE_SUBJECT + "register-child";
    public static final String POST_PARTUM_FORM_SUBJECT = FORM_BASE_SUBJECT + "post-partum";
    public static final String MOTHER_REFERRAL_SUBJECT = BASE_SUBJECT + "mother-referral";

    public static String getStringValue(MotechEvent event, String key) {
        String ret = null;
        try {
            ret = (String) event.getParameters().get(key);
        } catch (ClassCastException e) {
            logger.warn("Event: " + event + " Key: " + key + " is not a String");
        }

        return ret;
    }

    public static Integer getIntegerValue(MotechEvent event, String key) {
        Integer ret = null;
        try {
            ret = (Integer) event.getParameters().get(key);
        } catch (NumberFormatException e) {
            logger.warn("Event: " + event + " Key: " + key + " is not an Integer");
        }

        return ret;
    }
}

