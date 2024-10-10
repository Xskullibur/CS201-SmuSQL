package edu.smu.smusql;

import java.util.*;

public class SkipList<E extends Comparable<E>> {

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
        // Find the insertion points in all levels
        List<Node<E>> update = new ArrayList<>(levels);
        Node<E> current = head;

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
                update = extendUpdateArray(update);
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

    // Helper function to extend the update array
    private List<Node<E>> extendUpdateArray(List<Node<E>> update) {
        List<Node<E>> newUpdate = new ArrayList<>(levels);
        newUpdate.addAll(update);
        newUpdate.set(levels-1, head);
        return newUpdate;
    }

    public boolean search(E value) {
        Node<E> current = head;
        while (current != null) {
            while (current.next != null && current.next.value.compareTo(value) < 0) {
                current = current.next;
            }
            if (current.next != null && current.next.value.compareTo(value) == 0) {
                return true; // Value found
            }
            current = current.down; // Move down to the next level
        }
        return false; // Value not found
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Node<E> current = head;
        while (current != null) {
            Node<E> rowCurrent = current;
            while (rowCurrent != null) {
                sb.append(rowCurrent.value != null ? rowCurrent.value + " " : "H ");
                rowCurrent = rowCurrent.next;
            }
            sb.append("\n");
            current = current.down;
        }
        return sb.toString();
    }
}
