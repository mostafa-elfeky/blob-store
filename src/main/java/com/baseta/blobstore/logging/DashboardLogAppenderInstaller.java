package com.baseta.blobstore.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardLogAppenderInstaller {

    private static final String APPENDER_NAME = "DASHBOARD_IN_MEMORY";

    private final InMemoryLogStore logStore;

    @PostConstruct
    void install() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(APPENDER_NAME) != null) {
            return;
        }

        DashboardLogAppender appender = new DashboardLogAppender(logStore);
        appender.setContext(loggerContext);
        appender.setName(APPENDER_NAME);
        appender.start();
        rootLogger.addAppender(appender);
    }

    @PreDestroy
    void uninstall() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(APPENDER_NAME) != null) {
            rootLogger.detachAppender(APPENDER_NAME);
        }
    }

    private static final class DashboardLogAppender extends AppenderBase<ILoggingEvent> {

        private final InMemoryLogStore logStore;

        private DashboardLogAppender(InMemoryLogStore logStore) {
            this.logStore = logStore;
        }

        @Override
        protected void append(ILoggingEvent event) {
            String message = event.getFormattedMessage();
            if (event.getThrowableProxy() != null) {
                message = message + " | " + event.getThrowableProxy().getClassName() + ": " + event.getThrowableProxy().getMessage();
            }
            logStore.add(
                    event.getTimeStamp(),
                    event.getLevel().toString(),
                    event.getLoggerName(),
                    message
            );
        }
    }
}
