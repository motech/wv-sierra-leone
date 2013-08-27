package org.worldvision.sierraleone.rules.six;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
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
import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.motechproject.messagecampaign.EventKeys.CAMPAIGN_NAME_KEY;
import static org.motechproject.messagecampaign.EventKeys.ENROLL_USER_SUBJECT;
import static org.motechproject.messagecampaign.EventKeys.EXTERNAL_ID_KEY;
import static org.motechproject.messagecampaign.EventKeys.REFERENCE_DATE;
import static org.motechproject.tasks.domain.ParameterType.DATE;

public class EnrollPatientTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "4408354fae5368389825691bc5095421";
    private static final String FORM_ID_VALUE = "formId";
    private static final String CASE_ID_VALUE = "caseId";
    private static final String PARENT_CASE_ID_VALUE = "parentCaseId";
    private static final DateTime REFERANCE_DATE_VALUE = newDateTime(2013, 8, 27);

    @Mock
    private DataProvider commcareDataProvider;

    private CommcareForm commcareForm;

    private CaseInfo caseInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 27));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);

        setTask(6, "enroll_patient.json", "rule-6-enroll-patient");
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(ENROLL_USER_SUBJECT);
        actionEvent.addParameter(new ActionParameter(CAMPAIGN_NAME_KEY, CAMPAIGN_NAME_KEY), true);
        actionEvent.addParameter(new ActionParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_KEY), true);
        actionEvent.addParameter(new ActionParameter(REFERENCE_DATE, REFERENCE_DATE, DATE), true);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        Map<String, String> formlookup = new HashMap<>();
        formlookup.put("id", FORM_ID_VALUE);

        FormValueElement caseElement = new FormValueElement();
        caseElement.setElementName("case");
        caseElement.addAttribute("case_id", CASE_ID_VALUE);

        FormValueElement form = new FormValueElement();
        form.addAttribute("name", "Child Visit");
        form.addFormValueElement("case", caseElement);

        commcareForm = new CommcareForm();
        commcareForm.setForm(form);

        Map<String, String> caselookup = new HashMap<>();
        caselookup.put("id", CASE_ID_VALUE);

        Map<String, String> parent = new HashMap<>();
        parent.put("case_id", PARENT_CASE_ID_VALUE);

        Map<String, Map<String, String>> indices = new HashMap<>();
        indices.put("parent", parent);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("vitamin_a", "no");
        caseInfo.getFieldValues().put("dob", REFERANCE_DATE_VALUE.toString("yyyy-MM-dd"));
        caseInfo.setIndices(indices);

        when(commcareDataProvider.lookup("CommcareForm", formlookup)).thenReturn(commcareForm);
        when(commcareDataProvider.lookup("CaseInfo", caselookup)).thenReturn(caseInfo);

        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);
        handler.handle(commcareFormstub());

        verify(eventRelay, times(2)).sendEventMessage(captor.capture());

        MotechEvent event = (MotechEvent) CollectionUtils.find(captor.getAllValues(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return o instanceof MotechEvent && ENROLL_USER_SUBJECT.equalsIgnoreCase(((MotechEvent) o).getSubject());
            }
        });

        assertNotNull(event);

        assertEquals(ENROLL_USER_SUBJECT, event.getSubject());

        assertEquals(CHILD_VITAMIN_A_REMINDER, event.getParameters().get(CAMPAIGN_NAME_KEY));
        assertEquals(String.format("%s:%s", CASE_ID_VALUE, PARENT_CASE_ID_VALUE), event.getParameters().get(EXTERNAL_ID_KEY));
        assertEquals(REFERANCE_DATE_VALUE, event.getParameters().get(REFERENCE_DATE));
    }

    @Test
    public void shouldNotExecuteTaskIfFormNameIsWrong() throws Exception {
        Map<String, String> formlookup = new HashMap<>();
        formlookup.put("id", FORM_ID_VALUE);

        FormValueElement form = new FormValueElement();
        form.addAttribute("name", "Other visit");

        commcareForm = new CommcareForm();
        commcareForm.setForm(form);

        when(commcareDataProvider.lookup("CommcareForm", formlookup)).thenReturn(commcareForm);

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfTookVitaminA() throws Exception {
        Map<String, String> formlookup = new HashMap<>();
        formlookup.put("id", FORM_ID_VALUE);

        FormValueElement caseElement = new FormValueElement();
        caseElement.setElementName("case");
        caseElement.addAttribute("case_id", CASE_ID_VALUE);

        FormValueElement form = new FormValueElement();
        form.addAttribute("name", "Child Visit");
        form.addFormValueElement("case", caseElement);

        commcareForm = new CommcareForm();
        commcareForm.setForm(form);

        Map<String, String> caselookup = new HashMap<>();
        caselookup.put("id", CASE_ID_VALUE);

        Map<String, String> parent = new HashMap<>();
        parent.put("case_id", PARENT_CASE_ID_VALUE);

        Map<String, Map<String, String>> indices = new HashMap<>();
        indices.put("parent", parent);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("vitamin_a", "yes");
        caseInfo.setIndices(indices);


        when(commcareDataProvider.lookup("CommcareForm", formlookup)).thenReturn(commcareForm);
        when(commcareDataProvider.lookup("CaseInfo", caselookup)).thenReturn(caseInfo);

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    private MotechEvent commcareFormstub() {
        return new MotechEventBuilder()
                .withSubject(getTriggerSubject())
                .withParameter("formId", FORM_ID_VALUE)
                .build();
    }

}
