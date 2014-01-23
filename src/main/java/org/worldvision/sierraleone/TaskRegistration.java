package org.worldvision.sierraleone;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TaskRegistration implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRegistration.class);

    private static final String ACTIONS_FIELD = "actions";
    private static final String DATA_SOURCE = "DataSource";
    private static final String MODULE_NAME_FIELD = "moduleName";
    private static final String NAME_FIELD = "name";
    private static final String PROVIDER_NAME_FIELD = "providerName";
    private static final String STEPS_FIELD = "steps";
    private static final String SUBJECT_FIELD = "subject";
    private static final String TASK_CONFIG_FIELD = "taskConfig";
    private static final String TRIGGER_FIELD = "trigger";
    private static final String TYPE_FIELD = "@type";
    private static final int ONE_SECOND = 1000;

    private ManagementTasks managementTasks;
    private final List<Resource> toLoad;
    private boolean work;

    TaskRegistration(ManagementTasks managementTasks) {
        this.managementTasks = managementTasks;
        this.toLoad = managementTasks.getResources();
        work = true;
    }

    @Override
    public void run() {
        while (continueWork()) {
            for (int idx = toLoad.size() - 1; idx >= 0; --idx) {
                Resource resource = toLoad.get(idx);
                JsonNode node;

                try {
                    node = managementTasks.getTask(resource);
                } catch (IOException exp) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error(
                                String.format(
                                        "Can't convert resource: %s to JsonNode object",
                                        resource.getFilename()
                                ),
                                exp
                        );
                    }

                    node = null;
                }

                if (null != node && canImport(node)) {
                    registerTask(node);
                    toLoad.remove(idx);
                }
            }

            try {
                Thread.sleep(ONE_SECOND);
            } catch (InterruptedException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            work = toLoad.isEmpty();
        }
    }

    private boolean canImport(JsonNode node) {
        LOGGER.debug(String.format("Check if the task: %s can be imported.", getFieldValue(node, NAME_FIELD)));

        Map<String, Boolean> actionsExist = new HashMap<>();
        Map<String, Boolean> providersExist = new HashMap<>();
        boolean triggerExist = existsChannel(getTriggerFieldValue(node, MODULE_NAME_FIELD));

        for (JsonNode action : node.get(ACTIONS_FIELD)) {
            String channelName = getFieldValue(action, MODULE_NAME_FIELD);

            actionsExist.put(channelName, existsChannel(channelName));
        }

        for (JsonNode step : node.get(TASK_CONFIG_FIELD).get(STEPS_FIELD)) {
            if (DATA_SOURCE.equalsIgnoreCase(getFieldValue(step, TYPE_FIELD))) {
                String providerName = getFieldValue(step, PROVIDER_NAME_FIELD);

                providersExist.put(providerName, existsProvider(providerName));
            }
        }

        LOGGER.trace(String.format("triggerExist=%s", triggerExist));
        LOGGER.trace(String.format("actionsExist=%s", actionsExist));
        LOGGER.trace(String.format("providersExist=%s", providersExist));

        return triggerExist && !actionsExist.containsValue(false) && !providersExist.containsValue(false);
    }

    private void registerTask(JsonNode node) {
        String taskName = getFieldValue(node, NAME_FIELD);

        try {
            managementTasks.getTaskService().importTask(node.toString());
            managementTasks.getHandler().registerHandlerFor(getTriggerFieldValue(node, SUBJECT_FIELD));

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Register task with name: " + taskName);
            }
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Can't register task with name: %s", taskName), e);
            }
        }
    }

    private String getTriggerFieldValue(JsonNode node, String fieldName) {
        return getFieldValue(node.get(TRIGGER_FIELD), fieldName);
    }

    private String getFieldValue(JsonNode node, String fieldName) {
        return node.get(fieldName).getTextValue();
    }

    private boolean existsChannel(String moduleName) {
        return managementTasks.getChannelService().getChannel(moduleName) != null;
    }

    private boolean existsProvider(String providerName) {
        return managementTasks.getProviderService().getProvider(providerName) != null;
    }

    boolean continueWork() {
        return work;
    }

    void setWork(boolean work) {
        this.work = work;
    }
}
