import ramcloud
import threading


def write(table_id, object_id):
    print "writing object: {} to table: {}".format(object_id, table_id)
    rc.write(table_id, object_id, sample_value)


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
        t = threading.Thread(target=write, args=(table_id, object_id))
        t.start()
        t.join(timeout=5)
        if t.isAlive():
            print "assuming thread {} timed out!".format(i)
            break

    print "assuming memory full, deleting object"
    try:
        rc.delete(table_id, "object1")
    except Exception as e:
        print "delete failed with error: {}".format(e.message)
