package panda.tcp;

import java.util.HashMap;
import java.util.Map;

public class BooksDatabase {
    Map<String, Integer> copiesList = new HashMap<String, Integer>();

    public void addNewBook(String book) {
        copiesList.put(book, 1);
    }

    public void deleteBook(String book) {
        copiesList.remove(book);
    }

    public void addNewCopy(String book) {
        if (copiesList.containsKey(book)) {
            int copies = copiesList.get(book);
            copiesList.put(book, copies + 1);
        } else {
            System.out.println("Book not found");
        }
    }

    public void listBooks() {
        for (String book : copiesList.keySet()) {
            System.out.println(book);
        }
    }

    public int getBookCopies(String book) {
        if (copiesList.containsKey(book)) {
            return copiesList.get(book);
        } else {
            return -1;
        }
    }
}
