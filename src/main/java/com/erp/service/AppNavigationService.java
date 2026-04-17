package com.erp.service;

import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class AppNavigationService {

    public enum Destination {
        VENDAS,
        ORCAMENTOS
    }

    private final Map<Destination, Runnable> handlers = new EnumMap<>(Destination.class);

    public void register(Destination destination, Runnable handler) {
        if (destination != null && handler != null) {
            handlers.put(destination, handler);
        }
    }

    public void navigateTo(Destination destination) {
        Runnable handler = handlers.get(destination);
        if (handler == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            handler.run();
        } else {
            Platform.runLater(handler);
        }
    }
}
