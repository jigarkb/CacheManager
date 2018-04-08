import sys

sys.path.append('..')
import cachemanager


cm = cachemanager.CacheManager()
cm.connect(serverLocator='tcp:host=35.202.174.235,port=8001')
cm.create_table("table1")
table_id = cm.get_table_id("table1")
cm.write(table_id, 'object1', 'Hello from Python!')
value = cm.read(table_id, 'object1')
cm.delete(table_id, 'object1')

i = 1
sample_data_mb = open('sample_data/megabyte.txt', 'r').read()
while True:
    object_id = "object{}".format(i)
    cm.write(table_id, object_id, sample_data_mb)
    print "written {}".format(object_id)
    i += 1
