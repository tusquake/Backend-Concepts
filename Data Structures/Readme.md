# Data Structures in Spring Boot - A Beginner's Real World Guide

## Introduction

This guide explains various data structures used in real-world Spring Boot applications with simple analogies and practical examples. Beyond the common ones you mentioned (sliding window for rate limiting, LRU/LFU caches, heaps, and ConcurrentHashMap), here are other important data structures you'll encounter.

---

## 1. Queue - For Background Tasks & Async Processing

### Real-world Analogy
Think of a queue at a coffee shop. The first person in line gets served first (FIFO - First In, First Out). Everyone waits their turn patiently.

### Spring Boot Usage
```java
@Service
public class OrderService {
    private Queue<Order> orderQueue = new LinkedList<>();
    
    public void placeOrder(Order order) {
        orderQueue.add(order); // Add to back of queue
        System.out.println("Order added to queue");
    }
    
    @Scheduled(fixedDelay = 1000)
    private void processNextOrder() {
        Order order = orderQueue.poll(); // Remove from front
        if (order != null) {
            // Process order
            sendConfirmationEmail(order);
        }
    }
}
```

### Real Use Cases
- **Email sending queue** - Send emails one by one without blocking the main thread
- **Order processing** - Process e-commerce orders sequentially
- **Task scheduling** - Background jobs waiting to be executed
- **Message brokers** - RabbitMQ, Kafka internally use queues

---

## 2. PriorityQueue - For Task Scheduling with Priority

### Real-world Analogy
Emergency room in a hospital. Critical patients (high priority) get treated before people with minor injuries (low priority), even if the minor injury person arrived first.

### Spring Boot Usage
```java
@Service
public class TaskScheduler {
    private PriorityQueue<Task> taskQueue = new PriorityQueue<>(
        Comparator.comparing(Task::getPriority).reversed()
    );
    
    public void scheduleTask(Task task) {
        taskQueue.offer(task);
    }
    
    public void executeNextTask() {
        Task highestPriorityTask = taskQueue.poll();
        if (highestPriorityTask != null) {
            highestPriorityTask.execute();
        }
    }
}
```

### Real Use Cases
- **Job scheduling** - Execute critical jobs before normal ones
- **Alert processing** - Handle critical alerts immediately
- **Request processing** - VIP user requests processed first
- **Resource allocation** - Allocate server resources based on priority

---

## 3. TreeMap - For Sorted Data with Range Queries

### Real-world Analogy
Like a dictionary where words are alphabetically sorted. Want all words between "apple" and "banana"? Easy! Just flip to that section.

### Spring Boot Usage
```java
@Service
public class ProductService {
    private TreeMap<Double, List<Product>> productsByPrice = new TreeMap<>();
    
    public List<Product> getProductsInPriceRange(double min, double max) {
        List<Product> result = new ArrayList<>();
        productsByPrice.subMap(min, true, max, true)
                      .values()
                      .forEach(result::addAll);
        return result;
    }
    
    public Product getCheapestProduct() {
        return productsByPrice.firstEntry().getValue().get(0);
    }
}
```

### Real Use Cases
- **Price range filtering** - Find all products between $10-$50
- **Time-based queries** - Get all logs between 2 PM and 5 PM
- **Date range reports** - Sales report for specific date range
- **Leaderboard systems** - Find users ranked between 10-20

---

## 4. LinkedHashMap - For Maintaining Insertion Order

### Real-world Analogy
Like a playlist that remembers the exact order you added songs. Unlike a regular HashMap (which is like a shuffled deck), LinkedHashMap keeps everything in the order you put them in.

### Spring Boot Usage
```java
@Service
public class SearchHistoryService {
    // Can also use access-order mode for LRU behavior
    private LinkedHashMap<String, LocalDateTime> recentSearches = 
        new LinkedHashMap<>(16, 0.75f, false); // insertion order
    
    private static final int MAX_HISTORY = 10;
    
    public void recordSearch(String query) {
        recentSearches.put(query, LocalDateTime.now());
        
        // Remove oldest if exceeds limit
        if (recentSearches.size() > MAX_HISTORY) {
            String oldest = recentSearches.keySet().iterator().next();
            recentSearches.remove(oldest);
        }
    }
    
    public List<String> getRecentSearches() {
        return new ArrayList<>(recentSearches.keySet());
    }
}
```

### Real Use Cases
- **Browsing history** - Show pages in the order visited
- **Recently viewed items** - E-commerce "recently viewed products"
- **Activity logs** - User activity in chronological order
- **Simple LRU cache** - Using access-order mode

---

## 5. Trie (Prefix Tree) - For Autocomplete & Search

