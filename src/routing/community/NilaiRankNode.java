/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import core.DTNHost;
import java.util.Map;

/**
 *
 * @author Pauwerfull
 */
public interface NilaiRankNode {
    
    Map<DTNHost, Double> getAllRankings();

    int getTotalTeman(DTNHost host);
}
