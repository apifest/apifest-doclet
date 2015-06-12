package com.apifest.doclet.tests.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/{platform: komfo|sitecore}/{clientId}/followers")
public interface ParsingResource {

    /**
     * @apifest.external /twitter/followers/metrics
     * @apifest.internal {platform}/{clientId}/followers/metrics
     * @apifest.scope twitter_followers
     * @apifest.auth.type client-app
     * @apifest.re.clientId \d+
     * @apifest.re.platform \S+
     * @apifest.docs.group Twitter Followers
     * @return Response
     * @test this is any test annotation
     */
    @Path("/metrics")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getMetrics(@PathParam("clientId") String clientId, @QueryParam("ids") String ids,
            @QueryParam("fields") String fields, @QueryParam("limit") String limit);

    @Path("/{endpoint: stream|metrics}")
    @PUT
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response incorrectMethod();

    /**
     * @apifest.external /twitter/followers/stream
     * @apifest.internal {platform}/{clientId}/followers/stream
     * @apifest.scope twitter_followers
     * @apifest.auth.type client-app
     * @apifest.re.clientId \d+
     * @apifest.re.platform \S+
     * @apifest.docs.group Twitter Followers
     * @apifest.docs.summary Short summary goes here
     * @apifest.docs.description Long description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes hereLong description goes here Long description goes here
     * @return Response
     * @test2 this is annotation for testing purpose
     * #wrongtag
     * @@wrongtag
     */
    @Path("/stream")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getStream(@PathParam("clientId") String clientId, @QueryParam("ids") String ids,
            @QueryParam("fields") String fields, @QueryParam("since") String since, @QueryParam("until") String until, @QueryParam("limit") String limit);

}
