package org.worldvision.sierraleone.rules.five;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.CommcareUser;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commons.api.DataProvider;
import org.motechproject.event.MotechEvent;
import org.motechproject.tasks.domain.ActionEvent;
import org.motechproject.tasks.domain.ActionParameter;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.rules.RuleTest;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.sms.api.constants.EventDataKeys.MESSAGE;
import static org.motechproject.sms.api.constants.EventDataKeys.RECIPIENTS;
import static org.motechproject.sms.api.constants.EventSubjects.SEND_SMS;
import static org.motechproject.tasks.domain.ParameterType.LIST;
import static org.worldvision.sierraleone.constants.Commcare.CASE;
import static org.worldvision.sierraleone.constants.Commcare.NAME;

public class SendSmsToPHUTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "4408354fae5368389825691bc5056ebf";
    private static final String CMSLITE_PROVIDER_ID = "4408354fae5368389825691bc50583c2";
    private static final String CASE_ID_VALUE = "caseId";
    private static final String FORM_ID_VALUE = "formId";
    private static final String PHU_ID_VALUE = "phuId";
    private static final String USER_ID_VALUE = "userId";
    private static final String PHONE_VALUE = "777777777";
    private static final String MOTHER_NAME_VALUE = "motherName";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    @Mock
    private DataProvider commcareDataProvider;

    @Mock
    private DataProvider cmsliteDataProvider;

    private CommcareForm commcareForm;

    private CaseInfo caseInfo;

    private CommcareFixture commcareFixture;

    private CommcareUser commcareUser;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);
        handler.addDataProvider(CMSLITE_PROVIDER_ID, cmsliteDataProvider);

        setTask(5, "send_sms_to_phu.json", "rule-5-send-sms-to-phu");
        setMessages();
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(SEND_SMS);
        actionEvent.addParameter(new ActionParameter(MESSAGE, MESSAGE), true);
        actionEvent.addParameter(new ActionParameter(RECIPIENTS, RECIPIENTS, LIST), true);

        Map<String, String> formLookup = new HashMap<>();
        formLookup.put("id", FORM_ID_VALUE);

        FormValueElement placeOfBirth = new FormValueElement();
        placeOfBirth.setElementName("place_of_birth");
        placeOfBirth.setValue("home");

        FormValueElement postPartumVisit = new FormValueElement();
        postPartumVisit.setElementName("post_partum_visit");
        postPartumVisit.addFormValueElement("place_of_birth", placeOfBirth);

        FormValueElement caseElement = new FormValueElement();
        caseElement.setElementName("case");
        caseElement.addAttribute("case_id", CASE_ID_VALUE);

        FormValueElement form = new FormValueElement();
        form.setElementName("form");
        form.addAttribute(NAME, "Post-Partum Visit");
        form.addFormValueElement("post_partum_visit", postPartumVisit);
        form.addFormValueElement(CASE, caseElement);

        commcareForm = new CommcareForm();
        commcareForm.setForm(form);

        Map<String, String> caseLookup = new HashMap<>();
        caseLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("phu_id", PHU_ID_VALUE);
        caseInfo.getFieldValues().put("mother_name", MOTHER_NAME_VALUE);
        caseInfo.setUserId(USER_ID_VALUE);

        Map<String, String> fixtureLookup = new HashMap<>();
        fixtureLookup.put("id", PHU_ID_VALUE);

        commcareFixture = new CommcareFixture();
        commcareFixture.setFields(new HashMap<String, String>());
        commcareFixture.getFields().put("phone", PHONE_VALUE);

        Map<String, String> userLookup = new HashMap<>();
        userLookup.put("id", USER_ID_VALUE);

        commcareUser = new CommcareUser();
        commcareUser.setFirstName(FIRST_NAME);
        commcareUser.setLastName(LAST_NAME);

        Map<String, String> cmsliteLookup = new HashMap<>();
        cmsliteLookup.put("cmslite.dataname", "HomeBirthNotification");
        cmsliteLookup.put("cmslite.language", "English");

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        when(commcareDataProvider.lookup("CommcareForm", formLookup)).thenReturn(commcareForm);
        when(commcareDataProvider.lookup("CaseInfo", caseLookup)).thenReturn(caseInfo);
        when(commcareDataProvider.lookup("CommcareFixture", fixtureLookup)).thenReturn(commcareFixture);
        when(commcareDataProvider.lookup("CommcareUser", userLookup)).thenReturn(commcareUser);
        when(cmsliteDataProvider.lookup("StringContent", cmsliteLookup)).thenReturn(mesages.get("HomeBirthNotification"));

        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);
        handler.handle(commcareFormstub());

        verify(eventRelay, times(2)).sendEventMessage(captor.capture());

        MotechEvent event = (MotechEvent) CollectionUtils.find(captor.getAllValues(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return o instanceof MotechEvent && SEND_SMS.equalsIgnoreCase(((MotechEvent) o).getSubject());
            }
        });

        assertNotNull(event);

        assertEquals(SEND_SMS, event.getSubject());

        String message = mesages.get("HomeBirthNotification").getValue();

        assertEquals(String.format(message, FIRST_NAME, LAST_NAME, MOTHER_NAME_VALUE), event.getParameters().get(MESSAGE));
        assertEquals(asList(PHONE_VALUE), event.getParameters().get(RECIPIENTS));
    }

    @Test
    public void shouldNotExecuteTaskIfPlaceOfBirthIsNotEqualToHome() throws Exception {
        Map<String, String> formLookup = new HashMap<>();
        formLookup.put("id", FORM_ID_VALUE);

        FormValueElement placeOfBirth = new FormValueElement();
        placeOfBirth.setElementName("place_of_birth");
        placeOfBirth.setValue("hospital");

        FormValueElement postPartumVisit = new FormValueElement();
        postPartumVisit.setElementName("post_partum_visit");
        postPartumVisit.addFormValueElement("place_of_birth", placeOfBirth);

        FormValueElement form = new FormValueElement();
        form.setElementName("form");
        form.addAttribute(NAME, "Post-Partum Visit");
        form.addFormValueElement("post_partum_visit", postPartumVisit);

        commcareForm = new CommcareForm();
        commcareForm.setForm(form);

        when(commcareDataProvider.lookup("CommcareForm", formLookup)).thenReturn(commcareForm);

        handler.handle(commcareFormstub());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfFormNameIsWrong() throws Exception {
        Map<String, String> formLookup = new HashMap<>();
        formLookup.put("id", FORM_ID_VALUE);

        FormValueElement placeOfBirth = new FormValueElement();
        placeOfBirth.setElementName("place_of_birth");
        placeOfBirth.setValue("hospital");

        FormValueElement form = new FormValueElement();
        form.setElementName("form");
        form.addAttribute(NAME, "Some Visit");

        commcareForm = new CommcareForm();
        commcareForm.setForm(form);

        when(commcareDataProvider.lookup("CommcareForm", formLookup)).thenReturn(commcareForm);

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
