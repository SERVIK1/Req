package org.example.requests;

import java.io.Serializable;

public abstract class Request implements Serializable {

    public void execute() {
    }

    public void onFinish() {
    }

    public void onException() {
    }

    public void onCancel() {
    }

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public abstract void copy(Request request);
}
