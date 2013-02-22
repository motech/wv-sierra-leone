package org.worldvision.sierraleone.listener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.worldvision.sierraleone.constants.Commcare;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


/**
 * Unit test for Controller.
 */
public class CommCareFormStubListenerTest {
    @Mock
    CommcareCaseService commcareCaseService;

    @Mock
    private CommcareFormService commcareFormService;

    @Mock
    private EventRelay eventRelay;

    @InjectMocks
    private CommCareFormStubListener commCareFormStubListener = new CommCareFormStubListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldNotPublishEvent() {
        String formId = "formId";

        FormValueElement formValueElement = new FormValueElement();
        formValueElement.addAttribute(Commcare.NAME, "A Form We Don't Care About");

        CommcareForm form = new CommcareForm();
        form.setForm(formValueElement);

        MotechEvent event = new MotechEvent(EventSubjects.FORM_STUB_EVENT);
        event.getParameters().put(EventDataKeys.FORM_ID, formId);

        when(commcareFormService.retrieveForm(formId)).thenReturn(form);

        commCareFormStubListener.handle(event);

        verify(eventRelay, never()).sendEventMessage(null);

        assertTrue(true);
    }


}
