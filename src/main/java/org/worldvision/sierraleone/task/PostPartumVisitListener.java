package org.worldvision.sierraleone.task;

import org.apache.commons.lang.math.NumberUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
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
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.constants.EventKeys;

import java.util.Date;

import static org.worldvision.sierraleone.constants.Commcare.ATTENDED_PNC;
import static org.worldvision.sierraleone.constants.Commcare.BCG;
import static org.worldvision.sierraleone.constants.Commcare.CASE_ATTENDED_PNC;
import static org.worldvision.sierraleone.constants.Commcare.CASE_BCG;
import static org.worldvision.sierraleone.constants.Commcare.CASE_NEXT_VISIT;
import static org.worldvision.sierraleone.constants.Commcare.CASE_OPV_0;
import static org.worldvision.sierraleone.constants.Commcare.HIGH_RISK;
import static org.worldvision.sierraleone.constants.Commcare.MOTHER_ALIVE;
import static org.worldvision.sierraleone.constants.Commcare.OPV_0;
import static org.worldvision.sierraleone.constants.Commcare.VISIT_SUCCESS;
import static org.worldvision.sierraleone.constants.EventKeys.DATE_OF_BIRTH;

@Component
public class PostPartumVisitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostPartumVisitListener.class);
    private static final String YES = "yes";

    private CommcareCaseService commcareCaseService;
    private MotechSchedulerService schedulerService;

    @Autowired
    public PostPartumVisitListener(CommcareCaseService commcareCaseService,
                                   MotechSchedulerService schedulerService) {
        this.commcareCaseService = commcareCaseService;
        this.schedulerService = schedulerService;
    }

    // It should delete any scehduled events and reschedule new ones that fire on the pp_dates to
    // check if two consecutive have been missed

    @MotechListener(subjects = EventKeys.POST_PARTUM_FORM_SUBJECT)
    public void consecutiveMissedVisits(MotechEvent event) {
        LOGGER.debug(String.format("MotechEvent received on %s", EventKeys.POST_PARTUM_FORM_SUBJECT));

        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);

        CaseInfo motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);
        if (null == motherCase) {
            LOGGER.error("Unable to load mothercase " + motherCaseId + " from commcare");
            return;
        }

        DateTime nextVisit = calculateNextVisit(DateTime.now(), event);
        DateTime secondNextVisit = calculateNextVisit(nextVisit, event);

        LOGGER.info("Second Consecutive Apt Date: " + secondNextVisit);

        String baseSubject = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT + motherCaseId;

        // First we delete any scheduled events for child visit checks for this child
        schedulerService.safeUnscheduleAllJobs(baseSubject);

        MotechEvent e = new MotechEventBuilder()
                .withSubject(baseSubject)
                .withParameter(EventKeys.MOTHER_CASE_ID, motherCaseId)
                .build();

        Date startDate = secondNextVisit.plusDays(1).toDate();

        RunOnceSchedulableJob job = new RunOnceSchedulableJob(e, startDate);
        schedulerService.scheduleRunOnceJob(job);
    }

    private DateTime calculateNextVisit(DateTime startTime, MotechEvent event) {
        String visitSuccess = EventKeys.getStringValue(event, VISIT_SUCCESS);
        DateTime caseNextVisit = EventKeys.getDateTimeValue(event, CASE_NEXT_VISIT);

        DateTime nextVisitNormal = calculateNextVisitNormal(startTime, event);
        DateTime nextVisitFirstVaccines = calculateNextVisitFirstVaccines(startTime, event);
        DateTime nextVisitHighRisk = calculateNextVisitHighRisk(startTime, event);
        DateTime nextVisitAttendPnc = calculateNextVisitAttendPnc(startTime, event);

        long[] array = {
                nextVisitNormal.getMillis(), nextVisitFirstVaccines.getMillis(),
                nextVisitHighRisk.getMillis(), nextVisitAttendPnc.getMillis()
        };

        long min = NumberUtils.min(array);

        return YES.equalsIgnoreCase(visitSuccess)
                ? new DateTime(min)
                : caseNextVisit;
    }

    private DateTime calculateNextVisitNormal(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);

        Days daysBetween = startTime.isBefore(dob)
                ? Days.daysBetween(startTime, dob)
                : Days.daysBetween(dob, startTime);

        int days = daysBetween.getDays();

        DateTime date;

        if (days < 0) {
            date = dob.plusDays(1);
        } else if (days < 2) {
            date = dob.plusDays(3).minusDays(1);
        } else if (days < 5) {
            date = dob.plusDays(7).minusDays(1);
        } else if (days < 21) {
            date = dob.plusDays(28).minusDays(3);
        } else {
            date = dob.plusDays(112).minusDays(7);
        }

        return date;
    }

    private DateTime calculateNextVisitFirstVaccines(DateTime startTime, MotechEvent event) {
        String caseOpv0 = EventKeys.getStringValue(event, CASE_OPV_0);
        String caseBcg = EventKeys.getStringValue(event, CASE_BCG);
        String opv0 = EventKeys.getStringValue(event, OPV_0);
        String bcg = EventKeys.getStringValue(event, BCG);
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);

        DateTime datePlus2Weeks = dob.plusDays(14);

        boolean greater = startTime.getMillis() <= datePlus2Weeks.getMillis();
        boolean opv0NotEqualsYes = !YES.equalsIgnoreCase(caseOpv0) && !YES.equalsIgnoreCase(opv0);
        boolean bcgNotEqualsYes = !YES.equalsIgnoreCase(caseBcg) && !YES.equalsIgnoreCase(bcg);

        return (opv0NotEqualsYes && greater) || (bcgNotEqualsYes && greater)
                ? startTime.plusDays(7)
                : dob.plusDays(730);
    }

    private DateTime calculateNextVisitHighRisk(DateTime startTime, MotechEvent event) {
        String highRisk = EventKeys.getStringValue(event, HIGH_RISK);
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);

        DateTime datePlus30Days = dob.plusDays(30);

        boolean greater = startTime.getMillis() <= datePlus30Days.getMillis();
        boolean isHighRisk = YES.equalsIgnoreCase(highRisk);

        DateTime date;

        if (isHighRisk && greater) {
            date = startTime.plusDays(3);
        } else if (isHighRisk) {
            date = startTime.plusDays(30);
        } else {
            date = dob.plusDays(730);
        }

        return date;
    }

    private DateTime calculateNextVisitAttendPnc(DateTime startTime, MotechEvent event) {
        String caseAttendedPnc = EventKeys.getStringValue(event, CASE_ATTENDED_PNC);
        String attendedPnc = EventKeys.getStringValue(event, ATTENDED_PNC);
        String motherAlive = EventKeys.getStringValue(event, MOTHER_ALIVE);
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);

        boolean isNotCaseAttendedPnc = !YES.equalsIgnoreCase(caseAttendedPnc);
        boolean isNotAttendedPnc = !YES.equalsIgnoreCase(attendedPnc);
        boolean isMotherAlive = YES.equalsIgnoreCase(motherAlive);

        return isNotCaseAttendedPnc && isNotAttendedPnc && isMotherAlive
                ? startTime.plusDays(7)
                : dob.plusDays(730);
    }
}
