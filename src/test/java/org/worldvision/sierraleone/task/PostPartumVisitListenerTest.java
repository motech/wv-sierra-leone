package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.event.MotechEvent;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PostPartumVisitListenerTest {
    private static final String MOTHER_CASE_ID_VALUE = "motherCaseId";
    private static final String JOB_EVENT_SUBJECT = EventKeys.CONSECUTIVE_POST_PARTUM_VISIT_BASE_SUBJECT + MOTHER_CASE_ID_VALUE;
    private static final DateTime START_DATE = DateUtil.today().toDateTimeAtStartOfDay();
    private static final DateTime DOB = START_DATE.minusDays(1);

    @Mock
    private CommcareCaseService commcareCaseService;

    @Mock
    private MotechSchedulerService motechSchedulerService;

    @Captor
    private ArgumentCaptor<RunOnceSchedulableJob> captor;

    private PostPartumVisitListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new PostPartumVisitListener(commcareCaseService, motechSchedulerService);
    }

    @Test
    public void shouldNotScheduleJobIfMotherCaseNotFound() throws Exception {
        MotechEvent event = new MotechEventBuilder()
                .withSubject(EventKeys.POST_PARTUM_FORM_SUBJECT)
                .withParameter(EventKeys.MOTHER_CASE_ID, MOTHER_CASE_ID_VALUE)
                .build();

        listener.consecutiveMissedVisits(event);
        verify(commcareCaseService).getCaseByCaseId(MOTHER_CASE_ID_VALUE);
        verifyZeroInteractions(motechSchedulerService);
    }

    @Test
    public void shouldScheduleJob() throws Exception {
        MotechEvent event = new MotechEventBuilder()
                .withSubject(EventKeys.POST_PARTUM_FORM_SUBJECT)
                .withParameter(EventKeys.MOTHER_CASE_ID, MOTHER_CASE_ID_VALUE)
                .withParameter(Commcare.VISIT_SUCCESS, "yes")
                .withParameter(Commcare.CASE_NEXT_VISIT, START_DATE)
                .withParameter(EventKeys.DATE_OF_BIRTH, DOB)
                .withParameter(Commcare.CASE_OPV_0, "yes")
                .withParameter(Commcare.CASE_BCG, "yes")
                .withParameter(Commcare.OPV_0, "yes")
                .withParameter(Commcare.BCG, "yes")
                .withParameter(Commcare.HIGH_RISK, "no")
                .withParameter(Commcare.CASE_ATTENDED_PNC, "yes")
                .withParameter(Commcare.ATTENDED_PNC, "yes")
                .withParameter(Commcare.MOTHER_ALIVE, "yes")
                .build();

        CaseInfo caseInfo = new CaseInfo();
        doReturn(caseInfo).when(commcareCaseService).getCaseByCaseId(MOTHER_CASE_ID_VALUE);

        listener.consecutiveMissedVisits(event);

        verify(commcareCaseService).getCaseByCaseId(MOTHER_CASE_ID_VALUE);
        verify(motechSchedulerService).scheduleRunOnceJob(captor.capture());

        // because nextVisit = DOB.plusDays(3).minusDays(1);
        DateTime secondVisit = DOB.plusDays(7).minusDays(1).plusDays(1);

        RunOnceSchedulableJob job = captor.getValue();
        assertEquals(secondVisit.toDate(), job.getStartDate());

        MotechEvent jobEvent = job.getMotechEvent();
        assertEquals(JOB_EVENT_SUBJECT, jobEvent.getSubject());
        assertEquals(MOTHER_CASE_ID_VALUE, jobEvent.getParameters().get(EventKeys.MOTHER_CASE_ID));
    }
}
