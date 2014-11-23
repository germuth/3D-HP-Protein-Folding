package worker;
/**
 * State.java
 *  
 * Enum used by Worker representing the current state it is in
 * 
 * @author Lee Foster
 */
public enum State {
	 START,RECEIVE_CLIENT_INFO,RECEIVE_JOB,SEND_PARAM_REQUEST,RECEIVE_PARAMS,
	  PARSE_PARAMS,FOLD_JOB,RECEIVE_RESULT_REQUEST,SEND_RESULT,DONE
}
