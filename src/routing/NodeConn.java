package routing;

import java.util.*;
import core.*;

public interface NodeConn {
  Set<DTNHost> getNodeConnected(DTNHost host);
}