### Real-world Analogy
Like a branching tree of letters. You type "app" and it quickly finds all words starting with "app" - "apple", "application", "appointment" - by following one path.

### Spring Boot Usage
```java
@Service
public class AutocompleteService {
    
    class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
        String word;
    }
    
    private TrieNode root = new TrieNode();
    
    public void addWord(String word) {
        TrieNode current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        current.isEndOfWord = true;
        current.word = word;
    }
    
    public List<String> getSuggestions(String prefix) {
        List<String> suggestions = new ArrayList<>();
        TrieNode current = root;
        
        // Navigate to the prefix
        for (char ch : prefix.toLowerCase().toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return suggestions;
        }
        
        // Find all words with this prefix
        findAllWords(current, suggestions);
        return suggestions;
    }
    
    private void findAllWords(TrieNode node, List<String> words) {
        if (node.isEndOfWord) {
            words.add(node.word);
        }
        for (TrieNode child : node.children.values()) {
            findAllWords(child, words);
        }
    }
}
```

### Real Use Cases
- **Search autocomplete** - Google-style search suggestions
- **Product name search** - E-commerce product suggestions
- **Contact search** - Phone contacts autocomplete
- **Command suggestions** - CLI tools suggesting commands

---

## 6. Graph - For Relationships & Networks

### Real-world Analogy
Social media connections. You have friends, who have friends, who have friends. It's a web of connections, not a straight line.

### Spring Boot Usage
```java
@Service
public class SocialNetworkService {
    // Adjacency list representation
    private Map<String, List<String>> friendGraph = new HashMap<>();
    
    public void addFriendship(String user1, String user2) {
        friendGraph.computeIfAbsent(user1, k -> new ArrayList<>()).add(user2);
        friendGraph.computeIfAbsent(user2, k -> new ArrayList<>()).add(user1);
    }
    
    // BFS to find shortest connection path
    public List<String> findConnectionPath(String from, String to) {
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(from);
        visited.add(from);
        parent.put(from, null);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            if (current.equals(to)) {
                return buildPath(parent, from, to);
            }
            
            for (String friend : friendGraph.getOrDefault(current, new ArrayList<>())) {
                if (!visited.contains(friend)) {
                    visited.add(friend);
                    parent.put(friend, current);
                    queue.offer(friend);
                }
            }
        }
        return Collections.emptyList();
    }
    
    private List<String> buildPath(Map<String, String> parent, String from, String to) {
        LinkedList<String> path = new LinkedList<>();
        String current = to;
        while (current != null) {
            path.addFirst(current);
            current = parent.get(current);
        }
        return path;
    }
}
```

### Real Use Cases
- **Social networks** - Friend recommendations, shortest connection path
- **Recommendation systems** - "People who bought this also bought..."
- **Route planning** - Navigation apps (nodes = locations, edges = roads)
- **Dependency resolution** - Maven/Gradle dependency graphs
- **Organizational hierarchy** - Employee reporting structure

---

## 7. Deque (Double-Ended Queue) - For Undo/Redo Operations

### Real-world Analogy
Like a deck of cards where you can add or remove cards from both the top AND bottom of the deck. Very flexible!

### Spring Boot Usage
```java
@Service
public class TextEditorService {
    private Deque<String> undoStack = new ArrayDeque<>();
    private Deque<String> redoStack = new ArrayDeque<>();
    private String currentText = "";
    
    public void typeText(String newText) {
        undoStack.push(currentText); // Save current state
        currentText = newText;
        redoStack.clear(); // Clear redo history
    }
    
    public String undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(currentText);
            currentText = undoStack.pop();
        }
        return currentText;
    }
    
    public String redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(currentText);
            currentText = redoStack.pop();
        }
        return currentText;
    }
}
```

### Real Use Cases
- **Undo/Redo functionality** - Text editors, drawing apps
- **Browser history** - Back and forward navigation
- **Sliding window problems** - Maximum in sliding window (for monitoring)
- **Task scheduling** - Add tasks to front or back based on priority

---

## 8. Set (HashSet/TreeSet) - For Unique Collections

### Real-world Analogy
A guest list for a party. Each person can only be on the list once. No duplicates allowed!

### Spring Boot Usage
```java
@Service
public class UserActivityService {
    // Track unique active users
    private Set<String> activeUsers = new HashSet<>();
    
    // Track unique IP addresses
    private Set<String> uniqueVisitors = new HashSet<>();
    
    public void userLogin(String username, String ipAddress) {
        activeUsers.add(username);
        uniqueVisitors.add(ipAddress);
    }
    
    public int getActiveUserCount() {
        return activeUsers.size();
    }
    
    public boolean isUserActive(String username) {
        return activeUsers.contains(username);
    }
    
    // TreeSet for sorted unique elements
    private TreeSet<Integer> availableRoomNumbers = new TreeSet<>();
    
    public Integer getLowestAvailableRoom() {
        return availableRoomNumbers.first(); // O(log n)
    }
}
```

