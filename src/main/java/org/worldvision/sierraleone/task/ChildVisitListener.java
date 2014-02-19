package org.worldvision.sierraleone.task;

import org.apache.commons.lang.math.NumberUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.motechproject.cmslite.api.model.ContentNotFoundException;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;

import java.util.LinkedList;
import java.util.List;

import static org.worldvision.sierraleone.constants.Commcare.BEDNET;
import static org.worldvision.sierraleone.constants.Commcare.BIRTH_CERTIFICATE;
import static org.worldvision.sierraleone.constants.Commcare.CASE_BIRTH_CERTIFICATE;
import static org.worldvision.sierraleone.constants.Commcare.CASE_VITA_DATE;
import static org.worldvision.sierraleone.constants.Commcare.COMP_BREASTFEEDING;
import static org.worldvision.sierraleone.constants.Commcare.DEWORM;
import static org.worldvision.sierraleone.constants.Commcare.EATS_3_TIMES_A_DAY;
import static org.worldvision.sierraleone.constants.Commcare.HIGH_RISK;
import static org.worldvision.sierraleone.constants.Commcare.IRON_RICH_FOODS;
import static org.worldvision.sierraleone.constants.Commcare.START_DATE;
import static org.worldvision.sierraleone.constants.Commcare.VITA;
import static org.worldvision.sierraleone.constants.EventKeys.DATE_OF_BIRTH;

