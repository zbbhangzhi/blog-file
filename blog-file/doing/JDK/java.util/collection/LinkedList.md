LinkedList

https://github.com/Snailclimb/JavaGuide/blob/master/Java%E7%9B%B8%E5%85%B3/%E8%BF%99%E5%87%A0%E9%81%93Java%E9%9B%86%E5%90%88%E6%A1%86%E6%9E%B6%E9%9D%A2%E8%AF%95%E9%A2%98%E5%87%A0%E4%B9%8E%E5%BF%85%E9%97%AE.md

##### 问题

1. 元素插入排序：按照插入的顺序排列
2. 没有用数组来维护内容：没有用数组来维护内容 因为node自身携带上下文信息

##### todo

1. 和ArrayList比较，比较插入/寻找的时间复杂度

1. 因为我知道 linkFirst(o) unlinkFirst(o) removeAt(o) 等这些内部都是通过更改o的pre，next元素，影响其前后元素的内部结构 所以就不再叙述 

2. indexOf(o)  get(o)  get(index)等是通过循环遍历内部节点的前后节点 来找到o或者使用二分法找到o的索引

   - for (Node<E> x = first; x != null; x = x.next) {}

   - ```java
     Node<E> node(int index) {
         // 获取中间位置
         if (index < (size >> 1)) {
             // 从头开始查找
             Node<E> x = first;
             for (int i = 0; i < index; i++)
                 x = x.next;
             return x;
         } else {
             //从尾开始查找
             Node<E> x = last;
             for (int i = size - 1; i > index; i--)
                 x = x.prev;
             return x;
         }
     }
     ```

```java
public class LinkedList<E> extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    transient int size = 0;
    //transient修饰 
    transient Node<E> first;
    transient Node<E> last;
}
```

##### 构造方法

```java
public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

##### 添加

```java
public boolean addAll(Collection<? extends E> c) {
    return addAll(size, c);
}
//从index位置开始 附加集合c到列表中
public boolean addAll(int index, Collection<? extends E> c) {
    checkPositionIndex(index);
    Object[] a = c.toArray();
    int numNew = a.length;
    if (numNew == 0)
        return false;
    //声明前驱节点，后继节点
    Node<E> pred, succ;
    //如果索引等于当前数组的大小 即尾部 前驱节点为last 后继节点为null
    if (index == size) {
        succ = null;
        pred = last;
    } else {
        //反之 得到索引位置的节点为后继节点 前驱节点为后继节点的前继
        succ = node(index);
        pred = succ.prev;
    }

    for (Object o : a) {
        @SuppressWarnings("unchecked") E e = (E) o;
        Node<E> newNode = new Node<>(pred, e, null);
        //如果前驱节点为空 说明数据可能是空的或者todo 首元素置为当前元素 
        if (pred == null)
            first = newNode;
        else
            //反之 前驱节点的下一个元素是当前元素 
            pred.next = newNode;
        pred = newNode;
    }
	//如果从尾部插入 last节点就是最后声明的节点
    if (succ == null) {
        last = pred;
    } else {
        //如果不是从索引位置插入 前后链接插入的链表和原链表
        pred.next = succ;
        succ.prev = pred;
    }

    size += numNew;
    modCount++;
    return true;
}
```

