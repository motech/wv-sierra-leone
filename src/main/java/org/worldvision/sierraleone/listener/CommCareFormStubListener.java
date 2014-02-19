package org.worldvision.sierraleone.listener;

import org.apache.commons.lang.StringUtils;
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
import java.util.Map;

import static org.worldvision.sierraleone.constants.Commcare.CASE;
import static org.worldvision.sierraleone.constants.Commcare.CASE_ID;
import static org.worldvision.sierraleone.constants.Commcare.NAME;
import static org.worldvision.sierraleone.constants.EventKeys.CHILD_CASE_ID;
import static org.worldvision.sierraleone.constants.EventKeys.DATE_OF_VISIT;
import static org.worldvision.sierraleone.constants.EventKeys.MOTHER_CASE_ID;
import static org.worldvision.sierraleone.constants.EventKeys.MOTHER_REFERRAL_SUBJECT;
import static org.worldvision.sierraleone.constants.EventKeys.REFERRAL_CASE_ID;

@Component
public class CommCareFormStubListener {
    private static final Logger LOG = LoggerFactory.getLogger(CommCareFormStubListener.class);

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
        LOG.debug(String.format("MotechEvent received on %s", EventSubjects.FORM_STUB_EVENT));
        String formId = EventKeys.getStringValue(event, EventDataKeys.FORM_ID);
        List<String> caseIds = EventKeys.getListValue(event, EventDataKeys.CASE_IDS, String.class);

        if (formId == null) {
            LOG.error(String.format("No %s key in event: %s", EventDataKeys.FORM_ID, event.toString()));
            return;
        }

        // Get the form from CommCare
        CommcareForm form = commcareFormService.retrieveForm(formId);
        if (form == null) {
            LOG.error(String.format("Unable to load form %s from CommCare", formId));
            return;
        }

        String formName = form.getForm().getAttributes().get(NAME);
        LOG.info(String.format("Handle form: %s", formName));

