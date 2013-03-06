package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareFixtureService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.motechproject.server.messagecampaign.contract.CampaignRequest;
import org.motechproject.server.messagecampaign.service.MessageCampaignService;
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;
import org.worldvision.sierraleone.constants.SMSContent;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PostPartumVisitListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareCaseService commcareCaseService;

    @Autowired
    CommcareFixtureService commcareFixtureService;

    @Autowired
    FixtureIdMap fixtureIdMap;

    @Autowired
    MessageCampaignService messageCampaignService;

    @Autowired
    SmsService smsService;

    @Autowired
    MotechSchedulerService schedulerService;

    // It should delete any scehduled events and reschedule new ones that fire on the pp_dates to check if two
    // consecutive have been missed

    @MotechListener(subjects = EventKeys.POST_PARTUM_FORM_SUBJECT)
    public void postnatalConsultationAttendance(MotechEvent event) {
        logger.info("MotechEvent " + event + " received on " + EventKeys.POST_PARTUM_FORM_SUBJECT + " Rule: Postnatal Consultation");

        /*
        Rule 1:
        IF patient has delivered AND patient has not attended postnatal consultation at health center THEN send SMS to
        patient every week until 45 days after delivery.
        */
        String gaveBirth = EventKeys.getStringValue(event, EventKeys.GAVE_BIRTH);
        String attendedPostnatal = EventKeys.getStringValue(event, EventKeys.ATTENDED_POSTNATAL);
        Integer daysSinceBirth = EventKeys.getIntegerValue(event, EventKeys.DAYS_SINCE_BIRTH);
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        DateTime dateOfBirth = null;
        try {
            dateOfBirth = (DateTime) event.getParameters().get(EventKeys.DATE_OF_BIRTH);
        } catch (ClassCastException e) {
            logger.warn("Event: " + event + " Key: " + EventKeys.DATE_OF_BIRTH + " is not a DateTime");
        }

        logger.info("Date Of Birth: " + dateOfBirth.toString());

        if ("yes".equals(gaveBirth) && "no".equals(attendedPostnatal) &&
                new Integer(45).compareTo(daysSinceBirth) > 0) {
            // Enroll mother in message campaign reminding her to attend postnatal consultation

            CampaignRequest cr = new CampaignRequest(motherCaseId,
                                                     Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN,
                                                     dateOfBirth.toLocalDate(),
                                                     null, null);
            messageCampaignService.startFor(cr);
        }
    }

    @MotechListener(subjects = EventKeys.POST_PARTUM_FORM_SUBJECT)
    public void homeBirthNotification(MotechEvent event) {
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
            // TODO Handle CHW name
            String message = SMSContent.HOME_BIRTH_NOTIFICATION;
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

        List<DateTime> dates = new ArrayList<DateTime>();

        // TODO get actual dates from mother case
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_5A_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_5B_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_5C_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_5D_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_6_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_7_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_8_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_9_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_10_DATE));
        dates.add((DateTime) event.getParameters().get(EventKeys.CHILD_VISIT_11_DATE));

        String baseSubject = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT + motherCaseId;

        // First we delete any scheduled events for child visit checks for this child
        schedulerService.safeUnscheduleAllJobs(baseSubject);

        for (DateTime visitDate : dates) {
            String subject = baseSubject + "." + visitDate.toString();

            // if the date is in the future schedule an event to fire the next day so we can see if the visit has happened
            if (visitDate.isAfterNow()) {
                MotechEvent e = new MotechEvent(subject);
                e.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

                schedulerService.scheduleRunOnceJob(new RunOnceSchedulableJob(e, visitDate.plusDays(1).toDate()));
            }

        }
    }
}