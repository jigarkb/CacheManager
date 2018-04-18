package edu.usc.cs685.models;

public class KeyMetaData {
    long table_id;
    String id;
    int cost;
    int size;
    int cs_ratio;
    int priority;

    public KeyMetaData(long table_id, String id,int cost, int size, int cs_ratio, int priority) {
        this.table_id = table_id;
        this.id = id;
        this.cost = cost;
        this.size = size;
        this.cs_ratio = cs_ratio;
        this.priority = priority;
    }
}
