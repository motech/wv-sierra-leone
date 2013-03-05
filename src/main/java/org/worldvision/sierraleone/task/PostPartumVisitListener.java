package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareFixtureService;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
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

import java.util.Arrays;

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
        // TODO:  Will I send this notification multiple times?
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

            // This code could be better.  Basically I try to load from commcare.  If fixture has been updated
            // then it's fixtureId has changed and I'll get null back.  So then I refresh the in memory cache and try
            // to load it again.
            String fixtureId = fixtureIdMap.fixtureIdForPHUId(phuId);
            if (null == fixtureId) {
                logger.error("Unable to get fixtureId for phu " + phuId);
                return;
            }

            CommcareFixture fixture = commcareFixtureService.getCommcareFixtureById(fixtureId);
            if (null == fixture) {
                fixtureIdMap.refreshFixtureMap();

                fixtureId = fixtureIdMap.fixtureIdForPHUId(phuId);
                if (null == fixtureId) {
                    logger.error("Unable to get fixtureId for phu " + phuId);
                    return;
                }

                fixture = commcareFixtureService.getCommcareFixtureById(fixtureId);
            }

            if (null == fixture) {
                logger.error("Unable to load fixture " + fixtureId + " from commcare");
                return;
            }

            String phone = fixture.getFields().get(Commcare.PHONE);
            if (null == phone) {
                logger.error("No phone for phu " + phuId + " fixture " + fixtureId + " not sending home birth notification");
                return;
            }

            // Send SMS
            // TODO Handle CHW name
            String message = SMSContent.HOME_BIRTH_NOTIFICATION;
            smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
            logger.info("Sending home birth notification SMS to " + phone + " for mothercase: " + motherCaseId);
        }
    }
}