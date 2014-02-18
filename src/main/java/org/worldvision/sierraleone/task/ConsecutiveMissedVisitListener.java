package org.worldvision.sierraleone.task;


import org.joda.time.DateTime;
import org.motechproject.cmslite.api.model.ContentNotFoundException;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareUser;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareUserService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.Utils;
import org.worldvision.sierraleone.WorldVisionSettings;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ConsecutiveMissedVisitListener {
    public static final String CHILD_VISITS_MESSAGE_NAME = "MissedConsecutiveChildVisits";
    public static final String POST_PARTUM_VISITS_MESSAGE_NAME = "MissedConsecutivePostPartumVisits";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsecutiveMissedVisitListener.class);

    private CommcareCaseService commcareCaseService;
    private CommcareUserService commcareUserService;
    private FixtureIdMap fixtureIdMap;
    private SmsService smsService;
    private CMSLiteService cmsLiteService;
    private WorldVisionSettings settings;

    @Autowired
    public ConsecutiveMissedVisitListener(CommcareCaseService commcareCaseService,
                                          CommcareUserService commcareUserService,
                                          FixtureIdMap fixtureIdMap, SmsService smsService,
                                          CMSLiteService cmsLiteService,
                                          WorldVisionSettings settings) {
        this.commcareCaseService = commcareCaseService;
        this.commcareUserService = commcareUserService;
        this.fixtureIdMap = fixtureIdMap;
        this.smsService = smsService;
        this.cmsLiteService = cmsLiteService;
        this.settings = settings;
    }

    @MotechListener(subjects = EventKeys.CONSECUTIVE_CHILD_VISIT_WILDCARD_SUBJECT)
    public void childMissedVisitHandler(MotechEvent event) throws ContentNotFoundException {
        LOGGER.debug(String.format("MotechEvent received on %s", EventKeys.CONSECUTIVE_CHILD_VISIT_WILDCARD_SUBJECT));
        String childCaseId = EventKeys.getStringValue(event, EventKeys.CHILD_CASE_ID);
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        CaseInfo childCase = commcareCaseService.getCaseByCaseId(childCaseId);
        if (null == childCase) {
            LOGGER.error(String.format("Unable to load childCase %s from commcare not sending missed consecutive child visit", childCaseId));
            return;
        }

        CaseInfo motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);
        if (null == motherCase) {
            LOGGER.error(String.format("Unable to load mothercase %s from commcare not sending missed consecutive child visit", motherCaseId));
            return;
        }

        String phuId = motherCase.getFieldValues().get(Commcare.PHU_ID);
        if (null == phuId) {
            LOGGER.error(String.format("mothercase %s does not contain a phu not sending missed consecutive child visit", motherCaseId));
            return;
        }

        // Get last visit date
        DateTime lastVisitDate = getDateField(childCase, Commcare.DATE_OF_VISIT);

        // Place all expected visits dates in array
        List<DateTime> dates = new ArrayList<>();
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_5A_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_5B_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_5C_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_5D_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_6_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_7_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_8_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_9_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_10_DATE));
        dates.add(getDateField(childCase, Commcare.CHILD_VISIT_11_DATE));

        if (hasMissedConsecutiveAppointments(dates, lastVisitDate)) {
            // Get PHU phone
            String phone = fixtureIdMap.getPhoneForFixture(phuId);

            // Send SMS
            if (null != phone) {
                CommcareUser commcareUser = commcareUserService.getCommcareUserById(motherCase.getUserId());
                String chwName = commcareUser.getFirstName() + " " + commcareUser.getLastName();

                String message = String.format(getMessage(CHILD_VISITS_MESSAGE_NAME), chwName);
                smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
                LOGGER.info(String.format("Sending missed consecutive child visit SMS to %s at %s for mothercase: %s childcase: %s", phuId, phone, motherCaseId, childCaseId));
            } else {
                LOGGER.error(String.format("No phone for phu: %s  for mothercase: %s childcase: %s not sending missed consecutive child visit", phuId, motherCaseId, childCaseId));
            }
        }
    }

    @MotechListener(subjects = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_WILDCARD_SUBJECT)
    public void postPartumMissedVisitHandler(MotechEvent event) throws ContentNotFoundException {
        // The only time this will fire is if two consecutive apts have been missed
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        CaseInfo motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);
        if (null == motherCase) {
            LOGGER.error("Unable to load mothercase " + motherCaseId + " from commcare can't send missed consecutive post partum message");
            return;
        }

        String phuId = motherCase.getFieldValues().get(Commcare.PHU_ID);
        if (null == phuId) {
            LOGGER.error("mothercase " + motherCaseId + " does not contain a phu can't send missed consecutive post partum message");
            return;
        }

        String phone = fixtureIdMap.getPhoneForFixture(phuId);

        // Send SMS
        if (null != phone) {
            CommcareUser commcareUser = commcareUserService.getCommcareUserById(motherCase.getUserId());
            String chwName = commcareUser.getFirstName() + " " + commcareUser.getLastName();

            String message = String.format(getMessage(POST_PARTUM_VISITS_MESSAGE_NAME), chwName);
            smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
            LOGGER.info("Sending missed consecutive child visit SMS to " + phuId + " at " + phone + " for mothercase: " + motherCaseId);
        } else {
            LOGGER.error("No phone for phu: " + phuId + "  for mothercase: " + motherCaseId + " not sending missed consecutive post partum visit");
        }
    }

    private boolean hasMissedConsecutiveAppointments(List<DateTime> aptDates, DateTime lastVisitDate) {
        int missedApt = 0;

        for (DateTime aptDate : aptDates) {
            // If the apt is after the last visit but before now then it was missed
            if (null != aptDate && aptDate.isAfter(lastVisitDate) && aptDate.isBeforeNow()) {
                missedApt++;
            }
        }

        return (missedApt >= 2);
    }

    private DateTime getDateField(CaseInfo caseInfo, String fieldName) {
        String d = caseInfo.getFieldValues().get(fieldName);
        return Utils.parseDateTime(d);
    }

    private String getMessage(String name) throws ContentNotFoundException {
        return cmsLiteService.getStringContent(settings.getLanguage(), name).getValue();
    }
}