### Real Use Cases
- **Duplicate prevention** - Ensure unique emails, usernames
- **Tag systems** - Blog tags, product categories
- **Permission checking** - User roles and permissions
- **Visitor tracking** - Unique IP addresses, user IDs
- **Available resources** - Free parking spots, available rooms

---

## 9. Bloom Filter - For Fast Membership Testing

### Real-world Analogy
Like a bouncer with a photographic memory who can quickly tell you "definitely NOT on the list" or "maybe on the list" (need to double-check). Very fast but occasionally says "maybe" when it should be "no".

### Spring Boot Usage
```java
@Service
public class UsernameService {
    private BloomFilter<String> takenUsernames = 
        BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 
                          1000000, 0.01);
    
    public boolean isUsernamePossiblyTaken(String username) {
        return takenUsernames.mightContain(username);
    }
    
    public void registerUsername(String username) {
        // First check bloom filter
        if (takenUsernames.mightContain(username)) {
            // Double check in database
            if (userRepository.existsByUsername(username)) {
                throw new UsernameAlreadyExistsException();
            }
        }
        
        // Register user
        takenUsernames.put(username);
        userRepository.save(new User(username));
    }
}
```

### Real Use Cases
- **Username availability** - Quick check before hitting database
- **URL shortener** - Check if short URL already exists
- **Spam detection** - Check if email is from known spam domain
- **Cache optimization** - Check if item exists before expensive lookup
- **Malicious URL detection** - Quick check against blacklist

---

## 10. BitSet - For Efficient Boolean Arrays

### Real-world Analogy
Like a row of light switches (on/off). Instead of using a whole room for each switch, you fit thousands of switches in a small space.

### Spring Boot Usage
```java
@Service
public class SeatBookingService {
    // For a theater with 1000 seats
    private BitSet bookedSeats = new BitSet(1000);
    
    public boolean bookSeat(int seatNumber) {
        if (bookedSeats.get(seatNumber)) {
            return false; // Already booked
        }
        bookedSeats.set(seatNumber);
        return true;
    }
    
    public void cancelBooking(int seatNumber) {
        bookedSeats.clear(seatNumber);
    }
    
    public int getAvailableSeatsCount() {
        return 1000 - bookedSeats.cardinality();
    }
    
    public List<Integer> getAvailableSeats() {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            if (!bookedSeats.get(i)) {
                available.add(i);
            }
        }
        return available;
    }
}
```

### Real Use Cases
- **Seat booking systems** - Theater, airplane, event tickets
- **Feature flags** - Enable/disable features per user (memory efficient)
- **Access control** - User permissions bitmap
- **State tracking** - Track completion status of many items
- **Compression** - Efficiently store boolean values

---

## 11. CopyOnWriteArrayList - For Read-Heavy Thread-Safe Lists

### Real-world Analogy
Like a library with a special photocopier. When someone wants to add a new book, they photocopy the entire shelf, add the book to the copy, and replace the old shelf. Readers can keep reading the old shelf until the new one is ready.

### Spring Boot Usage
```java
@Service
public class NotificationService {
    // Thread-safe list for read-heavy operations
    private CopyOnWriteArrayList<NotificationListener> listeners = 
        new CopyOnWriteArrayList<>();
    
    public void addListener(NotificationListener listener) {
        listeners.add(listener); // Creates a copy
    }
    
    public void notifyAll(String message) {
        // Multiple threads can iterate safely
        for (NotificationListener listener : listeners) {
            listener.onNotification(message);
        }
    }
}
```

### Real Use Cases
- **Observer pattern** - Event listeners, notification subscribers
- **Configuration values** - Read frequently, updated rarely
- **Whitelist/Blacklist** - IP addresses, user IDs
- **Cache of frequently accessed data** - Product categories, country list

---

## 12. DelayQueue - For Scheduled/Delayed Tasks

### Real-world Analogy
Like a timer-based reminder system. You set reminders for different times, and they only pop up when their time comes.

