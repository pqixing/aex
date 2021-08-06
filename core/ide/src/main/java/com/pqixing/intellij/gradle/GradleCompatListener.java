package com.pqixing.intellij.gradle;

import com.intellij.build.BuildEventDispatcher;
import com.intellij.build.BuildViewManager;
import com.intellij.build.DefaultBuildDescriptor;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FailureResult;
import com.intellij.build.events.impl.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemEventDispatcher;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

import static com.intellij.openapi.externalSystem.util.ExternalSystemUtil.convert;
import static java.util.concurrent.TimeUnit.SECONDS;

public class GradleCompatListener extends ExternalSystemTaskNotificationListenerAdapter {

    private String executionName;
    private BuildEventDispatcher myBuildEventDispatcher;
    private BuildViewManager buildViewManager;

    private GradleCompatListener() {

    }

    private GradleCompatListener(Project myProject, ExternalSystemTaskId myTaskId, String executionName) {
        this.executionName = executionName;

        buildViewManager = ServiceManager.getService(myProject, BuildViewManager.class);
        // This is resource is closed when onEnd is called or an exception is generated in this function bSee b/70299236.
        // We need to keep this resource open since closing it causes BuildOutputInstantReaderImpl.myThread to stop, preventing parsers to run.
        //noinspection resource, IOResourceOpenedButNotSafelyClosed
        myBuildEventDispatcher = new ExternalSystemEventDispatcher(myTaskId, buildViewManager);
    }


    @Override
    public void onStart(@NotNull ExternalSystemTaskId id, String workingDir) {

        long eventTime = System.currentTimeMillis();
        StartBuildEventImpl event = new StartBuildEventImpl(new DefaultBuildDescriptor(id, executionName, workingDir, eventTime),
                "running...");
        myBuildEventDispatcher.onEvent(id, event);
    }

    @Override
    public void onStatusChange(@NotNull ExternalSystemTaskNotificationEvent event) {
        if (event instanceof ExternalSystemBuildEvent) {
            BuildEvent buildEvent = ((ExternalSystemBuildEvent) event).getBuildEvent();
            myBuildEventDispatcher.onEvent(event.getId(), buildEvent);
        } else if (event instanceof ExternalSystemTaskExecutionEvent) {
            BuildEvent buildEvent = convert(((ExternalSystemTaskExecutionEvent) event));
            myBuildEventDispatcher.onEvent(event.getId(), buildEvent);
        }
    }

    @Override
    public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
        myBuildEventDispatcher.setStdOut(stdOut);
        myBuildEventDispatcher.append(text);
    }

    @Override
    public void onEnd(@NotNull ExternalSystemTaskId id) {
        CountDownLatch eventDispatcherFinished = new CountDownLatch(1);
        myBuildEventDispatcher.invokeOnCompletion((t) -> {
            eventDispatcherFinished.countDown();
        });
        myBuildEventDispatcher.close();

        // The underlying output parsers are closed asynchronously. Wait for completion in tests.
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            try {
                eventDispatcherFinished.await(10, SECONDS);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Timeout waiting for event dispatcher to finish.", ex);
            }
        }
    }

    @Override
    public void onSuccess(@NotNull ExternalSystemTaskId id) {
        FinishBuildEventImpl event = new FinishBuildEventImpl(id, null, System.currentTimeMillis(), "finished",
                new SuccessResultImpl());
        myBuildEventDispatcher.onEvent(id, event);
    }

    @Override
    public void onFailure(@NotNull ExternalSystemTaskId id, @NotNull Exception e) {
        String title = executionName + " failed";
        try {
            FailureResult failureResult = new FailureResultImpl(e);
            myBuildEventDispatcher.onEvent(id, new FinishBuildEventImpl(id, null, System.currentTimeMillis(), "failed", failureResult));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void onCancel(@NotNull ExternalSystemTaskId id) {
        super.onCancel(id);
        // Cause build view to show as skipped all pending tasks (b/73397414)
        FinishBuildEventImpl event = new FinishBuildEventImpl(id, null, System.currentTimeMillis(), "cancelled", new SkippedResultImpl());
        myBuildEventDispatcher.onEvent(id, event);
        myBuildEventDispatcher.close();
    }


    public static ExternalSystemTaskNotificationListener createTaskListener(Project myProject, ExternalSystemTaskId myTaskId, String executionName) {
        try {
            return new GradleCompatListener(myProject, myTaskId, executionName);
        } catch (Exception exception) {
            return null;
        }
    }
}
