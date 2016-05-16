/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.haulmont.cuba.web.toolkit.ui.client.profiler.ScreenClientProfiler;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ValueMap;

/**
 * @author subbotin
 */
public class CubaApplicationConnection extends ApplicationConnection {

    protected int serverTimeOnClient;

    @Override
    protected void startHandlingJsonCommand(ValueMap json) {
        super.startHandlingJsonCommand(json);
        String profilerMarker = ScreenClientProfiler.getProfilerMarkerFromJson(json);
        ScreenClientProfiler profiler = ScreenClientProfiler.getInstance();
        if (profilerMarker != null) {
            profiler.setProfilerMarker(profilerMarker);
            profiler.setEnabled(true);
        } else {
            profiler.clearProfilerMarker();
        }
    }

    @Override
    protected void finishHandlingJsonCommand() {
        super.finishHandlingJsonCommand();
        ScreenClientProfiler profiler = ScreenClientProfiler.getInstance();
        String profilerMarker = profiler.getProfilerMarker();
        if (profilerMarker != null) {
            profiler.registerClientTime(profilerMarker, lastProcessingTime);
        }
        profiler.clearProfilerMarker();
    }

    @Override
    protected void beforeHandlingMessage(ValueMap json) {
        super.beforeHandlingMessage(json);
        ScreenClientProfiler profiler = ScreenClientProfiler.getInstance();
        String profilerMarker = ScreenClientProfiler.getProfilerMarkerFromJson(json);
        if (profilerMarker != null) {
            int serverTimeOnServer = ScreenClientProfiler.getServerTimeFromJson(json);
            if (serverTimeOnServer > 0) {
                profiler.registerServerTime(profilerMarker, serverTimeOnServer);
                profiler.registerNetworkTime(profilerMarker, serverTimeOnClient - serverTimeOnServer);
            } else {
                profiler.registerServerTime(profilerMarker, serverTimeOnClient);
            }
            profiler.registerEventTs(profilerMarker, ScreenClientProfiler.getEventTsFromJson(json));
        }
    }

    @Override
    public void handleOnResponseReceived(Request request, Response response) {
        super.handleOnResponseReceived(request, response);
        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            serverTimeOnClient = (int) (System.currentTimeMillis() - requestStartTime.getTime());
        }
    }
}
