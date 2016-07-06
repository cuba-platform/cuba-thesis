/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.communication;

import com.haulmont.cuba.web.toolkit.ui.client.profiler.ScreenClientProfiler;
import com.vaadin.client.ValueMap;
import com.vaadin.client.communication.MessageHandler;

public class CubaMessageHandler extends MessageHandler {

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
}