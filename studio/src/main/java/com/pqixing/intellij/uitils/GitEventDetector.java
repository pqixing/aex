package com.pqixing.intellij.uitils;

import com.intellij.openapi.util.Key;

import git4idea.commands.GitLineHandlerListener;

import java.util.ArrayList;
import java.util.List;

public class GitEventDetector implements GitLineHandlerListener {
    private boolean myHappened;
    private List<String> events = new ArrayList<>();

    public GitEventDetector(String... events) {
        for (String s : events) {
            this.events.add(s);
        }
    }

    public void detector(List<String> records) {
        for (String r : records) {
            onLineAvailable(r, null);
        }
    }

    @Override
    public void onLineAvailable(String s, Key key) {
        if (!myHappened) for (String e : events) {
            if (e.contains(s)) myHappened = true;
        }
    }

    public boolean hasHappened() {
        return myHappened;
    }
}
