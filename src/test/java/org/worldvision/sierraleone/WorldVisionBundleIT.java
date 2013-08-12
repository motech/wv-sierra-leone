package org.worldvision.sierraleone;

import org.ektorp.DocumentNotFoundException;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.motechproject.tasks.service.TaskService;
import org.motechproject.testing.osgi.BaseOsgiIT;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;

public class WorldVisionBundleIT extends BaseOsgiIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldVisionBundleIT.class);

    private static final String MESSAGE_LANG = "English";
    private static final Integer TRIES_COUNT = 50;

    public void testLoadSmsMessages() {
        CMSLiteService service = getService(CMSLiteService.class);

        for (String name : getSMSContentNames()) {
            assertTrue(service.isStringContentAvailable(MESSAGE_LANG, name));
        }
    }

    public void testLoadTasks() throws Exception {
        TaskService taskService = getService(TaskService.class);
        List<String> taskIDs = getTaskIDs();
        Set<String> IDs = new HashSet<>();
        int tries = 0;

        do {
            for (String id : taskIDs) {
                try {
                    IDs.add(taskService.getTask(id).getId());
                } catch (DocumentNotFoundException e) {
                    LOGGER.warn(String.format("task with ID: %s does not exist", id));
                }
            }

            ++tries;
            Thread.sleep(500);
        } while (!isEqualCollection(taskIDs, IDs) && tries < TRIES_COUNT);

        assertTrue("All tasks were not loaded.", isEqualCollection(taskIDs, IDs));
    }

    private <T> T getService(Class<T> clazz) {
        ServiceReference serviceReference = bundleContext.getServiceReference(clazz.getName());
        assertNotNull(serviceReference);
        Object service = bundleContext.getService(serviceReference);
        assertNotNull(service);

        return clazz.cast(service);
    }

    private List<String> getSMSContentNames() {
        return asList(
                "PostnatalConsultationReminder",
                "MotherReferralReminder",
                "ChildVitaminAReminder",
                "HomeBirthNotification",
                "MissedConsecutiveChildVisits",
                "MissedConsecutivePostPartumVisits"
        );
    }

    private List<String> getTaskIDs() {
        return asList(
                "rule-1-enroll-patient",
                "rule-1-send-sms-to-patient",
                "rule-1-unenroll-patient-if-attended-postnatal",
                "rule-1-unenroll_patient-if-no-alive",

                "rule-2-send-sms-to-patient",
                "rule-2-unenroll-patient-if-closed",

                "rule-3-set-reminder",

                "rule-4-set-reminder-child_v5a_date",
                "rule-4-set-reminder-child_v5b_date",
                "rule-4-set-reminder-child_v5c_date",
                "rule-4-set-reminder-child_v5d_date",
                "rule-4-set-reminder-child_v6_date",
                "rule-4-set-reminder-child_v7_date",
                "rule-4-set-reminder-child_v8_date",
                "rule-4-set-reminder-child_v9_date",
                "rule-4-set-reminder-child_v10_date",
                "rule-4-set-reminder-child_v11_date",

                "rule-5-send-sms-to-phu",

                "rule-6-enroll-patient",
                "rule-6-send-sms",
                "rule-6-unenroll-if-took-vitamin-a"
        );
    }

}
