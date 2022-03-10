package org.openlca.app.logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.openlca.app.rcp.Workspace;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

class Appenders {

	private Appenders() {
	}

	static ConsoleAppender<ILoggingEvent> createConsoleAppender() {
		var factory = LoggerFactory.getILoggerFactory();
		if (!(factory instanceof LoggerContext context))
			return null;

		var encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%d{HH:mm:ss} %level %logger{50} - %msg%n");
		encoder.start();

		var appender = new ConsoleAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("console");
		appender.setEncoder(encoder);
		appender.setTarget(ConsoleTarget.SystemOut.getName());
		appender.start();
		return appender;
	}

	static RollingFileAppender<ILoggingEvent> createHtmlRollingAppender() {

		var factory = LoggerFactory.getILoggerFactory();
		if (!(factory instanceof LoggerContext context))
			return null;

		var logDir = new File(Workspace.getDir(), "log");
		if (!logDir.exists()) {
			try {
				Files.createDirectories(logDir.toPath());
			} catch (IOException e) {
				var log = LoggerFactory.getLogger(Appenders.class);
				log.error("failed to create log-dir: " + logDir, e);
				return null;
			}
		}

		var appender = new RollingFileAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("html");

		var policy = new TimeBasedRollingPolicy<ILoggingEvent>();
		policy.setContext(context);
		policy.setFileNamePattern(
			logDir.getAbsolutePath() + "/log-%d{yyyy-MM-dd}.html");
		policy.setMaxHistory(3);
		policy.setTotalSizeCap(FileSize.valueOf("3MB"));
		policy.setParent(appender);
		policy.start();

		var layout = new HTMLLayout();
		layout.setContext(context);
		layout.setPattern("%d{HH:mm:ss}%level%logger%msg");
		layout.start();

		var encoder = new LayoutWrappingEncoder<ILoggingEvent>();
		encoder.setContext(context);
		encoder.setCharset(StandardCharsets.UTF_8);
		encoder.setLayout(layout);
		encoder.start();

		appender.setRollingPolicy(policy);
		appender.setEncoder(encoder);
		appender.setAppend(true);
		appender.start();

		return appender;
	}

}
