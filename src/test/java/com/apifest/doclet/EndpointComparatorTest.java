package com.apifest.doclet;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apifest.api.MappingEndpointDocumentation;

public class EndpointComparatorTest
{
    private EndpointComparator comparator;
    private List<ParsedEndpoint> endpointList;

    @BeforeMethod
    public void initialize(){
        comparator = new EndpointComparator();
        endpointList = new ArrayList<ParsedEndpoint>();
        for (int i=0; i < 10; i++) {
            ParsedEndpoint endpoint = new ParsedEndpoint();
            MappingEndpointDocumentation med = new MappingEndpointDocumentation();
            med.setOrder(i*i - 12*i + 36);
            endpoint.setMappingEndpointDocumentation(med);
            endpointList.add(endpoint);
        }
    }

    @Test
    public void comparator_first_greater(){
        //GIVEN
        ParsedEndpoint endpoint = new ParsedEndpoint();
        MappingEndpointDocumentation med = new MappingEndpointDocumentation();
        med.setOrder(12);
        endpoint.setMappingEndpointDocumentation(med);
        ParsedEndpoint endpoint2 = new ParsedEndpoint();
        MappingEndpointDocumentation med2 = new MappingEndpointDocumentation();
        med2.setOrder(5);
        endpoint2.setMappingEndpointDocumentation(med2);
        //THEN
        Assert.assertTrue(comparator.compare(endpoint, endpoint2) > 0);
    }

    @Test
    public void comparator_first_lower(){
        //GIVEN
        ParsedEndpoint endpoint = new ParsedEndpoint();
        MappingEndpointDocumentation med = new MappingEndpointDocumentation();
        med.setOrder(5);
        endpoint.setMappingEndpointDocumentation(med);
        ParsedEndpoint endpoint2 = new ParsedEndpoint();
        MappingEndpointDocumentation med2= new MappingEndpointDocumentation();
        med2.setOrder(12);
        endpoint2.setMappingEndpointDocumentation(med2);
        //THEN
        Assert.assertTrue(comparator.compare(endpoint, endpoint2) < 0);
    }

    @Test
    public void ordering_test(){
        EndpointComparator.orderEndpoints(endpointList);
        for (int i = 0; i < endpointList.size() - 1; i++){
            Assert.assertTrue(endpointList.get(i).getMappingEndpointDocumentation().getOrder() <= endpointList.get(i+1).getMappingEndpointDocumentation().getOrder());

        }
    }
}
