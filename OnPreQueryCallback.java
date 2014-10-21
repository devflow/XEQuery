/*
 * this is privacy project.
 * not applied any copyrights yet.
 * - author: admin@devflow.kr
 */

package **INSERT_YOUR_PACKAGE**;

public interface OnPreQueryCallback {
    /**
     *
     * @param queryMethod
     * @param queryId
     * @param queryParams
     * @return accept query. true is accept query then send or queue
     */
    boolean onPreQuery(String queryMethod, String queryId, QueryParam... queryParams);
}
