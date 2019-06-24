PriorityQueue

最小堆：优先级权重越小 离顶点越近

##### 案例

1. 实现一个top max n

   ```java
   publish static int[] topN(int[] nums, int l){
       int[] result = new int[l];
       Comparator c = new Comparator(){
           public int comparable(int a, int b){
               return a - b > 0; 
           }
       };
       PriorityQueue pq = new PriorityQueue(l, c);
       for(int n = 0; n < nums.length; n++){
           pq.add(nums[i]);
       }
       for(int n = 0; n < l; n++){
           result[n] = pq.peek();//拿出堆顶元素
       }
       return result;
   }
   public void main(String[] args){
       int[] nums = {10,5,69,2,14,55,63};
       
   }
   ```

##### 问题

1. 添加时向上调整：元素最开始插入的时候是从队尾进入的，所以一直向上比较大小
2. 移除时向下调整：删除时最开始是先将队尾置空，将队尾元素覆盖目标删除位置，然后向下和左右孩子比较

##### todo

1. 以集合/队列等方式初始化：调整树结构时从队尾开始向下调整

##### 属性及构造器

```java
public class PriorityQueue<E> extends AbstractQueue<E> implements java.io.Serializable {
    //表现为一个平衡二叉树：queue[n]为queue[2*n+1]和queue[2*(n+1)]的父节点
    transient Object[] queue;
    private int size = 0;
    //比较器：根据比较器排列元素在队列中的顺序
    private final Comparator<? super E> comparator;
    public PriorityQueue() { this(DEFAULT_INITIAL_CAPACITY, null);}
    public PriorityQueue(int initialCapacity) {this(initialCapacity, null);}
    public PriorityQueue(Comparator<? super E> comparator) {
        this(DEFAULT_INITIAL_CAPACITY, comparator);
    }
    public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        this.queue = new Object[initialCapacity];
        this.comparator = comparator;
    }
}
```

##### 内部类Itr：迭代器

```java
private final class Itr implements Iterator<E> {
    private int cursor = 0;
    //没被访问过的元素 迭代过程中被落下的元素
    private ArrayDeque<E> forgetMeNot = null;
    //最近被访问后的元素索引
    private int lastRet = -1;
    public boolean hasNext() {
        return cursor < size ||  (forgetMeNot != null && !forgetMeNot.isEmpty());
    }
    public E next() {
        //被其他线程修改过 抛出异常
        if (expectedModCount != modCount)
            throw new ConcurrentModificationException();
        if (cursor < size)
            return (E) queue[lastRet = cursor++];
        if (forgetMeNot != null) {
            lastRet = -1;
            //取 没被访问的集合 的第一个元素
            lastRetElt = forgetMeNot.poll();
            if (lastRetElt != null)
                return lastRetElt;
        }
        throw new NoSuchElementException();
    }
    public void remove() {
        if (expectedModCount != modCount)
            throw new ConcurrentModificationException();
        if (lastRet != -1) {
            E moved = PriorityQueue.this.removeAt(lastRet);
            lastRet = -1;
            if (moved == null)
                cursor--;
            else {
                if (forgetMeNot == null)
                    forgetMeNot = new ArrayDeque<>();
                forgetMeNot.add(moved);
            }
        } else if (lastRetElt != null) {
            PriorityQueue.this.removeEq(lastRetElt);
            lastRetElt = null;
        } else {
            throw new IllegalStateException();
        }
        expectedModCount = modCount;
    }
}
```

##### 扩容

```java
private void grow(int minCapacity) {
    int oldCapacity = queue.length;
    // 如果原尺寸小于64 则双倍扩容 反之扩容50%
    int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                     (oldCapacity + 2) :
                                     (oldCapacity >> 1));
    // 保证新尺寸小于Integer.MAX_VALUE 不然内存溢出
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    queue = Arrays.copyOf(queue, newCapacity);
}
```

##### 基本操作

```java
//取堆顶元素
public E peek() {
    return (size == 0) ? null : (E) queue[0];
}
```

##### 添加元素

```java
public boolean add(E e) {
    return offer(e);
}
public boolean offer(E e) {
    //优先队列不允许空值存在
    if (e == null)
        throw new NullPointerException();
    modCount++;
    int i = size;
    if (i >= queue.length)
        grow(i + 1);
    size = i + 1;
    if (i == 0)
        queue[0] = e;
    else
        siftUp(i, e);
    return true;
}
//调整插入 区分是否有自定义的比较器 没有则用对象默认实现的Comparable
//k 默认插入位置 x 插入元素
private void siftUp(int k, E x) {
    if (comparator != null)
        siftUpUsingComparator(k, x);
    else
        siftUpComparable(k, x);
}
private void siftUpComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;//（k-1）/2
        Object e = queue[parent];
        //当目标元素比父节点大时 停止向上比较
        if (key.compareTo((E) e) >= 0)
            break;
        //与父节点互换位置
        queue[k] = e;
        k = parent;
    }
    queue[k] = key;
}

@SuppressWarnings("unchecked")
private void siftUpUsingComparator(int k, E x) {
    while (k > 0) {
        //找到k位置所在的父节点 
        int parent = (k - 1) >>> 1;
        Object e = queue[parent];
        if (comparator.compare(x, (E) e) >= 0)
            break;
        queue[k] = e;
        k = parent;
    }
    queue[k] = x;
}
```

##### 移除特定元素：如果有多个相等的 只删除第一个

```java
public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1)
        return false;
    else {
        removeAt(i);
        return true;
    }
}
private E removeAt(int i) {
    // assert i >= 0 && i < size;
    modCount++;
    int s = --size;
    //如果位置在队尾 直接移除
    if (s == i) // removed last element
        queue[i] = null;
    else {
        //拿出并置空队尾元素
        E moved = (E) queue[s];
        queue[s] = null;
        //将队尾元素先覆盖位置i 然后向下做调整
        siftDown(i, moved);
        if (queue[i] == moved) {
            siftUp(i, moved);
            if (queue[i] != moved)
                return moved;
        }
    }
    return null;
}
private void siftDown(int k, E x) {
    if (comparator != null)
        siftDownUsingComparator(k, x);
    else
        siftDownComparable(k, x);
}

@SuppressWarnings("unchecked")
private void siftDownComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>)x;
    //计算非叶子节点元素的最大位置
    int half = size >>> 1;
    // 如果不是叶子节点则一直循环 loop while a non-leaf
    while (k < half) {
        int child = (k << 1) + 1; // 假设左孩子比右孩子更小
        Object c = queue[child];
        int right = child + 1;
        if (right < size &&
            ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
            c = queue[child = right];
        if (key.compareTo((E) c) <= 0)
            break;
        queue[k] = c;
        k = child;
    }
    queue[k] = key;
}

@SuppressWarnings("unchecked")
private void siftDownUsingComparator(int k, E x) {
    int half = size >>> 1;//计算非叶子节点元素的最大位置
    while (k < half) {
        //得到位置k的左孩子
        int child = (k << 1) + 1;
        Object c = queue[child];
        int right = child + 1;
        //如果左孩子大于右孩子 左孩子换到右孩子位置
        if (right < size &&
            comparator.compare((E) c, (E) queue[right]) > 0)
            c = queue[child = right];
        //如果左/右孩子大于等于目标元素x 跳出循环
        if (comparator.compare(x, (E) c) <= 0)
            break;
        queue[k] = c;
        k = child;
    }
    //将x放入 k是叶子节点
    queue[k] = x;
}
```















