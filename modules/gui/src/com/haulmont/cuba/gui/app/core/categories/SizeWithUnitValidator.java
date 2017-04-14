/*
 * Copyright (c) 2008-2017 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.core.categories;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.ValidationException;
import org.dom4j.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SizeWithUnitValidator implements Field.Validator {
    public static final String SIZE_PATTERN = "^(-?\\d+(?:\\.\\d+)?)(%|px)?$";
    public static final int MAX_SIZE_PERCENTS = 100;
    public static final int MAX_SIZE_PIXELS = 1920;

    protected String messagesPack;
    protected String message;

    public SizeWithUnitValidator(Element element, String messagesPack) {
        this.message = element.attributeValue("message");
        this.messagesPack = messagesPack;
    }

    public String getMessagesPack() {
        return messagesPack;
    }

    public void setMessagesPack(String messagesPack) {
        this.messagesPack = messagesPack;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void validate(Object value) throws ValidationException {
        if (value == null) {
            return;
        }

        MessageTools messages = AppBeans.get(MessageTools.NAME);

        if (!(value instanceof String)) {
            throw new ValidationException(messages.loadString(messagesPack, message));
        }

        String s = (String) value;

        s = s.trim();
        if ("".equals(value)) {
            return;
        }

        Matcher matcher = Pattern.compile(SIZE_PATTERN).matcher(s);
        if (!matcher.find()) {
            throw new ValidationException(messages.loadString(messagesPack, message));
        }

        double size = Double.parseDouble(matcher.group(1));

        String symbol = matcher.group(2);
        if ("%".equals(symbol)) {
            if (size < 1 || size > MAX_SIZE_PERCENTS) {
                throw new ValidationException(messages.loadString(messagesPack, message));
            }
        } else if ("px".equals(symbol) || symbol == null) {
            if (size < 1 || size > MAX_SIZE_PIXELS) {
                throw new ValidationException(messages.loadString(messagesPack, message));
            }
        }
    }
}