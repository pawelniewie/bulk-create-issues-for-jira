package com.pawelniewiadomski.devs.jira.webwork;

import com.pawelniewiadomski.devs.jira.engine.EngineManager;

public class TrackProgress extends Common {

	private final EngineManager engineManager;

	public TrackProgress(EngineManager engineManager) {
		this.engineManager = engineManager;
	}

	private String eid;

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	@Override
	protected String doExecute() throws Exception {
		if (!canAccessSelectedProject()) {
			return "denied";
		}

		if (engineManager.getEngine(eid) == null) {
			return getRedirect("/");
		}

		return SUCCESS;
	}
}
