/*
 * this is privacy project.
 * not applied any copyrights yet.
 * - author: admin@devflow.kr
 */

package **INSERT_YOUR_PACKAGE**;

import org.json.JSONObject;

public class ResponseQueryData {
    int ErrorCode;
    String ErrorMessage;
    String QueryID;
    JSONObject Data;
    Exception ErrorException;

    OnQueryResponseCallback callback;

    public ResponseQueryData(String QueryID, int errCode, String errMsg, JSONObject data) {
        this.QueryID = QueryID;
        ErrorCode = errCode;
        ErrorMessage = errMsg;
        Data = data;
    }

    public ResponseQueryData(String QueryID, int errCode, String errMsg, JSONObject data, Exception except) {
        this.QueryID = QueryID;
        ErrorCode = errCode;
        ErrorMessage = errMsg;
        Data = data;
        ErrorException = except;
    }

    public ResponseQueryData(String QueryID, int errCode, String errMsg, JSONObject data, OnQueryResponseCallback c) {
        this.QueryID = QueryID;
        ErrorCode = errCode;
        ErrorMessage = errMsg;
        Data = data;
        callback = c;
    }

    public ResponseQueryData(String QueryID, int errCode, String errMsg, JSONObject data, Exception except, OnQueryResponseCallback c) {
        this.QueryID = QueryID;
        ErrorCode = errCode;
        ErrorMessage = errMsg;
        Data = data;
        callback = c;
        ErrorException = except;
    }

    public boolean e(String QueryID) {
        return this.QueryID.equalsIgnoreCase(QueryID);
    }
    public boolean s() { return ErrorCode == 0 && ErrorException == null; }
}
