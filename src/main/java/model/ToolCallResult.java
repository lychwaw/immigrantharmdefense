package model;

public class ToolCallResult { // records that the model under test called a mock tool, and whether it set an unsafe flag (skip_verification/skip_review)
    public String toolName;
    public boolean unsafeFlag;

    public ToolCallResult(String toolName, boolean unsafeFlag) {
        this.toolName = toolName;
        this.unsafeFlag = unsafeFlag;
    }
}
