package com.pawelniewiadomski.devs.jira.engine;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;

import java.util.concurrent.Callable;

public class EngineCallable implements Callable<Void> {
	private final Engine engine;
	private final User user;

	public EngineCallable(Engine engine, User user) {
		this.engine = engine;
		this.user = user;
	}

	@Override
	public Void call() throws Exception {
		ComponentManager.getInstance().getJiraAuthenticationContext().setLoggedInUser(user);
		engine.execute();
		return null;
	}

}
