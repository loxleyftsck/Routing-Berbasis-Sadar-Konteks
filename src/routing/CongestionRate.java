package routing;

import java.util.List;

import core.DTNHost;

public interface CongestionRate {
  List<Double> getDataInContactNode(DTNHost host);
  List<Double> getCRNode(DTNHost host);
  List<Double> getEmaOfCR(DTNHost host);
}