@Component
public class ChildVisitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostPartumVisitListener.class);
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final Integer MAX_VISIT_COUNT = 25;

    private MotechSchedulerService schedulerService;

    @Autowired
    public ChildVisitListener(MotechSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @MotechListener(subjects = EventKeys.CHILD_VISIT_FORM_SUBJECT)
    public void childMissedVisitHandler(MotechEvent event) throws ContentNotFoundException {
        LOGGER.debug(String.format("MotechEvent received on %s", EventKeys.CHILD_VISIT_FORM_SUBJECT));

        String childCaseId = EventKeys.getStringValue(event, EventKeys.CHILD_CASE_ID);
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);
        List<DateTime> dates = calculateVisits(event);

        String baseSubject = EventKeys.CONSECUTIVE_CHILD_VISIT_BASE_SUBJECT + childCaseId;

        schedulerService.safeUnscheduleAllJobs(baseSubject);

        for (DateTime date : dates) {
            String subject = baseSubject + "." + date.toString();

            // if the date is in the future schedule an event to fire the next day so we can see if the visit has happened
            if (date.isAfterNow()) {
                MotechEvent e = new MotechEventBuilder()
                        .withSubject(subject)
                        .withParameter(EventKeys.CHILD_CASE_ID, childCaseId)
                        .withParameter(EventKeys.MOTHER_CASE_ID, motherCaseId)
                        .withParameter(EventKeys.CHILD_VISIT_DATES, dates)
                        .build();

                RunOnceSchedulableJob job = new RunOnceSchedulableJob(e, date.plusDays(1).toDate());
                schedulerService.scheduleRunOnceJob(job);
            }
        }
    }

    private List<DateTime> calculateVisits(MotechEvent event) {
        DateTime startDate = EventKeys.getDateTimeValue(event, Commcare.START_DATE);
        LinkedList<DateTime> dates = new LinkedList<>();

        for (int idx = 0; idx < MAX_VISIT_COUNT; ++idx) {
            DateTime previousVist = 0 == idx ? startDate : dates.getLast();
            DateTime nextVisit = null;

            if (null != previousVist) {
                nextVisit = calculateNextVisit(previousVist, event);

                if (null != nextVisit) {
                    dates.add(nextVisit);
                }
            }

            if (null == previousVist || null == nextVisit) {
                break;
            }
        }

        return dates;
    }

    private DateTime calculateNextVisit(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        DateTime datePlus700Days = dob.plusDays(700);

        boolean greater = startTime.getMillis() >= datePlus700Days.getMillis();

        DateTime date;

        if (greater) {
            date = null;
        } else {
            DateTime nextVisitNormal = calculateNextVisitNormal(startTime, event);
            DateTime nextVisitBirthCert = calculateNextVisitBirthCert(startTime, event);
            DateTime nextVisitFeed = calculateNextVisitFeed(startTime, event);
            DateTime nextVisitBednet = calculateNextVisitBednet(startTime, event);
            DateTime nextVisitHighRisk = calculateNextVisitHighRisk(startTime, event);
            DateTime nextVisitVita = calculateNextVisitVita(startTime, event);
            DateTime nextVisitDeworm = calculateNextVisitDeworm(startTime, event);

            long[] array = {
                    nextVisitNormal.getMillis(), nextVisitBirthCert.getMillis(),
                    nextVisitFeed.getMillis(), nextVisitBednet.getMillis(),
                    nextVisitHighRisk.getMillis(), nextVisitVita.getMillis(),
                    nextVisitDeworm.getMillis()
            };

            long min = NumberUtils.min(array);
            date = new DateTime(min);
        }

        return date;
    }

    private DateTime calculateNextVisitNormal(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);

        Days daysBetween = startTime.isBefore(dob)
                ? Days.daysBetween(startTime, dob)
                : Days.daysBetween(dob, startTime);

        int days = daysBetween.getDays();
        DateTime date;

        if (days < 92) {
            date = dob.plusDays(112).minusDays(7);
        } else if (days < 153) {
            date = dob.plusDays(183).minusDays(7);
        } else if (days < 245) {
            date = dob.plusDays(275).minusDays(7);
        } else if (days < 335) {
            date = dob.plusDays(365).minusDays(7);
        } else if (days < 518) {
            date = dob.plusDays(548).minusDays(7);
        } else {
            date = dob.plusDays(730).minusDays(7);
        }

        return date;
    }

    private DateTime calculateNextVisitBirthCert(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        String caseBirthCertificate = EventKeys.getStringValue(event, CASE_BIRTH_CERTIFICATE);
        String birthCertificate = EventKeys.getStringValue(event, BIRTH_CERTIFICATE);

        DateTime datePlus153Days = dob.plusDays(153);
        DateTime datePlus305Days = dob.plusDays(305);

        boolean greater = startTime.getMillis() >= datePlus153Days.getMillis();
        boolean smaller = startTime.getMillis() <= datePlus305Days.getMillis();
        boolean isNotYesCaseBirthCertificate = !YES.equalsIgnoreCase(caseBirthCertificate);
        boolean isNotYesBirthCertificate = !YES.equalsIgnoreCase(birthCertificate);

        return greater && smaller && isNotYesCaseBirthCertificate && isNotYesBirthCertificate
                ? startTime.plusDays(30)
                : dob.plusDays(731);
    }

    private DateTime calculateNextVisitFeed(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        String compBreastfeeding = EventKeys.getStringValue(event, COMP_BREASTFEEDING);
        String eats3TimesADay = EventKeys.getStringValue(event, EATS_3_TIMES_A_DAY);
        String ironRichFoods = EventKeys.getStringValue(event, IRON_RICH_FOODS);

        DateTime datePlus153Days = dob.plusDays(153);

        boolean greater = startTime.getMillis() >= datePlus153Days.getMillis();
        boolean isNoCompBreastfeeding = NO.equalsIgnoreCase(compBreastfeeding);
        boolean isNoEats3TimesADay = NO.equalsIgnoreCase(eats3TimesADay);
        boolean isNoIronRichFoods = NO.equalsIgnoreCase(ironRichFoods);

        return greater && (isNoCompBreastfeeding || isNoEats3TimesADay || isNoIronRichFoods)
                ? startTime.plusDays(30)
                : dob.plusDays(731);
    }

    private DateTime calculateNextVisitBednet(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        String bednet = EventKeys.getStringValue(event, BEDNET);

        boolean isNo = NO.equalsIgnoreCase(bednet);

        return isNo ? startTime.plusDays(30) : dob.plusDays(731);
    }

    private DateTime calculateNextVisitHighRisk(DateTime startTime, MotechEvent event) {
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        String highRisk = EventKeys.getStringValue(event, HIGH_RISK);

        boolean isHighRisk = YES.equalsIgnoreCase(highRisk);

        return isHighRisk ? startTime.plusDays(30) : dob.plusDays(731);
    }

    private DateTime calculateNextVisitVita(DateTime startTime, MotechEvent event) {
        DateTime caseVitaDate = EventKeys.getDateTimeValue(event, CASE_VITA_DATE);
        DateTime datePlus183Days = null == caseVitaDate ? null : caseVitaDate.plusDays(183);
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        String vita = EventKeys.getStringValue(event, VITA);

        boolean greater = null != caseVitaDate
                && startTime.getMillis() > datePlus183Days.getMillis();
        boolean isNotYes = !YES.equalsIgnoreCase(vita);

        return (null == caseVitaDate || greater) && isNotYes
                ? startTime.plusDays(30)
                : dob.plusDays(731);
    }

    private DateTime calculateNextVisitDeworm(DateTime startTime, MotechEvent event) {
        DateTime caseDewormDate = EventKeys.getDateTimeValue(event, START_DATE);
        DateTime datePlus183Days = null == caseDewormDate ? null : caseDewormDate.plusDays(183);
        DateTime dob = EventKeys.getDateTimeValue(event, DATE_OF_BIRTH);
        String deworm = EventKeys.getStringValue(event, DEWORM);

        boolean greater = null != caseDewormDate
                && startTime.getMillis() > datePlus183Days.getMillis();
        boolean isNotYes = !YES.equalsIgnoreCase(deworm);

        return (null == caseDewormDate || greater) && isNotYes
                ? startTime.plusDays(30)
                : dob.plusDays(731);
    }

}
