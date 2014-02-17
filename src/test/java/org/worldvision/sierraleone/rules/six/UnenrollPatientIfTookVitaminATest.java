package org.worldvision.sierraleone.rules.six;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commons.api.DataProvider;
import org.motechproject.event.MotechEvent;
import org.motechproject.tasks.domain.ActionEvent;
import org.motechproject.tasks.domain.ActionParameter;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.rules.RuleTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.messagecampaign.EventKeys.CAMPAIGN_NAME_KEY;
import static org.motechproject.messagecampaign.EventKeys.EXTERNAL_ID_KEY;
import static org.motechproject.messagecampaign.EventKeys.UNENROLL_USER_SUBJECT;

public class UnenrollPatientIfTookVitaminATest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "4408354fae5368389825691bc5095421";
    private static final String CASE_ID_VALUE = "caseId";
    private static final String PARENT_CASE_ID_VALUE = "parentCaseId";
    private static final String EXTERNAL_ID_VALUE = String.format("%s:%s", CASE_ID_VALUE, PARENT_CASE_ID_VALUE);

    @Mock
    private DataProvider commcareDataProvider;

    private CaseInfo caseInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);

        setTask(6, "unenroll_if_took_vitamin_a.json", "rule-6-unenroll-if-took-vitamin-a");
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(UNENROLL_USER_SUBJECT);
        actionEvent.addParameter(new ActionParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_KEY), true);
        actionEvent.addParameter(new ActionParameter(CAMPAIGN_NAME_KEY, CAMPAIGN_NAME_KEY), true);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("vitamin_a_mother", "yes");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);
        handler.handle(firedCampaignMessage());

        verify(eventRelay, times(2)).sendEventMessage(captor.capture());

        MotechEvent event = (MotechEvent) CollectionUtils.find(captor.getAllValues(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return o instanceof MotechEvent && UNENROLL_USER_SUBJECT.equalsIgnoreCase(((MotechEvent) o).getSubject());
            }
        });

        assertNotNull(event);

        assertEquals(UNENROLL_USER_SUBJECT, event.getSubject());

        assertEquals(CHILD_VITAMIN_A_REMINDER, event.getParameters().get(CAMPAIGN_NAME_KEY));
        assertEquals(EXTERNAL_ID_VALUE, event.getParameters().get(EXTERNAL_ID_KEY));
    }

    @Test
    public void shouldNotExecuteTaskIfVitaminAWasNotTaken() throws Exception {
        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("vitamin_a_mother", "no");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfCampaignNameIsIncorrect() throws Exception {
        MotechEvent event = firedCampaignMessage();
        event.getParameters().put(CAMPAIGN_NAME_KEY, "something");

        handler.handle(event);
    }

    private MotechEvent firedCampaignMessage() {
        return new MotechEventBuilder()
                .withSubject(getTriggerSubject())
                .withParameter(CAMPAIGN_NAME_KEY, CHILD_VITAMIN_A_REMINDER)
                .withParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE)
                .build();
    }

}
