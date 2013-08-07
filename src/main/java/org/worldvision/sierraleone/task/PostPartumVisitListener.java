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
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.WorldVisionSettings;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.Arrays;

import static org.worldvision.sierraleone.constants.SMSContent.HOME_BIRTH_NOTIFICATION;

@Component
public class PostPartumVisitListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CommcareCaseService commcareCaseService;

    @Autowired
    private CommcareUserService commcareUserService;

    @Autowired
    private FixtureIdMap fixtureIdMap;

    @Autowired
    private SmsService smsService;

    @Autowired
    private MotechSchedulerService schedulerService;

    @Autowired
    private CMSLiteService cmsLiteService;

    @Autowired
    private WorldVisionSettings settings;

    // It should delete any scehduled events and reschedule new ones that fire on the pp_dates to check if two
    // consecutive have been missed

    @MotechListener(subjects = EventKeys.POST_PARTUM_FORM_SUBJECT)
    public void homeBirthNotification(MotechEvent event) throws ContentNotFoundException {
        logger.info("MotechEvent " + event + " received on " + EventKeys.POST_PARTUM_FORM_SUBJECT + " Rule: Home Birth Notification");

        /*
        Rule 5:
        If CHW records a home delivery, send SMS reporting the delivery to PHU (health clinic)
        */
        String placeOfBirth = EventKeys.getStringValue(event, EventKeys.PLACE_OF_BIRTH);

        if ("home".equals(placeOfBirth)) {
            String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

            // Look up PHU
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

            String phone = fixtureIdMap.getPhoneForFixture(phuId);
            if (null == phone) {
                logger.error("No phone for phu " + phuId + " fixture not sending home birth notification");
                return;
            }

            // Send SMS
            String motherName = motherCase.getFieldValues().get(Commcare.MOTHER_NAME);
            CommcareUser commcareUser = commcareUserService.getCommcareUserById(motherCase.getUserId());
            String chwName = commcareUser.getFirstName() + " " + commcareUser.getLastName();

            String message = String.format(getMessage(HOME_BIRTH_NOTIFICATION), chwName, motherName);
            smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
            logger.info("Sending home birth notification SMS to " + phone + " for mothercase: " + motherCaseId);
        }
    }

    @MotechListener(subjects = EventKeys.POST_PARTUM_FORM_SUBJECT)
    public void consecutiveMissedVisits(MotechEvent event) {
        logger.info("MotechEvent " + event + " received on " + EventKeys.POST_PARTUM_FORM_SUBJECT + " Rule: Consecutive Missed Post Partum Visits");

        /*
        */
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        CaseInfo motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);
        if (null == motherCase) {
            logger.error("Unable to load mothercase " + motherCaseId + " from commcare");
            return;
        }

        DateTime secondConsecutiveAptDate = null;
        try {
            secondConsecutiveAptDate = (DateTime) event.getParameters().get(EventKeys.SECOND_CONSECUTIVE_POST_PARTUM_VISIT_DATE);
        } catch (ClassCastException e) {
            logger.warn("Event: " + event + " Key: " + EventKeys.SECOND_CONSECUTIVE_POST_PARTUM_VISIT_DATE + " is not a DateTime");
        }

        logger.info("Second Consecutive Apt Date: " + secondConsecutiveAptDate.toString());

        String baseSubject = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT + motherCaseId;

        // First we delete any scheduled events for child visit checks for this child
        schedulerService.safeUnscheduleAllJobs(baseSubject);

        MotechEvent e = new MotechEvent(baseSubject);
        e.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

        schedulerService.scheduleRunOnceJob(new RunOnceSchedulableJob(e, secondConsecutiveAptDate.plusDays(1).toDate()));
    }

    private String getMessage(String name) throws ContentNotFoundException {
        return cmsLiteService.getStringContent(settings.getLanguage(), name).getValue();
    }
}
