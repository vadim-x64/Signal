package com.test.myproject.signal.data;

import java.util.ArrayList;
import java.util.List;

public class SessionData {
    private static SessionData instance;
    private final List<TestResult> history;

    private SessionData() {
        history = new ArrayList<>();
    }

    public static SessionData getInstance() {
        if (instance == null) {
            instance = new SessionData();
        }
        return instance;
    }

    public void addResult(TestResult result) {
        history.add(result);
    }

    public List<TestResult> getHistory() {
        return history;
    }
}