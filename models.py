import ramcloud
import heapq
import linkedlist


class KeyMetaData(object):
    def __init__(self, table_id, id, cost, size, cs_ratio, priority):
        self.table_id = table_id
        self.id = id
        self.cost = cost
        self.size = size
        self.cs_ratio = cs_ratio
        self.priority = priority

    def __str__(self):
        print "table_id: {}\n" \
              "id: {}\n" \
              "cost: {}\n" \
              "size: {}\n" \
              "cs_ratio: {}\n" \
              "priority: {}".format(self.table_id, self.id, self.cost, self.size, self.cs_ratio, self.priority)


class CacheManager(ramcloud.RAMCloud):
    def __init__(self):
        super(CacheManager, self).__init__()
        self.camp_heap = []
        self.key_csratio = {}  # key -> cs_ratio
        self.csratio_ll = {}
        self.L = 0
        self.max_size = 0

    def write(self, table_id, id, data, want_version=None, cost=1):

        try:
            self.delete(table_id, id, want_version)
        except ramcloud.ObjectExistsError:
            pass

        data_size = len(data)
        if self.max_size < data_size:
            self.max_size = data_size

        try:
            super(CacheManager, self).write(table_id, id, data, want_version)
        except ramcloud.RetryExceptionError:
            removed = 0
            while removed > data_size:
                min_priority, cs_ratio = self.camp_heap[0]
                removed += self.delete(
                    self.csratio_ll[cs_ratio].anchor.next.data.table_id,
                    self.csratio_ll[cs_ratio].anchor.next.data.id
                )

            super(CacheManager, self).write(table_id, id, data, want_version)

            cs_ratio = self.get_cs_ratio(cost, data_size)
            priority = self.L + cs_ratio
            key_meta_data = KeyMetaData(
                table_id=table_id,
                id=id,
                cost=cost,
                size=data_size,
                cs_ratio=cs_ratio,
                priority=priority)

            if cs_ratio not in self.csratio_ll:
                self.csratio_ll[cs_ratio] = linkedlist.LinkedList()
                self.csratio_ll[cs_ratio].append(key_meta_data)
                heapq.heappush(self.camp_heap, (priority, cs_ratio))
            else:
                self.csratio_ll[cs_ratio].append(key_meta_data)
            key_ = "{}/{}".format(table_id, id)
            self.key_csratio[key_] = cs_ratio

    def get_cs_ratio(self, cost, size):
        integer = cost*self.max_size/float(size)

        rounded_value = integer  # ToDo perform rounding scheme

        return rounded_value

    def read(self, table_id, id, want_version=None):
        super(CacheManager, self).read(table_id, id, want_version)
        key_ = "{}/{}".format(table_id, id)
        cs_ratio = self.key_csratio.get(key_, None)
        if cs_ratio:
            ll = self.csratio_ll.get(cs_ratio, None)
            if ll and ll.len > 0:
                node = ll.anchor.next
                if node.data.table_id == table_id and node.data.id == id:
                    i = self.camp_heap.index((node.data.priority, cs_ratio))
                    self.camp_heap[i] = self.camp_heap[-1]
                    self.camp_heap.pop()
                    if i < len(self.camp_heap):
                        heapq._siftup(self.camp_heap, i)
                        heapq._siftdown(self.camp_heap, 0, i)
                    node_data = node.data
                    ll.unlink(node)

    def delete(self, table_id, id, want_version=None):
        super(CacheManager, self).delete(table_id, id, want_version)

        size = 0
        key_ = "{}/{}".format(table_id, id)
        cs_ratio = self.key_csratio.get(key_, None)
        if cs_ratio:
            ll = self.csratio_ll.get(cs_ratio, None)
            if ll and ll.len > 0:
                node = ll.anchor.next

                if node.data.table_id == table_id and node.data.id == id:
                    size = node.data.size
                    i = self.camp_heap.index((node.data.priority, cs_ratio))
                    self.camp_heap[i] = self.camp_heap[-1]
                    self.camp_heap.pop()
                    if i < len(self.camp_heap):
                        heapq._siftup(self.camp_heap, i)
                        heapq._siftdown(self.camp_heap, 0, i)
                    ll.unlink(node)
                    heapq.heappush(self.camp_heap, (ll.anchor.next.data.priority, cs_ratio))
                    self.L, cs_ratio = self.camp_heap[0]
                    return size
                else:
                    node = node.next

                while node is not None:
                    if node.data.table_id == table_id and node.data.id == id:
                        size = node.data.size
                        ll.unlink(node)
                        return size

        return size
