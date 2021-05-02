package com.reddragon.customcachesample;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class SampleDebugTree extends Timber.DebugTree {
    @Override
    protected String createStackElementTag(@NotNull StackTraceElement element) {
        // Add log statements line number to the log
        return super.createStackElementTag(element) + " - " + element.getLineNumber();
    }
}

