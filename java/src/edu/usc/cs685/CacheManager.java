package edu.usc.cs685;

import edu.stanford.ramcloud.*;
import edu.usc.cs685.models.HeapObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class CacheManager extends RAMCloud {
    public PriorityQueue<HeapObject> camp_heap;
    public HashMap<Integer, LinkedList> csratio_ll;
    public HashMap<String, Integer> key_csratio;
    public Integer L;
    public Integer max_size;

    public CacheManager(String locator, String clusterName) {
        super(locator, clusterName);
        camp_heap = new PriorityQueue<HeapObject>((o1, o2) -> o1.priority - o2.priority);
        csratio_ll = new HashMap<Integer, LinkedList>();
        key_csratio = new HashMap<String, Integer>();
        L = 0;
        max_size = 0;
    }




}
