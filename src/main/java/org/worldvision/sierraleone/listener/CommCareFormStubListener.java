package org.worldvision.sierraleone.listener;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.event.listener.annotations.MotechListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CommCareFormStubListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareFormService commcareFormService;

    @Autowired
    CommcareCaseService commcareCaseService;

    @Autowired
    EventRelay eventRelay;

    // Todo: Verify all events are only published if all fields are included

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

        String formName = form.getForm().getAttributes().get(Commcare.NAME);
        logger.info("form name " + formName);

        List<String> caseIds = (List<String>) event.getParameters().get(EventDataKeys.CASE_IDS);

        // Create MOTECH event, populate subject and params below
        List<MotechEvent> events = new ArrayList<>(0);

        switch (formName) {
            case "Register Child":
                events.addAll(convertRegisterChildFormToEvents(form));
                break;

            case "Post-Partum Visit":
                events.addAll(convertPostPartumFormToEvents(form, caseIds));
                break;

            case "Pregnancy Visit":
                events.addAll(convertPregnancyVisitFormToEvents(form, caseIds));
                break;

            case "Child Visit":
                events.addAll(convertChildVisitFormToEvents(form));
                break;

            default:
                logger.info("Ignoring commcare forwarded form of type: " + formName);
                break;
        }

        // publish event
        for (MotechEvent e : events) {
            if (null != e)
                eventRelay.sendEventMessage(e);
        }

    }

    private List<MotechEvent> convertChildVisitFormToEvents(CommcareForm form) {
        String dob = null;
        String childCaseId = null;
        String motherCaseId = null;
        String vitaminA = null;

        FormValueElement element = form.getForm().getElementByName(Commcare.CASE);
        childCaseId = element.getAttributes().get(Commcare.CASE_ID);

        CaseInfo childCase = commcareCaseService.getCaseByCaseId(childCaseId);
        if (childCase.getIndices().containsKey(Commcare.PARENT)) {
            Map<String, String> parent = childCase.getIndices().get(Commcare.PARENT);

            if (parent.containsKey(Commcare.CASE_TYPE) && "mother".equals(parent.get(Commcare.CASE_TYPE))) {
                motherCaseId = childCase.getIndices().get(Commcare.PARENT).get(Commcare.CASE_ID);
            } else {
                logger.error("Parent of childcase " + childCaseId + " is not a mothercase (" + parent.get(Commcare.CASE_TYPE) + ")");
            }
        } else {
            logger.error("No parent case for childcase: " + childCaseId);
        }

        vitaminA = childCase.getFieldValues().get(Commcare.VITAMIN_A);

        dob = childCase.getFieldValues().get(Commcare.DATE_OF_BIRTH);

        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendLiteral('-')
                .appendMonthOfYear(2)
                .appendLiteral('-')
                .appendDayOfMonth(2)
                .toFormatter();

        DateTime dateOfBirth = dateFormatter.parseDateTime(dob);

        logger.info("Child Case Id: " + childCaseId);
        logger.info("Mother Case Id: " + motherCaseId);
        logger.info("dateOfBirth: " + dateOfBirth);
        logger.info("vitaminA: " + vitaminA);

        List<MotechEvent> ret = new ArrayList<>();
        MotechEvent event = new MotechEvent(EventKeys.CHILD_VISIT_FORM_SUBJECT);

        event.getParameters().put(EventKeys.DATE_OF_BIRTH, dateOfBirth);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.VITAMIN_A, vitaminA);

        ret.add(event);

        return ret;
    }

    private List<MotechEvent> convertPregnancyVisitFormToEvents(CommcareForm form, List<String> caseIds) {
        String dov = null;
        String createReferral = null;
        String referralId = null;
        String motherCaseId = null;

        FormValueElement element = form.getForm().getElementByName(Commcare.CREATE_REFERRAL);
        createReferral = ((element != null) ? element.getValue() : null);

        element = form.getForm().getElementByName(Commcare.REFERRAL_ID);
        referralId = ((element != null) ? element.getValue() : null);

        element = form.getForm().getElementByName(Commcare.CASE);
        motherCaseId = element.getAttributes().get(Commcare.CASE_ID);

        element = form.getForm().getElementByName(Commcare.DATE_OF_VISIT);
        dov = ((element != null) ? element.getValue() : null);

        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendLiteral('-')
                .appendMonthOfYear(2)
                .appendLiteral('-')
                .appendDayOfMonth(2)
                .toFormatter();

        DateTime dateOfVisit = dateFormatter.parseDateTime(dov);

        logger.info("createReferral: " + createReferral);
        logger.info("referralId: " + referralId);
        logger.info("Mother Case Id: " + motherCaseId);
        logger.info("dateOfVisit: " + dateOfVisit);

        List<MotechEvent> ret = new ArrayList<>();
        MotechEvent event = null;

        if ("yes".equals(createReferral)) {
            event = getReferralEvent(motherCaseId, form, caseIds, dateOfVisit);

            ret.add(event);
        }

        return ret;
    }

    // Pulls data out of the commcare form and constructs events used by rule handlers
    private List<MotechEvent> convertPostPartumFormToEvents(CommcareForm form, List<String> caseIds) {
        String dov = null;
        String stillAlive = null;
        String gaveBirth = null;
        String createReferral = null;
        String referralId = null;
        String dob = null;
        String attendedPostnatal = null;
        String placeOfBirth = null;
        String motherCaseId = null;

        FormValueElement element = form.getForm().getElementByName(Commcare.STILL_ALIVE);
        stillAlive = ((element != null) ? element.getValue() : null);

        element = form.getForm().getElementByName(Commcare.GAVE_BIRTH);
        gaveBirth = ((element != null) ? element.getValue() : null);

        element = form.getForm().getElementByName(Commcare.CREATE_REFERRAL);
        createReferral = ((element != null) ? element.getValue() : null);

        element = form.getForm().getElementByName(Commcare.REFERRAL_ID);
        referralId = ((element != null) ? element.getValue() : null);

        element = form.getForm().getElementByName(Commcare.DATE_OF_VISIT);
        dov = ((element != null) ? element.getValue() : null);

        FormValueElement postPartumVisit = form.getForm().getElementByName(Commcare.POST_PARTUM_VISIT);
        element = postPartumVisit.getElementByName(Commcare.DATE_OF_BIRTH);
        dob = ((element != null) ? element.getValue() : null);

        element = postPartumVisit.getElementByName(Commcare.ATTENDED_POSTNATAL);
        attendedPostnatal = ((element != null) ? element.getValue() : null);

        element = postPartumVisit.getElementByName(Commcare.PLACE_OF_BIRTH);
        placeOfBirth = ((element != null) ? element.getValue() : null);

        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendLiteral('-')
                .appendMonthOfYear(2)
                .appendLiteral('-')
                .appendDayOfMonth(2)
                .toFormatter();

        DateTime dateOfVisit = dateFormatter.parseDateTime(dov);
        DateTime dateOfBirth = dateFormatter.parseDateTime(dob);
        int daysSinceBirth = Days.daysBetween(dateOfBirth, new DateTime()).getDays();

        element = form.getForm().getElementByName(Commcare.CASE);
        motherCaseId = element.getAttributes().get(Commcare.CASE_ID);

        logger.info("still_alive: " + stillAlive);
        logger.info("gaveBirth: " + gaveBirth);
        logger.info("createReferral: " + createReferral);
        logger.info("referralId: " + referralId);
        logger.info("dob: " + dob);
        logger.info("daysSinceBirth: " + daysSinceBirth);
        logger.info("attendedPostnatal: " + attendedPostnatal);
        logger.info("placeOfBirth: " + placeOfBirth);
        logger.info("Mother Case Id: " + motherCaseId);

        List<MotechEvent> ret = new ArrayList<>();
        MotechEvent event = new MotechEvent(EventKeys.POST_PARTUM_FORM_SUBJECT);

        event.getParameters().put(EventKeys.MOTHER_ALIVE, stillAlive);
        event.getParameters().put(EventKeys.GAVE_BIRTH, gaveBirth);
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, dateOfBirth);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, dateOfVisit);
        event.getParameters().put(EventKeys.DAYS_SINCE_BIRTH, daysSinceBirth);
        event.getParameters().put(EventKeys.ATTENDED_POSTNATAL, attendedPostnatal);
        event.getParameters().put(EventKeys.PLACE_OF_BIRTH, placeOfBirth);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

        ret.add(event);

        if ("yes".equals(createReferral)) {
            event = getReferralEvent(motherCaseId, form, caseIds, dateOfVisit);

            ret.add(event);
        }

        return ret;
    }

    // The commcare stub form event contains a list of the cases affected by the form.  This method iterates
    // over that list finding the one that is the case.  This requires loading the case from commcare, so
    // we filter out the mother case so we can eliminate one remote call
    private MotechEvent getReferralEvent(String motherCaseId, CommcareForm form, List<String> caseIds, DateTime dateOfVisit) {
        String referralId = null;

        for (String caseId: caseIds) {
            if (!motherCaseId.equals(caseId)) {
                CaseInfo caseInfo = commcareCaseService.getCaseByCaseId(caseId);

                if ("referral".equals(caseInfo.getFieldValues().get(Commcare.CASE_TYPE))) {
                    referralId = caseId;
                }
            }
        }

        MotechEvent event;
        event = new MotechEvent(EventKeys.MOTHER_REFERRAL_SUBJECT);

        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.REFERRAL_CASE_ID, referralId);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, dateOfVisit);

        return event;
    }

    private List<MotechEvent> convertRegisterChildFormToEvents(CommcareForm form) {
        FormValueElement registration = form.getForm().getElementByName(Commcare.REGISTRATION);
        FormValueElement dob = registration.getElementByName(Commcare.DATE_OF_BIRTH);

        FormValueElement motherAlive = form.getForm().getElementByName(Commcare.MOTHER_ALIVE);

        logger.info("dob: " + dob.getValue());
        logger.info("mother_alive: " + motherAlive.getValue());

        String childCaseId = null;
        String motherCaseId = null;

        FormValueElement subcase_0 = form.getForm().getElementByName(Commcare.SUBCASE);
        FormValueElement _case = subcase_0.getElementByName(Commcare.CASE);

        if (null != _case) {
            childCaseId = _case.getAttributes().get(Commcare.CASE_ID);

            try {
                FormValueElement parentElement = _case.getElementByName(Commcare.INDEX).getElementByName(Commcare.PARENT);

                if ("mother".equals(parentElement.getAttributes().get(Commcare.CASE_TYPE))) {
                    motherCaseId = parentElement.getValue();
                } else {
                    logger.debug("case_type(" + parentElement.getAttributes().get(Commcare.CASE_TYPE) + ") != 'mother'");
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

        return null;
    }
}
