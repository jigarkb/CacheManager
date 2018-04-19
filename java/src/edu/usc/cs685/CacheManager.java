package edu.usc.cs685;

import edu.stanford.ramcloud.*;
import edu.usc.cs685.models.*;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheManager extends RAMCloud {
    public PriorityQueue<HeapObject> camp_heap;
    public HashMap<Integer, LinkedList> csratio_ll;
    public HashMap<String, Integer> key_csratio;
    public Integer L;
    public Integer max_size;
    public Logger logger = Logger.getLogger(CacheManager.class.getName());


    public CacheManager(String locator, String clusterName) {
        super(locator, clusterName);
        camp_heap = new PriorityQueue<HeapObject>((o1, o2) -> o1.priority - o2.priority);
        csratio_ll = new HashMap<Integer, LinkedList>();
        key_csratio = new HashMap<String, Integer>();
        L = 0;
        max_size = 0;
    }

    public long write(long table_id, String id, String value, Integer cost) {
        long retval;
        Integer data_size = value.length();
        if(max_size < data_size){
            max_size = data_size;
        }
        try{
            remove(table_id, id);
        }catch (ClientException.ObjectExistsException e){
            logger.log(Level.ALL, String.format("CacheManager.write deleting object table_id: %s, id: %s which does not exists!", table_id, id));
        }

        try {
            retval = super.write(table_id, id, value);
        }catch (ClientException.RetryException e){
            logger.log(Level.ALL, "CacheManager.write: No free memory, will delete some data...");
            long removed = 0;
            while(removed <= data_size){
                HeapObject temp = camp_heap.peek();
                LinkedList ll = csratio_ll.get(temp.cs_ratio);
                KeyMetaData camp_root = (KeyMetaData) ll.anchor.next.data;
                removed += remove(camp_root.table_id, camp_root.id);
            }
            logger.log(Level.ALL, String.format("CacheManager.write: data removed size: %s", removed));
            retval = super.write(table_id, id, value);

        }
        logger.log(Level.ALL,"CacheManager.write: successfully cached new data");
        Integer cs_ratio = get_cs_ratio(cost, data_size);
        Integer priority = L + cs_ratio;
        KeyMetaData key_meta_data = new KeyMetaData(table_id, id, cost, data_size, cs_ratio, priority);

        if(csratio_ll.get(cs_ratio) == null){
            logger.log(Level.ALL, String.format("CacheManager.write: new cs_ratio %s seen, creating linkedlist", cs_ratio));
            csratio_ll.put(cs_ratio, new LinkedList());
            csratio_ll.get(cs_ratio).append(key_meta_data);
            logger.log(Level.ALL, String.format("CacheManager.write: heappush (%s, %s)", priority, cs_ratio));
            camp_heap.add(new HeapObject(priority, cs_ratio));
        } else{
            csratio_ll.get(cs_ratio).append(key_meta_data);
        }

        String key_ = String.format("%s/%s", table_id, id);
        logger.log(Level.ALL, String.format("CacheManager.write: associating key %sto cs_ratio %s", key_, cs_ratio));
        key_csratio.put(key_, cs_ratio);
        return retval;
    }

    private Integer get_cs_ratio(Integer cost, Integer size){
        logger.log(Level.ALL, String.format("CacheManager.get_cs_ratio called for cost: %s, size: %s", cost, size));
        Integer integer = cost*max_size/size;

        Integer rounded_value = integer;  // ToDo perform rounding scheme

        logger.log(Level.ALL, String.format("CacheManager.get_cs_ratio: rounded value: %s", rounded_value));
        return rounded_value;
    }

    public long remove(long table_id, String id){
        logger.log(Level.ALL, String.format("CacheManager.delete called with table_id: %s, id: %s", table_id, id));
        super.remove(table_id, id);

        Integer size = 0;
        String key_ = String.format("%s/%s", table_id, id);
        Integer cs_ratio = key_csratio.get(key_);
        if(cs_ratio != null){
            LinkedList ll = csratio_ll.get(cs_ratio);
            if (ll != null && ll.len > 0){
                Node node = ll.anchor.next;
                KeyMetaData key_meta_data = (KeyMetaData) node.data;
                if(key_meta_data.table_id == table_id && key_meta_data.id.equals(id)){
                    logger.log(Level.ALL, "CacheManager.delete: deleting head node");
                    size = key_meta_data.size;

                    HeapObject to_remove = null;
                    for (HeapObject iter : camp_heap) {
                        if (iter.priority.equals(key_meta_data.priority)) {
                            to_remove = iter;
                            break;
                        }
                    }
                    camp_heap.remove(to_remove);
                    ll.unlink(node);
                    key_csratio.remove(key_);
                    if(ll.len > 0){
                        Node next_node = ll.anchor.next;
                        KeyMetaData next_key_meta_data = (KeyMetaData) next_node.data;
                        logger.log(Level.ALL, String.format("CacheManager.delete: heappush (%s, %s)", next_key_meta_data.priority, cs_ratio));
                        camp_heap.add(new HeapObject(next_key_meta_data.priority, cs_ratio));
                        L = camp_heap.peek().priority;
                        logger.log(Level.ALL, String.format("CacheManager.delete: updated L to: %s",L));
                    } else{
                        csratio_ll.remove(cs_ratio);
                    }

                    logger.log(Level.ALL, String.format("CacheManager.delete: deleted %s bytes", size));
                    return size;
                } else{
                    node = node.next;
                    while(node != null){
                        key_meta_data = (KeyMetaData) node.data;
                        if (key_meta_data.table_id == table_id && key_meta_data.id.equals(id)){
                            size = key_meta_data.size;
                            ll.unlink(node);
                            key_csratio.remove(key_);
                            logger.log(Level.ALL, String.format("CacheManager.delete: deleted %s bytes", size));
                            return size;
                        }
                        node = node.next;
                    }
                }
            }
        }

        logger.log(Level.ALL, String.format("CacheManager.delete: deleted %s bytes", size));
        return size;
    }
}
