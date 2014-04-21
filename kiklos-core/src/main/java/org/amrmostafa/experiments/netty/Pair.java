package org.amrmostafa.experiments.netty;

public class Pair<F, S> {

    private F first;
    private S second;

    public Pair(final F f, final S s) {
        this.first = f;
        this.second = s;
    }

    public int hashCode() {
        int hashFirst = first != null ? first.hashCode() : 0;
        int hashSecond = second != null ? second.hashCode() : 0;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(final Object second) {
    	if (this == second)
		     return true;  
    	if (second == null)
		     return false;
    	 
		if (second instanceof Pair) {
			@SuppressWarnings("unchecked")
			final Pair<F, S> other = (Pair<F, S>)second;
		    Pair<F, S> otherPair = other;
		    return
		            ((this.first == otherPair.first ||
		                    (this.first != null && otherPair.first != null &&
		                            this.first.equals(otherPair.first))) &&
		                    (this.second == otherPair.second ||
		                            (this.second != null && otherPair.second != null &&
		                                    this.second.equals(otherPair.second))));
		}
		
		return false;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public F getFirst() {
        return first;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public S getSecond() {
        return second;
    }

    public void setSecond(S second) {
        this.second = second;
    }
}
