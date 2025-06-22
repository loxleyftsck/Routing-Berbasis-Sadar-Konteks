/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.*;

/**
 *
 * @author Jarkom
 */
public interface RoutingDecisionEngineImproved {

    void connectionUp(DTNHost thisHost, DTNHost peer);

    void connectionDown(DTNHost thisHost, DTNHost peer);

    void doExchangeForNewConnection(Connection con, DTNHost peer);

    boolean newMessage(Message m);

    boolean isFinalDest(Message m, DTNHost aHost);

    boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost);

    boolean shouldSendMessageToHost(Message m, DTNHost otherHost);

    boolean shouldDeleteSentMessage(Message m, DTNHost otherHost);

    boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld);
    
    void update(DTNHost thisHost);

    RoutingDecisionEngineImproved replicate();
}
