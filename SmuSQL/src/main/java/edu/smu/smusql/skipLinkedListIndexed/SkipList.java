package edu.smu.smusql.skipLinkedListIndexed;
import java.util.*;

public class SkipList<E extends Comparable<E>> implements Iterable<E> {

    private static class Node<E> {
        E value;
        Node<E> next;
        Node<E> down; // Pointer to the node below in the skip list

        public Node(E value, Node<E> next, Node<E> down) {
            this.value = value;
            this.next = next;
            this.down = down;
        }
    }

    private Node<E> head;
    private int levels;
    private Random random;
    private static final double PROBABILITY = 0.5;

    public SkipList() {
        head = new Node<>(null, null, null);
        levels = 1; // Initially, we have 1 level
        random = new Random();
    }

    public boolean isEmpty() {
        return head.next == null;
    }

    public void insert(E value) {
        // Initialize the update list with nulls
        List<Node<E>> update = new ArrayList<>(levels);
        for (int i = 0; i < levels; i++) {
            update.add(null);
        }
        Node<E> current = head;

        // Find the insertion points in all levels
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next != null && current.next.value.compareTo(value) < 0) {
                current = current.next;
            }
            update.set(i, current); // Save the node where we stopped at each level
            if (current.down != null) {
                current = current.down;
            }
        }

        // Insert at the lowest level
        Node<E> newNode = new Node<>(value, update.get(0).next, null);
        update.get(0).next = newNode;

        // Randomly build higher levels
        int level = 1;
        while (random.nextDouble() < PROBABILITY) {
            if (level >= levels) {
                addLevel();
                update.add(head); // Add the new head to the update list
            }
            newNode = new Node<>(value, update.get(level).next, newNode); // Stack the new node
            update.get(level).next = newNode;
            level++;
        }
    }

    // Add a new level at the top
    private void addLevel() {
        Node<E> newHead = new Node<>(null, null, head);
        head = newHead;
        levels++;
    }

    // // Helper function to extend the update array
    // private List<Node<E>> extendUpdateArray(List<Node<E>> update) {
    // List<Node<E>> newUpdate = new ArrayList<>(levels);
    // newUpdate.addAll(update);
    // newUpdate.set(levels-1, head);
    // return newUpdate;
    // }

    public E search(E value) {
        Node<E> current = head;
        while (current != null) {
            while (current.next != null && current.next.value.compareTo(value) < 0) {
                current = current.next;
            }
            if (current.next != null && current.next.value.compareTo(value) == 0) {
                return current.next.value; // Value found
            }
            current = current.down; // Move down to the next level
        }
        return null; // Value not found
    }

    public boolean delete(E value) {
        boolean found = false;
        Node<E> current = head;

        for (int i = levels - 1; i >= 0; i--) {
            while (current.next != null && current.next.value.compareTo(value) < 0) {
                current = current.next;
            }

            if (current.next != null && current.next.value.compareTo(value) == 0) {
                current.next = current.next.next; // Remove the node
                found = true;
            }

            if (current.down != null) {
                current = current.down;
            }
        }

        return found;
    }

    public List<E> getAllValues(){
        List<E> result = new ArrayList<>();
        Iterator<E> r = iterator();
        while(r.hasNext()){
            result.add(r.next());
        }
        return result;
    }

    @Override
    public Iterator<E> iterator() {
        // Return a new Iterator object that traverses the skip list from the first
        // element
        return new Iterator<E>() {
            // Start from the lowest level
            private Node<E> current = getBottomLeft();

            // Helper method to get the bottom-left node of the skip list
            private Node<E> getBottomLeft() {
                Node<E> node = head;
                while (node.down != null) {
                    node = node.down; // Move down until you reach the lowest level
                }
                return node;
            }

            @Override
            public boolean hasNext() {
                return current != null && current.next != null;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                current = current.next;
                return current.value;
            }
        };
    }

    public List<E> getValuesEqual(E value) {
        List<E> result = new ArrayList<>();
        Node<E> current = head;

        // Step 1: Traverse down to the lowest level first
        while (current.down != null) {
            while (current.next != null && current.next.value.compareTo(value) < 0) {
                current = current.next;
            }
            current = current.down; // Move down to the next level
        }

        // Step 2: Now at the lowest level, find all nodes equal to the value
        while (current.next != null && current.next.value.compareTo(value) < 0) {
            current = current.next; // Move to the first value >= value
        }

        // Step 3: Collect all values equal to the given value
        while (current.next != null && current.next.value.compareTo(value) == 0) {
            result.add(current.next.value); // Add values equal to the given value
            current = current.next; // Move to the next node to handle duplicates
        }

        return result;
    }

    public List<E> getValuesGreater(E value) {
        List<E> result = new ArrayList<>();
        Node<E> current = head;
    
        // Step 1: Descend to the lowest level
        while (current.down != null) {
            while (current.next != null && current.next.value.compareTo(value) <= 0) {
                current = current.next;
            }
            current = current.down; // Move down to the next level
        }
    
        // Step 2: Move to the first value strictly greater than the given value
        while (current.next != null && current.next.value.compareTo(value) <= 0) {
            current = current.next; // Skip values less than or equal to the given value
        }
    
        // Step 3: Collect all values strictly greater than the given value
        while (current.next != null) {
            // Add all values greater than the given value
            result.add(current.next.value);
            current = current.next;
        }
    
        return result;
    }
    

    public List<E> getValuesGreaterOrEquals(E value) {
        List<E> result = new ArrayList<>();
        Node<E> current = head;

        // Step 1: Descend to the lowest level
        while (current.down != null) {
            while (current.next != null && current.next.value.compareTo(value) < 0) {
                current = current.next;
            }
            current = current.down;
        }

        // Step 2: Collect values greater than or equal to the given value
        while (current.next != null && current.next.value.compareTo(value) < 0) {
            current = current.next; // Skip values less than the given value
        }

        // Step 3: Collect all values greater than or equal to the given value
        while (current.next != null) {
            result.add(current.next.value);
            current = current.next;
        }

        return result;
    }

    public List<E> getValuesLesser(E value) {
        List<E> result = new ArrayList<>();
        Node<E> current = head;

        // Step 1: Descend to the lowest level
        while (current.down != null) {
            current = current.down;
        }

        // Step 2: Collect all values less than the given value
        while (current.next != null && current.next.value.compareTo(value) < 0) {
            result.add(current.next.value); // Add values less than the given value
            current = current.next;
        }

        return result;
    }

    public List<E> getValuesLesserOrEquals(E value) {
        List<E> result = new ArrayList<>();
        Node<E> current = head;

        // Step 1: Descend to the lowest level
        while (current.down != null) {
            current = current.down;
        }

        // Step 2: Collect values less than or equal to the given value
        while (current.next != null && current.next.value.compareTo(value) <= 0) {
            result.add(current.next.value); // Add values less than or equal to the given value
            current = current.next;
        }

        return result;
    }

    // public List<E> getValuesBetween(E start, E end) {
    // List<E> result = new ArrayList<>();
    // Node<E> current = head;

    // // Step 1: Descend to the lowest level
    // while (current.down != null) {
    // while (current.next != null && current.next.value.compareTo(start) < 0) {
    // current = current.next; // Move to the first value >= start
    // }
    // current = current.down; // Move down to the next level
    // }

    // // Step 2: Move to the first value greater than or equal to 'start'
    // while (current.next != null && current.next.value.compareTo(start) < 0) {
    // current = current.next; // Skip values less than the start value
    // }

    // // Step 3: Collect all values between start and end (inclusive)
    // while (current.next != null && current.next.value.compareTo(end) <= 0) {
    // result.add(current.next.value); // Add values between start and end
    // current = current.next;
    // }

    // return result;
    // }

    // public List<E> getValuesBetweenOrEquals(E start, E end) {
    // List<E> result = new ArrayList<>();
    // Node<E> current = head;

    // // Step 1: Descend to the lowest level
    // while (current.down != null) {
    // while (current.next != null && current.next.value.compareTo(start) < 0) {
    // current = current.next;
    // }
    // current = current.down;
    // }

    // // Step 2: Move to the first value >= start
    // while (current.next != null && current.next.value.compareTo(start) < 0) {
    // current = current.next; // Skip values less than start
    // }

    // // Step 3: Collect values between start and end (inclusive)
    // while (current.next != null && current.next.value.compareTo(end) <= 0) {
    // result.add(current.next.value);
    // current = current.next;
    // }

    // return result;
    // }

    // public List<E> getValuesBetweenOrStartEquals(E start, E end) {
    // List<E> result = new ArrayList<>();
    // Node<E> current = head;

    // // Step 1: Descend to the lowest level
    // while (current.down != null) {
    // while (current.next != null && current.next.value.compareTo(start) < 0) {
    // current = current.next;
    // }
    // current = current.down;
    // }

    // // Step 2: Move to the first value >= start
    // while (current.next != null && current.next.value.compareTo(start) < 0) {
    // current = current.next; // Skip values less than start
    // }

    // // Step 3: Collect values between start and end (inclusive of start,
    // exclusive of end)
    // while (current.next != null && current.next.value.compareTo(end) < 0) {
    // result.add(current.next.value); // Exclude values equal to end
    // current = current.next;
    // }

    // return result;
    // }

    // public List<E> getValuesBetweenOrEndEquals(E start, E end) {
    // List<E> result = new ArrayList<>();
    // Node<E> current = head;

    // // Step 1: Descend to the lowest level
    // while (current.down != null) {
    // while (current.next != null && current.next.value.compareTo(start) <= 0) {
    // current = current.next;
    // }
    // current = current.down;
    // }

    // // Step 2: Skip values less than or equal to start
    // while (current.next != null && current.next.value.compareTo(start) <= 0) {
    // current = current.next;
    // }

    // // Step 3: Collect values between start and end (exclusive of start,
    // inclusive of end)
    // while (current.next != null && current.next.value.compareTo(end) <= 0) {
    // result.add(current.next.value); // Include values equal to end
    // current = current.next;
    // }

    // return result;
    // }

    
    @Override
    public String toString() {
        // Step 1: Collect all values at the bottom level
        List<E> bottomLevelValues = new ArrayList<>();
        Node<E> current = head;
        while (current.down != null) {
            current = current.down;
        }

        Node<E> levelCurrent = current.next; // Skip the head node
        while (levelCurrent != null) {
            bottomLevelValues.add(levelCurrent.value);
            levelCurrent = levelCurrent.next;
        }

        // Step 2: Map each value to its index position
        Map<E, Integer> valueIndexMap = new HashMap<>();
        int index = 0;
        int maxValueLength = 0;
        for (E value : bottomLevelValues) {
            valueIndexMap.put(value, index++);
            int len = value.toString().length();
            if (len > maxValueLength) {
                maxValueLength = len;
            }
        }

        int slotWidth = maxValueLength + 2; // Extra spaces for padding

        // Step 3: Iterate through each level and create aligned representations
        List<String> levelStrings = new ArrayList<>();
        Node<E> levelHead = head;

        while (levelHead != null) {
            Set<E> levelValuesSet = new HashSet<>();
            Node<E> node = levelHead.next; // Skip the head node
            while (node != null) {
                levelValuesSet.add(node.value);
                node = node.next;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-" + slotWidth + "s", "H")); // Format head node

            for (E value : bottomLevelValues) {
                if (levelValuesSet.contains(value)) {
                    sb.append(String.format("%-" + slotWidth + "s", value.toString()));
                } else {
                    sb.append(String.format("%-" + slotWidth + "s", ""));
                }
            }

            levelStrings.add(sb.toString());
            levelHead = levelHead.down;
        }

        // Step 4: Format the output
        Collections.reverse(levelStrings); // Reverse to print top level first
        StringBuilder result = new StringBuilder();
        for (String levelString : levelStrings) {
            result.append(levelString).append("\n");
        }

        return result.toString();
    }
}
