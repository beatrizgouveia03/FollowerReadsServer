package panda;

import java.util.Vector;

public class BooksDatabase {
    Vector<String> books = new Vector<String>();
    public void addBook(String book) {
        books.add(book);
    }

    public void deleteBook(String book) {
        books.remove(book);
    }

    public void updateBook(String oldBook, String newBook) {
        int index = books.indexOf(oldBook);
        if (index != -1) {
            books.set(index, newBook);
        }
    }

    public void listBooks() {
        for (String book : books) {
            System.out.println(book);
        }
    }

    public String getBook(int index) {
        if (index >= 0 && index < books.size()) {
            return books.get(index);
        } else {
            return null;
        }
    }
}