### Spring Boot Usage
```java
@Service
public class OrderTimeoutService {
    
    static class DelayedOrder implements Delayed {
        private Order order;
        private long executeAt;
        
        public DelayedOrder(Order order, long delayMinutes) {
            this.order = order;
            this.executeAt = System.currentTimeMillis() + 
                           TimeUnit.MINUTES.toMillis(delayMinutes);
        }
        
        @Override
        public long getDelay(TimeUnit unit) {
            long diff = executeAt - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }
        
        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.executeAt, 
                              ((DelayedOrder)o).executeAt);
        }
    }
    
    private DelayQueue<DelayedOrder> orderTimeouts = new DelayQueue<>();
    
    public void addOrderWithTimeout(Order order, long timeoutMinutes) {
        orderTimeouts.put(new DelayedOrder(order, timeoutMinutes));
    }
    
    @Scheduled(fixedDelay = 1000)
    public void processTimedOutOrders() throws InterruptedException {
        DelayedOrder delayedOrder = orderTimeouts.poll();
        if (delayedOrder != null) {
            // Cancel order if not paid
            cancelOrder(delayedOrder.order);
        }
    }
}
```

### Real Use Cases
- **Order timeout** - Cancel unpaid orders after 15 minutes
- **Session expiry** - Auto logout after inactivity
- **Scheduled notifications** - Send reminder emails at specific times
- **Rate limiting** - Token bucket algorithm implementation
- **TTL cache** - Cache entries that expire after time

---

## Summary Table

| Data Structure | Best For | Time Complexity | Use When |
|---------------|----------|-----------------|----------|
| Queue | FIFO processing | O(1) add/remove | Background tasks, message processing |
| PriorityQueue | Priority-based processing | O(log n) add/remove | Task scheduling, alert handling |
| TreeMap | Sorted data, range queries | O(log n) | Price ranges, time ranges |
| LinkedHashMap | Insertion order, simple LRU | O(1) access | Recent history, ordered cache |
| Trie | Prefix matching | O(m) m=length | Autocomplete, search suggestions |
| Graph | Relationships, networks | Varies | Social networks, recommendations |
| Deque | Both ends access | O(1) both ends | Undo/redo, sliding window |
| Set | Unique elements | O(1) HashSet | Duplicate prevention, tracking |
| Bloom Filter | Fast membership | O(k) k=hash count | Quick existence checks |
| BitSet | Boolean arrays | O(1) per bit | Seat booking, feature flags |
| CopyOnWriteArrayList | Read-heavy | O(1) read, O(n) write | Event listeners, config |
| DelayQueue | Scheduled tasks | O(log n) | Timeouts, scheduled jobs |

---

## Key Takeaways

1. **Choose based on your use case**, not just what's popular
2. **Read vs Write heavy** - Different structures optimize differently
3. **Thread safety** - Consider concurrent access patterns
4. **Memory vs Speed** - Trade-offs exist (Bloom Filter vs HashSet)
5. **Start simple** - Use HashMap/ArrayList, optimize only when needed

---

---

## 13. CircularBuffer (Ring Buffer) - For Fixed-Size Streaming Data

### Real-world Analogy
Like a circular race track with fixed parking spots. When all spots are full, new cars replace the oldest parked cars. Always a fixed number of spots!

### Spring Boot Usage
```java
@Service
public class MetricsService {
    private static final int BUFFER_SIZE = 100;
    private double[] metricsBuffer = new double[BUFFER_SIZE];
    private int writeIndex = 0;
    private int count = 0;
    
    public void recordMetric(double value) {
        metricsBuffer[writeIndex] = value;
        writeIndex = (writeIndex + 1) % BUFFER_SIZE; // Circular wrap
        if (count < BUFFER_SIZE) count++;
    }
    
    public double getAverage() {
        if (count == 0) return 0;
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += metricsBuffer[i];
        }
        return sum / count;
    }
    
    public double[] getLastNMetrics(int n) {
        n = Math.min(n, count);
        double[] result = new double[n];
        int startIndex = (writeIndex - n + BUFFER_SIZE) % BUFFER_SIZE;
        for (int i = 0; i < n; i++) {
            result[i] = metricsBuffer[(startIndex + i) % BUFFER_SIZE];
        }
        return result;
    }
}
```

### Real Use Cases
- **Performance monitoring** - Last 100 response times
- **Logging systems** - Keep last N log entries in memory
- **Real-time data streams** - Stock prices, sensor data
- **Audio/Video buffering** - Media players
- **Network packet buffers** - Fixed-size packet queues

---

## 14. Skip List - For Fast Search in Sorted Data

### Real-world Analogy
Like a multi-level highway system. Express lanes let you skip many exits to get to your destination faster, then you drop down to local roads for the final approach.

