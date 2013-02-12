package org.worldvision.sierraleone.listener;

import com.google.common.collect.Multimap;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.EventKeys;

import java.util.Date;
import java.util.Map;

@Component
public class CommCareFormStubListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SettingsFacade settings;

    @Autowired
    MotechSchedulerService schedulerService;

    @Autowired
    CommcareFormService commcareFormService;

    @Autowired
    EventRelay eventRelay;

    @MotechListener(subjects = EventSubjects.FORM_STUB_EVENT )
    public void handle(MotechEvent event) {
        logger.info("MotechEvent received on " + EventSubjects.FORM_STUB_EVENT);

        if (commcareFormService == null) {
            logger.error("CommCare service is not available!");
            return;
        }

        // read event payload
        Map<String, Object> eventParams = event.getParameters();
        String formId = (String) eventParams.get(EventDataKeys.FORM_ID);
        if (formId == null) {
            logger.error("No " + EventDataKeys.FORM_ID + " key in event: " + event.toString());
            return;
        }

        // Get the form from CommCare
        CommcareForm form = commcareFormService.retrieveForm(formId);
        if (form == null) {
            logger.error("Unable to load form " + formId + " from CommCare");
            return;
        }

        String formName = form.getForm().getAttributes().get("name");
        logger.info("form name " + formName);

        // Create MOTECH event, populate subject and params below
        MotechEvent formEvent = null;

        switch (formName) {
            case "Register Child":
                handleRegisterChildForm(form);

                break;

            case "Post-Partum Visit":
                handlePostPartumVisitForm(form);

                break;

            case "Child Visit":
                break;
        }

        // publish event
//        if (null != formEvent)
//            eventRelay.sendEventMessage(formEvent);

/*
        Map<String, String> attributes = form.getForm().getAttributes();
        for (Map.Entry entry : attributes.entrySet()) {
            logger.info("Attribute: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }

        Multimap<String, FormValueElement> subElements = form.getForm().getSubElements();
        for (Map.Entry<String, FormValueElement> entry : subElements.entries()) {
            FormValueElement value = entry.getValue();
            logger.info("Form Key(" + entry.getKey() + "): " + value.getElementName() + " Value: " + value.getValue());
        }
*/
    }

    private void handleRegisterChildForm(CommcareForm form) {
        FormValueElement registration = form.getForm().getElementByName("registration");
        FormValueElement dob = registration.getElementByName("dob");

        FormValueElement motherAlive = form.getForm().getElementByName("mother_alive");

        logger.info("dob: " + dob.getValue());
        logger.info("mother_alive: " + motherAlive.getValue());

        String childCaseId = null;
        String motherCaseId = null;

        FormValueElement subcase_0 = form.getForm().getElementByName("subcase_0");
        FormValueElement _case = subcase_0.getElementByNameIncludeCase("case");

        if (null != _case) {
            childCaseId = _case.getAttributes().get("case_id");

            try {
                FormValueElement parentElement = _case.getElementByNameIncludeCase("index").getElementByName("parent");

                if ("mother".equals(parentElement.getAttributes().get("case_type"))) {
                    motherCaseId = parentElement.getValue();
                } else {
                    logger.debug("case_type(" + parentElement.getAttributes().get("case_type") + ") != 'mother'");
                }
            } catch (NullPointerException e) {
                logger.debug(e.getMessage());
                // I just don't want to die if this happens.  I'll deal with the missing values below
                // when I verify I have a value for motherCaseId
            }
        } else {
            logger.info("case is null");
        }

        logger.info("Child Case Id: " + childCaseId);
        logger.info("Mother Case Id: " + motherCaseId);

        //formEvent.getParameters().put(EventKeys.DATE_OF_BIRTH, form.getForm().getSubElements().get());
    }

    private void handlePostPartumVisitForm(CommcareForm form) {
        String stillAlive = null;
        String gaveBirth = null;
        String createReferral = null;
        String referralId = null;
        String dob = null;
        String attendedPostnatal = null;
        String placeOfBirth = null;
        String motherCaseId = null;

        FormValueElement element = form.getForm().getElementByName("still_alive");
        stillAlive = element != null ? element.getValue() : null;

        element = form.getForm().getElementByName("gave_birth");
        gaveBirth = element != null ? element.getValue() : null;

        element = form.getForm().getElementByName("create_referral");
        createReferral = element != null ? element.getValue() : null;

        element = form.getForm().getElementByName("referral_id");
        referralId = element != null ? element.getValue() : null;

        FormValueElement postPartumVisit = form.getForm().getElementByName("post_partum_visit");
        element = postPartumVisit.getElementByName("dob");
        dob = element != null ? element.getValue() : null;

        element = postPartumVisit.getElementByName("attended_postnatal");
        attendedPostnatal = element != null ? element.getValue() : null;

        element = postPartumVisit.getElementByName("place_of_birth");
        placeOfBirth = element != null ? element.getValue() : null;

        DateTimeFormatter dobFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendLiteral('-')
                .appendMonthOfYear(2)
                .appendLiteral('-')
                .appendDayOfMonth(2)
                .toFormatter();

        DateTime dateOfBirth = dobFormatter.parseDateTime(dob);
        int days = Days.daysBetween(dateOfBirth, new DateTime()).getDays();

        element = form.getForm().getElementByNameIncludeCase("case");
        motherCaseId = element.getAttributes().get("case_id");

        logger.info("still_alive: " + stillAlive);
        logger.info("gaveBirth: " + gaveBirth);
        logger.info("createReferral: " + createReferral);
        logger.info("referralId: " + referralId);
        logger.info("dob: " + dob);
        logger.info("daysSinceBirth: " + days);
        logger.info("attendedPostnatal: " + attendedPostnatal);
        logger.info("placeOfBirth: " + placeOfBirth);
        logger.info("Mother Case Id: " + motherCaseId);
    }
}
