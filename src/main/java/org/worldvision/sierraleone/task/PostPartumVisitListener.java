package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.server.messagecampaign.contract.CampaignRequest;
import org.motechproject.server.messagecampaign.service.MessageCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.Constants;
import org.worldvision.sierraleone.EventKeys;

@Component
public class PostPartumVisitListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareFormService commcareFormService;

    @Autowired
    MessageCampaignService messageCampaignService;

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
            String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

            CampaignRequest cr = new CampaignRequest(motherCaseId,
                                                     Constants.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN,
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
            // Look up PHU

            // Send SMS
        }
    }
}