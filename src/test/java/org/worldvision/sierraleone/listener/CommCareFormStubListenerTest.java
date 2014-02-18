package org.worldvision.sierraleone.listener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.worldvision.sierraleone.constants.Commcare;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class CommCareFormStubListenerTest {
    @Mock
    private CommcareCaseService commcareCaseService;

    @Mock
    private CommcareFormService commcareFormService;

    @Mock
    private EventRelay eventRelay;

    private CommCareFormStubListener commCareFormStubListener;

    @Before
    public void setUp() {
        initMocks(this);

        commCareFormStubListener = new CommCareFormStubListener(
                commcareFormService, commcareCaseService, eventRelay
        );
    }

    @Test
    public void shouldNotPublishEventIfEventNotContainFormId() {
        commCareFormStubListener.handle(CommcareFormStubEvent(null));

        verify(eventRelay, never()).sendEventMessage(null);
    }

    @Test
    public void shouldNotPublishEventIfFormNotFound() {
        when(commcareFormService.retrieveForm("formId")).thenReturn(null);

        commCareFormStubListener.handle(CommcareFormStubEvent("formId"));

        verify(eventRelay, never()).sendEventMessage(null);
    }

    @Test
    public void shouldNotPublishEventFromFormWeDontNeed() {
        String formId = "formId";
        CommcareForm form = CommcareForm("A Form We Don't Care About");

        when(commcareFormService.retrieveForm(formId)).thenReturn(form);

        commCareFormStubListener.handle(CommcareFormStubEvent(formId));

        verify(eventRelay, never()).sendEventMessage(null);
    }

    @Test
    public void postPartumVisitReferralShouldPublish() {
        String formId = "formId";
        String referralCaseId = "referralCaseId";
        String motherCaseId = "motherCaseId";
        CommcareForm form = CommcareForm("Post Partum Visit");
        form.getForm().setElementName("form");

        AddSingleValueFormField(form, Commcare.MOTHER_ALIVE, "yes");
        AddSingleValueFormField(form, Commcare.DELIVERED, "yes");
        AddSingleValueFormField(form, Commcare.CREATE_REFERRAL, "yes");
        AddSingleValueFormField(form, Commcare.REFERRAL_ID, "referralId");
        AddSingleValueFormField(form, Commcare.DATE_OF_VISIT, "2013-01-03");

        FormValueElement aCase = AddSingleValueFormField(form, Commcare.CASE, "case");
        aCase.addAttribute(Commcare.CASE_ID, motherCaseId);

        FormValueElement postPartumVisit = new FormValueElement();
        postPartumVisit.setElementName(Commcare.POST_PARTUM_VISIT);

        AddSubelementValueFormField(postPartumVisit, Commcare.DATE_OF_BIRTH, "2013-01-03");
        AddSubelementValueFormField(postPartumVisit, Commcare.ATTENDED_PNC, "yes");
        AddSubelementValueFormField(postPartumVisit, Commcare.PLACE_OF_BIRTH, "home");

        form.getForm().addFormValueElement(Commcare.POST_PARTUM_VISIT, postPartumVisit);

        CaseInfo referralCase = new CaseInfo();
        referralCase.setCaseType("referral");

        when(commcareCaseService.getCaseByCaseId(referralCaseId)).thenReturn(referralCase);

        CaseInfo motherCase = new CaseInfo();
        motherCase.setCaseType("mother");

        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(commcareFormService.retrieveForm(formId)).thenReturn(form);

        List<String> list = Arrays.asList(motherCaseId, referralCaseId);
        commCareFormStubListener.handle(CommcareFormStubEvent(formId, list));

        verify(eventRelay).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void pregnancyVisitWithoutReferralShouldPublishNoEvent() {
        String formId = "formId";
        CommcareForm form = CommcareForm("Pregnancy Visit");
        form.getForm().setElementName("form");

        AddSingleValueFormField(form, Commcare.CREATE_REFERRAL, "no");
        AddSingleValueFormField(form, Commcare.REFERRAL_ID, null);
        AddSingleValueFormField(form, Commcare.DATE_OF_VISIT, "2013-01-03");

        FormValueElement aCase = AddSingleValueFormField(form, Commcare.CASE, "case");
        aCase.addAttribute(Commcare.CASE_ID, "caseId");

        when(commcareFormService.retrieveForm(formId)).thenReturn(form);

        commCareFormStubListener.handle(CommcareFormStubEvent(formId));

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void pregnancyVisitWithReferralShouldPublishOneEvent() {
        String formId = "formId";
        String referralCaseId = "referralCaseId";
        String motherCaseId = "motherCaseId";
        CommcareForm form = CommcareForm("Pregnancy Visit");
        form.getForm().setElementName("form");

        AddSingleValueFormField(form, Commcare.CREATE_REFERRAL, "yes");
        AddSingleValueFormField(form, Commcare.REFERRAL_ID, "referralId");
        AddSingleValueFormField(form, Commcare.DATE_OF_VISIT, "2013-01-03");

        FormValueElement aCase = AddSingleValueFormField(form, Commcare.CASE, "case");
        aCase.addAttribute(Commcare.CASE_ID, motherCaseId);

        CaseInfo referralCase = new CaseInfo();
        referralCase.setCaseType("referral");

        when(commcareCaseService.getCaseByCaseId(referralCaseId)).thenReturn(referralCase);

        CaseInfo motherCase = new CaseInfo();
        motherCase.setCaseType("mother");

        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(commcareFormService.retrieveForm(formId)).thenReturn(form);

        List<String> list = Arrays.asList(motherCaseId, referralCaseId);
        commCareFormStubListener.handle(CommcareFormStubEvent(formId, list));

        verify(eventRelay).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void pregnancyVisitWithReferralWithMissingFieldShouldNotPublishEvent() {
        String formId = "formId";
        String referralCaseId = "referralCaseId";
        String motherCaseId = "motherCaseId";
        CommcareForm form = CommcareForm("Pregnancy Visit");
        form.getForm().setElementName("form");

        AddSingleValueFormField(form, Commcare.CREATE_REFERRAL, "yes");
        AddSingleValueFormField(form, Commcare.DATE_OF_VISIT, "2013-01-03");

        FormValueElement aCase = AddSingleValueFormField(form, Commcare.CASE, "case");
        aCase.addAttribute(Commcare.CASE_ID, motherCaseId);

        CaseInfo referralCase = new CaseInfo();
        referralCase.setCaseType("referral");

        when(commcareCaseService.getCaseByCaseId(referralCaseId)).thenReturn(referralCase);

        CaseInfo motherCase = new CaseInfo();
        motherCase.setCaseType("mother");

        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(commcareFormService.retrieveForm(formId)).thenReturn(form);

        List<String> list = Arrays.asList(motherCaseId, referralCaseId);
        commCareFormStubListener.handle(CommcareFormStubEvent(formId, list));

        verify(eventRelay).sendEventMessage(any(MotechEvent.class));
    }

    private void AddSubelementValueFormField(FormValueElement formValueElement, String elementName, String value) {
        FormValueElement child = new FormValueElement();

        child.setElementName(elementName);
        child.setValue(value);

        formValueElement.addFormValueElement(elementName, child);
    }

    private FormValueElement AddSingleValueFormField(CommcareForm form, String elementName, String value) {
        FormValueElement formValueElement = new FormValueElement();

        formValueElement.setElementName(elementName);
        formValueElement.setValue(value);

        form.getForm().addFormValueElement(elementName, formValueElement);

        return formValueElement;
    }

    private CommcareForm CommcareForm(String formName) {
        FormValueElement formValueElement = new FormValueElement();
        formValueElement.addAttribute(Commcare.NAME, formName);

        CommcareForm form = new CommcareForm();
        form.setForm(formValueElement);
        return form;
    }

    private MotechEvent CommcareFormStubEvent(String formId) {
        MotechEvent event = new MotechEvent(EventSubjects.FORM_STUB_EVENT);
        event.getParameters().put(EventDataKeys.FORM_ID, formId);
        return event;
    }

    private MotechEvent CommcareFormStubEvent(String formId, List<String> cases) {
        MotechEvent event = CommcareFormStubEvent(formId);

        event.getParameters().put(EventDataKeys.CASE_IDS, cases);

        return event;
    }
}
