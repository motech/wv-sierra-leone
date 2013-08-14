package org.worldvision.sierraleone.listener;

import org.joda.time.DateTime;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareForm;
import org.motechproject.commcare.domain.FormValueElement;
import org.motechproject.commcare.events.constants.EventDataKeys;
import org.motechproject.commcare.events.constants.EventSubjects;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.event.listener.annotations.MotechListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.FormChecker;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.Utils;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;

import java.util.List;

import static org.worldvision.sierraleone.constants.Commcare.CASE;
import static org.worldvision.sierraleone.constants.Commcare.NAME;
import static org.worldvision.sierraleone.constants.EventKeys.DATE_OF_VISIT;
import static org.worldvision.sierraleone.constants.EventKeys.MOTHER_CASE_ID;
import static org.worldvision.sierraleone.constants.EventKeys.MOTHER_REFERRAL_SUBJECT;
import static org.worldvision.sierraleone.constants.EventKeys.REFERRAL_CASE_ID;

@Component
public class CommCareFormStubListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private CommcareFormService commcareFormService;
    private CommcareCaseService commcareCaseService;
    private EventRelay eventRelay;

    @Autowired
    public CommCareFormStubListener(CommcareFormService commcareFormService,
                                    CommcareCaseService commcareCaseService,
                                    EventRelay eventRelay) {
        this.commcareFormService = commcareFormService;
        this.commcareCaseService = commcareCaseService;
        this.eventRelay = eventRelay;
    }

    @MotechListener(subjects = EventSubjects.FORM_STUB_EVENT)
    public void handle(MotechEvent event) {
        logger.info("MotechEvent received on " + EventSubjects.FORM_STUB_EVENT);
        String formId = EventKeys.getStringValue(event, EventDataKeys.FORM_ID);
        List<String> caseIds = EventKeys.getListValue(event, EventDataKeys.CASE_IDS, String.class);

        if (formId == null) {
            logger.error("No " + EventDataKeys.FORM_ID + " key in event: " + event.toString());
            return;
        }

        // Get the form from CommCare
        CommcareForm form = commcareFormService.retrieveForm(formId);
        if (form == null) {
            logger.error("Unable to load form " + formId + " from CommCare");
            return;
        }

        String formName = form.getForm().getAttributes().get(NAME);
        logger.info("form name " + formName);

        switch (formName) {
            case "Post-Partum Visit":
                /* fall through */
            case "Pregnancy Visit":
                handleForm(caseIds, form);
                break;
            default:
                logger.info("Ignoring commcare forwarded form of type: " + formName);
                break;
        }
    }

    private void handleForm(List<String> caseIds, CommcareForm form) {
        String dov = getValue(form.getForm(), Commcare.DATE_OF_VISIT);
        String createReferral = getValue(form.getForm(), Commcare.CREATE_REFERRAL);
        String motherCaseId = form.getForm().getElement(CASE).getAttributes().get(Commcare.CASE_ID);
        DateTime dateOfVisit = Utils.dateTimeFromCommcareDateString(dov);

        logger.info("createReferral: " + createReferral);
        logger.info("Mother Case Id: " + motherCaseId);
        logger.info("dateOfVisit: " + dateOfVisit);

        FormChecker checker = new FormChecker();
        checker.addMetadata("type", form.getId());
        checker.addMetadata("name", form.getForm().getAttributes().get(NAME));
        checker.addMetadata("id", form.getId());

        checker.checkFieldExists(MOTHER_CASE_ID, motherCaseId);
        checker.checkFieldExists(DATE_OF_VISIT, dateOfVisit);

        if (checker.check() && "yes".equals(createReferral)) {
            sendReferralEvent(motherCaseId, form, caseIds, dateOfVisit);
        }
    }

    private String getValue(FormValueElement form, String key) {
        FormValueElement element = form.getElement(key);
        return element != null ? element.getValue() : null;
    }

    // The commcare stub form event contains a list of the cases affected by the form.  This method iterates
    // over that list finding the one that is the case.  This requires loading the case from commcare, so
    // we filter out the mother case so we can eliminate one remote call
    private void sendReferralEvent(String motherCaseId, CommcareForm form, List<String> caseIds, DateTime dateOfVisit) {
        String referralId = null;

        for (String caseId : caseIds) {
            if (!motherCaseId.equals(caseId)) {
                CaseInfo caseInfo = commcareCaseService.getCaseByCaseId(caseId);

                if ("referral".equals(caseInfo.getCaseType())) {
                    referralId = caseId;
                }
            }
        }

        FormChecker checker = new FormChecker();
        checker.addMetadata("type", "referral");
        checker.addMetadata("name", form.getForm().getAttributes().get(NAME));
        checker.addMetadata("id", form.getId());

        checker.checkFieldExists(Commcare.DATE_OF_VISIT, dateOfVisit);
        checker.checkFieldExists(REFERRAL_CASE_ID, referralId);
        checker.checkFieldExists(MOTHER_CASE_ID, motherCaseId);

        if (checker.check()) {
            MotechEvent event = new MotechEventBuilder()
                    .withSubject(MOTHER_REFERRAL_SUBJECT)
                    .withParameter(MOTHER_CASE_ID, motherCaseId)
                    .withParameter(REFERRAL_CASE_ID, referralId)
                    .withParameter(DATE_OF_VISIT, dateOfVisit)
                    .build();

            eventRelay.sendEventMessage(event);
        }
    }
}
