package com.pawelniewiadomski.devs.jira.engine;

import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ProgressModel {
	private final List<String> messages = Lists.newArrayList();

	private final List<String> issueKeys = Lists.newArrayList();

	private boolean running;

	private int currentRow;

	private int rowsToImport;

	public void addMessage(@Nonnull String msg) {
		messages.add(msg);
	}

	public void addIssueKey(@Nonnull String id) {
		issueKeys.add(id);
	}

	public ProgressModel(int rowsToImport) {
		this.rowsToImport = rowsToImport;
		this.currentRow = 0;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void increaseRow() {
		++this.currentRow;
	}
}
