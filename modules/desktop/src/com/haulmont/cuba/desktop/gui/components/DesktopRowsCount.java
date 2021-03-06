/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.desktop.sys.layout.LayoutAdapter;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.components.ListComponent;
import com.haulmont.cuba.gui.components.RowsCount;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenerAdapter;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DesktopRowsCount extends DesktopAbstractComponent<DesktopRowsCount.RowsCountComponent> implements RowsCount {

    protected CollectionDatasource datasource;
    protected boolean refreshing;
    protected State state;
    protected int start;
    protected int size;
    protected ListComponent owner;

    public DesktopRowsCount() {
        impl = new RowsCountComponent();
    }

    @Override
    public CollectionDatasource getDatasource() {
        return datasource;
    }

    @Override
    public void setDatasource(CollectionDatasource datasource) {
        this.datasource = datasource;
        if (datasource != null) {
            this.datasource.addListener(
                    new CollectionDsListenerAdapter<Entity>() {
                        @Override
                        public void collectionChanged(CollectionDatasource ds, Operation operation, java.util.List<Entity> items) {
                            onCollectionChanged();
                        }
                    }
            );
            impl.getCountButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onLinkClick();
                }
            });

            impl.getPrevButton().addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            onPrevClick();
                        }
                    }
            );
            impl.getNextButton().addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            onNextClick();
                        }
                    }
            );
            if (datasource.getState() == Datasource.State.VALID) {
                onCollectionChanged();
            }
        }
    }

    @Override
    public ListComponent getOwner() {
        return owner;
    }

    @Override
    public void setOwner(ListComponent owner) {
        this.owner = owner;
    }

    protected void onCollectionChanged() {
        if (datasource == null)
            return;

        String msgKey;
        size = datasource.size();
        start = 0;

        if (datasource instanceof CollectionDatasource.SupportsPaging) {
            CollectionDatasource.SupportsPaging ds = (CollectionDatasource.SupportsPaging) datasource;
            if ((size == 0 || size < ds.getMaxResults()) && ds.getFirstResult() == 0) {
                state = State.FIRST_COMPLETE;
            } else if (size == ds.getMaxResults() && ds.getFirstResult() == 0) {
                state = State.FIRST_INCOMPLETE;
            } else if (size == ds.getMaxResults() && ds.getFirstResult() > 0) {
                state = State.MIDDLE;
                start = ds.getFirstResult();
            } else if (size < ds.getMaxResults() && ds.getFirstResult() > 0) {
                state = State.LAST;
                start = ds.getFirstResult();
            } else
                state = State.FIRST_COMPLETE;
        } else {
            state = State.FIRST_COMPLETE;
        }

        String countValue;
        switch (state) {
            case FIRST_COMPLETE:
                impl.getCountButton().setVisible(false);
                impl.getPrevButton().setVisible(false);
                impl.getNextButton().setVisible(false);
                if (size % 100 > 10 && size % 100 < 20) {
                    msgKey = "table.rowsCount.msg2Plural1";
                } else {
                    switch (size % 10) {
                        case 1:
                            msgKey = "table.rowsCount.msg2Singular";
                            break;
                        case 2:
                        case 3:
                        case 4:
                            msgKey = "table.rowsCount.msg2Plural2";
                            break;
                        default:
                            msgKey = "table.rowsCount.msg2Plural1";
                    }
                }
                countValue = String.valueOf(size);
                break;
            case FIRST_INCOMPLETE:
                impl.getCountButton().setVisible(true);
                impl.getPrevButton().setVisible(false);
                impl.getNextButton().setVisible(true);
                msgKey = "table.rowsCount.msg1";
                countValue = "1-" + size;
                break;
            case MIDDLE:
                impl.getCountButton().setVisible(true);
                impl.getPrevButton().setVisible(true);
                impl.getNextButton().setVisible(true);
                msgKey = "table.rowsCount.msg1";
                countValue = (start + 1) + "-" + (start + size);
                break;
            case LAST:
                impl.getCountButton().setVisible(false);
                impl.getPrevButton().setVisible(true);
                impl.getNextButton().setVisible(false);
                msgKey = "table.rowsCount.msg2Plural2";
                countValue = (start + 1) + "-" + (start + size);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        String messagesPack = AppConfig.getMessagesPack();
        Messages messages = AppBeans.get(Messages.NAME);
        impl.getLabel().setText(messages.formatMessage(messagesPack, msgKey, countValue));

        if (impl.getCountButton().isVisible() && !refreshing) {
            impl.getCountButton().setText(messages.getMessage(messagesPack, "table.rowsCount.msg3"));
        }
        impl.repaint();
        impl.revalidate();
    }

    private void onLinkClick() {
        if (datasource == null || !(datasource instanceof CollectionDatasource.SupportsPaging))
            return;

        int count = ((CollectionDatasource.SupportsPaging) datasource).getCount();
        impl.getCountButton().setText(String.valueOf(count));
    }

    private void onNextClick() {
        if (!(datasource instanceof CollectionDatasource.SupportsPaging))
            return;

        CollectionDatasource.SupportsPaging ds = (CollectionDatasource.SupportsPaging) datasource;
        int firstResult = ds.getFirstResult();
        ds.setFirstResult(ds.getFirstResult() + ds.getMaxResults());
        refreshDatasource(ds);

        if (state.equals(State.LAST) && size == 0) {
            ds.setFirstResult(firstResult);
            int maxResults = ds.getMaxResults();
            ds.setMaxResults(maxResults + 1);
            refreshDatasource(ds);
            ds.setMaxResults(maxResults);
        }
        if (owner instanceof DesktopAbstractTable) {
            JXTable table = (JXTable) ((DesktopAbstractTable) owner).getComponent();
            table.scrollRowToVisible(0);
        }
    }

    private void onPrevClick() {
        if (!(datasource instanceof CollectionDatasource.SupportsPaging))
            return;

        CollectionDatasource.SupportsPaging ds = (CollectionDatasource.SupportsPaging) datasource;
        int newStart = ds.getFirstResult() - ds.getMaxResults();
        ds.setFirstResult(newStart < 0 ? 0 : newStart);
        refreshDatasource(ds);
        if (owner instanceof DesktopAbstractTable) {
            JXTable table = (JXTable) ((DesktopAbstractTable) owner).getComponent();
            table.scrollRowToVisible(0);
        }
    }

    private void refreshDatasource(CollectionDatasource.SupportsPaging ds) {
        refreshing = true;
        try {
            ds.refresh();
        } finally {
            refreshing = false;
        }
    }

    public static class RowsCountComponent extends JPanel {

        private JButton prevButton;
        private JButton nextButton;
        private JLabel label;
        private JButton countButton;
        private MigLayout layout;

        private final Dimension size = new Dimension(35, 25);

        public RowsCountComponent() {
            LC lc = new LC();
            lc.insetsAll("2");

            layout = new MigLayout(lc);
            if (LayoutAdapter.isDebug()) {
                lc.debug(1000);
            }
            setLayout(layout);

            prevButton = new JButton("<");
            add(prevButton);
            prevButton.setPreferredSize(size);
            prevButton.setMinimumSize(size);

            label = new JLabel();
            add(label);

            countButton = new JXHyperlink();
            countButton.setText("[?]");
            add(countButton);

            nextButton = new JButton(">");
            add(nextButton);
            nextButton.setPreferredSize(size);
            nextButton.setMinimumSize(size);
        }

        public JLabel getLabel() {
            return label;
        }

        public JButton getCountButton() {
            return countButton;
        }

        public JButton getPrevButton() {
            return prevButton;
        }

        public JButton getNextButton() {
            return nextButton;
        }
    }
}