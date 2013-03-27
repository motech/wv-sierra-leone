package org.worldvision.sierraleone.task;


import org.joda.time.DateTime;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.Utils;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;
import org.worldvision.sierraleone.constants.SMSContent;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.Arrays;
import java.util.List;

@Component
public class ConsecutiveMissedVisitListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareCaseService commcareCaseService;

    @Autowired
    FixtureIdMap fixtureIdMap;

    @Autowired
    SmsService smsService;

    @MotechListener(subjects = EventKeys.CONSECUTIVE_CHILD_VISIT_WILDCARD_SUBJECT)
    public void childMissedVisitHandler(MotechEvent event) {
        List<DateTime> dates = null;
        DateTime lastVisitDate = null;

        String childCaseId = EventKeys.getStringValue(event, EventKeys.CHILD_CASE_ID);
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        CaseInfo childCase = commcareCaseService.getCaseByCaseId(childCaseId);
        if (null == childCase) {
            logger.error("Unable to load childCase " + childCaseId + " from commcare");
            return;
        }

        CaseInfo motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);
        if (null == motherCase) {
            logger.error("Unable to load mothercase " + motherCaseId + " from commcare");
            return;
        }

        String phuId = motherCase.getFieldValues().get(Commcare.PHU_ID);
        if (null == phuId) {
            logger.error("mothercase " + motherCaseId + " does not contain a phu");
            return;
        }

        // Get last visit date
        lastVisitDate = getDateField(childCase, Commcare.DATE_OF_VISIT);

        // Place all expected visits dates in array
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
            if (null == phone) {
                logger.error("No phone for phu " + phuId + " fixture not sending missed consecutive child visit message");
                return;
            }

            // Send SMS
            if (null != phone) {
                // TODO: Handle using CHWs name
                String message = SMSContent.MISSED_CONSECUTIVE_CHILD_VISITS;
                smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
                logger.info("Sending missed consecutive child visit SMS to " + phuId + " at " + phone + " for mothercase: " + motherCaseId + " childcase: " + childCaseId);
            } else {
                logger.error("No phone for phu: " + phuId + "  for mothercase: " + motherCaseId + " childcase: " + childCaseId + " not sending missed consecutive child visit");
            }
        }
    }

    @MotechListener(subjects = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_WILDCARD_SUBJECT)
    public void postPartumMissedVisitHandler(MotechEvent event) {
        List<DateTime> dates = null;
        DateTime lastVisitDate = null;

        // Get last visit date

        // Place all expected visits dates in array

        if (hasMissedConsecutiveAppointments(dates, lastVisitDate)) {
            // Get PHU phone

            // Send SMS
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
        DateTime dateTime = Utils.dateTimeFromCommcareDateString(d);

        return dateTime;
    }
}