/*
 * Copyright (c) 2008-2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * Author: Alexander Budarov
 * Created: 22.02.2010 15:17:06
 * $Id$
 */
package com.haulmont.cuba.gui.components.validators;

import com.haulmont.cuba.core.global.MessageUtils;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.ValidationException;
import org.apache.commons.lang.ObjectUtils;
import org.dom4j.Element;

import java.text.ParseException;

public class LongValidator implements Field.Validator {

    protected String message;
    protected String messagesPack;
    protected String onlyPositive;

    private static final long serialVersionUID = -1312187225804302418L;

    public LongValidator(Element element, String messagesPack) {
        message = element.attributeValue("message");
        onlyPositive = element.attributeValue("onlyPositive");
        this.messagesPack = messagesPack;
    }

    public LongValidator(String message) {
        this.message = message;
    }

    private boolean checkPositive(Long value) {
        return !ObjectUtils.equals("true", onlyPositive) || value != null && value >= 0;
    }

    public void validate(Object value) throws ValidationException {
        boolean result;
        if (value instanceof String) {
            try {
                Number num = ValidationHelper.parseNumber((String) value, MessageUtils.getLongFormat());
                result = checkPositive(num.longValue());
            }
            catch (ParseException e) {
                result = false;
            }
        }
        else {
            result = value instanceof Long && checkPositive((Long) value);
        }
        if (!result) {
            String msg = message != null ? MessageUtils.loadString(messagesPack, message) : "Invalid value '%s'";
            throw new ValidationException(String.format(msg, value));
        }
    }
}