### Spring Boot Usage
```java
@Service
public class LeaderboardService {
    private ConcurrentSkipListMap<Integer, String> leaderboard = 
        new ConcurrentSkipListMap<>(Collections.reverseOrder());
    
    public void updateScore(String player, int score) {
        leaderboard.put(score, player);
    }
    
    public List<String> getTopPlayers(int count) {
        return leaderboard.values().stream()
                         .limit(count)
                         .collect(Collectors.toList());
    }
    
    public int getPlayerRank(String player) {
        int rank = 1;
        for (String p : leaderboard.values()) {
            if (p.equals(player)) return rank;
            rank++;
        }
        return -1;
    }
}
```

### Real Use Cases
- **Leaderboards** - Gaming scores, rankings
- **Sorted indexes** - Database-like sorted indexes
- **Concurrent sorted data** - Thread-safe sorted collections
- **Time-series data** - Sorted by timestamp

---

## 15. Segment Tree - For Range Queries on Arrays

### Real-world Analogy
Like a company org chart where each manager knows the total of their team. Want total sales for a department? Ask the department head instead of asking each individual!

### Spring Boot Usage
```java
@Service
public class SalesAnalyticsService {
    private int[] segmentTree;
    private int[] salesData;
    
    public void initializeSalesData(int[] data) {
        this.salesData = data;
        int n = data.length;
        segmentTree = new int[4 * n];
        buildSegmentTree(0, 0, n - 1);
    }
    
    private void buildSegmentTree(int node, int start, int end) {
        if (start == end) {
            segmentTree[node] = salesData[start];
            return;
        }
        int mid = (start + end) / 2;
        buildSegmentTree(2 * node + 1, start, mid);
        buildSegmentTree(2 * node + 2, mid + 1, end);
        segmentTree[node] = segmentTree[2 * node + 1] + 
                           segmentTree[2 * node + 2];
    }
    
    public int getTotalSales(int rangeStart, int rangeEnd) {
        return querySegmentTree(0, 0, salesData.length - 1, 
                               rangeStart, rangeEnd);
    }
    
    private int querySegmentTree(int node, int start, int end, 
                                 int l, int r) {
        if (r < start || l > end) return 0;
        if (l <= start && end <= r) return segmentTree[node];
        
        int mid = (start + end) / 2;
        int leftSum = querySegmentTree(2 * node + 1, start, mid, l, r);
        int rightSum = querySegmentTree(2 * node + 2, mid + 1, end, l, r);
        return leftSum + rightSum;
    }
}
```

### Real Use Cases
- **Analytics dashboards** - Quick sum/min/max over date ranges
- **Inventory management** - Total stock across warehouses
- **Range statistics** - Average, sum, min, max queries
- **Monitoring systems** - Aggregate metrics over time periods

---

## 16. Disjoint Set (Union-Find) - For Grouping & Connectivity

### Real-world Analogy
Like social circles at a party. Initially everyone is alone, but as people meet and become friends, they form groups. You can quickly check if two people are in the same friend circle.

### Spring Boot Usage
```java
@Service
public class NetworkConnectivityService {
    private Map<String, String> parent = new HashMap<>();
    private Map<String, Integer> rank = new HashMap<>();
    
    public void addUser(String userId) {
        parent.put(userId, userId);
        rank.put(userId, 0);
    }
    
    public String find(String userId) {
        if (!parent.get(userId).equals(userId)) {
            // Path compression
            parent.put(userId, find(parent.get(userId)));
        }
        return parent.get(userId);
    }
    
    public void connectUsers(String user1, String user2) {
        String root1 = find(user1);
        String root2 = find(user2);
        
        if (root1.equals(root2)) return;
        
        // Union by rank
        if (rank.get(root1) < rank.get(root2)) {
            parent.put(root1, root2);
        } else if (rank.get(root1) > rank.get(root2)) {
            parent.put(root2, root1);
        } else {
            parent.put(root2, root1);
            rank.put(root1, rank.get(root1) + 1);
        }
    }
    
    public boolean areConnected(String user1, String user2) {
        return find(user1).equals(find(user2));
    }
    
    public int getNumberOfGroups() {
        Set<String> roots = new HashSet<>();
        for (String user : parent.keySet()) {
            roots.add(find(user));
        }
        return roots.size();
    }
}
```

### Real Use Cases
- **Network connectivity** - Check if servers are connected
- **Social networks** - Find connected components
- **Image processing** - Find connected regions
- **Cycle detection** - Detect cycles in graphs
- **Clustering** - Group similar items

---

## 17. Suffix Tree/Array - For String Pattern Matching

### Real-world Analogy
Like an index at the back of a book, but for every possible phrase, not just important words. Want to find all places where "Spring Boot" appears? Check the index instantly!

