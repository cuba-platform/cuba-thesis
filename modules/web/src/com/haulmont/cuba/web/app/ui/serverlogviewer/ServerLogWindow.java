/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.app.ui.serverlogviewer;

import com.haulmont.cuba.core.app.LogManagerService;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.BoxLayout;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.OptionsField;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.gui.export.SimpleFileDataProvider;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.*;

/**
 * @author shatokhin
 * @version $Id$
 */
public class ServerLogWindow extends AbstractWindow {

    @Inject
    private LogManagerService logManagerService;

    @Inject
    private BoxLayout logFieldBox;

    @Inject
    private OptionsField logFileNamesField;

    @Inject
    private OptionsField levelField;

    @Inject
    private OptionsField logNameField;

    @Inject
    private CheckBox autoRefreshCheck;

    private Map<String, Object> map;

    private final Label vLabel = new Label();
    private final Panel panel = new Panel();

    @Override
    public void init(Map<String, Object> params) {
        logFileNamesField.setOptionsList(logManagerService.getLogFileNames());

        panel.setSizeFull();
//        vaadin7
        VerticalLayout panelContent = new VerticalLayout();
        panel.setContent(panelContent);
//        panel.setScrollable(true);

        AbstractOrderedLayout vBox = (AbstractOrderedLayout) WebComponentsHelper.unwrap(logFieldBox);
        panelContent.addComponent(vLabel);
        vBox.addComponent(panel);
        vLabel.setReadOnly(true);
        vLabel.setSizeFull();
        vLabel.setContentMode(ContentMode.HTML);

        levelField.setOptionsList(getAllLevels());

        logNameField.setOptionsList(getLogs());

        initTimers();

        map = new HashMap<>();
        map.put("winLog", this);

        logNameField.addListener(new ValueListener<Object>() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (value != null) {
                    if (value.equals(getLogs().get(0))) {
                        openWindow("additionLogger", WindowManager.OpenType.DIALOG, map);
                        logNameField.setValue(null);
                        levelField.setValue(null);
                    }
                }
            }
        });
    }

    public void showTail() {
        fillingTextArea();
    }

    public void getLevel() {
        if (logNameField.getValue() != null && logNameField.getValue().toString().trim().length() != 0) {
            Level level = logManagerService.getLogLevel(logNameField.<String>getValue());
            if (level != null)
                levelField.setValue(level);
        } else {
            logNameField.setValue(null);
            showNotification(getMessage("noRegName"), NotificationType.HUMANIZED);
        }
    }

    public void setLevel() {
        if (logNameField.getValue() != null && logNameField.getValue().toString().trim().length() != 0) {
            if (levelField.getValue() != null) {
                String logName = logNameField.getValue();
                Level level = levelField.getValue();
                logManagerService.setLogLevel(logName, level);
                showNotification(String.format(getMessage("logSetMessage"), logName, level.toString()), NotificationType.HUMANIZED);
            } else
                showNotification(getMessage("noSelectedLevel"), NotificationType.HUMANIZED);
        } else {
            logNameField.setValue(null);
            showNotification(getMessage("noRegName"), NotificationType.HUMANIZED);
        }
    }

    public void download() {
        String fileName = logFileNamesField.getValue();
        String zipName = logManagerService.packLog(fileName);
        if (zipName != null) {
            AppConfig.createExportDisplay(this).show(new SimpleFileDataProvider(zipName), fileName + ".zip");
            logManagerService.deleteTempFile(zipName);
        } else
            showNotification(getMessage("fileDownload"), NotificationType.HUMANIZED);
    }

    private void initTimers() {
//        vaadin7
//        Timer timer = getTimer("timer");
//
//        timer.addTimerListener(new Timer.TimerListener() {
//            @Override
//            public void onTimer(Timer timer) {
//                boolean flag = (Boolean.parseBoolean(autoRefreshCheck.getValue().toString()));
//                if (flag) {
//                    fillingTextArea();
//                }
//            }
//
//            @Override
//            public void onStopTimer(Timer timer) {
//            }
//        });
    }

    private void fillingTextArea() {
        vLabel.setReadOnly(false);

        if (logFileNamesField.getValue() != null) {
            String selectValue = logFileNamesField.getValue();
            String value = logManagerService.getTail(selectValue);
            value = StringUtils.replace(value, " ", "&nbsp;");
            vLabel.setValue(value);
        } else
            showNotification(getMessage("noSelectedLog"), NotificationType.HUMANIZED);

        int scrollPos = panel.getScrollTop() + 30000;
        panel.setScrollTop(scrollPos);

    }

    public static List<Level> getAllLevels() {
        List<Level> levelList = new ArrayList<>();
        levelList.add(Level.TRACE);
        levelList.add(Level.DEBUG);
        levelList.add(Level.INFO);
        levelList.add(Level.WARN);
        levelList.add(Level.ERROR);
        levelList.add(Level.FATAL);
        return levelList;
    }

    private List<String> getLogs() {
        List<Logger> listLogs = Collections.list(LogManager.getCurrentLoggers());
        List<String> listNameLogs = new ArrayList<>();
        for (Logger logger : listLogs) {
            if (logger.getLevel() != null) {
                listNameLogs.add(logger.getName());
            }
        }
        Collections.sort(listNameLogs);
        listNameLogs.add(0, getMessage("newLogger"));
        return listNameLogs;
    }

    public void refreshLogs() {
        logNameField.setOptionsList(getLogs());
    }

    public void callControlLogger() {
        App.getInstance().getWindowManager().getDialogParams().setWidth(420);
        Map<String, Object> map = new HashMap<>();
        map.put("controlWin", this);
        openWindow("controlLogger", WindowManager.OpenType.DIALOG, map);
    }
}