/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.testContext;

import core.SimScenario;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import java.util.List;
import routing.EpidemicRouter;

/**
 *
 * @author L
 */
public class BufferTest extends EpidemicRouter {


    protected BufferTest(BufferTest r) {
        super(r);
    }
    
    public BufferTest(Settings s) {
        super(s);
    }
    
    @Override
     public void update() {
        super.update(); // Jalankan update bawaan EpidemicRouter

        // Cek buffer setiap 5 menit
        if (SimClock.getTime() % 300 == 0) { 
            List<DTNHost> hosts = SimScenario.getInstance().getHosts();
            System.out.println("\n=== Custom Router Checking Buffers at Time: " + SimClock.getTime() + " ===");
            bufferCalculator.neighborBuffers(hosts);
        }
    }
}
