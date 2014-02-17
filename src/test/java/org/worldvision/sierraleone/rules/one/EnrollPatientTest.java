package org.worldvision.sierraleone.rules.one;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
import static org.worldvision.sierraleone.constants.Commcare.ATTENDED_PNC;
import static org.worldvision.sierraleone.constants.Commcare.CASE;
import static org.worldvision.sierraleone.constants.Commcare.CASE_ID;
import static org.worldvision.sierraleone.constants.Commcare.DATE_OF_BIRTH;
import static org.worldvision.sierraleone.constants.Commcare.DELIVERED;
import static org.worldvision.sierraleone.constants.Commcare.NAME;
import static org.worldvision.sierraleone.constants.Commcare.POST_PARTUM_VISIT;
import static org.worldvision.sierraleone.constants.Commcare.MOTHER_ALIVE;

public class EnrollPatientTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "eec2fa1b5d77bd5c536def1b150bdae7";
    private static final String FORM_ID_VALUE = "formId";
    private static final String CASE_ID_VALUE = "caseId";
    private static final DateTime REFERANCE_DATE_VALUE = newDateTime(2013, 8, 14);

    @Mock
    private DataProvider commcareDataProvider;

    @Mock
    private CommcareForm commcareForm;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);

        setTask(1, "enroll_patient.json", "rule-1-enroll-patient");
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(ENROLL_USER_SUBJECT);
        actionEvent.addParameter(new ActionParameter(CAMPAIGN_NAME_KEY, CAMPAIGN_NAME_KEY), true);
        actionEvent.addParameter(new ActionParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_KEY), true);
        actionEvent.addParameter(new ActionParameter(REFERENCE_DATE, REFERENCE_DATE, DATE), true);

        Map<String, String> lookupFields = new HashMap<>();
        lookupFields.put("id", FORM_ID_VALUE);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        when(commcareDataProvider.lookup("CommcareForm", lookupFields)).thenReturn(commcareForm);

        when(commcareForm.getForm()).thenReturn(createForm("2013-08-14", "no", "yes", "yes", "Post Partum Visit"));

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

        assertEquals(HEALTH_CENTER_POSTNATAL_CONSULTATION_REMINDER, event.getParameters().get(CAMPAIGN_NAME_KEY));
        assertEquals(CASE_ID_VALUE, event.getParameters().get(EXTERNAL_ID_KEY));
        assertEquals(REFERANCE_DATE_VALUE, event.getParameters().get(REFERENCE_DATE));
    }

    @Test
    public void shouldNotExecuteTaskIfAttendedPostnatal() throws Exception {
        Map<String, String> lookupFields = new HashMap<>();
        lookupFields.put("id", FORM_ID_VALUE);

        when(commcareDataProvider.lookup("CommcareForm", lookupFields)).thenReturn(commcareForm);

        when(commcareForm.getForm()).thenReturn(createForm("2013-08-14", "yes", "yes", "yes", "Post Partum Visit"));

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfDaysFromDateOfBirthIsMoreThan45() throws Exception {
        Map<String, String> lookupFields = new HashMap<>();
        lookupFields.put("id", FORM_ID_VALUE);

        when(commcareDataProvider.lookup("CommcareForm", lookupFields)).thenReturn(commcareForm);

        when(commcareForm.getForm()).thenReturn(createForm("2013-06-14", "no", "yes", "yes", "Post Partum Visit"));

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfNotAlive() throws Exception {
        Map<String, String> lookupFields = new HashMap<>();
        lookupFields.put("id", FORM_ID_VALUE);

        when(commcareDataProvider.lookup("CommcareForm", lookupFields)).thenReturn(commcareForm);

        when(commcareForm.getForm()).thenReturn(createForm("2013-08-14", "no", "yes", "no", "Post Partum Visit"));

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfNoGaveBirth() throws Exception {
        Map<String, String> lookupFields = new HashMap<>();
        lookupFields.put("id", FORM_ID_VALUE);

        when(commcareDataProvider.lookup("CommcareForm", lookupFields)).thenReturn(commcareForm);

        when(commcareForm.getForm()).thenReturn(createForm("2013-08-14", "no", "no", "yes", "Post Partum Visit"));

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfFormNameIsIncorrect() throws Exception {
        Map<String, String> lookupFields = new HashMap<>();
        lookupFields.put("id", FORM_ID_VALUE);

        when(commcareDataProvider.lookup("CommcareForm", lookupFields)).thenReturn(commcareForm);

        when(commcareForm.getForm()).thenReturn(createForm("2013-08-14", "no", "yes", "yes", "something"));

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    private FormValueElement createForm(String dob, String attendedPostnatal, String gaveBirth, String stillAlive, String formName) {
        FormValueElement caseElement = new FormValueElement();
        caseElement.setElementName(CASE);
        caseElement.addAttribute(CASE_ID, CASE_ID_VALUE);

        FormValueElement postPartum = new FormValueElement();
        postPartum.setElementName(POST_PARTUM_VISIT);
        postPartum.addFormValueElement(DATE_OF_BIRTH, formValueElement(dob, DATE_OF_BIRTH));
        postPartum.addFormValueElement(ATTENDED_PNC, formValueElement(attendedPostnatal, ATTENDED_PNC));

        FormValueElement form = new FormValueElement();
        form.addAttribute(NAME, formName);
        form.addFormValueElement(DELIVERED, formValueElement(gaveBirth, DELIVERED));
        form.addFormValueElement(MOTHER_ALIVE, formValueElement(stillAlive, MOTHER_ALIVE));
        form.addFormValueElement(POST_PARTUM_VISIT, postPartum);
        form.addFormValueElement(CASE, caseElement);
        return form;
    }

    private MotechEvent commcareFormstub() {
        return new MotechEventBuilder()
                .withSubject(getTriggerSubject())
                .withParameter("formId", FORM_ID_VALUE)
                .build();
    }

    private FormValueElement formValueElement(String value, String elementName) {
        FormValueElement element = new FormValueElement();
        element.setElementName(elementName);
        element.setValue(value);

        return element;
    }

}
