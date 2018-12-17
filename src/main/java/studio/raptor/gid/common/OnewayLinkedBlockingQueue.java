package studio.raptor.gid.common;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单向读阻塞队列。
 *
 * <pre>
 *   基于 {@link java.util.concurrent.LinkedBlockingQueue}
 *
 *   <b>Attention:</b> 不支持并发写入的线程安全
 * </pre>
 *
 * @author bruce
 * @since 0.1
 */
public class OnewayLinkedBlockingQueue<E> {

  /**
   * The capacity bound, or Integer.MAX_VALUE if none
   */
  private final int capacity;
  /**
   * Current number of elements
   */
  private final AtomicInteger count = new AtomicInteger(0);
  /**
   * Lock held by take, poll, etc
   */
  private final ReentrantLock takeLock = new ReentrantLock();
  /**
   * Wait queue for waiting takes
   */
  private final Condition notEmpty = takeLock.newCondition();
  /**
   * Head of linked list.
   * Invariant: head.item == null
   */
  private transient OnewayLinkedBlockingQueue.Node<E> head;
  /**
   * Tail of linked list.
   * Invariant: last.next == null
   */
  private transient OnewayLinkedBlockingQueue.Node<E> last;

  /**
   * Creates a {@code OnewayLinkedBlockingQueue} with the given (fixed) capacity.
   *
   * @param capacity the capacity of this queue
   * @throws IllegalArgumentException if {@code capacity} is not greater than zero
   */
  public OnewayLinkedBlockingQueue(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException();
    }
    this.capacity = capacity;
    last = head = new OnewayLinkedBlockingQueue.Node<E>(null);
  }

  /**
   * Signals a waiting take. Called only from put/offer (which do not
   * otherwise ordinarily lock takeLock.)
   */
  private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
  }

  /**
   * 入队列
   * Links node at end of queue.
   *
   * @param node the node
   */
  private void enqueue(OnewayLinkedBlockingQueue.Node<E> node) {
    last = last.next = node;
  }

  /**
   * 出队列
   * Removes a node from head of queue.
   *
   * @return the node
   */
  private E dequeue() {
    OnewayLinkedBlockingQueue.Node<E> h = head;
    OnewayLinkedBlockingQueue.Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    return x;
  }

  /**
   * Returns the number of elements in this queue.
   *
   * @return the number of elements in this queue
   */
  public int size() {
    return count.get();
  }

  /**
   * Returns the number of additional elements that this queue can ideally
   * (in the absence of memory or resource constraints) accept without
   * blocking. This is always equal to the initial capacity of this queue
   * less the current {@code size} of this queue.
   *
   * <p>Note that you <em>cannot</em> always tell if an attempt to insert
   * an element will succeed by inspecting {@code remainingCapacity}
   * because it may be the case that another thread is about to
   * insert or remove an element.
   *
   * @return 队列剩余容量
   */
  public int remainingCapacity() {
    return capacity - count.get();
  }

  /**
   * Inserts the specified element at the tail of this queue, waiting if
   * necessary for space to become available.
   *
   * @param e 元素
   */
  public void put(E e) {
    // Note: convention in all put/take/etc is to preset local var
    // holding count negative to indicate failure unless set.
    int c = -1;
    OnewayLinkedBlockingQueue.Node<E> node = new OnewayLinkedBlockingQueue.Node<>(e);

    final AtomicInteger count = this.count;

    // 队列满 则等待
    while (count.get() == capacity) {
      LockSupport.parkNanos(1000);
    }
    //入队列
    enqueue(node);
    //计数+1
    c = count.getAndIncrement();
    //之前是空的，则通知其他因空而等待的线程
    if (c == 0) {
      signalNotEmpty();
    }
  }

  /**
   * Retrieves and removes the head of this queue, waiting up to the
   * specified wait time if necessary for an element to become available.
   *
   * @param timeout how long to wait before giving up, in units of
   *        <tt>unit</tt>
   * @param unit a <tt>TimeUnit</tt> determining how to interpret the
   *        <tt>timeout</tt> parameter
   * @return the head of this queue, or <tt>null</tt> if the
   *         specified waiting time elapses before an element is available
   * @throws InterruptedException if interrupted while waiting
   */
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E x = null;
    int c = -1;
    long nanos = unit.toNanos(timeout);
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
      // 队列空，等待至超时还没有node则返回null
      while (count.get() == 0) {
        if (nanos <= 0) {
          return null;
        }
        nanos = notEmpty.awaitNanos(nanos);
      }
      // 出队列
      x = dequeue();
      c = count.getAndDecrement();
      if (c > 1) {
        notEmpty.signal();
      }
    } finally {
      takeLock.unlock();
    }
    return x;
  }

  /**
   * Atomically removes all of the elements from this queue.
   * The queue will be empty after this call returns.
   */
  public void clear(){
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      for (Node<E> p, h = head; (p = h.next) != null; h = p) {
        h.next = h;
        p.item = null;
      }
      head = last;
      //20170112 原clear()后没有处理 this.count
      //重置后 缓存池应该为空
      this.count.set( 0 );
    } finally {
      takeLock.unlock();
    }
  }

  static class Node<E> {

    E item;
    OnewayLinkedBlockingQueue.Node<E> next;

    Node(E x) {
      item = x;
    }
  }

}
