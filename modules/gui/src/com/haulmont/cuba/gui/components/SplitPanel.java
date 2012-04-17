/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 20.01.2009 17:22:42
 * $Id$
 */
package com.haulmont.cuba.gui.components;

public interface SplitPanel extends Component.Container, Component.Expandable, Component.BelongToFrame {

    String NAME = "split";

    public static int ORIENTATION_VERTICAL = 0;
    public static int ORIENTATION_HORIZONTAL = 1;

    int getOrientation();
    void setOrientation(int orientation);

    void setSplitPosition(int pos);

    void setLocked(boolean locked);
    boolean isLocked();

    void setPositionUpdateListener(PositionUpdateListener positionListener);
    PositionUpdateListener getPositionUpdateListener();

    interface PositionUpdateListener {
        void updatePosition(int previousPosition, int newPosition);
    }
}
