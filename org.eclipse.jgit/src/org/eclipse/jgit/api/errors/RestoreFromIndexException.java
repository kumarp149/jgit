package org.eclipse.jgit.api.errors;

public class RestoreFromIndexException extends GitAPIException {
    private static final long serialVersionUID = 1L;

    protected RestoreFromIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestoreFromIndexException(String message){
        super(message);
    }
}