### ByteBuffer(java.nio.ByteBuffer)

#### 例子

```java
ByteBuffer byteBuffer = writeBuffer.slice();
byteBuffer.position(lastCommittedPosition);
byteBuffer.limit(writePos);
this.fileChannel.position(lastCommittedPosition);
this.fileChannel.write(byteBuffer);
```

#### 介绍

JDK的IO与NIO有相同的作用，但效率与工作方式大有区别

![1553500468447](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1553500468447.png)

ByteBuffer是I/O操作的数据中转站，继承缓冲区Buffer类，用来临时存储数据，缓冲区直接为通道Channel服务，利用缓冲区传递数据。

```java
public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer>{
	//非空的堆内缓冲？
    final byte[] hb;                  
    final int offset;
    boolean isReadOnly;  
    
    //父类Buffer的属性
    private int mark = -1;//标记 mark(){mark=postion;}
    private int position = 0;//下一个要被读写的位置
    private int limit;
    private int capacity;
    
    //从堆空间中分配一个capacity大小的byte数组作为缓冲区的byte数据存储器
    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException();
        return new HeapByteBuffer(capacity, capacity);
    }
    //使用的是系统内存，不是使用JVM堆栈；因为分配直接缓冲区的系统开销很大，慎用TODO
    public static ByteBuffer allocateDirect(int capacity) {
        return new DirectByteBuffer(capacity);
    }
    //将字节数组包装到堆内缓冲中
    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        try {
            return new HeapByteBuffer(array, offset, length);
        } catch (IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }
    //用于创建一个共享了原始缓冲区子序列的新缓冲区。
    public abstract ByteBuffer slice();
    //用于创建一个与原始缓冲区共享内容的新缓冲区
    public abstract ByteBuffer duplicate();
    //读取当前position的byte，position+1
    public abstract byte get();
    //写入byte，position+1
    public abstract ByteBuffer put(byte b);
    //压缩缓冲区
    public abstract ByteBuffer compact();
    //HeapByteBuffer
    public ByteBuffer compact() {
        System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();//mark=-1
        return this;
    }
    
    //父类Buffer：翻转缓冲区；写完数据需要开始读的时候
    //也就是让flip之后的position到limit这块区域变成之前的0到position这块，
    //翻转就是将一个处于存数据状态的缓冲区变为一个处于准备取数据的状态
    public final Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }
}
```