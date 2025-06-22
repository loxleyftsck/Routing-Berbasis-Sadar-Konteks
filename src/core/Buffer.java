/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.util.*;

/**
 *
 * @author Pauwerfull
 */
public class Buffer {

    public static final String B_SIZE_S = "buffersize";
    private int buffer;

    public int getBufferSize(DTNHost host) {
        buffer = Integer.MAX_VALUE;

        Settings s = new Settings();

        if (s.contains(B_SIZE_S)) {
            buffer = s.getInt(B_SIZE_S);
        }
        return buffer;
    }

    public Iterator<Message> Iterator() {
        List<Message> messages = new ArrayList<>();
        return messages.iterator();
    }
}