        switch (formName) {
            case "Child Visit":
                handleChildVisit(form);
                /* fall through */
            case "Post Partum Visit":
                handlePostPartumVisit(form);
                /* fall through */
            case "Pregnancy Visit":
                handleForm(caseIds, form);
                break;
            default:
                LOG.warn(String.format("Ignoring commcare forwarded form of type: %s", formName));
                break;
        }
    }

    @SuppressWarnings("PMD.NcssMethodCount")
    private void handleChildVisit(CommcareForm form) {
        String childCaseId = form.getForm().getElement(CASE).getAttributes().get(CASE_ID);
        CaseInfo childCase = commcareCaseService.getCaseByCaseId(childCaseId);
        if (null == childCase) {
            LOG.error("Unable to load case " + childCaseId + " from commcare");
            return;
        }

        Map<String, String> map = childCase.getIndices().get("parent");
        String motherCaseId = map.get(CASE_ID);
        String caseType = map.get("case_type");
        boolean isMotherType = "mother".equalsIgnoreCase(caseType);

        String dobAsString = getValue(form.getForm(), Commcare.CASE_DOB);
        String caseBirthCertificate = getValue(form.getForm(), Commcare.CASE_BIRTH_CERTIFICATE);
        String caseVitaDateAsString = getValue(form.getForm(), Commcare.CASE_VITA_DATE);
        String caseDewormDateAsString = getValue(form.getForm(), Commcare.CASE_DEWORM_DATE);
        String startDateAsString = getValue(form.getForm(), Commcare.START_DATE);
        String birthCertificate = null;
        String compBreastfeeding = null;
        String eats3TimesADay = null;
        String ironRichFoods = null;
        String bednet = null;
        String highRisk = null;
        String vita = null;
        String deworm = null;

        FormValueElement visitQuestions = form.getForm().getElement(Commcare.VISIT_QUESTIONS);
        if (null != visitQuestions) {
            birthCertificate = getValue(visitQuestions, Commcare.BIRTH_CERTIFICATE);
            compBreastfeeding = getValue(visitQuestions, Commcare.COMP_BREASTFEEDING);
            eats3TimesADay = getValue(visitQuestions, Commcare.EATS_3_TIMES_A_DAY);
            ironRichFoods = getValue(visitQuestions, Commcare.IRON_RICH_FOODS);
            bednet = getValue(visitQuestions, Commcare.BEDNET);
            highRisk = getValue(visitQuestions, Commcare.HIGH_RISK);
            vita = getValue(visitQuestions, Commcare.VITA);
            deworm = getValue(visitQuestions, Commcare.DEWORM);
        }

        FormChecker checker = getFormChecker(form);

        checker.checkFieldExists(CHILD_CASE_ID, childCaseId);
        checker.checkFieldExists(MOTHER_CASE_ID, motherCaseId);
        checker.checkFieldExists(Commcare.CASE_DOB, dobAsString);
        checker.checkFieldExists(Commcare.START_DATE, startDateAsString);
        checker.checkFieldExists(Commcare.CASE_BIRTH_CERTIFICATE, caseBirthCertificate);
        checker.checkFieldExists(Commcare.BIRTH_CERTIFICATE, birthCertificate);
        checker.checkFieldExists(Commcare.COMP_BREASTFEEDING, compBreastfeeding);
        checker.checkFieldExists(Commcare.EATS_3_TIMES_A_DAY, eats3TimesADay);
        checker.checkFieldExists(Commcare.IRON_RICH_FOODS, ironRichFoods);
        checker.checkFieldExists(Commcare.BEDNET, bednet);
        checker.checkFieldExists(Commcare.HIGH_RISK, highRisk);
        checker.checkFieldExists(Commcare.VITA, vita);
        checker.checkFieldExists(Commcare.DEWORM, deworm);

        if (checker.check() && isMotherType) {
            DateTime dob = Utils.parseDateTime(dobAsString);
            DateTime caseVitaDate = StringUtils.isBlank(caseVitaDateAsString)
                    ? null
                    : Utils.parseDateTime(caseVitaDateAsString);
            DateTime caseDewormDate = StringUtils.isBlank(caseDewormDateAsString)
                    ? null
                    : Utils.parseDateTime(caseDewormDateAsString);
            DateTime startDate = StringUtils.isBlank(startDateAsString)
                    ? null
                    : Utils.parseDateTime(startDateAsString);

            MotechEvent event = new MotechEventBuilder()
                    .withSubject(EventKeys.CHILD_VISIT_FORM_SUBJECT)
                    .withParameter(CHILD_CASE_ID, childCaseId)
                    .withParameter(MOTHER_CASE_ID, motherCaseId)
                    .withParameter(EventKeys.DATE_OF_BIRTH, dob)
                    .withParameter(Commcare.START_DATE, startDate)
                    .withParameter(Commcare.CASE_BIRTH_CERTIFICATE, caseBirthCertificate)
                    .withParameter(Commcare.CASE_VITA_DATE, caseVitaDate)
                    .withParameter(Commcare.START_DATE, caseDewormDate)
                    .withParameter(Commcare.BIRTH_CERTIFICATE, birthCertificate)
                    .withParameter(Commcare.COMP_BREASTFEEDING, compBreastfeeding)
                    .withParameter(Commcare.EATS_3_TIMES_A_DAY, eats3TimesADay)
                    .withParameter(Commcare.IRON_RICH_FOODS, ironRichFoods)
                    .withParameter(Commcare.BEDNET, bednet)
                    .withParameter(Commcare.HIGH_RISK, highRisk)
                    .withParameter(Commcare.VITA, vita)
                    .withParameter(Commcare.DEWORM, deworm)
                    .build();

            eventRelay.sendEventMessage(event);
        }
    }

    private void handlePostPartumVisit(CommcareForm form) {
        String motherCaseId = form.getForm().getElement(CASE).getAttributes().get(Commcare.CASE_ID);
        String visitSuccess = getValue(form.getForm(), Commcare.VISIT_SUCCESS);
        String caseNextVisitAsString = getValue(form.getForm(), Commcare.CASE_NEXT_VISIT);
        String dobAsString = getValue(form.getForm(), Commcare.CASE_DOB);
        String caseOpv0 = getValue(form.getForm(), Commcare.CASE_OPV_0);
        String caseBcg = getValue(form.getForm(), Commcare.CASE_BCG);
        String caseAttendedPnc = getValue(form.getForm(), Commcare.CASE_ATTENDED_PNC);
        String highRisk = null;
        String attendedPnc = null;
        String motherAlive = null;
        String opv0 = null;
        String bcg = null;

        FormValueElement visitQuestions = form.getForm().getElement(Commcare.VISIT_QUESTIONS);
        if (null != visitQuestions) {
            highRisk = getValue(visitQuestions, Commcare.HIGH_RISK);
            attendedPnc = getValue(visitQuestions, Commcare.ATTENDED_PNC);
            motherAlive = getValue(visitQuestions, Commcare.MOTHER_ALIVE);

            FormValueElement vaccinations = visitQuestions.getElement(Commcare.VACCINATIONS);
            if (null != vaccinations) {
                opv0 = getValue(vaccinations, Commcare.OPV_0);
                bcg = getValue(vaccinations, Commcare.BCG);
            }
        }

        FormChecker checker = getFormChecker(form);

        checker.checkFieldExists(MOTHER_CASE_ID, motherCaseId);
        checker.checkFieldExists(Commcare.VISIT_SUCCESS, visitSuccess);
        checker.checkFieldExists(Commcare.CASE_NEXT_VISIT, caseNextVisitAsString);
        checker.checkFieldExists(Commcare.CASE_DOB, dobAsString);
        checker.checkFieldExists(Commcare.CASE_OPV_0, caseOpv0);
        checker.checkFieldExists(Commcare.CASE_BCG, caseBcg);
        checker.checkFieldExists(Commcare.OPV_0, opv0);
        checker.checkFieldExists(Commcare.BCG, bcg);
        checker.checkFieldExists(Commcare.HIGH_RISK, highRisk);
        checker.checkFieldExists(Commcare.CASE_ATTENDED_PNC, caseAttendedPnc);
        checker.checkFieldExists(Commcare.ATTENDED_PNC, attendedPnc);
        checker.checkFieldExists(Commcare.MOTHER_ALIVE, motherAlive);

        if (checker.check()) {
            DateTime caseNextVisit = Utils.parseDateTime(caseNextVisitAsString);
            DateTime dob = Utils.parseDateTime(dobAsString);

            MotechEvent event = new MotechEventBuilder()
                    .withSubject(EventKeys.POST_PARTUM_FORM_SUBJECT)
                    .withParameter(EventKeys.MOTHER_CASE_ID, motherCaseId)
                    .withParameter(Commcare.VISIT_SUCCESS, visitSuccess)
                    .withParameter(Commcare.CASE_NEXT_VISIT, caseNextVisit)
                    .withParameter(EventKeys.DATE_OF_BIRTH, dob)
                    .withParameter(Commcare.CASE_OPV_0, caseOpv0)
                    .withParameter(Commcare.CASE_BCG, caseBcg)
                    .withParameter(Commcare.OPV_0, opv0)
                    .withParameter(Commcare.BCG, bcg)
                    .withParameter(Commcare.HIGH_RISK, highRisk)
                    .withParameter(Commcare.CASE_ATTENDED_PNC, caseAttendedPnc)
                    .withParameter(Commcare.ATTENDED_PNC, attendedPnc)
                    .withParameter(Commcare.MOTHER_ALIVE, motherAlive)
                    .build();

            eventRelay.sendEventMessage(event);
        }
    }

    private void handleForm(List<String> caseIds, CommcareForm form) {
        String dov = getValue(form.getForm(), Commcare.DATE_OF_VISIT);
        String createReferral = getValue(form.getForm(), Commcare.CREATE_REFERRAL);
        String motherCaseId = form.getForm().getElement(CASE).getAttributes().get(Commcare.CASE_ID);
        DateTime dateOfVisit = Utils.parseDateTime(dov);

        LOG.debug(String.format("createReferral: %s", createReferral));
        LOG.debug(String.format("Mother Case Id: %s", motherCaseId));
        LOG.debug(String.format("dateOfVisit: %s", dateOfVisit));

        FormChecker checker = getFormChecker(form);

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

    private FormChecker getFormChecker(CommcareForm form) {
        FormChecker checker = new FormChecker();
        checker.addMetadata("type", form.getId());
        checker.addMetadata("name", form.getForm().getAttributes().get(NAME));
        checker.addMetadata("id", form.getId());

        return checker;
    }
}
