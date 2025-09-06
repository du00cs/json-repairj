package org.jsonrepairj.parser;

import lombok.Getter;

import java.util.*;

public class JsonContext {
    private List<ContextValue> context;
    @Getter
    private ContextValue current;

    public JsonContext() {
        this.context = new ArrayList<>();
        this.current = null;
    }

    /**
     * Set a new context value.
     *
     * @param value The context value to be added.
     */
    public void set(ContextValue value) {
        context.add(value);
        current = value;
    }

    /**
     * Remove the most recent context value.
     */
    public void reset() {
        try {
            context.remove(context.size() - 1);
            current = context.isEmpty() ? null : context.get(context.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            current = null;
        }
    }

    public boolean contains(ContextValue value) {
        return context.contains(value);
    }

    public boolean isEmpty() {
        return this.context.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("%s", context);
    }
}