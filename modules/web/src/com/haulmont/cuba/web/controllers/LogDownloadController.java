/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.controllers;

import com.haulmont.cuba.core.global.LogControl;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.core.sys.logging.LogArchiver;
import com.haulmont.cuba.core.sys.logging.LogFileNotFoundException;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * @author artamonov
 * @version $Id$
 */
@Controller
public class LogDownloadController {

    private final static Log log = LogFactory.getLog(LogDownloadController.class);

    @Inject
    protected LogControl logControl;

    @Inject
    protected UserSessionService userSessionService;

    @RequestMapping(value = "/log/{file:[a-zA-Z0-9\\.\\-_]+}", method = RequestMethod.GET)
    public void getLogFile(HttpServletResponse response,
                           @RequestParam(value = "s") String sessionId,
                           @RequestParam(value = "full", required = false) Boolean downloadFull,
                           @PathVariable(value = "file") String logFileName) throws IOException {
        UserSession userSession = getSession(sessionId, response);
        if (userSession == null)
            return;

        if (!userSession.isSpecificPermitted("cuba.gui.administration.downloadlogs")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // security check, handle only valid file name
        String filename = FilenameUtils.getName(logFileName);

        try {
            File logFile = logControl.getLogFile(filename);

            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Content-Type", "application/zip");
            response.setHeader("Pragma", "no-cache");

            response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".zip");

            OutputStream outputStream = null;
            try {
                outputStream = response.getOutputStream();

                if (BooleanUtils.isTrue(downloadFull)) {
                    LogArchiver.writeArchivedLogToStream(logFile, outputStream);
                } else {
                    LogArchiver.writeArchivedLogTailToStream(logFile, outputStream);
                }
            } catch (RuntimeException | IOException ex) {
                log.error("Unable to download file", ex);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                IOUtils.closeQuietly(outputStream);
            }

        } catch (LogFileNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected UserSession getSession(String sessionId, HttpServletResponse response) throws IOException {
        UUID sessionUUID;
        try {
            sessionUUID = UUID.fromString(sessionId);
        } catch (Exception e) {
            log.error("Error parsing sessionId from URL param", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        AppContext.setSecurityContext(new SecurityContext(sessionUUID));
        try {
            UserSession session = userSessionService.getUserSession(sessionUUID);
            if (session == null)
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return session;
        } finally {
            AppContext.setSecurityContext(null);
        }
    }
}