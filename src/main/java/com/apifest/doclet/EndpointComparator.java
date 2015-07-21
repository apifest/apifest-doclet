package com.apifest.doclet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EndpointComparator implements Comparator<ParsedEndpoint>
{

    @Override
    public int compare(ParsedEndpoint o1, ParsedEndpoint o2)
    {
        return o1.getMappingEndpointDocumentation().getOrder() - o2.getMappingEndpointDocumentation().getOrder();
    }

    public static void orderEndpoints(List<ParsedEndpoint> parsedEndpoints)
    {
        EndpointComparator comparator = new EndpointComparator();
        Collections.sort(parsedEndpoints, comparator);
    }
}
