/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.ScreenProfiler;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.auth.RequestContext;
import com.haulmont.cuba.web.toolkit.ui.CubaFileUpload;
import com.vaadin.server.*;
import com.vaadin.server.communication.*;
import com.vaadin.ui.UI;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CubaVaadinServletService extends VaadinServletService {

    private Log log = LogFactory.getLog(CubaVaadinServletService.class);

    protected WebConfig webConfig;

    protected final String webResourceTimestamp;

    public CubaVaadinServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        super(servlet, deploymentConfiguration);

        Configuration configuration = AppBeans.get(Configuration.NAME);
        webConfig = configuration.getConfig(WebConfig.class);

        ServletContext sc = servlet.getServletContext();
        String resourcesTimestamp = sc.getInitParameter("webResourcesTs");
        if (StringUtils.isNotEmpty(resourcesTimestamp)) {
            this.webResourceTimestamp = resourcesTimestamp;
        } else {
            this.webResourceTimestamp = "DEBUG";
        }

        addSessionInitListener(new SessionInitListener() {
            @Override
            public void sessionInit(SessionInitEvent event) throws ServiceException {
                event.getSession().getSession().setMaxInactiveInterval(webConfig.getHttpSessionExpirationTimeoutSec());
                log.debug("HTTP session " + event.getSession() + " initialized, timeout="
                        + event.getSession().getSession().getMaxInactiveInterval() + "sec");
            }
        });

        addSessionDestroyListener(new SessionDestroyListener() {
            @Override
            public void sessionDestroy(SessionDestroyEvent event) {
                log.debug("HTTP session destroyed: " + event.getSession());
                App app = event.getSession().getAttribute(App.class);
                if (app != null) {
                    app.cleanupBackgroundTasks();
                }
            }
        });

        setSystemMessagesProvider(new SystemMessagesProvider() {
            @Override
            public SystemMessages getSystemMessages(SystemMessagesInfo systemMessagesInfo) {
                Locale locale = systemMessagesInfo.getLocale();

                CustomizedSystemMessages msgs = new CustomizedSystemMessages();

                if (AppContext.isStarted()) {
                    try {
                        Messages messages = AppBeans.get(Messages.NAME);

                        msgs.setInternalErrorCaption(messages.getMainMessage("internalErrorCaption", locale));
                        msgs.setInternalErrorMessage(messages.getMainMessage("internalErrorMessage", locale));

                        msgs.setCommunicationErrorCaption(messages.getMainMessage("communicationErrorCaption", locale));
                        msgs.setCommunicationErrorMessage(messages.getMainMessage("communicationErrorMessage", locale));

                        msgs.setSessionExpiredCaption(messages.getMainMessage("sessionExpiredErrorCaption", locale));
                        msgs.setSessionExpiredMessage(messages.getMainMessage("sessionExpiredErrorMessage", locale));
                    } catch (Exception e) {
                        log.error("Unable to set system messages", e);
                        throw new RuntimeException("Unable to set system messages. " +
                                "It usually happens when the middleware web application is not responding due to " +
                                "errors on start. See logs for details.", e);
                    }
                }

                String redirectUri;
                if (RequestContext.get() != null) {
                    HttpServletRequest request = RequestContext.get().getRequest();
                    redirectUri = StringUtils.replace(request.getRequestURI(), "/UIDL", "");
                } else {
                    String webContext = AppContext.getProperty("cuba.webContextName");
                    redirectUri = "/" + webContext;
                }

                msgs.setInternalErrorURL(redirectUri + "?restartApp");

                return msgs;
            }
        });
    }

    @Override
    public String getConfiguredTheme(VaadinRequest request) {
        return webConfig.getAppWindowTheme();
    }

    @Override
    public String getApplicationVersion() {
        return webResourceTimestamp;
    }

    @Override
    protected List<RequestHandler> createRequestHandlers() throws ServiceException {
        List<RequestHandler> requestHandlers = super.createRequestHandlers();

        List<RequestHandler> cubaRequestHandlers = new ArrayList<>();

        for (RequestHandler handler : requestHandlers) {
            if (handler instanceof ServletUIInitHandler) {
                cubaRequestHandlers.add(new CubaServletUiInitHandler());
            } else if (handler instanceof UidlRequestHandler) {
                // replace UidlRequestHandler with CubaUidlRequestHandler
                cubaRequestHandlers.add(new CubaUidlRequestHandler());
            } else if (handler instanceof PublishedFileHandler) {
                // replace PublishedFileHandler with CubaPublishedFileHandler
                // for support resources from VAADIN directory
                cubaRequestHandlers.add(new CubaPublishedFileHandler());
            } else if (handler instanceof ServletBootstrapHandler) {
                // replace ServletBootstrapHandler with CubaApplicationBootstrapHandler
                cubaRequestHandlers.add(new CubaApplicationBootstrapHandler());
            } else if (handler instanceof HeartbeatHandler) {
                // replace HeartbeatHandler with CubaHeartbeatHandler
                cubaRequestHandlers.add(new CubaHeartbeatHandler());
            } else if (handler instanceof FileUploadHandler) {
                // add support for jquery file upload
                cubaRequestHandlers.add(handler);
                cubaRequestHandlers.add(new CubaFileUploadHandler());
            } else if (handler instanceof ConnectorResourceHandler) {
                cubaRequestHandlers.add(new CubaConnectorResourceHandler());
            } else {
                cubaRequestHandlers.add(handler);
            }
        }

        return cubaRequestHandlers;
    }

    protected static boolean withUserSession(VaadinSession session, RequestHandlerAction<Boolean> handler) throws IOException {
        UserSession userSession = session.getAttribute(UserSession.class);
        if (userSession != null) {
            AppContext.setSecurityContext(new SecurityContext(userSession));
        }

        try {
            return handler.handle();
        } finally {
            AppContext.setSecurityContext(null);
        }
    }

    // Add ability to load JS and CSS resources from VAADIN directory
    protected static class CubaPublishedFileHandler extends PublishedFileHandler {
        @Override
        protected InputStream getApplicationResourceAsStream(Class<?> contextClass, String fileName) {
            return VaadinServlet.getCurrent().getServletContext().getResourceAsStream("/VAADIN/" + fileName);
        }
    }

    // Add support for CubaFileUpload component with XHR upload mechanism
    protected static class CubaFileUploadHandler extends FileUploadHandler {

        private Log log = LogFactory.getLog(CubaFileUploadHandler.class);

        @Override
        protected boolean isSuitableUploadComponent(ClientConnector source) {
            if (!(source instanceof CubaFileUpload)) {
                // this is not jquery upload request
                return false;
            }

            log.trace("Uploading file using jquery file upload mechanism");

            return true;
        }

        @Override
        protected void sendUploadResponse(VaadinRequest request, VaadinResponse response,
                                          String fileName, long contentLength) throws IOException {
            JsonArray json = Json.createArray();
            JsonObject fileInfo = Json.createObject();
            fileInfo.put("name", fileName);
            fileInfo.put("size", contentLength);

            // just fake addresses and parameters
            fileInfo.put("url", fileName);
            fileInfo.put("thumbnail_url", fileName);
            fileInfo.put("delete_url", fileName);
            fileInfo.put("delete_type", "POST");
            json.set(0, fileInfo);

            PrintWriter writer = response.getWriter();
            writer.write(json.toJson());
            writer.close();
        }

        @Override
        public boolean handleRequest(final VaadinSession session, final VaadinRequest request, final VaadinResponse response)
                throws IOException {
            return withUserSession(session, new RequestHandlerAction<Boolean>() {
                @Override
                public Boolean handle() throws IOException {
                    return CubaFileUploadHandler.super.handleRequest(session, request, response);
                }
            });
        }
    }

    // Add ability to redirect to base application URL if we have unparsable path tail
    protected static class CubaApplicationBootstrapHandler extends ServletBootstrapHandler {

        @Override
        public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
                throws IOException {
            String requestPath = request.getPathInfo();

            // redirect to base URL if we have unparsable path tail
            if (!StringUtils.equals("/", requestPath)) {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", request.getContextPath());

                return true;
            }

            return super.handleRequest(session, request, response);
        }
    }

    // Add ability to handle hearbeats in App
    protected static class CubaHeartbeatHandler extends HeartbeatHandler {

        private Log log = LogFactory.getLog(CubaHeartbeatHandler.class);

        @Override
        public boolean synchronizedHandleRequest(final VaadinSession session, final VaadinRequest request, final VaadinResponse response)
                throws IOException {
            return withUserSession(session, new RequestHandlerAction<Boolean>() {
                @Override
                public Boolean handle() throws IOException {
                    boolean result = CubaHeartbeatHandler.super.synchronizedHandleRequest(session, request, response);

                    if (log.isTraceEnabled()) {
                        log.trace("Handle heartbeat " + request.getRemoteHost() + " " + request.getRemoteAddr());
                    }

                    if (result && App.isBound()) {
                        App.getInstance().onHeartbeat();
                    }

                    return result;
                }
            });
        }
    }

    // Set security context to AppContext for normal UI requests
    protected static class CubaUidlRequestHandler extends UidlRequestHandler {
        protected ScreenProfiler profiler = AppBeans.get(ScreenProfiler.NAME);

        @Override
        public boolean synchronizedHandleRequest(final VaadinSession session, final VaadinRequest request, final VaadinResponse response)
                throws IOException {
            return withUserSession(session, new RequestHandlerAction<Boolean>() {
                @Override
                public Boolean handle() throws IOException {
                    return CubaUidlRequestHandler.super.synchronizedHandleRequest(session, request, response);
                }
            });
        }

        @Override
        protected UidlWriter createUidlWriter() {
            return new UidlWriter() {
                @Override
                protected void writePerformanceData(UI ui, Writer writer) throws IOException {
                    super.writePerformanceData(ui, writer);

                    String profilerMarker = profiler.getCurrentProfilerMarker(ui);
                    if (profilerMarker != null) {
                        profiler.setCurrentProfilerMarker(ui, null);
                        long lastRequestTimestamp = ui.getSession().getLastRequestTimestamp();
                        writer.write(String.format(", \"profilerMarker\": \"%s\", \"profilerEventTs\": \"%s\", \"profilerServerTime\": %s",
                                profilerMarker, lastRequestTimestamp, System.currentTimeMillis() - lastRequestTimestamp));
                    }
                }
            };
        }
    }

    // Set security context to AppContext for UI init requests
    protected static class CubaServletUiInitHandler extends ServletUIInitHandler {
        @Override
        public boolean synchronizedHandleRequest(final VaadinSession session, final VaadinRequest request, final VaadinResponse response)
                throws IOException {
            return withUserSession(session, new RequestHandlerAction<Boolean>() {
                @Override
                public Boolean handle() throws IOException {
                    return CubaServletUiInitHandler.super.synchronizedHandleRequest(session, request, response);
                }
            });
        }
    }

    // Set security context to AppContext for connector resources
    protected static class CubaConnectorResourceHandler extends ConnectorResourceHandler {
        @Override
        public boolean handleRequest(final VaadinSession session, final VaadinRequest request, final VaadinResponse response)
                throws IOException {
            return withUserSession(session, new RequestHandlerAction<Boolean>() {
                @Override
                public Boolean handle() throws IOException {
                    return CubaConnectorResourceHandler.super.handleRequest(session, request, response);
                }
            });
        }
    }

    protected interface RequestHandlerAction<T> {
        T handle() throws IOException;
    }
}