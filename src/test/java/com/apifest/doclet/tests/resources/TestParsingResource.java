package com.apifest.doclet.tests.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/{platform: komfo|sitecore}/{clientId}/followers")
public interface TestParsingResource {

    /**
     * @apifest.external /twitter/followers/metrics
     * @apifest.internal {platform}/{clientId}/followers/metrics
     * @apifest.scope twitter_followers
     * @apifest.auth.type client-app
     * @apifest.re.clientId \w[\w\s%]+(?<!\s)
     * @apifest.docs.group Twitter Followers
     * @return Response
     * @test this is any test annotation**
     * @apifest.docs.params.ids ** user ids goes here **
     * @apifest.docs.params.ids.type string
     * @apifest.docs.params.ids.required
     * @apifest.docs.params.fields ** The keys from result json can be added as filter**
     * @apifest.docs.params.fields.type list
     * @apifest.docs.params.fields.optional
     * @apifest.docs.results.channel The **channel** description
     * @apifest.docs.results.channel.type string
     * @apifest.docs.results.updated_time The **updated_time** description
     * @apifest.docs.results.updated_time.type string
     * @apifest.docs.results.request_handle  The **request_handle** description
     * @apifest.docs.results.request_handle.type string
     * @apifest.docs.results.sentiment_score.name  sentiment.score
     * @apifest.docs.results.sentiment_score.type string
     * @apifest.docs.results.sentiment_score The **sentiment_score** description
     * @apifest.docs.results.sentiment_positive.name  sentiment.positive
     * @apifest.docs.results.sentiment_positive.type string
     * @apifest.docs.results.sentiment_positive The **sentiment_positive** description
     * @apifest.docs.results.sentiment_neutral.name  sentiment.neutral
     * @apifest.docs.results.sentiment_neutral.type string
     * @apifest.docs.results.sentiment_neutral The **sentiment_neutral** description
     * @apifest.docs.results.sentiment_negative.name  sentiment.negative
     * @apifest.docs.results.sentiment_negative.type string
     * @apifest.docs.results.sentiment_negative The **sentiment_negative** description
     * @apifest.docs.results.engagement_replies.name  engagement.replies
     * @apifest.docs.results.engagement_replies.type integer
     * @apifest.docs.results.engagement_replies The **engagement_replies** description
     * @apifest.docs.results.engagement_tweets.name  engagement.tweets
     * @apifest.docs.results.engagement_tweets.type integer
     * @apifest.docs.results.engagement_tweets The **engagement_tweets** description
     * @apifest.docs.exceptions.invalid_parameter The parameter is invalid
     * @apifest.docs.exceptions.invalid_parameter.description Please add valid parameter
     * @apifest.docs.exceptions.invalid_parameter.code 400 
     * @apifest.docs.order 2
     */
    @Path("/metrics")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @CustomAnnotation(value = {"test", "test2"} )
    @Multiple(names = {"test", "test2"}, value = {2, 1} )
    Response getMetrics(@PathParam("clientId") String clientId, @QueryParam("ids") String ids, @QueryParam("fields") String fields, @QueryParam("limit") String limit);

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
     * @apifest.docs.group Twitter Followers
     * @apifest.docs.summary Short summary goes here
     * @apifest.docs.description Long description goes here Long description
     *                           goes hereLong description goes here Long
     *                           description goes hereLong description goes here
     *                           Long description goes hereLong description goes
     *                           here Long description goes hereLong description
     *                           goes here Long description goes hereLong
     *                           description goes here Long description goes
     *                           hereLong description goes here Long description
     *                           goes hereLong description goes here Long
     *                           description goes hereLong description goes here
     *                           Long description goes here
     * @apifest.docs.paramsDescription ** Parameter description is going here!**
     * @apifest.docs.params.ids ** user ids goes here **
     * @apifest.docs.params.ids.type string
     * @apifest.docs.params.ids.required
     * @apifest.docs.params.since ** since is optional parameter**
     * @apifest.docs.params.since.type integer
     * @apifest.docs.params.since.optional
     * @apifest.docs.params.until ** until is optional parameter**
     * @apifest.docs.params.until.type integer
     * @apifest.docs.params.until.optional
     * @apifest.docs.params.fields ** The keys from result json can be added as filter**
     * @apifest.docs.params.fields.type list
     * @apifest.docs.params.fields.optional
     * @apifest.docs.resultsDescription ** Result description is the best! **
     * @apifest.docs.results.tw_id The ** tw_id ** description
     * @apifest.docs.results.tw_id.type integer
     * @apifest.docs.results.request_handle The ** request_handle ** description
     * @apifest.docs.results.request_handle.type string
     * @apifest.docs.results.in_reply_to_screen_name The ** in_reply_to_screen_name ** description
     * @apifest.docs.results.in_reply_to_screen_name.type string
     * @apifest.docs.results.in_reply_to_status_id The ** in_reply_to_status_id ** description
     * @apifest.docs.results.in_reply_to_status_id.type string
     * @apifest.docs.results.channel The ** channel ** description
     * @apifest.docs.results.channel.type string
     * @apifest.docs.exceptions.invalid_since_until The period is invalid
     * @apifest.docs.exceptions.invalid_since_until.description Since/until parameter must be within the last 30 days
     * @apifest.docs.exceptions.invalid_since_until.code 400
     * @apifest.docs.order 1
     * @return Response
     * @test2 this is annotation for testing purpose #wrongtag
     * @@wrongtag
     */
    @Path("/stream")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getStream(@PathParam("clientId") String clientId, @QueryParam("ids") String ids, @QueryParam("fields") String fields, @QueryParam("since") String since,
            @QueryParam("until") String until, @QueryParam("limit") String limit);

}
