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

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final TaskRegistration taskRegistrationThread;

    @Autowired
    public ManagementTasks(BundleContext context) {
        this(new OsgiBundleResourcePatternResolver(context.getBundle()));
    }

    public ManagementTasks(ResourcePatternResolver resourcePatternResolver) {
        this.mapper = new ObjectMapper();
        this.resourcePatternResolver = resourcePatternResolver;
        this.taskRegistrationThread = new TaskRegistration(this);

        LOGGER.trace("Created instance of ManagementTasks class");
    }

    @Override
    public synchronized void bind(Object service, Map properties) {
        if (service instanceof TriggerHandler) {
            handler = (TriggerHandler) service;
            traceBind(TriggerHandler.class);
        } else if (service instanceof TaskService) {
            taskService = (TaskService) service;
            traceBind(TaskService.class);
        } else if (service instanceof ChannelService) {
            channelService = (ChannelService) service;
            traceBind(ChannelService.class);
        } else if (service instanceof TaskDataProviderService) {
            providerService = (TaskDataProviderService) service;
            traceBind(TaskDataProviderService.class);
        }

        if (null != handler && null != taskService && null != channelService
                && null != providerService) {
            LOGGER.info("All needed services became available. Run task registration thread.");
            new Thread(taskRegistrationThread).start();
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
                LOGGER.warn("TaskService became unavailable");
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (null != taskRegistrationThread && taskRegistrationThread.continueWork()) {
            LOGGER.info("Disable task registration thread.");
            taskRegistrationThread.setWork(false);
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

    List<Resource> getResources() {
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

    JsonNode getTask(Resource resource) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(resource.getInputStream(), writer);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Load a task definition: " + resource);
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

    TriggerHandler getHandler() {
        return handler;
    }

    TaskService getTaskService() {
        return taskService;
    }

    ChannelService getChannelService() {
        return channelService;
    }

    TaskDataProviderService getProviderService() {
        return providerService;
    }

    private void traceBind(Class clazz) {
        LOGGER.trace("Service " + clazz.getName() + " became available");
    }
}
