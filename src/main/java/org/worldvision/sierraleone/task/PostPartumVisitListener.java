package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.constants.EventKeys;

@Component
public class PostPartumVisitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostPartumVisitListener.class);

    private CommcareCaseService commcareCaseService;
    private MotechSchedulerService schedulerService;

    @Autowired
    public PostPartumVisitListener(CommcareCaseService commcareCaseService,
                                   MotechSchedulerService schedulerService) {
        this.commcareCaseService = commcareCaseService;
        this.schedulerService = schedulerService;
    }

    // It should delete any scehduled events and reschedule new ones that fire on the pp_dates to check if two
    // consecutive have been missed

    @MotechListener(subjects = EventKeys.POST_PARTUM_FORM_SUBJECT)
    public void consecutiveMissedVisits(MotechEvent event) {
        LOGGER.info("MotechEvent " + event + " received on " + EventKeys.POST_PARTUM_FORM_SUBJECT + " Rule: Consecutive Missed Post Partum Visits");

        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        CaseInfo motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);
        if (null == motherCase) {
            LOGGER.error("Unable to load mothercase " + motherCaseId + " from commcare");
            return;
        }

        DateTime secondConsecutiveAptDate = null;
        try {
            secondConsecutiveAptDate = (DateTime) event.getParameters().get(EventKeys.SECOND_CONSECUTIVE_POST_PARTUM_VISIT_DATE);
        } catch (ClassCastException e) {
            LOGGER.error("Event: " + event + " Key: " + EventKeys.SECOND_CONSECUTIVE_POST_PARTUM_VISIT_DATE + " is not a DateTime");
            return;
        }

        LOGGER.info("Second Consecutive Apt Date: " + secondConsecutiveAptDate.toString());

        String baseSubject = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT + motherCaseId;

        // First we delete any scheduled events for child visit checks for this child
        schedulerService.safeUnscheduleAllJobs(baseSubject);

        MotechEvent e = new MotechEvent(baseSubject);
        e.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

        schedulerService.scheduleRunOnceJob(new RunOnceSchedulableJob(e, secondConsecutiveAptDate.plusDays(1).toDate()));
    }

}