### Spring Boot Usage
```java
@Service
public class DocumentSearchService {
    private Map<String, List<Integer>> suffixIndex = new HashMap<>();
    
    public void indexDocument(String documentId, String content) {
        // Build suffix index
        for (int i = 0; i < content.length(); i++) {
            for (int j = i + 1; j <= content.length(); j++) {
                String suffix = content.substring(i, j);
                suffixIndex.computeIfAbsent(suffix, k -> new ArrayList<>())
                          .add(i);
            }
        }
    }
    
    public List<Integer> findPattern(String pattern) {
        return suffixIndex.getOrDefault(pattern, new ArrayList<>());
    }
    
    public int countOccurrences(String pattern) {
        return suffixIndex.getOrDefault(pattern, new ArrayList<>()).size();
    }
}
```

### Real Use Cases
- **Full-text search** - Search within documents
- **DNA sequence matching** - Bioinformatics
- **Plagiarism detection** - Find copied content
- **Code search** - Search through codebases
- **Log analysis** - Find patterns in logs

---

## 18. B-Tree / B+ Tree - For Database Indexing

### Real-world Analogy
Like a filing cabinet with multiple drawers, where each drawer has folders, and each folder has sections. You don't need to open every page to find what you need - the organization helps you jump directly to the right section.

### Spring Boot Usage
```java
// Usually handled by databases, but here's a conceptual example
@Service
public class CustomIndexService {
    
    static class BTreeNode {
        List<Integer> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BTreeNode> children = new ArrayList<>();
        boolean isLeaf = true;
        int t; // Minimum degree
        
        BTreeNode(int t) {
            this.t = t;
        }
    }
    
    private BTreeNode root;
    private static final int T = 3; // Minimum degree
    
    public void insert(int key, Object value) {
        if (root == null) {
            root = new BTreeNode(T);
            root.keys.add(key);
            root.values.add(value);
            return;
        }
        // B-Tree insertion logic
    }
    
    public Object search(int key) {
        return searchNode(root, key);
    }
    
    private Object searchNode(BTreeNode node, int key) {
        if (node == null) return null;
        
        int i = 0;
        while (i < node.keys.size() && key > node.keys.get(i)) {
            i++;
        }
        
        if (i < node.keys.size() && key == node.keys.get(i)) {
            return node.values.get(i);
        }
        
        if (node.isLeaf) return null;
        
        return searchNode(node.children.get(i), key);
    }
}
```

### Real Use Cases
- **Database indexes** - MySQL, PostgreSQL use B+ trees
- **File systems** - Directory structures
- **In-memory indexes** - Fast lookups on sorted data
- **Range queries** - Efficiently query ranges of data

---

## 19. Red-Black Tree - Self-Balancing BST

### Real-world Analogy
Like a balanced scale that automatically adjusts weights to stay level. No matter what you add or remove, it keeps itself balanced.

### Spring Boot Usage
```java
// Java's TreeMap and TreeSet use Red-Black trees internally
@Service
public class TimestampedEventService {
    // TreeMap internally uses Red-Black tree
    private TreeMap<Long, Event> events = new TreeMap<>();
    
    public void addEvent(Event event) {
        events.put(event.getTimestamp(), event);
    }
    
    public List<Event> getEventsInTimeRange(long startTime, long endTime) {
        return new ArrayList<>(
            events.subMap(startTime, true, endTime, true).values()
        );
    }
    
    public Event getNextEvent(long afterTimestamp) {
        Map.Entry<Long, Event> entry = events.higherEntry(afterTimestamp);
        return entry != null ? entry.getValue() : null;
    }
    
    public Event getPreviousEvent(long beforeTimestamp) {
        Map.Entry<Long, Event> entry = events.lowerEntry(beforeTimestamp);
        return entry != null ? entry.getValue() : null;
    }
}
```

### Real Use Cases
- **Sorted collections** - Java's TreeMap/TreeSet
- **Event scheduling** - Ordered events with fast insertion
- **Priority systems** - Maintain sorted priorities
- **Database indexing** - Alternative to B-trees

---

## 20. Merkle Tree - For Data Verification

### Real-world Analogy
Like a family tree where each parent node is a "signature" of their children. If any child changes, the parent's signature changes, all the way up to the root. Great for detecting tampering!

