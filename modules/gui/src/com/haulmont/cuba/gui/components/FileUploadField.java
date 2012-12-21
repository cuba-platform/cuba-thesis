/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 11.03.2009 17:42:22
 * $Id$
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.core.entity.FileDescriptor;

import java.util.UUID;

public interface FileUploadField
        extends Component, Component.HasCaption, Component.BelongToFrame
{
    String NAME = "upload";

    interface Listener {
        class Event {
            String filename;

            public Event(String filename) {
                this.filename = filename;
            }

            public String getFilename() {
                return filename;
            }
        }

        void uploadStarted(Event event);
        void uploadFinished(Event event);

        void uploadSucceeded(Event event);
        void uploadFailed(Event event);

        void updateProgress(long readBytes, long contentLength);
    }

    public class ListenerAdapter implements Listener {
        @Override
        public void uploadStarted(Event event) {
        }

        @Override
        public void uploadFinished(Event event) {
        }

        @Override
        public void uploadSucceeded(Event event) {
        }

        @Override
        public void uploadFailed(Event event) {
        }

        @Override
        public void updateProgress(long readBytes, long contentLength) {
        }
    }

    /**
     * Get id for uploaded file in {@link com.haulmont.cuba.gui.upload.FileUploading}
     * @return File Id
     */
    UUID getFileId();
    String getFileName();
    FileDescriptor getFileDescriptor();

    /**
     * Get content bytes for uploaded file
     * @return Bytes for uploaded file
     * @deprecated Please use {@link FileUploadField#getFileId()} method and {@link com.haulmont.cuba.gui.upload.FileUploading}
     */
    byte[] getBytes();

    void addListener(Listener listener);
    void removeListener(Listener listener);
}
