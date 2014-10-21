/*
 * this is privacy project.
 * not applied any copyrights yet.
 * - author: admin@devflow.kr
 */

package **INSERT_YOUR_PACKAGE**;


public class RequestQueryData {
    String QueryMethod;
    String QueryID;
    QueryParam[] QueryParams;

    OnQueryResponseCallback callback;

    public RequestQueryData(String qm, String id, QueryParam[] params) {
        QueryMethod = qm;
        QueryID = id;
        QueryParams = params;
    }

    public RequestQueryData(String qm, String id, QueryParam[] params, OnQueryResponseCallback c) {
        QueryMethod = qm;
        QueryID = id;
        QueryParams = params;
        callback = c;
    }

    public boolean e(String QueryID) {
        return this.QueryID.equalsIgnoreCase(QueryID);
    }
}
