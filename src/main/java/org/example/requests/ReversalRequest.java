package org.example.requests;

import java.io.Serializable;

public class ReversalRequest extends Request implements Serializable {
    private String content;

    public ReversalRequest(String content) {
        this.content = content;
    }

    public ReversalRequest() {

    }

    @Override
    public void execute() {
        content = new StringBuilder(content).reverse().toString();
    }

    @Override
    public void copy(Request request) {
        if (request == null || request.getClass() != this.getClass())
            throw new IllegalArgumentException("");
        this.content = ((ReversalRequest) request).content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ReversalRequest{" +
                "id=" + getId() +
                "; content='" + content + '\'' +
                '}';
    }
}
