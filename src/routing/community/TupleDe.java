/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

/**
 *
 * @author Pauwerfull
 */
public class TupleDe<A, B> {

    private A first;
    private final B second;

    public TupleDe(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public A getFirst() {
        return first;
    }

    @Override
    public String toString() {
        return "Tuple [first=" + first + ", second=" + second + "]";
    }

    public B getSecond() {
        return second;
    }

}
