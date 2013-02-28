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
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.EventKeys;

@Component
public class ChildVisitListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareFormService commcareFormService;

    @Autowired
    MessageCampaignService messageCampaignService;

    @MotechListener(subjects = EventKeys.CHILD_VISIT_FORM_SUBJECT)
    public void childVitaminAReminder(MotechEvent event) {
        logger.info("MotechEvent " + event + " received on " + EventKeys.CHILD_VISIT_FORM_SUBJECT + " Rule: Child Vitamin A");

        /*
        Rule 6:
	     IF Child has missed vitamin A dose, send a message to mother reminding her
	     (NN: I looked back at this SMS again, and what it is saying is to send an SMS to the mother.  This SMS should
	     go out once per month until the field "has received it in last 6 months" is changed to yes.  I'll be
	     seperately alerting the CHW in the app to go check if this has been done, so the CHW will mark that the
	     vitamin A dose has been received.  So you would filter on: DOB > 6 months ago AND vitamin_A_received = 'No'
	     then just send that once monthly until it changes to yes.)
	     (RL: is there an upper limit to the number of reminders to send?)
        */
        String childCaseId = EventKeys.getStringValue(event, EventKeys.CHILD_CASE_ID);
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);
        String vitaminA = EventKeys.getStringValue(event, EventKeys.VITAMIN_A);

        DateTime dateOfBirth = null;
        try {
            dateOfBirth = (DateTime) event.getParameters().get(EventKeys.DATE_OF_BIRTH);
        } catch (ClassCastException e) {
            logger.warn("Event: " + event + " Key: " + EventKeys.DATE_OF_BIRTH + " is not a DateTime");
        }

        logger.info("Child Case Id: " + childCaseId);
        logger.info("Mother Case Id: " + motherCaseId);
        logger.info("dateOfBirth: " + dateOfBirth);
        logger.info("vitaminA: " + vitaminA);

        if ("no".equals(vitaminA)) {
            // Enroll mother in message campaign reminding her to get vitamin a for her child
            String externalId = childCaseId + ":" + motherCaseId;
            CampaignRequest cr = new CampaignRequest(externalId,
                    Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN,
                    dateOfBirth.toLocalDate(),
                    null, null);

            messageCampaignService.startFor(cr);
        }
    }
}