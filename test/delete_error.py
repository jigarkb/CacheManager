import ramcloud
import threading
import time


def thread_write(table_id, object_id):
    print "writing object: {} to table: {}".format(object_id, table_id)
    rc.write(table_id, object_id, sample_value)


def thread_delete(table_id, object_id):
    print "deleting object: {} of table: {}".format(object_id, table_id)
    rc.delete(table_id, object_id)


if __name__ == "__main__":
    sample_value = "\0"*1024*1024

    rc = ramcloud.RAMCloud()
    rc.connect(serverLocator='tcp:host=35.202.174.235,port=8001')
    rc.create_table("table1")
    table_id = rc.get_table_id("table1")
    timeout = 5
    i = 0
    while True:
        i += 1
        object_id = "object{}".format(i)
        t = threading.Thread(target=thread_write, args=(table_id, object_id))
        t.start()
        t.join(timeout=5)
        if t.isAlive():
            print "Thread {} timed out!".format(i)
            break
        time.sleep(2)

    print "assuming memory full, trying to deleting object"
    t = threading.Thread(target=thread_delete, args=(table_id, object_id))
    t.start()
    t.join(timeout=5)
    if t.isAlive():
        print "Delete thread timed out! Not able to delete???!!!".format(i)
