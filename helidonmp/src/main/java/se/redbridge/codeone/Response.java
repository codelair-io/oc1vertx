package se.redbridge.codeone;

public class Response {
    private long workTime;
    private ResponseCode code;
    private String message;

    public Response() {
    }

    public Response(ResponseCode code, String message, long workTime) {
        this.code = code;
        this.message = message;
        this.workTime = workTime;
    }

    public ResponseCode getCode() {
        return code;
    }

    public void setCode(ResponseCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getWorkTime() {
        return workTime;
    }

    public void setWorkTime(long workTime) {
        this.workTime = workTime;
    }
}
