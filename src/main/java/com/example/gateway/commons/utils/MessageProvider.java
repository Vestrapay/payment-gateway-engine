package com.example.gateway.commons.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageProvider {

    private final MessageSource messageSource;

    private String getMessageByKey(String messageKey, String... params) {
        return messageSource.getMessage(messageKey,
                params,
                LocaleContextHolder.getLocale());
    }

    public String getMessage(String messageKey) {
        return getMessageByKey(messageKey);
    }

    public String getMessage(String messageKey, String... params) {
        return getMessageByKey(messageKey, params);
    }

    public String getNotFoundMessage(String entity, String id) {
        return getMessage("not.found.error", entity, id);
    }

    public String getSuccessMessage() {
        return getMessage("response.success");
    }

    public String getInternalErrorMessage() {
        return getMessage("response.internal.error");
    }

    public String getSystemError() {
        return getMessage("system.error");
    }

    public String getErrorOccurredMessage() {
        return getMessage("error.occurred");
    }

    public String getServerError() {
        return getMessage("response.server.error");
    }

    public String getAPICommError() {
        return getMessage("api.communication.error");
    }

    public String getAccountCreationSuccess() {
        return getMessage("account.creation.success");
    }
}
