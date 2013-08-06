package com.pawelniewiadomski.devs.jira.rest;

import com.pawelniewiadomski.devs.jira.engine.Engine;
import com.pawelniewiadomski.devs.jira.engine.EngineManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("progress")
public class ProgressResource {

	private final EngineManager engineManager;

	public ProgressResource(EngineManager engineManager) {
		this.engineManager = engineManager;
	}

	@GET
	@Path("/{eid}")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getProgress(@PathParam("eid") String eid) {
		final Engine engine = engineManager.getEngine(eid);
		if (engine == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		return Response.ok(engine.getProgress()).cacheControl(cc).build();
	}
}
