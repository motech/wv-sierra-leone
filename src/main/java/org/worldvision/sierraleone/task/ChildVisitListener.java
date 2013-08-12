package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.EventKeys;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChildVisitListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MessageCampaignService messageCampaignService;

    @Autowired
    MotechSchedulerService schedulerService;

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

        if (null == dateOfBirth) {
            logger.error("Event: " + event + " did not provide a valid " + EventKeys.DATE_OF_BIRTH);
            return;
        }

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

    @MotechListener(subjects = EventKeys.CHILD_VISIT_FORM_SUBJECT)
    public void childMissedVisitHandler(MotechEvent event) {
        logger.info("MotechEvent " + event + " received on " + EventKeys.CHILD_VISIT_FORM_SUBJECT + " Rule: Consecutive Missed Infant Visits");

        /*
          If CHW has missed 2 consecutive infant visits for the same patient, send SMS to supervisor
        */
        String childCaseId = EventKeys.getStringValue(event, EventKeys.CHILD_CASE_ID);
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        List<DateTime> dates = new ArrayList<DateTime>();

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

        String baseSubject = EventKeys.CONSECUTIVE_CHILD_VISIT_BASE_SUBJECT + childCaseId;

        // First we delete any scheduled events for child visit checks for this child
        schedulerService.safeUnscheduleAllJobs(baseSubject);

        for (DateTime visitDate : dates) {
            String subject = baseSubject + "." + visitDate.toString();

            // if the date is in the future schedule an event to fire the next day so we can see if the visit has happened
            if (visitDate.isAfterNow()) {
                MotechEvent e = new MotechEvent(subject);
                e.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
                e.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

                schedulerService.scheduleRunOnceJob(new RunOnceSchedulableJob(e, visitDate.plusDays(1).toDate()));
            }
        }
    }
}