### Spring Boot Usage
```java
@Service
public class DataIntegrityService {
    
    static class MerkleNode {
        String hash;
        MerkleNode left;
        MerkleNode right;
        
        MerkleNode(String hash) {
            this.hash = hash;
        }
    }
    
    public MerkleNode buildMerkleTree(List<String> dataBlocks) {
        List<MerkleNode> nodes = dataBlocks.stream()
            .map(data -> new MerkleNode(calculateHash(data)))
            .collect(Collectors.toList());
        
        while (nodes.size() > 1) {
            List<MerkleNode> parents = new ArrayList<>();
            
            for (int i = 0; i < nodes.size(); i += 2) {
                MerkleNode left = nodes.get(i);
                MerkleNode right = (i + 1 < nodes.size()) ? 
                                  nodes.get(i + 1) : left;
                
                String combinedHash = calculateHash(left.hash + right.hash);
                MerkleNode parent = new MerkleNode(combinedHash);
                parent.left = left;
                parent.right = right;
                parents.add(parent);
            }
            nodes = parents;
        }
        return nodes.get(0);
    }
    
    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean verifyDataIntegrity(String rootHash, 
                                      List<String> dataBlocks) {
        MerkleNode tree = buildMerkleTree(dataBlocks);
        return tree.hash.equals(rootHash);
    }
}
```

### Real Use Cases
- **Blockchain** - Bitcoin, Ethereum use Merkle trees
- **Version control** - Git uses Merkle trees
- **Data synchronization** - Detect changes in distributed systems
- **File integrity** - Verify file downloads (torrents)
- **Database replication** - Verify data consistency

---

## 21. Fenwick Tree (Binary Indexed Tree) - For Prefix Sums

### Real-world Analogy
Like a smart calculator that remembers partial sums. Instead of adding 1+2+3+4+5 every time, it remembers (1+2) and (3+4) and (5), so it just adds the remembered chunks.

### Spring Boot Usage
```java
@Service
public class PageViewAnalyticsService {
    private int[] fenwickTree;
    private int size;
    
    public void initializeAnalytics(int numberOfPages) {
        this.size = numberOfPages;
        this.fenwickTree = new int[size + 1];
    }
    
    public void recordPageView(int pageId) {
        updateFenwickTree(pageId, 1);
    }
    
    private void updateFenwickTree(int index, int value) {
        index++; // Fenwick tree is 1-indexed
        while (index <= size) {
            fenwickTree[index] += value;
            index += index & (-index); // Add last set bit
        }
    }
    
    public int getTotalViewsUpToPage(int pageId) {
        int sum = 0;
        pageId++; // 1-indexed
        while (pageId > 0) {
            sum += fenwickTree[pageId];
            pageId -= pageId & (-pageId); // Remove last set bit
        }
        return sum;
    }
    
    public int getViewsInRange(int startPage, int endPage) {
        return getTotalViewsUpToPage(endPage) - 
               getTotalViewsUpToPage(startPage - 1);
    }
}
```

### Real Use Cases
- **Cumulative statistics** - Running totals, prefix sums
- **Range queries** - Sum of elements in a range
- **Frequency counting** - Count occurrences efficiently
- **2D range queries** - 2D Fenwick trees for matrices
- **Dynamic arrays** - Arrays with frequent updates and queries

---

## 22. Inverted Index - For Search Engines

### Real-world Analogy
Like the index at the back of a textbook, but for every single word. Want to find all pages mentioning "Spring"? The inverted index tells you instantly: pages 5, 12, 47, 89.

### Spring Boot Usage
```java
@Service
public class SearchEngineService {
    // Word -> List of (DocumentId, Positions)
    private Map<String, Map<String, List<Integer>>> invertedIndex = 
        new HashMap<>();
    
    public void indexDocument(String docId, String content) {
        String[] words = content.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-z0-9]", "");
            if (word.isEmpty()) continue;
            
            invertedIndex
                .computeIfAbsent(word, k -> new HashMap<>())
                .computeIfAbsent(docId, k -> new ArrayList<>())
                .add(i);
        }
    }
    
    public List<String> search(String query) {
        String[] words = query.toLowerCase().split("\\s+");
        
        if (words.length == 0) return Collections.emptyList();
        
        // Get documents containing first word
        Map<String, List<Integer>> result = 
            invertedIndex.getOrDefault(words[0], new HashMap<>());
        
        // Intersect with documents containing other words
        for (int i = 1; i < words.length; i++) {
            Map<String, List<Integer>> wordDocs = 
                invertedIndex.getOrDefault(words[i], new HashMap<>());
            result.keySet().retainAll(wordDocs.keySet());
        }
        
        return new ArrayList<>(result.keySet());
    }
    
    public List<String> phraseSearch(String phrase) {
        String[] words = phrase.toLowerCase().split("\\s+");
        if (words.length == 0) return Collections.emptyList();
        
        Map<String, List<Integer>> firstWordDocs = 
            invertedIndex.getOrDefault(words[0], new HashMap<>());
        
        List<String> results = new ArrayList<>();
        
        for (String docId : firstWordDocs.keySet()) {
            if (containsPhrase(docId, words)) {
                results.add(docId);
            }
        }
        return results;
    }
    
    private boolean containsPhrase(String docId, String[] words) {
        List<Integer> positions = 
            invertedIndex.get(words[0]).get(docId);
        
        for (int startPos : positions) {
            boolean found = true;
            for (int i = 1; i < words.length; i++) {
                Map<String, List<Integer>> wordDocs = 
                    invertedIndex.get(words[i]);
                if (wordDocs == null || !wordDocs.containsKey(docId)) {
                    found = false;
                    break;
                }
                if (!wordDocs.get(docId).contains(startPos + i)) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }
        return false;
    }
}
```

