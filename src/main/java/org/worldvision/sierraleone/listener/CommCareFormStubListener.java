package org.worldvision.sierraleone.listener;

import com.google.common.collect.Multimap;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommCareFormStubListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SettingsFacade settings;

    @Autowired
    MotechSchedulerService schedulerService;

    @Autowired
    CommcareFormService commcareFormService;

    @MotechListener(subjects = EventSubjects.FORM_STUB_EVENT )
    public void handle(MotechEvent event) {
        logger.error("MotechEvent received on " + EventSubjects.FORM_STUB_EVENT);

        if (commcareFormService == null) {
            logger.error("CommCare service is not available!");
            return;
        }

        // read event payload
        Map<String, Object> eventParams = event.getParameters();
        String formId = (String) eventParams.get(EventDataKeys.FORM_ID);
        if (formId == null) {
            logger.error("No " + EventDataKeys.FORM_ID + " key in event: " + event.toString());
            return;
        }

        // Get the form from CommCare
        logger.error("About to request from from commcare");
        CommcareForm form = commcareFormService.retrieveForm(formId);
        if (form == null) {
            logger.error("Unable to load form " + formId + " from CommCare");
            return;
        }

        logger.error("loaded form " + form.toString());

        String formName = form.getType();
        logger.error("form name" + formName);
        logger.info("name " + formName);

        Map<String, String> attributes = form.getForm().getAttributes();
        for (Map.Entry entry : attributes.entrySet()) {
            logger.error("Attribute: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }

        Multimap<String, FormValueElement> subElements = form.getForm().getSubElements();
        for (Map.Entry<String, FormValueElement> entry : subElements.entries()) {
            FormValueElement value = entry.getValue();
            logger.error("Form Key(" + entry.getKey() + "): " + value.getElementName() + " Value: " + value.getValue());
        }
    }
}
