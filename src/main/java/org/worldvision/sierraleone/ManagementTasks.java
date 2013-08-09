package org.worldvision.sierraleone;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.gemini.blueprint.io.OsgiBundleResourcePatternResolver;
import org.eclipse.gemini.blueprint.service.importer.OsgiServiceLifecycleListener;
import org.motechproject.tasks.domain.Task;
import org.motechproject.tasks.service.ChannelService;
import org.motechproject.tasks.service.TaskDataProviderService;
import org.motechproject.tasks.service.TaskService;
import org.motechproject.tasks.service.TriggerHandler;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ManagementTasks implements OsgiServiceLifecycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementTasks.class);
    private static final String TASKS_DIR_PATTERN = "/tasks/**/*.json";
    private static final String ID_FIELD = "_id";

    private volatile TriggerHandler handler;
    private volatile TaskService taskService;
    private volatile ChannelService channelService;
    private volatile TaskDataProviderService providerService;

    private final ObjectMapper mapper;
    private final ResourcePatternResolver resourcePatternResolver;
    private final Thread taskRegistrationThread;

    @Autowired
    public ManagementTasks(BundleContext context) {
        this.mapper = new ObjectMapper();
        this.resourcePatternResolver = new OsgiBundleResourcePatternResolver(context.getBundle());
        this.taskRegistrationThread = new Thread(new TaskRegistration());
    }

    @Override
    public synchronized void bind(Object service, Map properties) {
        if (service instanceof TriggerHandler) {
            handler = (TriggerHandler) service;
        } else if (service instanceof TaskService) {
            taskService = (TaskService) service;
        } else if (service instanceof ChannelService) {
            channelService = (ChannelService) service;
        } else if (service instanceof TaskDataProviderService) {
            providerService = (TaskDataProviderService) service;
        }

        if (null != handler && null != taskService && null != channelService
                && null != providerService) {
            taskRegistrationThread.start();
        }
    }

    @Override
    public synchronized void unbind(Object service, Map properties) {
        if (service instanceof TaskService) {
            try {
                disableTasks();

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Disabled tasks from: %s", TASKS_DIR_PATTERN));
                }
            } catch (Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Can't disable tasks: ", e);
                }
            }

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("TaskService is unavailable");
            }
        }
    }

    private synchronized void disableTasks() throws IOException {
        for (Resource resource : getResources()) {
            JsonNode node = getTask(resource);

            Task task = getTask(node);
            task.setEnabled(false);

            taskService.save(task);

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(String.format("Disabled task with name: %s", task.getName()));
            }
        }
    }

    private List<Resource> getResources() {
        List<Resource> resources = new ArrayList<>();

        try {
            Collections.addAll(resources, resourcePatternResolver.getResources(TASKS_DIR_PATTERN));
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Can't get resources from: %s", TASKS_DIR_PATTERN), e);
            }
        }

        return resources;
    }

    private JsonNode getTask(Resource resource) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(resource.getInputStream(), writer);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Load a task definition");
        }

        return mapper.readTree(writer.toString());
    }

    private Task getTask(JsonNode node) {
        String id = node.get(ID_FIELD).getTextValue();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Get task with ID: %s", id));
        }

        return taskService.getTask(id);
    }

    private final class TaskRegistration implements Runnable {
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

        private final List<Resource> toLoad;

        private TaskRegistration() {
            this.toLoad = getResources();
        }

        @Override
        public void run() {
            while (toLoad.size() > 0) {
                for (int idx = toLoad.size() - 1; idx >= 0; --idx) {
                    Resource resource = toLoad.get(idx);
                    JsonNode node;

                    try {
                        node = getTask(resource);
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
            }
        }

        private boolean canImport(JsonNode node) {
            Set<Boolean> actionsExist = new HashSet<>();
            Set<Boolean> providersExist = new HashSet<>();
            boolean triggerExist = existsChannel(getTriggerFieldValue(node, MODULE_NAME_FIELD));

            for (JsonNode action : node.get(ACTIONS_FIELD)) {
                actionsExist.add(existsChannel(getFieldValue(action, MODULE_NAME_FIELD)));
            }

            for (JsonNode step : node.get(TASK_CONFIG_FIELD).get(STEPS_FIELD)) {
                if (DATA_SOURCE.equalsIgnoreCase(getFieldValue(step, TYPE_FIELD))) {
                    providersExist.add(existsProvider(getFieldValue(step, PROVIDER_NAME_FIELD)));
                }
            }

            return triggerExist && !actionsExist.contains(false) && !providersExist.contains(false);
        }

        private void registerTask(JsonNode node) {
            String taskName = node.get(NAME_FIELD).getTextValue();

            try {
                taskService.importTask(node.toString());
                handler.registerHandlerFor(getTriggerFieldValue(node, SUBJECT_FIELD));

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
            return channelService.getChannel(moduleName) != null;
        }

        private boolean existsProvider(String providerName) {
            return providerService.getProvider(providerName) != null;
        }

    }

}