### Real Use Cases
- **Search engines** - Google, Elasticsearch
- **Document search** - Full-text search in applications
- **Log analysis** - Quick log searching
- **Code search** - IDE search functionality
- **Email search** - Find emails by keywords

---

## Updated Summary Table

| Data Structure | Best For | Time Complexity | Use When |
|---------------|----------|-----------------|----------|
| Queue | FIFO processing | O(1) add/remove | Background tasks, message processing |
| PriorityQueue | Priority-based processing | O(log n) add/remove | Task scheduling, alert handling |
| TreeMap | Sorted data, range queries | O(log n) | Price ranges, time ranges |
| LinkedHashMap | Insertion order, simple LRU | O(1) access | Recent history, ordered cache |
| Trie | Prefix matching | O(m) m=length | Autocomplete, search suggestions |
| Graph | Relationships, networks | Varies | Social networks, recommendations |
| Deque | Both ends access | O(1) both ends | Undo/redo, sliding window |
| Set | Unique elements | O(1) HashSet | Duplicate prevention, tracking |
| Bloom Filter | Fast membership | O(k) k=hash count | Quick existence checks |
| BitSet | Boolean arrays | O(1) per bit | Seat booking, feature flags |
| CopyOnWriteArrayList | Read-heavy | O(1) read, O(n) write | Event listeners, config |
| DelayQueue | Scheduled tasks | O(log n) | Timeouts, scheduled jobs |
| **CircularBuffer** | **Fixed-size streams** | **O(1)** | **Metrics, logging, media buffers** |
| **Skip List** | **Concurrent sorted data** | **O(log n)** | **Leaderboards, concurrent indexes** |
| **Segment Tree** | **Range queries** | **O(log n)** | **Analytics, range statistics** |
| **Union-Find** | **Grouping, connectivity** | **O(α(n)) ≈ O(1)** | **Network connectivity, clustering** |
| **Suffix Tree** | **Pattern matching** | **O(m)** | **Full-text search, DNA matching** |
| **B-Tree** | **Disk-based data** | **O(log n)** | **Database indexes, file systems** |
| **Red-Black Tree** | **Self-balancing BST** | **O(log n)** | **TreeMap/TreeSet internals** |
| **Merkle Tree** | **Data verification** | **O(log n)** | **Blockchain, version control** |
| **Fenwick Tree** | **Prefix sums** | **O(log n)** | **Cumulative stats, range queries** |
| **Inverted Index** | **Search engines** | **O(m)** | **Full-text search, log analysis** |

---

## Advanced Patterns & Combinations

### Pattern 1: Cache + Bloom Filter
```java
// Check Bloom filter before expensive cache lookup
if (bloomFilter.mightContain(key)) {
    return cache.get(key);
}
return null;
```

### Pattern 2: Queue + PriorityQueue
```java
// Fair scheduling: Use queue for FIFO, priority queue for urgent
if (task.isUrgent()) {
    urgentQueue.offer(task);
} else {
    normalQueue.offer(task);
}
```

### Pattern 3: Trie + Inverted Index
```java
// Autocomplete with ranking
List<String> suggestions = trie.getSuggestions(prefix);
suggestions.sort((a, b) -> 
    invertedIndex.getDocumentCount(b) - 
    invertedIndex.getDocumentCount(a)
);
```

---

## Next Steps

1. Try implementing these in a small Spring Boot project
2. Use profiling tools to identify bottlenecks
3. Read about Big O notation to understand performance
4. Study the Java Collections Framework documentation
5. Practice data structure problems on LeetCode/HackerRank
6. Explore open-source projects to see real-world usage
7. Learn about hybrid approaches (combining multiple data structures)

Remember: **The best data structure is the one that solves YOUR specific problem efficiently! Start simple, optimize when needed.**